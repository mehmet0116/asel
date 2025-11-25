package com.aikodasistani.aikodasistani.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Manages image operations including conversion, optimization, and thumbnail creation
 * Handles bitmap to base64 conversion and size optimization for AI APIs
 */
class ImageManager(private val context: Context) {

    companion object {
        private const val THUMBNAIL_MAX_SIZE = 240
        private const val MAX_FILE_SIZE = 4 * 1024 * 1024 // 4MB for API limits
    }

    /**
     * Convert URI to Bitmap
     */
    suspend fun uriToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("ImageManager", "URI to Bitmap conversion failed", e)
            null
        }
    }

    /**
     * Convert Bitmap to Base64 string
     */
    suspend fun bitmapToBase64(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ImageManager", "Bitmap to Base64 conversion failed", e)
            null
        }
    }

    /**
     * Convert Base64 string to Bitmap
     */
    fun base64ToBitmap(b64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e("ImageManager", "Base64 to Bitmap failed", e)
            null
        }
    }

    /**
     * Optimize bitmap size for API limits
     * Reduces quality and/or dimensions to stay under MAX_FILE_SIZE
     */
    fun optimizeBitmapSize(bitmap: Bitmap): Bitmap {
        var optimizedBitmap = bitmap

        try {
            var quality = 85
            var outputStream: ByteArrayOutputStream

            do {
                outputStream = ByteArrayOutputStream()
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val byteCount = outputStream.size()

                if (byteCount <= MAX_FILE_SIZE) {
                    break
                }

                quality -= 5
                if (quality < 60) {
                    // Keep minimum 60 quality for OCR, only reduce size
                    val scale = 0.75f
                    val newWidth = (optimizedBitmap.width * scale).toInt()
                    val newHeight = (optimizedBitmap.height * scale).toInt()
                    optimizedBitmap = Bitmap.createScaledBitmap(optimizedBitmap, newWidth, newHeight, true)
                    quality = 75
                }
            } while (quality > 50)

            outputStream.close()
            Log.d("ImageManager", "Optimized - Quality: $quality%, Size: ${outputStream.size()} bytes")

        } catch (e: Exception) {
            Log.e("ImageManager", "Optimization error", e)
        }

        return optimizedBitmap
    }

    /**
     * Create thumbnail from bitmap
     */
    fun createThumbnail(bitmap: Bitmap): Bitmap {
        val scale = min(
            THUMBNAIL_MAX_SIZE.toFloat() / bitmap.width.toFloat(),
            THUMBNAIL_MAX_SIZE.toFloat() / bitmap.height.toFloat()
        ).coerceAtMost(1f)

        val targetWidth = maxOf(1, (bitmap.width * scale).toInt())
        val targetHeight = maxOf(1, (bitmap.height * scale).toInt())
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Get file size from URI
     */
    suspend fun getFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (sizeIndex != -1) {
                        return@withContext it.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageManager", "Failed to get file size", e)
        }
        return@withContext 0L
    }

    /**
     * Get file name from URI
     */
    suspend fun getFileName(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return@withContext it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageManager", "Failed to get file name", e)
        }
        return@withContext "Bilinmeyen dosya"
    }
}
