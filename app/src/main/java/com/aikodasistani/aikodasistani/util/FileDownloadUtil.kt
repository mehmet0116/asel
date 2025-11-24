package com.aikodasistani.aikodasistani.util

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object FileDownloadUtil {

    fun showActionDialog(context: Context, messageText: String, fileName: String?) {
        val options = arrayOf("Dosyayı İndir", "Paylaş")

        AlertDialog.Builder(context)
            .setTitle("Dosya İşlemleri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> downloadFile(context, messageText, fileName)
                    1 -> shareContent(context, messageText)
                }
            }
            .show()
    }

    private fun downloadFile(context: Context, content: String, fileName: String?) {
        try {
            val actualFileName = fileName ?: "code_${System.currentTimeMillis()}.txt"
            val file = File(context.getExternalFilesDir(null), actualFileName)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(content.toByteArray())
            }

            Toast.makeText(context, "Dosya indirildi: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "İndirme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareContent(context: Context, content: String) {
        try {
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, content)
                type = "text/plain"
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşım hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}