package com.aikodasistani.aikodasistani.projectgenerator.data

import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectFile
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectMetadata
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectStructure

/**
 * Enterprise-grade parser for AI-generated project structure output.
 * 
 * Expected AI output format:
 * ```
 * /project_root/
 * /subfolderA/
 * fileA.kt:
 *     <file content here>
 * 
 * /subfolderB/subfolderC/
 * fileB.json:
 *     <file content here>
 * 
 * fileC.py:
 *     <file content here>
 * ```
 * 
 * Rules:
 * 1. Every file ends with `filename.ext:`
 * 2. Indented lines ARE file content
 * 3. Unlimited nesting allowed
 * 4. Unlimited file count allowed
 * 5. Arbitrary file names and types allowed
 * 6. Content MUST NOT be trimmed or post-processed
 */
object ProjectStructureParser {
    
    private const val TAG = "ProjectStructureParser"
    
    // Logger abstraction for testing
    private var logger: Logger = AndroidLogger()
    
    interface Logger {
        fun d(tag: String, message: String)
        fun v(tag: String, message: String)
        fun e(tag: String, message: String)
        fun w(tag: String, message: String)
    }
    
    /**
     * Android-specific logger implementation.
     * Catches RuntimeException specifically to handle test environments where
     * android.util.Log is not available.
     */
    private class AndroidLogger : Logger {
        override fun d(tag: String, message: String) {
            try { 
                android.util.Log.d(tag, message) 
            } catch (_: RuntimeException) { 
                // Expected in unit test environments where Android Log is not available
            }
        }
        override fun v(tag: String, message: String) {
            try { 
                android.util.Log.v(tag, message) 
            } catch (_: RuntimeException) { 
                // Expected in unit test environments where Android Log is not available
            }
        }
        override fun e(tag: String, message: String) {
            try { 
                android.util.Log.e(tag, message) 
            } catch (_: RuntimeException) { 
                // Expected in unit test environments where Android Log is not available
            }
        }
        override fun w(tag: String, message: String) {
            try { 
                android.util.Log.w(tag, message) 
            } catch (_: RuntimeException) { 
                // Expected in unit test environments where Android Log is not available
            }
        }
    }
    
    // For testing: allow setting a no-op logger
    fun setLogger(customLogger: Logger) {
        logger = customLogger
    }
    
    // Common content delimiters used by AI models
    private val CONTENT_DELIMITERS = listOf(
        "```",
        "---BEGIN FILE---",
        "---END FILE---",
        "<!-- FILE: ",
        "// FILE: ",
        "# FILE: "
    )
    
    // Regex patterns
    private val FOLDER_PATTERN = Regex("^/([^/]+(?:/[^/]+)*)/\\s*$")
    private val FILE_HEADER_PATTERN = Regex("^([\\w\\-./]+\\.[\\w]+):\\s*$")
    private val ALTERNATIVE_FILE_HEADER = Regex("^```\\s*([\\w\\-./]+\\.[\\w]+)\\s*$")
    private val CODE_BLOCK_START = Regex("^```[\\w]*\\s*$")
    private val CODE_BLOCK_END = Regex("^```\\s*$")
    
    /**
     * Result of parsing operation.
     */
    sealed class ParseResult {
        data class Success(val structure: ProjectStructure) : ParseResult()
        data class Error(
            val message: String,
            val lineNumber: Int? = null,
            val details: String? = null
        ) : ParseResult()
    }
    
    /**
     * Parses the raw AI output into a structured ProjectStructure.
     * 
     * @param rawOutput The raw text output from the AI
     * @param projectRoot The name of the project root folder
     * @return ParseResult indicating success or failure with details
     */
    fun parse(rawOutput: String, projectRoot: String = "project"): ParseResult {
        if (rawOutput.isBlank()) {
            return ParseResult.Error("Empty AI response", details = "The AI returned an empty response")
        }
        
        return try {
            val files = parseFiles(rawOutput)
            
            if (files.isEmpty()) {
                return ParseResult.Error(
                    "No files found in AI output",
                    details = "The parser could not extract any files from the AI response. " +
                            "Ensure the AI output follows the expected format with file headers ending in ':'"
                )
            }
            
            // Validate for duplicate paths
            val duplicates = files.groupBy { it.path }.filter { it.value.size > 1 }
            if (duplicates.isNotEmpty()) {
                return ParseResult.Error(
                    "Duplicate file paths detected",
                    details = "Duplicate paths: ${duplicates.keys.joinToString(", ")}"
                )
            }
            
            val totalSize = files.sumOf { it.content.length.toLong() }
            
            val structure = ProjectStructure(
                root = projectRoot,
                files = files,
                metadata = ProjectMetadata(
                    totalFiles = files.size,
                    totalSize = totalSize
                )
            )
            
            logger.d(TAG, "Successfully parsed ${files.size} files, total size: $totalSize bytes")
            ParseResult.Success(structure)
            
        } catch (e: Exception) {
            logger.e(TAG, "Parsing failed: ${e.message}")
            ParseResult.Error(
                "Parsing failed: ${e.message}",
                details = e.stackTraceToString()
            )
        }
    }
    
    /**
     * Parses files from the raw AI output using multiple strategies.
     */
    private fun parseFiles(rawOutput: String): List<ProjectFile> {
        // Try standard format first
        val standardFiles = parseStandardFormat(rawOutput)
        if (standardFiles.isNotEmpty()) {
            return standardFiles
        }
        
        // Try code block format (```filename.ext\n content \n```)
        val codeBlockFiles = parseCodeBlockFormat(rawOutput)
        if (codeBlockFiles.isNotEmpty()) {
            return codeBlockFiles
        }
        
        // Try alternative formats
        return parseAlternativeFormats(rawOutput)
    }
    
    /**
     * Parses the standard format:
     * /folder/
     * filename.ext:
     *     content
     */
    private fun parseStandardFormat(rawOutput: String): List<ProjectFile> {
        val lines = rawOutput.lines()
        val files = mutableListOf<ProjectFile>()
        
        var currentFolder = ""
        var currentFileName: String? = null
        val contentBuilder = StringBuilder()
        var isInContent = false
        var contentIndent: String? = null
        
        for ((lineIndex, line) in lines.withIndex()) {
            // Check for folder marker
            val folderMatch = FOLDER_PATTERN.matchEntire(line)
            if (folderMatch != null) {
                // Save previous file if any
                saveFile(currentFolder, currentFileName, contentBuilder, files)
                currentFileName = null
                isInContent = false
                contentIndent = null
                
                currentFolder = folderMatch.groupValues[1]
                logger.v(TAG, "Found folder: $currentFolder at line $lineIndex")
                continue
            }
            
            // Check for file header
            val fileMatch = FILE_HEADER_PATTERN.matchEntire(line)
            if (fileMatch != null) {
                // Save previous file if any
                saveFile(currentFolder, currentFileName, contentBuilder, files)
                
                currentFileName = fileMatch.groupValues[1]
                isInContent = true
                contentIndent = null
                contentBuilder.clear()
                logger.v(TAG, "Found file: $currentFileName at line $lineIndex")
                continue
            }
            
            // If we're in content mode, accumulate content
            if (isInContent && currentFileName != null) {
                if (line.isBlank() && contentBuilder.isEmpty()) {
                    // Skip leading blank lines
                    continue
                }
                
                // Detect indentation from first non-blank line
                if (contentIndent == null && line.isNotBlank()) {
                    contentIndent = line.takeWhile { it == ' ' || it == '\t' }
                }
                
                // Remove common indentation if present
                val processedLine = if (contentIndent != null && line.startsWith(contentIndent)) {
                    line.removePrefix(contentIndent)
                } else {
                    line
                }
                
                if (contentBuilder.isNotEmpty()) {
                    contentBuilder.append("\n")
                }
                contentBuilder.append(processedLine)
            }
        }
        
        // Save last file
        saveFile(currentFolder, currentFileName, contentBuilder, files)
        
        return files
    }
    
    /**
     * Parses code block format:
     * ```filename.ext
     * content
     * ```
     */
    private fun parseCodeBlockFormat(rawOutput: String): List<ProjectFile> {
        val files = mutableListOf<ProjectFile>()
        val lines = rawOutput.lines()
        
        var currentFolder = ""
        var currentFileName: String? = null
        val contentBuilder = StringBuilder()
        var isInCodeBlock = false
        
        for ((lineIndex, line) in lines.withIndex()) {
            // Check for folder marker
            val folderMatch = FOLDER_PATTERN.matchEntire(line)
            if (folderMatch != null && !isInCodeBlock) {
                currentFolder = folderMatch.groupValues[1]
                continue
            }
            
            // Check for code block with filename: ```filename.ext
            val altFileMatch = ALTERNATIVE_FILE_HEADER.matchEntire(line)
            if (altFileMatch != null && !isInCodeBlock) {
                currentFileName = altFileMatch.groupValues[1]
                isInCodeBlock = true
                contentBuilder.clear()
                continue
            }
            
            // Check for code block end
            if (isInCodeBlock && CODE_BLOCK_END.matches(line)) {
                if (currentFileName != null) {
                    saveFile(currentFolder, currentFileName, contentBuilder, files)
                }
                currentFileName = null
                isInCodeBlock = false
                continue
            }
            
            // Accumulate content in code block
            if (isInCodeBlock) {
                if (contentBuilder.isNotEmpty()) {
                    contentBuilder.append("\n")
                }
                contentBuilder.append(line)
            }
        }
        
        return files
    }
    
    /**
     * Parses alternative formats used by some AI models.
     */
    private fun parseAlternativeFormats(rawOutput: String): List<ProjectFile> {
        val files = mutableListOf<ProjectFile>()
        
        // Try to find patterns like:
        // --- filename.ext ---
        // content
        // --- end ---
        
        // Or:
        // // File: filename.ext
        // content
        
        // Or:
        // **filename.ext**
        // ```
        // content
        // ```
        
        val filePatterns = listOf(
            Regex("---\\s*([\\w\\-./]+\\.[\\w]+)\\s*---"),
            Regex("//\\s*[Ff]ile:\\s*([\\w\\-./]+\\.[\\w]+)"),
            Regex("#\\s*[Ff]ile:\\s*([\\w\\-./]+\\.[\\w]+)"),
            Regex("\\*\\*([\\w\\-./]+\\.[\\w]+)\\*\\*"),
            Regex("<!-- [Ff]ile:\\s*([\\w\\-./]+\\.[\\w]+)\\s*-->")
        )
        
        val lines = rawOutput.lines()
        var currentFileName: String? = null
        val contentBuilder = StringBuilder()
        var isInCodeBlock = false
        
        for (line in lines) {
            // Check for file header patterns
            var foundHeader = false
            for (pattern in filePatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    // Save previous file
                    if (currentFileName != null && contentBuilder.isNotEmpty()) {
                        files.add(ProjectFile(sanitizePath(currentFileName), contentBuilder.toString().trim()))
                    }
                    currentFileName = match.groupValues[1]
                    contentBuilder.clear()
                    isInCodeBlock = false
                    foundHeader = true
                    break
                }
            }
            
            if (foundHeader) continue
            
            // Handle code blocks
            if (CODE_BLOCK_START.matches(line)) {
                isInCodeBlock = true
                continue
            }
            if (CODE_BLOCK_END.matches(line) && isInCodeBlock) {
                isInCodeBlock = false
                continue
            }
            
            // Accumulate content
            if (currentFileName != null && (isInCodeBlock || !line.startsWith("```"))) {
                if (contentBuilder.isNotEmpty()) {
                    contentBuilder.append("\n")
                }
                contentBuilder.append(line)
            }
        }
        
        // Save last file
        if (currentFileName != null && contentBuilder.isNotEmpty()) {
            files.add(ProjectFile(sanitizePath(currentFileName), contentBuilder.toString().trim()))
        }
        
        return files
    }
    
    /**
     * Saves a file if valid.
     */
    private fun saveFile(
        folder: String,
        fileName: String?,
        contentBuilder: StringBuilder,
        files: MutableList<ProjectFile>
    ) {
        if (fileName == null) return
        
        val content = contentBuilder.toString()
        if (content.isBlank()) return
        
        val fullPath = if (folder.isNotEmpty()) {
            "$folder/$fileName"
        } else {
            fileName
        }
        
        val sanitizedPath = sanitizePath(fullPath)
        files.add(ProjectFile(sanitizedPath, content.trimEnd()))
        
        logger.v(TAG, "Saved file: $sanitizedPath (${content.length} chars)")
        contentBuilder.clear()
    }
    
    /**
     * Sanitizes a file path to ensure it's valid and safe.
     */
    private fun sanitizePath(path: String): String {
        // Remove leading slashes
        var sanitized = path.trimStart('/', '\\')
        
        // Replace backslashes with forward slashes
        sanitized = sanitized.replace('\\', '/')
        
        // Remove any path traversal attempts
        sanitized = sanitized.replace("../", "").replace("..\\", "")
        
        // Remove any double slashes
        while (sanitized.contains("//")) {
            sanitized = sanitized.replace("//", "/")
        }
        
        // Remove any invalid characters
        val invalidChars = listOf(':', '*', '?', '"', '<', '>', '|')
        for (char in invalidChars) {
            sanitized = sanitized.replace(char.toString(), "_")
        }
        
        return sanitized
    }
    
    /**
     * Validates that a path doesn't contain illegal characters or patterns.
     */
    fun validatePath(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.contains("..")) return false
        if (path.startsWith("/")) return false
        
        val invalidChars = listOf(':', '*', '?', '"', '<', '>', '|', '\u0000')
        for (char in invalidChars) {
            if (path.contains(char)) return false
        }
        
        return true
    }
}
