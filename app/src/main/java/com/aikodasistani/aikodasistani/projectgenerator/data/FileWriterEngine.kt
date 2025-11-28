package com.aikodasistani.aikodasistani.projectgenerator.data

import android.content.Context
import android.util.Log
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectFile
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectStructure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * File Writer Engine for project generation.
 * 
 * Responsibilities:
 * - Create every directory (mkdirs)
 * - Create every file
 * - Write content EXACTLY as given
 * - Preserve tabs, spaces, newlines
 * - UTF-8 output required
 * - Fail gracefully (log + propagate clean error object)
 * 
 * Supports all file types including:
 * .kt, .java, .xml, .html, .css, .gradle, .kts, .js, .ts, .json, .yaml, .yml,
 * .dart, .py, .md, .txt, env files, config files, assets, any extension
 */
object FileWriterEngine {
    
    private const val TAG = "FileWriterEngine"
    
    /**
     * Result of a file write operation.
     */
    sealed class WriteResult {
        data class Success(
            val outputDirectory: File,
            val filesWritten: Int,
            val totalBytesWritten: Long
        ) : WriteResult()
        
        data class Error(
            val message: String,
            val failedFile: String? = null,
            val cause: Throwable? = null
        ) : WriteResult()
    }
    
    /**
     * Callback for progress updates during file writing.
     */
    fun interface ProgressCallback {
        fun onProgress(current: Int, total: Int, currentFile: String)
    }
    
    /**
     * Writes a project structure to the file system.
     * 
     * @param context Android context for accessing app-specific directories
     * @param structure The project structure to write
     * @param progressCallback Optional callback for progress updates
     * @return WriteResult indicating success or failure
     */
    suspend fun writeProject(
        context: Context,
        structure: ProjectStructure,
        progressCallback: ProgressCallback? = null
    ): WriteResult = withContext(Dispatchers.IO) {
        try {
            // Get app-specific output directory (sandbox-safe)
            val baseDir = getProjectOutputDirectory(context)
            val projectDir = File(baseDir, sanitizeDirectoryName(structure.root))
            
            Log.d(TAG, "Writing project to: ${projectDir.absolutePath}")
            
            // Clean up existing directory if it exists
            if (projectDir.exists()) {
                if (!deleteRecursively(projectDir)) {
                    Log.w(TAG, "Could not fully delete existing project directory")
                }
            }
            
            // Create project root directory
            if (!projectDir.mkdirs()) {
                if (!projectDir.exists()) {
                    return@withContext WriteResult.Error(
                        "Failed to create project directory: ${projectDir.absolutePath}"
                    )
                }
            }
            
            var totalBytesWritten = 0L
            val totalFiles = structure.files.size
            
            for ((index, file) in structure.files.withIndex()) {
                try {
                    // Validate and sanitize path
                    val sanitizedPath = sanitizePath(file.path)
                    if (sanitizedPath.isEmpty()) {
                        Log.w(TAG, "Skipping file with invalid path: ${file.path}")
                        continue
                    }
                    
                    val outputFile = File(projectDir, sanitizedPath)
                    
                    // Security check: ensure file is within project directory
                    if (!outputFile.canonicalPath.startsWith(projectDir.canonicalPath)) {
                        Log.e(TAG, "Path traversal attempt detected: ${file.path}")
                        return@withContext WriteResult.Error(
                            "Security error: Invalid file path detected",
                            failedFile = file.path
                        )
                    }
                    
                    // Create parent directories
                    val parentDir = outputFile.parentFile
                    if (parentDir != null && !parentDir.exists()) {
                        if (!parentDir.mkdirs()) {
                            return@withContext WriteResult.Error(
                                "Failed to create directory: ${parentDir.absolutePath}",
                                failedFile = file.path
                            )
                        }
                    }
                    
                    // Write file content with UTF-8 encoding
                    val bytesWritten = writeFileContent(outputFile, file.content)
                    totalBytesWritten += bytesWritten
                    
                    Log.v(TAG, "Wrote file: ${file.path} ($bytesWritten bytes)")
                    
                    // Report progress
                    progressCallback?.onProgress(index + 1, totalFiles, file.path)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write file: ${file.path}", e)
                    return@withContext WriteResult.Error(
                        "Failed to write file: ${e.message}",
                        failedFile = file.path,
                        cause = e
                    )
                }
            }
            
            Log.d(TAG, "Successfully wrote $totalFiles files ($totalBytesWritten bytes)")
            WriteResult.Success(
                outputDirectory = projectDir,
                filesWritten = totalFiles,
                totalBytesWritten = totalBytesWritten
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Project write failed", e)
            WriteResult.Error(
                "Project write failed: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * Writes a single file to the project directory.
     */
    suspend fun writeFile(
        context: Context,
        projectName: String,
        file: ProjectFile
    ): WriteResult = withContext(Dispatchers.IO) {
        try {
            val baseDir = getProjectOutputDirectory(context)
            val projectDir = File(baseDir, sanitizeDirectoryName(projectName))
            
            if (!projectDir.exists() && !projectDir.mkdirs()) {
                return@withContext WriteResult.Error("Failed to create project directory")
            }
            
            val sanitizedPath = sanitizePath(file.path)
            val outputFile = File(projectDir, sanitizedPath)
            
            // Security check
            if (!outputFile.canonicalPath.startsWith(projectDir.canonicalPath)) {
                return@withContext WriteResult.Error("Security error: Invalid file path")
            }
            
            // Create directories and write
            outputFile.parentFile?.mkdirs()
            val bytesWritten = writeFileContent(outputFile, file.content)
            
            WriteResult.Success(
                outputDirectory = projectDir,
                filesWritten = 1,
                totalBytesWritten = bytesWritten
            )
        } catch (e: Exception) {
            WriteResult.Error("Failed to write file: ${e.message}", cause = e)
        }
    }
    
    /**
     * Gets the output directory for generated projects.
     * Uses app-specific external storage to ensure sandbox compliance.
     */
    fun getProjectOutputDirectory(context: Context): File {
        // Prefer external files directory, fallback to internal
        val externalDir = context.getExternalFilesDir("GeneratedProjects")
        if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())) {
            return externalDir
        }
        
        val internalDir = File(context.filesDir, "GeneratedProjects")
        internalDir.mkdirs()
        return internalDir
    }
    
    /**
     * Writes content to a file with UTF-8 encoding, preserving all whitespace.
     */
    private fun writeFileContent(file: File, content: String): Long {
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(content)
                writer.flush()
            }
        }
        return file.length()
    }
    
    /**
     * Recursively deletes a directory and all its contents.
     */
    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                if (!deleteRecursively(child)) {
                    return false
                }
            }
        }
        return file.delete()
    }
    
    /**
     * Sanitizes a directory name to ensure it's valid.
     */
    private fun sanitizeDirectoryName(name: String): String {
        // Remove invalid characters and limit length
        val sanitized = name
            .replace(Regex("[^a-zA-Z0-9_\\-.]"), "_")
            .take(100)
        
        return if (sanitized.isEmpty()) "project" else sanitized
    }
    
    /**
     * Sanitizes a file path to ensure it's valid and safe.
     */
    private fun sanitizePath(path: String): String {
        // Remove leading slashes
        var sanitized = path.trimStart('/', '\\')
        
        // Replace backslashes with forward slashes
        sanitized = sanitized.replace('\\', '/')
        
        // Remove path traversal attempts
        val parts = sanitized.split("/").filter { it != ".." && it != "." && it.isNotEmpty() }
        sanitized = parts.joinToString("/")
        
        // Remove double slashes
        while (sanitized.contains("//")) {
            sanitized = sanitized.replace("//", "/")
        }
        
        // Remove invalid characters for filenames (keeping path separators)
        val invalidChars = listOf(':', '*', '?', '"', '<', '>', '|', '\u0000')
        for (char in invalidChars) {
            sanitized = sanitized.replace(char.toString(), "_")
        }
        
        return sanitized
    }
    
    /**
     * Checks if there's enough storage space for the project.
     */
    fun hasEnoughSpace(context: Context, requiredBytes: Long): Boolean {
        val dir = getProjectOutputDirectory(context)
        return dir.usableSpace > requiredBytes + (1024 * 1024) // Add 1MB buffer
    }
    
    /**
     * Gets the size of a project directory.
     */
    fun getProjectSize(projectDir: File): Long {
        if (!projectDir.exists()) return 0
        
        var size = 0L
        projectDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
    
    /**
     * Lists all generated projects.
     */
    fun listGeneratedProjects(context: Context): List<File> {
        val dir = getProjectOutputDirectory(context)
        return dir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
