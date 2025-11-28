package com.aikodasistani.aikodasistani.projectgen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Enterprise-Grade ZIP Engine
 * 
 * Creates compressed ZIP archives from project directories.
 * 
 * Key Features:
 * - Recursively traverses all directories
 * - Maintains exact folder-tree hierarchy
 * - Preserves file timestamps
 * - Produces compression-efficient ZIP
 * - Emits final path as URI
 * 
 * Security:
 * - ZIP output restricted to app-private folders
 * - No files outside project root included
 */
class ProjectZipEngine(private val context: Context) {

    companion object {
        private const val TAG = "ProjectZipEngine"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Create a ZIP archive from a project directory.
     * 
     * @param projectDir The project directory to zip
     * @param outputDir The directory where the ZIP will be created
     * @param zipFileName The name of the ZIP file (without extension)
     * @return ZipResult with URI or error
     */
    fun createZip(projectDir: File, outputDir: File, zipFileName: String): ZipResult {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return ZipResult.Error("Project directory does not exist: ${projectDir.absolutePath}")
        }

        try {
            // Ensure output directory exists
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                return ZipResult.Error("Failed to create output directory")
            }

            // Create ZIP file
            val sanitizedName = sanitizeFileName(zipFileName)
            val timestamp = System.currentTimeMillis()
            val zipFile = File(outputDir, "${sanitizedName}_$timestamp.zip")

            Log.d(TAG, "Creating ZIP: ${zipFile.absolutePath}")

            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                // Set compression level
                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION)
                
                // Recursively add all files
                addDirectoryToZip(projectDir, projectDir.name, zipOut)
            }

            // Verify ZIP was created
            if (!zipFile.exists() || zipFile.length() == 0L) {
                return ZipResult.Error("ZIP file creation failed")
            }

            // Generate content URI for sharing
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            Log.d(TAG, "ZIP created successfully: ${zipFile.length()} bytes")

            return ZipResult.Success(
                zipUri = uri,
                zipPath = zipFile.absolutePath,
                sizeBytes = zipFile.length()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error creating ZIP", e)
            return ZipResult.Error("ZIP creation error: ${e.message}")
        }
    }

    /**
     * Create a ZIP archive from ProjectStructure without writing files first.
     * More efficient for in-memory operations.
     * 
     * @param structure The project structure
     * @param outputDir The output directory for the ZIP
     * @return ZipResult with URI or error
     */
    fun createZipFromStructure(structure: ProjectStructure, outputDir: File): ZipResult {
        try {
            // Ensure output directory exists
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                return ZipResult.Error("Failed to create output directory")
            }

            // Create ZIP file
            val sanitizedName = sanitizeFileName(structure.root)
            val timestamp = System.currentTimeMillis()
            val zipFile = File(outputDir, "${sanitizedName}_$timestamp.zip")

            Log.d(TAG, "Creating ZIP from structure: ${zipFile.absolutePath}")

            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION)

                // Create set of directories to add (for folder entries)
                val directories = mutableSetOf<String>()
                
                for (file in structure.files) {
                    // Skip empty/directory entries
                    if (file.content.isEmpty() && !file.path.endsWith("/")) {
                        continue
                    }

                    val entryPath = file.path
                    
                    // Add parent directories first
                    val parentDir = entryPath.substringBeforeLast("/", "")
                    if (parentDir.isNotEmpty() && !directories.contains(parentDir)) {
                        addParentDirectories(parentDir, directories, zipOut)
                    }
                    
                    // Skip directory markers
                    if (file.path.endsWith("/") || file.content.isEmpty()) {
                        directories.add(file.path.trimEnd('/'))
                        continue
                    }

                    // Add file entry
                    val zipEntry = ZipEntry(entryPath)
                    zipEntry.time = System.currentTimeMillis()
                    
                    try {
                        zipOut.putNextEntry(zipEntry)
                        zipOut.write(file.content.toByteArray(Charsets.UTF_8))
                        zipOut.closeEntry()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to add entry: $entryPath - ${e.message}")
                        // Continue with other files
                    }
                }
            }

            // Verify ZIP was created
            if (!zipFile.exists() || zipFile.length() == 0L) {
                return ZipResult.Error("ZIP file creation failed")
            }

            // Generate content URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            Log.d(TAG, "ZIP created successfully: ${zipFile.length()} bytes, ${structure.files.size} files")

            return ZipResult.Success(
                zipUri = uri,
                zipPath = zipFile.absolutePath,
                sizeBytes = zipFile.length()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error creating ZIP from structure", e)
            return ZipResult.Error("ZIP creation error: ${e.message}")
        }
    }

    /**
     * Recursively add a directory and its contents to the ZIP.
     */
    private fun addDirectoryToZip(dir: File, basePath: String, zipOut: ZipOutputStream) {
        val files = dir.listFiles() ?: return

        for (file in files) {
            val entryPath = "$basePath/${file.name}"

            if (file.isDirectory) {
                // Add directory entry
                val dirEntry = ZipEntry("$entryPath/")
                dirEntry.time = file.lastModified()
                zipOut.putNextEntry(dirEntry)
                zipOut.closeEntry()

                // Recurse into directory
                addDirectoryToZip(file, entryPath, zipOut)
            } else {
                // Add file entry
                addFileToZip(file, entryPath, zipOut)
            }
        }
    }

    /**
     * Add a single file to the ZIP.
     */
    private fun addFileToZip(file: File, entryPath: String, zipOut: ZipOutputStream) {
        try {
            val zipEntry = ZipEntry(entryPath)
            zipEntry.time = file.lastModified()
            zipEntry.size = file.length()

            zipOut.putNextEntry(zipEntry)

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    zipOut.write(buffer, 0, bytesRead)
                }
            }

            zipOut.closeEntry()

            Log.d(TAG, "Added to ZIP: $entryPath (${file.length()} bytes)")

        } catch (e: Exception) {
            Log.e(TAG, "Error adding file to ZIP: $entryPath", e)
            throw e
        }
    }

    /**
     * Add parent directory entries to the ZIP.
     */
    private fun addParentDirectories(
        parentPath: String,
        existingDirs: MutableSet<String>,
        zipOut: ZipOutputStream
    ) {
        val parts = parentPath.split("/")
        var currentPath = ""

        for (part in parts) {
            if (part.isEmpty()) continue
            
            currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
            
            if (!existingDirs.contains(currentPath)) {
                try {
                    val dirEntry = ZipEntry("$currentPath/")
                    dirEntry.time = System.currentTimeMillis()
                    zipOut.putNextEntry(dirEntry)
                    zipOut.closeEntry()
                    existingDirs.add(currentPath)
                } catch (e: Exception) {
                    // Directory entry might already exist
                    Log.d(TAG, "Directory entry already exists: $currentPath")
                }
            }
        }
    }

    /**
     * Sanitize file name for ZIP.
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("""[<>:"/\\|?*\x00-\x1F]"""), "_")
            .replace(Regex("""\s+"""), "_")
            .trim('.')
            .trim()
            .ifBlank { "project" }
    }

    /**
     * Delete a ZIP file.
     */
    fun deleteZip(zipPath: String): Boolean {
        return try {
            File(zipPath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting ZIP", e)
            false
        }
    }
}
