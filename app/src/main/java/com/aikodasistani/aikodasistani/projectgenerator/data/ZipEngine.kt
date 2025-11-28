package com.aikodasistani.aikodasistani.projectgenerator.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ZIP Engine for project archiving.
 * 
 * Requirements:
 * - Recursively traverse root project directory
 * - Maintain exact folder-tree hierarchy
 * - Maintain timestamps
 * - Maintain file permissions where applicable
 * - Produce compression-efficient ZIP file
 * - Emit final path as URI
 */
object ZipEngine {
    
    private const val TAG = "ZipEngine"
    private const val BUFFER_SIZE = 8192
    
    /**
     * Result of a ZIP operation.
     */
    sealed class ZipResult {
        data class Success(
            val zipFile: File,
            val uri: Uri,
            val filesZipped: Int,
            val originalSize: Long,
            val compressedSize: Long
        ) : ZipResult() {
            val compressionRatio: Float
                get() = if (originalSize > 0) {
                    1f - (compressedSize.toFloat() / originalSize.toFloat())
                } else 0f
        }
        
        data class Error(
            val message: String,
            val cause: Throwable? = null
        ) : ZipResult()
    }
    
    /**
     * Callback for ZIP progress updates.
     */
    fun interface ProgressCallback {
        fun onProgress(current: Int, total: Int, currentFile: String)
    }
    
    /**
     * Creates a ZIP archive from a project directory.
     * 
     * @param context Android context for FileProvider access
     * @param projectDir The directory to ZIP
     * @param outputName Optional name for the ZIP file (without extension)
     * @param progressCallback Optional callback for progress updates
     * @return ZipResult indicating success or failure
     */
    suspend fun createZip(
        context: Context,
        projectDir: File,
        outputName: String? = null,
        progressCallback: ProgressCallback? = null
    ): ZipResult = withContext(Dispatchers.IO) {
        try {
            if (!projectDir.exists()) {
                return@withContext ZipResult.Error("Project directory does not exist: ${projectDir.absolutePath}")
            }
            
            if (!projectDir.isDirectory) {
                return@withContext ZipResult.Error("Path is not a directory: ${projectDir.absolutePath}")
            }
            
            // Collect all files first for progress tracking
            val allFiles = projectDir.walkTopDown()
                .filter { it.isFile }
                .toList()
            
            if (allFiles.isEmpty()) {
                return@withContext ZipResult.Error("No files to ZIP in directory")
            }
            
            val totalFiles = allFiles.size
            Log.d(TAG, "Creating ZIP with $totalFiles files from: ${projectDir.absolutePath}")
            
            // Calculate original size
            val originalSize = allFiles.sumOf { it.length() }
            
            // Create ZIP file in the output directory
            val zipFileName = "${outputName ?: projectDir.name}_${System.currentTimeMillis()}.zip"
            val outputDir = getZipOutputDirectory(context)
            val zipFile = File(outputDir, zipFileName)
            
            // Create ZIP with compression
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                // Set compression level for efficiency
                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION)
                
                val basePathLength = projectDir.absolutePath.length + 1
                val projectName = projectDir.name
                
                allFiles.forEachIndexed { index, file ->
                    try {
                        // Calculate relative path within ZIP, including project root folder
                        val relativePath = file.absolutePath.substring(basePathLength)
                        val zipEntryPath = "$projectName/$relativePath"
                        
                        // Create ZIP entry with metadata
                        val entry = ZipEntry(zipEntryPath).apply {
                            time = file.lastModified()
                            // Note: File permissions can't be fully preserved on all platforms
                        }
                        
                        zipOut.putNextEntry(entry)
                        
                        // Write file content with buffering
                        BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                zipOut.write(buffer, 0, bytesRead)
                            }
                        }
                        
                        zipOut.closeEntry()
                        
                        // Report progress
                        progressCallback?.onProgress(index + 1, totalFiles, relativePath)
                        
                        Log.v(TAG, "Zipped: $zipEntryPath")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error zipping file: ${file.absolutePath}", e)
                        throw e
                    }
                }
            }
            
            val compressedSize = zipFile.length()
            
            // Get content URI via FileProvider for sharing
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FileProvider URI", e)
                Uri.fromFile(zipFile)
            }
            
            Log.d(TAG, "ZIP created: ${zipFile.absolutePath}")
            Log.d(TAG, "Original: $originalSize bytes, Compressed: $compressedSize bytes")
            
            ZipResult.Success(
                zipFile = zipFile,
                uri = uri,
                filesZipped = totalFiles,
                originalSize = originalSize,
                compressedSize = compressedSize
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "ZIP creation failed", e)
            ZipResult.Error(
                message = "ZIP creation failed: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * Creates a ZIP from a list of ProjectFiles without first writing to disk.
     * More memory-efficient for smaller projects.
     * 
     * @param context Android context for FileProvider access
     * @param projectName Name of the project (root folder in ZIP)
     * @param files List of files to include in the ZIP
     * @param progressCallback Optional callback for progress updates
     * @return ZipResult indicating success or failure
     */
    suspend fun createZipFromFiles(
        context: Context,
        projectName: String,
        files: List<com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectFile>,
        progressCallback: ProgressCallback? = null
    ): ZipResult = withContext(Dispatchers.IO) {
        try {
            if (files.isEmpty()) {
                return@withContext ZipResult.Error("No files to ZIP")
            }
            
            val sanitizedName = sanitizeName(projectName)
            val zipFileName = "${sanitizedName}_${System.currentTimeMillis()}.zip"
            val outputDir = getZipOutputDirectory(context)
            val zipFile = File(outputDir, zipFileName)
            
            var originalSize = 0L
            
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION)
                
                files.forEachIndexed { index, file ->
                    val contentBytes = file.content.toByteArray(Charsets.UTF_8)
                    originalSize += contentBytes.size
                    
                    val zipEntryPath = "$sanitizedName/${file.path}"
                    val entry = ZipEntry(zipEntryPath).apply {
                        time = System.currentTimeMillis()
                    }
                    
                    zipOut.putNextEntry(entry)
                    zipOut.write(contentBytes)
                    zipOut.closeEntry()
                    
                    progressCallback?.onProgress(index + 1, files.size, file.path)
                }
            }
            
            val compressedSize = zipFile.length()
            
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
            } catch (e: Exception) {
                Uri.fromFile(zipFile)
            }
            
            ZipResult.Success(
                zipFile = zipFile,
                uri = uri,
                filesZipped = files.size,
                originalSize = originalSize,
                compressedSize = compressedSize
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "ZIP from files failed", e)
            ZipResult.Error(
                message = "ZIP creation failed: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * Gets the output directory for ZIP files.
     * Uses app-specific storage to ensure sandbox compliance.
     */
    fun getZipOutputDirectory(context: Context): File {
        val externalDir = context.getExternalFilesDir("GeneratedZips")
        if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())) {
            return externalDir
        }
        
        val internalDir = File(context.filesDir, "GeneratedZips")
        internalDir.mkdirs()
        return internalDir
    }
    
    /**
     * Lists all generated ZIP files.
     */
    fun listGeneratedZips(context: Context): List<File> {
        val dir = getZipOutputDirectory(context)
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
    
    /**
     * Deletes old ZIP files to manage storage.
     * 
     * @param context Android context
     * @param keepCount Number of recent ZIPs to keep
     * @return Number of files deleted
     */
    fun cleanOldZips(context: Context, keepCount: Int = 10): Int {
        val zips = listGeneratedZips(context)
        if (zips.size <= keepCount) return 0
        
        var deleted = 0
        zips.drop(keepCount).forEach { file ->
            if (file.delete()) deleted++
        }
        
        Log.d(TAG, "Cleaned up $deleted old ZIP files")
        return deleted
    }
    
    /**
     * Gets total storage used by generated ZIPs.
     */
    fun getTotalZipStorage(context: Context): Long {
        return listGeneratedZips(context).sumOf { it.length() }
    }
    
    /**
     * Sanitizes a project name for use in filenames.
     */
    private fun sanitizeName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(50)
            .ifEmpty { "project" }
    }
    
    /**
     * Validates a ZIP file is readable.
     */
    fun validateZip(zipFile: File): Boolean {
        return try {
            java.util.zip.ZipFile(zipFile).use { zip ->
                zip.entries().hasMoreElements()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ZIP validation failed", e)
            false
        }
    }
}
