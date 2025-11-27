package com.aikodasistani.aikodasistani.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aikodasistani.aikodasistani.models.Message
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for exporting chat messages in various formats
 */
object ChatExportUtil {

    private const val PAGE_WIDTH = 595  // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40
    private const val LINE_SPACING = 6

    /**
     * Export result containing file path and success status
     */
    data class ExportResult(
        val success: Boolean,
        val filePath: String?,
        val message: String
    )

    /**
     * Export messages as Markdown file
     */
    fun exportToMarkdown(
        context: Context,
        messages: List<Message>,
        sessionName: String = "Chat"
    ): ExportResult {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "chat_${sessionName.replace(" ", "_")}_$timestamp.md"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val markdown = buildMarkdownContent(messages, sessionName)
            
            FileWriter(file).use { writer ->
                writer.write(markdown)
            }
            
            ExportResult(
                success = true,
                filePath = file.absolutePath,
                message = "Markdown dosyası kaydedildi: $fileName"
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                message = "Dışa aktarma hatası: ${e.message}"
            )
        }
    }

    /**
     * Build Markdown content from messages
     */
    private fun buildMarkdownContent(messages: List<Message>, sessionName: String): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        
        sb.appendLine("# $sessionName")
        sb.appendLine()
        sb.appendLine("*Dışa aktarma tarihi: ${dateFormat.format(Date())}*")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        
        messages.forEach { message ->
            if (message.isSentByUser) {
                sb.appendLine("## 👤 Kullanıcı")
            } else {
                sb.appendLine("## 🤖 AI")
            }
            sb.appendLine()
            sb.appendLine(message.text)
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }
        
        sb.appendLine()
        sb.appendLine("*AI Kod Asistanı ile oluşturuldu*")
        
        return sb.toString()
    }

    /**
     * Export messages as PDF file
     */
    fun exportToPdf(
        context: Context,
        messages: List<Message>,
        sessionName: String = "Chat"
    ): ExportResult {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "chat_${sessionName.replace(" ", "_")}_$timestamp.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val document = PdfDocument()
            var pageNumber = 1
            var currentY = MARGIN.toFloat()
            
            // Text paints
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 18f
                isFakeBoldText = true
            }
            
            val headerPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 14f
                isFakeBoldText = true
            }
            
            val textPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 12f
            }
            
            val metaPaint = TextPaint().apply {
                color = Color.GRAY
                textSize = 10f
                isAntiAlias = true
            }
            
            val userBubblePaint = Paint().apply {
                color = Color.parseColor("#E3F2FD") // Light blue
                style = Paint.Style.FILL
            }
            
            val aiBubblePaint = Paint().apply {
                color = Color.parseColor("#F0FDFA") // Light teal
                style = Paint.Style.FILL
            }
            
            // Create first page
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            // Draw title
            canvas.drawText(sessionName, MARGIN.toFloat(), currentY + 18, titlePaint)
            currentY += 30f
            
            // Draw date
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            canvas.drawText("Dışa aktarma: ${dateFormat.format(Date())}", MARGIN.toFloat(), currentY, metaPaint)
            currentY += 25f
            
            // Draw separator line
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawLine(MARGIN.toFloat(), currentY, (PAGE_WIDTH - MARGIN).toFloat(), currentY, linePaint)
            currentY += 15f
            
            // Process each message
            for (message in messages) {
                // Calculate text height
                val textWidth = PAGE_WIDTH - (2 * MARGIN) - 20
                val layout = StaticLayout.Builder.obtain(
                    message.text,
                    0,
                    message.text.length,
                    textPaint,
                    textWidth
                ).build()
                
                val messageHeight = layout.height + 50f // Header + padding
                
                // Check if we need a new page
                if (currentY + messageHeight > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN.toFloat()
                }
                
                // Draw message bubble background
                val bubblePaint = if (message.isSentByUser) userBubblePaint else aiBubblePaint
                canvas.drawRoundRect(
                    MARGIN.toFloat(),
                    currentY,
                    (PAGE_WIDTH - MARGIN).toFloat(),
                    currentY + messageHeight,
                    8f, 8f,
                    bubblePaint
                )
                
                // Draw sender header
                val senderText = if (message.isSentByUser) "👤 Kullanıcı" else "🤖 AI"
                canvas.drawText(senderText, MARGIN + 10f, currentY + 20f, headerPaint)
                currentY += 30f
                
                // Draw message text
                canvas.save()
                canvas.translate(MARGIN + 10f, currentY)
                layout.draw(canvas)
                canvas.restore()
                
                currentY += layout.height + 25f
            }
            
            // Finish last page
            document.finishPage(page)
            
            // Write to file
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            
            document.close()
            
            ExportResult(
                success = true,
                filePath = file.absolutePath,
                message = "PDF dosyası kaydedildi: $fileName"
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                message = "PDF oluşturma hatası: ${e.message}"
            )
        }
    }

    /**
     * Export messages as plain text
     */
    fun exportToText(
        context: Context,
        messages: List<Message>,
        sessionName: String = "Chat"
    ): ExportResult {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "chat_${sessionName.replace(" ", "_")}_$timestamp.txt"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val content = buildTextContent(messages, sessionName)
            
            FileWriter(file).use { writer ->
                writer.write(content)
            }
            
            ExportResult(
                success = true,
                filePath = file.absolutePath,
                message = "Metin dosyası kaydedildi: $fileName"
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                message = "Dışa aktarma hatası: ${e.message}"
            )
        }
    }

    /**
     * Build plain text content from messages
     */
    private fun buildTextContent(messages: List<Message>, sessionName: String): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("  $sessionName")
        sb.appendLine("  Dışa aktarma: ${dateFormat.format(Date())}")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()
        
        messages.forEach { message ->
            val sender = if (message.isSentByUser) "[KULLANICI]" else "[AI]"
            sb.appendLine("$sender")
            sb.appendLine(message.text)
            sb.appendLine()
            sb.appendLine("───────────────────────────────────────")
            sb.appendLine()
        }
        
        sb.appendLine()
        sb.appendLine("AI Kod Asistanı ile oluşturuldu")
        
        return sb.toString()
    }

    /**
     * Share exported file
     */
    fun shareFile(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val mimeType = when {
                filePath.endsWith(".md") -> "text/markdown"
                filePath.endsWith(".pdf") -> "application/pdf"
                filePath.endsWith(".txt") -> "text/plain"
                else -> "*/*"
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Sohbeti Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşım hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
