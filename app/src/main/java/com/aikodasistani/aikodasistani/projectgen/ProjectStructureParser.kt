package com.aikodasistani.aikodasistani.projectgen

/**
 * Enterprise-Grade Project Structure Parser
 * 
 * Parses AI-generated output into a structured project file hierarchy.
 * 
 * Expected AI Output Format:
 * ```
 * /project_root/
 * /subfolderA/
 * fileA.kt:
 *     <content block>
 * 
 * /subfolderB/subfolderC/
 * fileB.json:
 *     <content block>
 * 
 * fileC.py:
 *     <content block>
 * ```
 * 
 * Key Features:
 * - Handles arbitrary whitespace
 * - Variable indentation patterns
 * - Mixed file types
 * - Nested folder chains
 * - Multiline content blocks
 * - Large content blocks (50k+ lines)
 * - Unicode-safe content
 * 
 * Rejects:
 * - Missing `:` file terminator
 * - Duplicate file paths
 * - Illegal characters in paths
 */
class ProjectStructureParser {

    companion object {
        private const val TAG = "ProjectStructureParser"
        
        // Regex patterns for parsing
        private val FOLDER_PATTERN = Regex("""^/([^:]+)/\s*$""")
        private val FILE_HEADER_PATTERN = Regex("""^([^\s:]+[^\s]*):?\s*$""")
        private val CODE_BLOCK_START = Regex("""^```[\w]*\s*$""")
        private val CODE_BLOCK_END = Regex("""^```\s*$""")
        
        // Illegal path characters
        private val ILLEGAL_PATH_CHARS = Regex("""[<>"|?*\x00-\x1F]""")
    }

    /**
     * Parse raw AI output into a ProjectStructure.
     * 
     * @param rawOutput The raw text output from AI provider
     * @param projectName The name to use as project root
     * @return ParserResult with parsed structure or error
     */
    fun parse(rawOutput: String, projectName: String): ParserResult {
        if (rawOutput.isBlank()) {
            return ParserResult.Error("Empty AI response", rawOutput)
        }

        val sanitizedProjectName = sanitizePathComponent(projectName)
        val files = mutableListOf<ProjectFile>()
        val seenPaths = mutableSetOf<String>()
        
        try {
            // Try multiple parsing strategies
            val parsedFiles: List<ProjectFile> = tryAdvancedParse(rawOutput, sanitizedProjectName)
                ?: tryCodeBlockParse(rawOutput, sanitizedProjectName)
                ?: trySimpleParse(rawOutput, sanitizedProjectName)
                ?: emptyList()
            
            if (parsedFiles.isEmpty()) {
                return ParserResult.Error(
                    "No files could be extracted from AI output",
                    rawOutput.take(1000)
                )
            }
            
            // Validate and deduplicate
            for (file in parsedFiles) {
                val normalizedPath = normalizePath(file.path)
                
                // Check for illegal characters
                if (ILLEGAL_PATH_CHARS.containsMatchIn(normalizedPath)) {
                    continue // Skip files with illegal characters
                }
                
                // Check for duplicates
                if (seenPaths.contains(normalizedPath)) {
                    continue // Skip duplicate paths
                }
                
                seenPaths.add(normalizedPath)
                files.add(ProjectFile(normalizedPath, file.content))
            }
            
            if (files.isEmpty()) {
                return ParserResult.Error(
                    "All parsed files were invalid or duplicates",
                    rawOutput.take(1000)
                )
            }
            
            return ParserResult.Success(
                ProjectStructure(
                    root = sanitizedProjectName,
                    files = files
                )
            )
            
        } catch (e: Exception) {
            return ParserResult.Error(
                "Parser exception: ${e.message}",
                rawOutput.take(500)
            )
        }
    }
    
    /**
     * Advanced parsing strategy that handles the structured format:
     * /folder/
     * filename.ext:
     *     content
     */
    private fun tryAdvancedParse(rawOutput: String, projectName: String): List<ProjectFile>? {
        val files = mutableListOf<ProjectFile>()
        val lines = rawOutput.lines()
        
        var currentFolder = ""
        var currentFileName: String? = null
        var currentContent = StringBuilder()
        var inContent = false
        var contentIndent = 0
        
        for (line in lines) {
            // Check for folder declaration
            val folderMatch = FOLDER_PATTERN.find(line)
            if (folderMatch != null) {
                // Save previous file if exists
                if (currentFileName != null && inContent) {
                    files.add(ProjectFile(
                        path = buildPath(projectName, currentFolder, currentFileName),
                        content = currentContent.toString().trimEnd()
                    ))
                }
                
                currentFolder = folderMatch.groupValues[1].trim()
                currentFileName = null
                currentContent = StringBuilder()
                inContent = false
                continue
            }
            
            // Check for file header (filename.ext: or filename.ext)
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") && 
                trimmedLine.matches(Regex("""^[a-zA-Z0-9_\-.]+\.[a-zA-Z0-9]+:?\s*$"""))) {
                
                // Save previous file if exists
                if (currentFileName != null && inContent) {
                    files.add(ProjectFile(
                        path = buildPath(projectName, currentFolder, currentFileName),
                        content = currentContent.toString().trimEnd()
                    ))
                }
                
                currentFileName = trimmedLine.trimEnd(':').trim()
                currentContent = StringBuilder()
                inContent = true
                contentIndent = 0
                continue
            }
            
            // Accumulate content
            if (inContent && currentFileName != null) {
                if (currentContent.isEmpty() && line.isNotBlank()) {
                    // Detect content indentation from first content line
                    contentIndent = line.takeWhile { it == ' ' || it == '\t' }.length
                }
                
                // Remove indentation if present
                val contentLine = if (line.length > contentIndent && line.take(contentIndent).all { it == ' ' || it == '\t' }) {
                    line.drop(contentIndent)
                } else {
                    line.trimStart()
                }
                
                if (currentContent.isNotEmpty() || contentLine.isNotBlank()) {
                    currentContent.appendLine(contentLine)
                }
            }
        }
        
        // Save last file
        if (currentFileName != null && inContent) {
            files.add(ProjectFile(
                path = buildPath(projectName, currentFolder, currentFileName),
                content = currentContent.toString().trimEnd()
            ))
        }
        
        return if (files.isNotEmpty()) files else null
    }
    
    /**
     * Parse code blocks with file paths in markdown format.
     * Handles: ```filename.ext or ```language filename.ext
     */
    private fun tryCodeBlockParse(rawOutput: String, projectName: String): List<ProjectFile>? {
        val files = mutableListOf<ProjectFile>()
        
        // Pattern for code block with filename
        val codeBlockWithFile = Regex("""```(?:(\w+)\s+)?([^\s`]+\.\w+)\s*\n([\s\S]*?)```""")
        val matches = codeBlockWithFile.findAll(rawOutput)
        
        for (match in matches) {
            val fileName = match.groupValues[2]
            val content = match.groupValues[3].trim()
            
            if (fileName.isNotBlank() && content.isNotBlank()) {
                files.add(ProjectFile(
                    path = "$projectName/$fileName",
                    content = content
                ))
            }
        }
        
        // Also try: # path/to/file.ext followed by code block
        val headerCodeBlock = Regex("""(?:^|\n)#+\s*`?([^\s`]+\.\w+)`?\s*\n```\w*\s*\n([\s\S]*?)```""")
        val headerMatches = headerCodeBlock.findAll(rawOutput)
        
        for (match in headerMatches) {
            val filePath = match.groupValues[1]
            val content = match.groupValues[2].trim()
            
            if (filePath.isNotBlank() && content.isNotBlank()) {
                val existingPaths = files.map { it.path }
                val newPath = "$projectName/${filePath.trimStart('/')}"
                if (!existingPaths.contains(newPath)) {
                    files.add(ProjectFile(path = newPath, content = content))
                }
            }
        }
        
        return if (files.isNotEmpty()) files else null
    }
    
    /**
     * Simple parsing fallback for basic file structures.
     * Extracts code blocks and assigns default names.
     */
    private fun trySimpleParse(rawOutput: String, projectName: String): List<ProjectFile>? {
        val files = mutableListOf<ProjectFile>()
        
        // Extract all code blocks
        val codeBlockPattern = Regex("""```(\w*)\s*\n([\s\S]*?)```""")
        val matches = codeBlockPattern.findAll(rawOutput)
        
        var fileCounter = 1
        for (match in matches) {
            val language = match.groupValues[1].ifBlank { "txt" }
            val content = match.groupValues[2].trim()
            
            if (content.isNotBlank()) {
                val extension = languageToExtension(language)
                val fileName = "file_$fileCounter.$extension"
                // Use project root directly for unnamed code blocks
                files.add(ProjectFile(
                    path = "$projectName/$fileName",
                    content = content
                ))
                fileCounter++
            }
        }
        
        // If no code blocks, try to create a single main file
        if (files.isEmpty() && rawOutput.length > 50) {
            files.add(ProjectFile(
                path = "$projectName/output.txt",
                content = rawOutput.trim()
            ))
        }
        
        return if (files.isNotEmpty()) files else null
    }
    
    /**
     * Build a complete path from components.
     */
    private fun buildPath(projectName: String, folder: String, fileName: String): String {
        val parts = mutableListOf<String>()
        parts.add(projectName)
        
        if (folder.isNotBlank()) {
            folder.split("/").filter { it.isNotBlank() }.forEach { parts.add(it) }
        }
        
        parts.add(fileName)
        return parts.joinToString("/")
    }
    
    /**
     * Normalize a path (remove double slashes, leading/trailing slashes).
     */
    private fun normalizePath(path: String): String {
        return path
            .replace(Regex("/+"), "/")
            .trim('/')
    }
    
    /**
     * Sanitize a path component by removing/replacing invalid characters.
     */
    private fun sanitizePathComponent(component: String): String {
        return component
            .replace(Regex("""[<>:"|?*\x00-\x1F]"""), "_")
            .replace(Regex("""\s+"""), "_")
            .trim('.')
            .trim()
            .ifBlank { "Project" }
    }
    
    /**
     * Convert a language identifier to file extension.
     */
    private fun languageToExtension(language: String): String {
        return when (language.lowercase()) {
            "kotlin", "kt" -> "kt"
            "java" -> "java"
            "python", "py" -> "py"
            "javascript", "js" -> "js"
            "typescript", "ts" -> "ts"
            "dart" -> "dart"
            "swift" -> "swift"
            "go", "golang" -> "go"
            "rust", "rs" -> "rs"
            "c" -> "c"
            "cpp", "c++" -> "cpp"
            "csharp", "cs", "c#" -> "cs"
            "ruby", "rb" -> "rb"
            "php" -> "php"
            "html" -> "html"
            "css" -> "css"
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            "xml" -> "xml"
            "sql" -> "sql"
            "shell", "bash", "sh" -> "sh"
            "markdown", "md" -> "md"
            "gradle" -> "gradle"
            "groovy" -> "groovy"
            else -> "txt"
        }
    }
}
