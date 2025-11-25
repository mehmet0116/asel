package com.aikodasistani.aikodasistani.util

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.aikodasistani.aikodasistani.R
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

object FileDownloadUtil {

    fun showActionDialog(context: Context, messageText: String, fileName: String?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_code_preview, null)
        
        val tvFileName = dialogView.findViewById<TextView>(R.id.tvFileName)
        val tvCodePreview = dialogView.findViewById<TextView>(R.id.tvCodePreview)
        val editTextFileName = dialogView.findViewById<EditText>(R.id.editTextFileName)
        val textViewExtension = dialogView.findViewById<TextView>(R.id.textViewExtension)
        val btnShare = dialogView.findViewById<MaterialButton>(R.id.btnShare)
        val btnDownload = dialogView.findViewById<MaterialButton>(R.id.btnDownload)
        
        // Extract file extension from fileName or use .txt as default
        val actualFileName = fileName ?: "code_${System.currentTimeMillis()}.txt"
        val extension = if (actualFileName.contains(".")) {
            "." + actualFileName.substringAfterLast(".")
        } else {
            ".txt"
        }
        val baseName = actualFileName.substringBeforeLast(".")
        
        // Set initial values
        tvFileName.text = actualFileName
        editTextFileName.setText(baseName)
        textViewExtension.text = extension
        
        // Set code preview with max 500 chars for preview
        val previewText = if (messageText.length > 500) {
            messageText.take(500) + "\n\n... (${messageText.length - 500} karakter daha)"
        } else {
            messageText
        }
        tvCodePreview.text = previewText
        
        val dialog = AlertDialog.Builder(context, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .create()
        
        btnShare.setOnClickListener {
            shareContent(context, messageText)
            dialog.dismiss()
        }
        
        btnDownload.setOnClickListener {
            val newFileName = editTextFileName.text.toString().trim() + extension
            downloadFile(context, messageText, newFileName)
            dialog.dismiss()
        }
        
        dialog.show()
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