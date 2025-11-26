package com.aikodasistani.aikodasistani.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.aikodasistani.aikodasistani.models.AttachmentDescriptor
import com.aikodasistani.aikodasistani.models.AttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for processing attachments from URIs.
 * Detects file types, extracts metadata, and creates AttachmentDescriptor objects.
 */
object AttachmentProcessor {

    private const val TAG = "AttachmentProcessor"
    private const val PREVIEW_MAX_LINES = 20
    private const val PREVIEW_MAX_CHARS = 2000
    private const val THUMBNAIL_SIZE = 256

    // Code file extensions for detection
    private val CODE_EXTENSIONS = setOf(
        "kt", "java", "kts", "py", "js", "ts", "jsx", "tsx", 
        "html", "css", "scss", "vue", "c", "cpp", "h", "hpp",
        "cs", "rb", "go", "rs", "swift", "php", "sh", "bash",
        "gradle", "xml", "json", "yaml", "yml", "toml", "properties"
    )

    // Text file extensions
    private val TEXT_EXTENSIONS = setOf(
        "txt", "md", "readme", "log", "cfg", "conf", "ini"
    )

    /**
     * Process a URI and create an AttachmentDescriptor with all metadata.
     */
    suspend fun processAttachment(
        context: Context,
        uri: Uri
    ): AttachmentDescriptor = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        
        // Get basic metadata
        val displayName = getDisplayName(contentResolver, uri)
        val mimeType = contentResolver.getType(uri)
        val sizeBytes = getFileSize(contentResolver, uri)
        
        // Detect attachment type
        val attachmentType = detectAttachmentType(mimeType, displayName)
        
        // Get preview text for text/code files
        val previewText = if (attachmentType == AttachmentType.TEXT || attachmentType == AttachmentType.CODE) {
            try {
                getTextPreview(contentResolver, uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get text preview", e)
                null
            }
        } else null
        
        // Get thumbnail for images/videos
        val thumbnail = when (attachmentType) {
            AttachmentType.IMAGE -> getImageThumbnail(contentResolver, uri)
            AttachmentType.VIDEO -> getVideoThumbnail(context, uri)
            else -> null
        }
        
        AttachmentDescriptor(
            uri = uri,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            type = attachmentType,
            previewText = previewText,
            thumbnail = thumbnail
        )
    }

    /**
     * Detect attachment type from MIME type and file extension.
     */
    fun detectAttachmentType(mimeType: String?, fileName: String): AttachmentType {
        // Check MIME type first
        val type = when {
            mimeType == null -> AttachmentType.OTHER
            
            // ZIP files
            mimeType == "application/zip" ||
            mimeType == "application/x-zip-compressed" ||
            mimeType == "application/x-zip" -> AttachmentType.ZIP
            
            // Excel files
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
            mimeType == "application/vnd.ms-excel" -> AttachmentType.EXCEL
            
            // CSV files
            mimeType == "text/csv" ||
            mimeType == "application/csv" -> AttachmentType.CSV
            
            // Word files
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
            mimeType == "application/msword" -> AttachmentType.WORD
            
            // PDF files
            mimeType == "application/pdf" -> AttachmentType.PDF
            
            // Image files
            mimeType.startsWith("image/") -> AttachmentType.IMAGE
            
            // Video files
            mimeType.startsWith("video/") -> AttachmentType.VIDEO
            
            // Audio files
            mimeType.startsWith("audio/") -> AttachmentType.AUDIO
            
            // Text files
            mimeType.startsWith("text/") ||
            mimeType == "application/json" ||
            mimeType == "application/javascript" ||
            mimeType == "application/xml" -> {
                // Check if it's a code file
                val extension = getFileExtension(fileName)
                if (CODE_EXTENSIONS.contains(extension)) {
                    AttachmentType.CODE
                } else {
                    AttachmentType.TEXT
                }
            }
            
            else -> AttachmentType.OTHER
        }
        
        // If still OTHER, check by extension
        if (type == AttachmentType.OTHER) {
            val extension = getFileExtension(fileName)
            return when {
                extension == "zip" -> AttachmentType.ZIP
                extension == "xlsx" || extension == "xls" -> AttachmentType.EXCEL
                extension == "csv" -> AttachmentType.CSV
                extension == "docx" || extension == "doc" -> AttachmentType.WORD
                extension == "pdf" -> AttachmentType.PDF
                CODE_EXTENSIONS.contains(extension) -> AttachmentType.CODE
                TEXT_EXTENSIONS.contains(extension) -> AttachmentType.TEXT
                else -> AttachmentType.OTHER
            }
        }
        
        return type
    }

    /**
     * Get the display name from a content URI.
     */
    private fun getDisplayName(contentResolver: ContentResolver, uri: Uri): String {
        var displayName = "Unknown"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get display name", e)
        }
        return displayName
    }

    /**
     * Get file size from a content URI.
     */
    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        var size = 0L
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file size", e)
        }
        return size
    }

    /**
     * Get file extension from file name.
     */
    private fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) {
            fileName.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * Get text preview (first N lines) for text/code files.
     */
    private fun getTextPreview(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val sb = StringBuilder()
                var line: String?
                var lineCount = 0
                var charCount = 0
                
                while (reader.readLine().also { line = it } != null && 
                       lineCount < PREVIEW_MAX_LINES && 
                       charCount < PREVIEW_MAX_CHARS) {
                    sb.append(line).append('\n')
                    lineCount++
                    charCount += (line?.length ?: 0) + 1
                }
                
                if (sb.isNotEmpty()) {
                    sb.toString().trimEnd()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read text preview", e)
            null
        }
    }

    /**
     * Get thumbnail for image files.
     */
    private fun getImageThumbnail(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                // Calculate sample size
                val maxSize = THUMBNAIL_SIZE
                var sampleSize = 1
                while (options.outWidth / sampleSize > maxSize * 2 || 
                       options.outHeight / sampleSize > maxSize * 2) {
                    sampleSize *= 2
                }
                
                // Reopen stream and decode with sample size
                contentResolver.openInputStream(uri)?.use { stream2 ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    val bitmap = BitmapFactory.decodeStream(stream2, null, decodeOptions)
                    
                    // Scale to thumbnail size
                    bitmap?.let {
                        val scale = minOf(
                            maxSize.toFloat() / it.width,
                            maxSize.toFloat() / it.height
                        ).coerceAtMost(1f)
                        
                        val width = (it.width * scale).toInt().coerceAtLeast(1)
                        val height = (it.height * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(it, width, height, true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create image thumbnail", e)
            null
        }
    }

    /**
     * Get thumbnail for video files.
     */
    private fun getVideoThumbnail(context: Context, uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0)
            retriever.release()
            
            frame?.let {
                val maxSize = THUMBNAIL_SIZE
                val scale = minOf(
                    maxSize.toFloat() / it.width,
                    maxSize.toFloat() / it.height
                ).coerceAtMost(1f)
                
                val width = (it.width * scale).toInt().coerceAtLeast(1)
                val height = (it.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(it, width, height, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video thumbnail", e)
            null
        }
    }

    /**
     * Get icon resource for attachment type.
     */
    fun getIconForType(type: AttachmentType): Int {
        return when (type) {
            AttachmentType.ZIP -> android.R.drawable.ic_menu_save
            AttachmentType.EXCEL -> android.R.drawable.ic_menu_agenda
            AttachmentType.CSV -> android.R.drawable.ic_menu_agenda
            AttachmentType.WORD -> android.R.drawable.ic_menu_edit
            AttachmentType.PDF -> android.R.drawable.ic_menu_info_details
            AttachmentType.IMAGE -> android.R.drawable.ic_menu_gallery
            AttachmentType.VIDEO -> android.R.drawable.ic_menu_slideshow
            AttachmentType.AUDIO -> android.R.drawable.ic_lock_silent_mode_off
            AttachmentType.TEXT -> android.R.drawable.ic_menu_sort_by_size
            AttachmentType.CODE -> android.R.drawable.ic_menu_manage
            AttachmentType.OTHER -> android.R.drawable.ic_menu_help
        }
    }

    /**
     * Get emoji icon for attachment type (for display in chat).
     */
    fun getEmojiForType(type: AttachmentType): String {
        return when (type) {
            AttachmentType.ZIP -> "📦"
            AttachmentType.EXCEL -> "📊"
            AttachmentType.CSV -> "📋"
            AttachmentType.WORD -> "📝"
            AttachmentType.PDF -> "📄"
            AttachmentType.IMAGE -> "🖼️"
            AttachmentType.VIDEO -> "🎬"
            AttachmentType.AUDIO -> "🎵"
            AttachmentType.TEXT -> "📃"
            AttachmentType.CODE -> "💻"
            AttachmentType.OTHER -> "📁"
        }
    }

    /**
     * Format file size for display.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }
}
