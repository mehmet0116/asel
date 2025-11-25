package com.aikodasistani.aikodasistani.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.R
import com.aikodasistani.aikodasistani.models.Message
import com.aikodasistani.aikodasistani.util.CodeDetectionUtil
import io.noties.markwon.Markwon

/**
 * RecyclerView adapter for displaying chat messages
 * Handles user messages, AI responses, and thinking/processing states
 */
class MessageAdapter(
    private val context: Context,
    private val messages: List<Message>,
    private val markwon: Markwon,
    private val onDownloadClick: (String) -> Unit,
    private val onAnalyzeCodeClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_THINKING = 3
    }

    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewMessage)
    }

    class AiMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewMessage)
        val cardView: CardView = view.findViewById(R.id.cardViewMessage)
        val downloadButton: ImageButton = view.findViewById(R.id.downloadButton)
    }

    class ThinkingMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewThinking)
        val progressBar: android.widget.ProgressBar = view.findViewById(R.id.progressThinking)
        val stepsContainer: android.widget.LinearLayout = view.findViewById(R.id.thinkingStepsContainer)

        fun bind(thinkingSteps: List<String>) {
            stepsContainer.removeAllViews()

            thinkingSteps.forEachIndexed { index, step ->
                val stepView = TextView(itemView.context).apply {
                    text = "• $step"
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.dark_gray))
                    textSize = 12f
                    setPadding(0, 4, 0, 4)

                    // Animasyon
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .setDuration(400L)
                        .setStartDelay((index * 300).toLong())
                        .start()
                }
                stepsContainer.addView(stepView)
            }

            // Progress bar'ı güncelle
            val progress = minOf(100, (thinkingSteps.size * 100) / 6)
            progressBar.progress = progress
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            messages[position].isThinking -> VIEW_TYPE_THINKING
            messages[position].isSentByUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_AI -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_ai, parent, false)
                AiMessageViewHolder(view).apply {
                    textView.movementMethod = LinkMovementMethod.getInstance()
                }
            }
            VIEW_TYPE_THINKING -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_thinking_message, parent, false)
                ThinkingMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> {
                markwon.setMarkdown(holder.textView, message.text)
            }
            is AiMessageViewHolder -> {
                markwon.setMarkdown(holder.textView, message.text)

                // Kod tespiti ve indir butonu görünürlüğü
                val showDownload = CodeDetectionUtil.shouldShowDownloadButton(message.text)
                holder.downloadButton.visibility = if (showDownload) View.VISIBLE else View.GONE

                holder.downloadButton.setOnClickListener {
                    onDownloadClick(message.text)
                }

                holder.cardView.setOnLongClickListener { view ->
                    val popup = PopupMenu(context, view)
                    popup.menu.add("Tümünü Kopyala")
                    val codeBlocks = extractCodeBlocks(message.text)
                    if (codeBlocks.isNotEmpty()) {
                        popup.menu.add("Kodu Kopyala")
                        popup.menu.add("🔧 Kodu Analiz Et")
                    }

                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.title) {
                            "🔧 Kodu Analiz Et" -> {
                                if (codeBlocks.isNotEmpty()) {
                                    onAnalyzeCodeClick?.invoke(codeBlocks.joinToString("\n\n"))
                                }
                                true
                            }
                            "Kodu Kopyala" -> {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied Text", codeBlocks.joinToString("\n\n"))
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                true
                            }
                            else -> {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied Text", message.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                true
                            }
                        }
                    }
                    if (popup.menu.size() > 0) popup.show()
                    true
                }
            }
            is ThinkingMessageViewHolder -> {
                holder.textView.text = context.getString(R.string.thinking_started)
                holder.bind(message.thinkingSteps)
            }
        }
    }

    override fun getItemCount() = messages.size

    private fun extractCodeBlocks(text: String): List<String> {
        val pattern = Regex("```(?:\\w+)?\\s*([\\s\\S]*?)```")
        return pattern.findAll(text).map { it.groupValues[1].trim() }.toList()
    }
}
