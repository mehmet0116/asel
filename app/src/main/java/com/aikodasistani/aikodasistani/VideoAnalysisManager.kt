package com.aikodasistani.aikodasistani

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoAnalysisManager(
    private val context: Context,
    private val visionCallback: suspend (String) -> String
) {

    suspend fun analyzeVideo(
        videoUri: Uri,
        frameIntervalMs: Long = DEFAULT_FRAME_INTERVAL_MS,
        maxFrames: Int = DEFAULT_MAX_FRAMES,
        progressCallback: (Int, Int, String) -> Unit
    ): VideoAnalysisResult = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()

        try {
            progressCallback(0, 0, "Video hazırlanıyor...")
            retriever.setDataSource(context, videoUri)

            // Video bilgilerini al
            val videoInfo = getVideoInfo(videoUri)
            progressCallback(25, 0, "Video bilgileri alınıyor...")

            // Frame analizi yap
            progressCallback(40, 0, "Videodan frame'ler çıkarılıyor...")
            val frames = extractKeyFrames(
                retriever = retriever,
                durationMs = videoInfo.durationMs,
                frameIntervalMs = frameIntervalMs,
                maxFrames = maxFrames
            )

            progressCallback(60, frames.size, "Frame'ler analiz ediliyor...")
            val frameAnalyses = analyzeFrames(frames, progressCallback)

            progressCallback(90, 0, "Analiz sonuçları birleştiriliyor...")
            val analysis = buildAnalysisReport(videoInfo, frameAnalyses)

            progressCallback(100, 1, "Analiz tamamlandı")
            VideoAnalysisResult.Success(analysis)

        } catch (e: Exception) {
            Log.e("VideoAnalysis", "Video analiz hatası", e)
            VideoAnalysisResult.Error("Video analiz edilemedi: ${e.message}")
        } finally {
            retriever.release()
        }
    }

    private suspend fun extractKeyFrames(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        frameIntervalMs: Long,
        maxFrames: Int
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()

        try {
            val safeInterval = frameIntervalMs.coerceAtLeast(1000L)
            val safeMaxFrames = maxFrames.coerceAtLeast(1)

            for (timeMs in 0 until durationMs step safeInterval) {
                val frame = retriever.getFrameAtTime(
                    timeMs * 1000, // Mikrosaniyeye çevir
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                frame?.let {
                    // Frame boyutunu optimize et
                    val optimizedFrame = optimizeFrameSize(it)
                    frames.add(optimizedFrame)
                }

                // Uzun videolar için sınırlama
                if (frames.size >= safeMaxFrames) break // Maksimum frame
            }
        } catch (e: Exception) {
            Log.e("VideoAnalysis", "Frame extraction error", e)
        }

        return@withContext frames
    }

    private suspend fun analyzeFrames(
        frames: List<Bitmap>,
        progressCallback: (Int, Int, String) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        val analyses = mutableListOf<String>()

        if (frames.isEmpty()) {
            progressCallback(60, 0, "Frame bulunamadı")
            return@withContext analyses
        }

        frames.forEachIndexed { index, frame ->
            try {
                val progress = 60 + (index * 30 / frames.size)
                progressCallback(progress, frames.size, "Frame ${index + 1}/${frames.size} analiz ediliyor...")

                // Frame'i base64'e çevir
                val base64Frame = bitmapToBase64(frame)
                base64Frame?.let {
                    // Görsel analiz yap
                    val analysis = visionCallback(it)
                    analyses.add("Frame ${index + 1}:\n$analysis")
                }

                // Frame'i temizle
                frame.recycle()

            } catch (e: Exception) {
                Log.e("VideoAnalysis", "Frame analysis error", e)
                analyses.add("Frame ${index + 1}: Analiz başarısız")
            }
        }

        return@withContext analyses
    }

    private fun buildAnalysisReport(
        videoInfo: VideoInfo,
        frameAnalyses: List<String>
    ): String {
        return buildString {
            append("🎥 **DETAYLI VIDEO ANALİZ RAPORU**\n\n")

            append("**📊 TEMEL BİLGİLER:**\n")
            append("• **Süre:** ${videoInfo.durationMs / 1000} saniye\n")
            append("• **Boyut:** ${videoInfo.size / (1024 * 1024)} MB\n")
            append("• **Çözünürlük:** ${videoInfo.resolution}\n")
            append("• **Analiz Tarihi:** ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            append("• **Analiz Edilen Frame Sayısı:** ${frameAnalyses.size}\n\n")

            if (frameAnalyses.isNotEmpty()) {
                append("**🔍 FRAME ANALİZLERİ:**\n")
                frameAnalyses.forEachIndexed { index, analysis ->
                    append("${analysis}\n")
                    if (index < frameAnalyses.size - 1) append("---\n")
                }
                append("\n")
            }

            append("**📋 ÖZET:**\n")
            append("Video başarıyla analiz edildi. ${frameAnalyses.size} adet frame üzerinden detaylı inceleme yapıldı.\n")
        }
    }

    private fun getVideoInfo(videoUri: Uri): VideoInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0

            // Dosya boyutunu al
            val cursor = context.contentResolver.query(videoUri, null, null, null, null)
            var size = 0L
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(MediaStore.Video.Media.SIZE)
                    if (sizeIndex != -1) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }

            VideoInfo(
                durationMs = duration,
                resolution = "${width}x${height}",
                size = size
            )
        } finally {
            retriever.release()
        }
    }

    private fun optimizeFrameSize(bitmap: Bitmap): Bitmap {
        val maxWidth = 800
        val maxHeight = 600

        return if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            val scale = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            )
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()

            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("VideoAnalysis", "Bitmap to Base64 error", e)
            null
        }
    }

    sealed class VideoAnalysisResult {
        data class Success(val analysis: String) : VideoAnalysisResult()
        data class Error(val message: String) : VideoAnalysisResult()
    }

    data class VideoInfo(
        val durationMs: Long,
        val resolution: String,
        val size: Long
    )

    companion object {
        const val DEFAULT_FRAME_INTERVAL_MS = 5_000L
        const val DEFAULT_MAX_FRAMES = 10
    }
}