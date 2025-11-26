package com.aikodasistani.aikodasistani.models

import android.graphics.Bitmap
import android.net.Uri

/**
 * Represents different types of attachments that can be handled by the application.
 * Used for unified attachment processing and UI display.
 */
enum class AttachmentType {
    ZIP,
    EXCEL,
    CSV,
    WORD,
    PDF,
    IMAGE,
    VIDEO,
    AUDIO,
    TEXT,
    CODE,
    OTHER
}

/**
 * Data class describing an attachment with all relevant metadata.
 * Used for unified handling of files attached to messages.
 * 
 * @param uri The content URI of the attachment
 * @param displayName Human-readable file name
 * @param mimeType The MIME type of the file
 * @param sizeBytes File size in bytes
 * @param type High-level attachment type category
 * @param previewText Optional text preview for text/code files (first N lines)
 * @param thumbnail Optional bitmap thumbnail for images/videos
 */
data class AttachmentDescriptor(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val type: AttachmentType,
    val previewText: String? = null,
    val thumbnail: Bitmap? = null
)

/**
 * Represents a generated document file with its metadata.
 * Used for AI-powered document generation feature.
 * 
 * @param file The actual File object
 * @param fileName Display name of the file
 * @param mimeType MIME type of the generated file
 * @param sizeBytes File size in bytes
 * @param contentUri Content URI via FileProvider for sharing/opening
 * @param fileType Type of document (e.g., "csv", "xlsx", "txt", "md")
 */
data class GeneratedDocument(
    val file: java.io.File,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentUri: Uri,
    val fileType: String
)
