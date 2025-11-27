package com.aikodasistani.aikodasistani.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.R
import com.aikodasistani.aikodasistani.data.Snippet
import com.google.android.material.chip.Chip

/**
 * RecyclerView Adapter for displaying code snippets
 */
class SnippetAdapter(
    private val onSnippetClick: (Snippet) -> Unit,
    private val onUseClick: (Snippet) -> Unit,
    private val onCopyClick: (Snippet) -> Unit,
    private val onFavoriteClick: (Snippet) -> Unit,
    private val onEditClick: (Snippet) -> Unit,
    private val onDeleteClick: (Snippet) -> Unit
) : ListAdapter<Snippet, SnippetAdapter.SnippetViewHolder>(SnippetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnippetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_snippet, parent, false)
        return SnippetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SnippetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SnippetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSnippetTitle)
        private val tvLanguageBadge: TextView = itemView.findViewById(R.id.tvLanguageBadge)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        private val tvCodePreview: TextView = itemView.findViewById(R.id.tvCodePreview)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tagsContainer: LinearLayout = itemView.findViewById(R.id.tagsContainer)
        private val tvUsageCount: TextView = itemView.findViewById(R.id.tvUsageCount)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopy)
        private val btnUse: View = itemView.findViewById(R.id.btnUse)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)

        fun bind(snippet: Snippet) {
            val context = itemView.context
            
            // Title
            tvTitle.text = snippet.title
            
            // Language badge
            if (!snippet.language.isNullOrBlank()) {
                tvLanguageBadge.visibility = View.VISIBLE
                tvLanguageBadge.text = snippet.language.uppercase()
                tvLanguageBadge.setBackgroundColor(getLanguageColor(context, snippet.language))
            } else {
                tvLanguageBadge.visibility = View.GONE
            }
            
            // Favorite button
            btnFavorite.setImageResource(
                if (snippet.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            btnFavorite.setColorFilter(
                ContextCompat.getColor(context, 
                    if (snippet.isFavorite) R.color.red else R.color.gray
                )
            )
            
            // Code preview (first 4 lines)
            val codeLines = snippet.code.lines().take(4)
            tvCodePreview.text = codeLines.joinToString("\n")
            
            // Description
            if (!snippet.description.isNullOrBlank()) {
                tvDescription.visibility = View.VISIBLE
                tvDescription.text = snippet.description
            } else {
                tvDescription.visibility = View.GONE
            }
            
            // Tags
            val tags = snippet.getTagsList()
            if (tags.isNotEmpty()) {
                tagsContainer.visibility = View.VISIBLE
                tagsContainer.removeAllViews()
                tags.take(3).forEach { tag ->
                    val chip = Chip(context).apply {
                        text = tag
                        textSize = 10f
                        isClickable = false
                        chipMinHeight = 24f
                        setPadding(4, 0, 4, 0)
                    }
                    tagsContainer.addView(chip)
                }
            } else {
                tagsContainer.visibility = View.GONE
            }
            
            // Usage count
            tvUsageCount.text = context.getString(R.string.usage_count, snippet.usageCount)
            
            // Click listeners
            itemView.setOnClickListener { onSnippetClick(snippet) }
            btnFavorite.setOnClickListener { onFavoriteClick(snippet) }
            btnCopy.setOnClickListener { onCopyClick(snippet) }
            btnUse.setOnClickListener { onUseClick(snippet) }
            
            btnMore.setOnClickListener { view ->
                showPopupMenu(view, snippet)
            }
        }
        
        private fun showPopupMenu(anchor: View, snippet: Snippet) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.menu_snippet_options, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditClick(snippet)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(snippet)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        
        private fun getLanguageColor(context: Context, language: String): Int {
            return when (language.lowercase()) {
                "kotlin" -> ContextCompat.getColor(context, R.color.kotlin_color)
                "java" -> ContextCompat.getColor(context, R.color.java_color)
                "python" -> ContextCompat.getColor(context, R.color.python_color)
                "javascript", "js" -> ContextCompat.getColor(context, R.color.javascript_color)
                "html" -> ContextCompat.getColor(context, R.color.html_color)
                "css" -> ContextCompat.getColor(context, R.color.css_color)
                "json" -> ContextCompat.getColor(context, R.color.json_color)
                "xml" -> ContextCompat.getColor(context, R.color.xml_color)
                "swift" -> ContextCompat.getColor(context, R.color.swift_color)
                "go" -> ContextCompat.getColor(context, R.color.go_color)
                "rust" -> ContextCompat.getColor(context, R.color.rust_color)
                "c", "cpp", "c++" -> ContextCompat.getColor(context, R.color.cpp_color)
                else -> ContextCompat.getColor(context, R.color.primary_main)
            }
        }
    }

    class SnippetDiffCallback : DiffUtil.ItemCallback<Snippet>() {
        override fun areItemsTheSame(oldItem: Snippet, newItem: Snippet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Snippet, newItem: Snippet): Boolean {
            return oldItem == newItem
        }
    }
}
