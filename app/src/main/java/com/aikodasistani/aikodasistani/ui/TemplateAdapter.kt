package com.aikodasistani.aikodasistani.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.R
import com.aikodasistani.aikodasistani.data.CodeTemplate

class TemplateAdapter(
    private val onUseClick: (CodeTemplate) -> Unit,
    private val onCopyClick: (CodeTemplate) -> Unit,
    private val onFavoriteClick: (CodeTemplate) -> Unit,
    private val onEditClick: (CodeTemplate) -> Unit,
    private val onDeleteClick: (CodeTemplate) -> Unit
) : ListAdapter<CodeTemplate, TemplateAdapter.ViewHolder>(TemplateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTemplateTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvTemplateDescription)
        private val tvLanguage: TextView = itemView.findViewById(R.id.tvTemplateLanguage)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvTemplateCategory)
        private val tvUsageCount: TextView = itemView.findViewById(R.id.tvUsageCount)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        private val btnUse: ImageButton = itemView.findViewById(R.id.btnUseTemplate)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopyTemplate)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMoreOptions)

        fun bind(template: CodeTemplate) {
            tvTitle.text = template.title
            tvDescription.text = template.description
            tvLanguage.text = template.language
            tvCategory.text = getCategoryDisplayName(template.category)
            tvUsageCount.text = itemView.context.getString(R.string.usage_count, template.usageCount)
            
            // Set language badge color
            tvLanguage.setBackgroundResource(getLanguageBackground(template.language))
            
            // Favorite icon
            ivFavorite.setImageResource(
                if (template.isFavorite) R.drawable.ic_favorite 
                else R.drawable.ic_favorite_border
            )
            ivFavorite.setOnClickListener { onFavoriteClick(template) }
            
            // Action buttons
            btnUse.setOnClickListener { onUseClick(template) }
            btnCopy.setOnClickListener { onCopyClick(template) }
            
            // More options menu
            btnMore.setOnClickListener { view ->
                showPopupMenu(view, template)
            }
            
            // Click on item to use
            itemView.setOnClickListener { onUseClick(template) }
        }
        
        private fun getCategoryDisplayName(category: String): String {
            return when (category) {
                "android" -> "Android"
                "algorithm" -> "Algoritma"
                "web" -> "Web"
                "api" -> "API"
                "database" -> "Veritabanı"
                "utility" -> "Yardımcı"
                else -> "Diğer"
            }
        }
        
        private fun getLanguageBackground(language: String): Int {
            return when (language.lowercase()) {
                "kotlin" -> R.drawable.bg_lang_kotlin
                "java" -> R.drawable.bg_lang_java
                "python" -> R.drawable.bg_lang_python
                "javascript", "js" -> R.drawable.bg_lang_javascript
                "typescript", "ts" -> R.drawable.bg_lang_typescript
                "swift" -> R.drawable.bg_lang_swift
                "html" -> R.drawable.bg_lang_html
                "css" -> R.drawable.bg_lang_css
                "sql" -> R.drawable.bg_lang_sql
                else -> R.drawable.bg_language_badge
            }
        }
        
        private fun showPopupMenu(view: View, template: CodeTemplate) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.menu_template_options, menu)
                
                // Hide delete for built-in templates
                menu.findItem(R.id.action_delete)?.isVisible = !template.isBuiltIn
                menu.findItem(R.id.action_edit)?.isVisible = !template.isBuiltIn
                
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_copy -> {
                            onCopyClick(template)
                            true
                        }
                        R.id.action_edit -> {
                            onEditClick(template)
                            true
                        }
                        R.id.action_delete -> {
                            onDeleteClick(template)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    class TemplateDiffCallback : DiffUtil.ItemCallback<CodeTemplate>() {
        override fun areItemsTheSame(oldItem: CodeTemplate, newItem: CodeTemplate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CodeTemplate, newItem: CodeTemplate): Boolean {
            return oldItem == newItem
        }
    }
}
