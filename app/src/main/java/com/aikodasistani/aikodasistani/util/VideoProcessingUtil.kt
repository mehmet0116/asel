package com.aikodasistani.aikodasistani.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.aikodasistani.aikodasistani.VideoInfo

object VideoProcessingUtil {
    fun getVideoInfo(context: Context, videoUri: Uri): VideoInfo {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, videoUri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

            // Dosya boyutunu al
            val size = getFileSize(context, videoUri)

            VideoInfo(
                durationMs = duration,
                resolution = "${width}x${height}",
                size = size
            )
        } catch (e: Exception) {
            VideoInfo(0L, "Bilinmiyor", 0L)
        } finally {
            retriever.release()
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
                parcelFileDescriptor.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}