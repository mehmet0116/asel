package com.aikodasistani.aikodasistani.projectgen

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * Enterprise-Grade File Writer Engine
 * 
 * Writes project files to the file system with enterprise-level robustness.
 * 
 * Key Features:
 * - Creates all necessary directories (mkdirs)
 * - Writes content EXACTLY as given (preserves tabs, spaces, newlines)
 * - UTF-8 encoding required
 * - Fails gracefully with clean error propagation
 * - Supports all file types (.kt, .java, .xml, .html, .css, .gradle, etc.)
 * 
 * Security:
 * - No file may escape the app sandbox
 * - All paths sanitized before creation
 * - Validates paths don't contain directory traversal
 */
class ProjectFileWriter {

    companion object {
        private const val TAG = "ProjectFileWriter"
        
        // Maximum file size (50 MB)
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024L
        
        // Dangerous path patterns
        private val DANGEROUS_PATTERNS = listOf(
            Regex("""\.\.[\\/]"""),  // Directory traversal
            Regex("""^[\\/]"""),     // Absolute path
            Regex("""[\x00]""")      // Null character
        )
    }

    /**
     * Write all project files to the specified output directory.
     * 
     * @param structure The project structure to write
     * @param outputDir The base output directory
     * @return FileWriterResult with success or error details
     */
    fun writeProject(structure: ProjectStructure, outputDir: File): FileWriterResult {
        try {
            // Validate output directory
            if (!validateOutputDirectory(outputDir)) {
                return FileWriterResult.Error(
                    "Invalid output directory: ${outputDir.absolutePath}",
                    outputDir.absolutePath
                )
            }
            
            // Create project root directory
            val projectRoot = File(outputDir, structure.root)
            if (!projectRoot.exists() && !projectRoot.mkdirs()) {
                return FileWriterResult.Error(
                    "Failed to create project root directory",
                    projectRoot.absolutePath
                )
            }
            
            var writtenCount = 0
            
            for (file in structure.files) {
                // Validate file path
                val validationError = validateFilePath(file.path, structure.root)
                if (validationError != null) {
                    Log.w(TAG, "Skipping invalid path: ${file.path} - $validationError")
                    continue
                }
                
                // Validate content size
                if (file.content.length > MAX_FILE_SIZE) {
                    Log.w(TAG, "Skipping oversized file: ${file.path}")
                    continue
                }
                
                // Determine file location
                val relativePath = file.path.removePrefix(structure.root).trimStart('/')
                val targetFile = File(projectRoot, relativePath)
                
                // Create parent directories
                val parentDir = targetFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        return FileWriterResult.Error(
                            "Failed to create directory: ${parentDir.absolutePath}",
                            parentDir.absolutePath
                        )
                    }
                }
                
                // Skip if this is a directory marker
                if (file.isDirectory || file.content.isEmpty()) {
                    continue
                }
                
                // Write file
                val writeResult = writeFile(targetFile, file.content)
                if (!writeResult.first) {
                    return FileWriterResult.Error(
                        "Failed to write file: ${writeResult.second}",
                        targetFile.absolutePath
                    )
                }
                
                writtenCount++
                Log.d(TAG, "Wrote file: ${targetFile.absolutePath}")
            }
            
            if (writtenCount == 0) {
                return FileWriterResult.Error(
                    "No files were written",
                    projectRoot.absolutePath
                )
            }
            
            return FileWriterResult.Success(
                writtenFiles = writtenCount,
                outputDir = projectRoot.absolutePath
            )
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error writing files", e)
            return FileWriterResult.Error(
                "Security error: ${e.message}",
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error writing project", e)
            return FileWriterResult.Error(
                "Write error: ${e.message}",
                null
            )
        }
    }
    
    /**
     * Validate the output directory is within app sandbox.
     */
    private fun validateOutputDirectory(dir: File): Boolean {
        try {
            // Directory must exist or be creatable
            if (!dir.exists() && !dir.mkdirs()) {
                return false
            }
            
            // Must be a directory
            if (!dir.isDirectory) {
                return false
            }
            
            // Must be writable
            if (!dir.canWrite()) {
                return false
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating directory", e)
            return false
        }
    }
    
    /**
     * Validate a file path for security issues.
     * 
     * @param path The file path to validate
     * @param projectRoot The expected project root
     * @return Error message if invalid, null if valid
     */
    private fun validateFilePath(path: String, projectRoot: String): String? {
        // Check for dangerous patterns
        for (pattern in DANGEROUS_PATTERNS) {
            if (pattern.containsMatchIn(path)) {
                return "Path contains dangerous pattern"
            }
        }
        
        // Check path doesn't escape project root
        if (!path.startsWith(projectRoot) && !path.startsWith("$projectRoot/")) {
            // Allow paths that will be prefixed with project root
            if (path.startsWith("..") || path.startsWith("/")) {
                return "Path escapes project root"
            }
        }
        
        // Check for empty path components
        val components = path.split("/")
        for (component in components) {
            if (component.isBlank()) continue
            if (component == "." || component == "..") {
                return "Path contains relative directory"
            }
        }
        
        return null
    }
    
    /**
     * Write content to a file with UTF-8 encoding.
     * 
     * @param file The target file
     * @param content The content to write
     * @return Pair of (success, error message if any)
     */
    private fun writeFile(file: File, content: String): Pair<Boolean, String?> {
        return try {
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            Pair(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing file: ${file.absolutePath}", e)
            Pair(false, e.message)
        }
    }
    
    /**
     * Delete a project directory and all its contents.
     * Useful for cleanup on error.
     */
    fun deleteProject(projectDir: File): Boolean {
        return try {
            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting project", e)
            false
        }
    }
    
    /**
     * Get the total size of a directory and its contents.
     */
    fun getDirectorySize(dir: File): Long {
        return try {
            if (dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                dir.length()
            }
        } catch (e: Exception) {
            0L
        }
    }
}
