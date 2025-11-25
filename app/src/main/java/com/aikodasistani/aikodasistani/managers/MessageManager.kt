package com.aikodasistani.aikodasistani.managers

import android.util.Log
import android.widget.EditText
import com.aikodasistani.aikodasistani.models.Message

/**
 * Manages message operations including adding messages, appending chunks,
 * and safely updating UI text fields
 */
class MessageManager {

    /**
     * Create a new message
     */
    fun createMessage(text: String, isSentByUser: Boolean): Message {
        return Message(text = text, isSentByUser = isSentByUser)
    }

    /**
     * Create a thinking message
     */
    fun createThinkingMessage(levelName: String): Message {
        return Message(
            text = "🧠 $levelName Düşünme Modu Başlatıldı...",
            isSentByUser = false,
            isThinking = true
        )
    }

    /**
     * Add thinking step to a message
     */
    fun addThinkingStep(message: Message, step: String) {
        message.thinkingSteps.add(step)
    }

    /**
     * Safely set text in EditText, handling large texts
     * Note: For very large texts (>100K chars), this falls back to direct setText
     * Consider using a more sophisticated approach with coroutines if needed
     */
    fun setTextSafely(editText: EditText, text: String) {
        try {
            if (text.length > 100000) {
                // For extremely large texts, truncate with warning
                val truncated = text.take(50000)
                editText.setText("$truncated\n\n⚠️ [Text truncated - original length: ${text.length} chars]")
            } else {
                editText.setText(text)
            }
        } catch (e: Exception) {
            Log.e("MessageManager", "Text set error", e)
            val safeText = if (text.length > 1000) text.take(1000) + "\n[...]" else text
            editText.setText(safeText)
        }
    }

    /**
     * Format file size in a simple readable format
     */
    fun formatFileSizeSimple(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    /**
     * Get cancellation note text
     */
    fun getCancellationNote(): String {
        return "\n\n⚠️ *İşlem iptal edildi*"
    }

    /**
     * Extract code blocks from markdown text
     */
    fun extractCodeBlocks(text: String): List<String> {
        val pattern = Regex("```(?:\\w+)?\\s*([\\s\\S]*?)```")
        return pattern.findAll(text).map { it.groupValues[1].trim() }.toList()
    }
}
