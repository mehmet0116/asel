package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.CodeTemplate
import com.aikodasistani.aikodasistani.data.Snippet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesCenterActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var totalFavoritesText: TextView

    private var favoriteSnippets = listOf<Snippet>()
    private var favoriteTemplates = listOf<CodeTemplate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites_center)

        setupViews()
        loadFavorites()
    }

    private fun setupViews() {
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        totalFavoritesText = findViewById(R.id.totalFavoritesText)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showAllFavorites()
                    1 -> showSnippetFavorites()
                    2 -> showTemplateFavorites()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@FavoritesCenterActivity)
            
            favoriteSnippets = withContext(Dispatchers.IO) {
                db.snippetDao().getFavorites()
            }
            
            favoriteTemplates = withContext(Dispatchers.IO) {
                db.codeTemplateDao().getFavorites()
            }

            val total = favoriteSnippets.size + favoriteTemplates.size
            totalFavoritesText.text = getString(R.string.total_favorites, total)

            showAllFavorites()
        }
    }

    private fun showAllFavorites() {
        val items = mutableListOf<FavoriteItem>()
        favoriteSnippets.forEach { items.add(FavoriteItem.SnippetItem(it)) }
        favoriteTemplates.forEach { items.add(FavoriteItem.TemplateItem(it)) }
        
        updateRecyclerView(items)
    }

    private fun showSnippetFavorites() {
        val items = favoriteSnippets.map { FavoriteItem.SnippetItem(it) }
        updateRecyclerView(items)
    }

    private fun showTemplateFavorites() {
        val items = favoriteTemplates.map { FavoriteItem.TemplateItem(it) }
        updateRecyclerView(items)
    }

    private fun updateRecyclerView(items: List<FavoriteItem>) {
        if (items.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            recyclerView.adapter = FavoritesAdapter(items) { item ->
                when (item) {
                    is FavoriteItem.SnippetItem -> copyToClipboard(item.snippet.code)
                    is FavoriteItem.TemplateItem -> copyToClipboard(item.template.code)
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    sealed class FavoriteItem {
        data class SnippetItem(val snippet: Snippet) : FavoriteItem()
        data class TemplateItem(val template: CodeTemplate) : FavoriteItem()
    }

    inner class FavoritesAdapter(
        private val items: List<FavoriteItem>,
        private val onCopy: (FavoriteItem) -> Unit
    ) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.titleText)
            val typeChip: Chip = view.findViewById(R.id.typeChip)
            val languageChip: Chip = view.findViewById(R.id.languageChip)
            val codePreview: TextView = view.findViewById(R.id.codePreview)
            val copyButton: ImageButton = view.findViewById(R.id.copyButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            
            when (item) {
                is FavoriteItem.SnippetItem -> {
                    holder.title.text = item.snippet.title
                    holder.typeChip.text = getString(R.string.type_snippet)
                    holder.typeChip.setChipBackgroundColorResource(R.color.chip_snippet)
                    holder.languageChip.text = item.snippet.language ?: "Code"
                    holder.codePreview.text = item.snippet.code.take(200)
                }
                is FavoriteItem.TemplateItem -> {
                    holder.title.text = item.template.title
                    holder.typeChip.text = getString(R.string.type_template)
                    holder.typeChip.setChipBackgroundColorResource(R.color.chip_template)
                    holder.languageChip.text = item.template.language
                    holder.codePreview.text = item.template.code.take(200)
                }
            }

            holder.copyButton.setOnClickListener { onCopy(item) }
            holder.itemView.setOnClickListener { onCopy(item) }
        }

        override fun getItemCount() = items.size
    }
}
