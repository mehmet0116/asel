package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.Bookmark
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksActivity : AppCompatActivity() {
    
    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookmarkAdapter
    private lateinit var searchEditText: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var emptyView: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    
    private var allBookmarks = listOf<Bookmark>()
    private var currentCategory = "Tümü"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)
        
        supportActionBar?.apply {
            title = getString(R.string.bookmarks_title)
            setDisplayHomeAsUpEnabled(true)
        }
        
        db = AppDatabase.getDatabase(this)
        
        initViews()
        setupSearch()
        setupCategoryChips()
        loadBookmarks()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewBookmarks)
        searchEditText = findViewById(R.id.etSearchBookmarks)
        chipGroup = findViewById(R.id.chipGroupCategories)
        emptyView = findViewById(R.id.emptyView)
        fabAdd = findViewById(R.id.fabAddBookmark)
        
        adapter = BookmarkAdapter(
            onItemClick = { bookmark -> showBookmarkDetailDialog(bookmark) },
            onCopyClick = { bookmark -> copyToClipboard(bookmark.content) },
            onDeleteClick = { bookmark -> confirmDelete(bookmark) },
            onEditClick = { bookmark -> showEditBookmarkDialog(bookmark) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        fabAdd.setOnClickListener { showAddBookmarkDialog() }
    }
    
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterBookmarks(s?.toString() ?: "")
            }
        })
    }
    
    private fun setupCategoryChips() {
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                listOf("Tümü") + db.bookmarkDao().getAllCategories()
            }
            
            chipGroup.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(this@BookmarksActivity).apply {
                    text = category
                    isCheckable = true
                    isChecked = category == currentCategory
                    setOnClickListener {
                        currentCategory = category
                        filterBookmarks(searchEditText.text?.toString() ?: "")
                    }
                }
                chipGroup.addView(chip)
            }
        }
    }
    
    private fun loadBookmarks() {
        lifecycleScope.launch {
            db.bookmarkDao().getAllBookmarks().collect { bookmarks ->
                allBookmarks = bookmarks
                filterBookmarks(searchEditText.text?.toString() ?: "")
                setupCategoryChips()
            }
        }
    }
    
    private fun filterBookmarks(query: String) {
        val filtered = allBookmarks.filter { bookmark ->
            val matchesQuery = query.isEmpty() || 
                bookmark.title.contains(query, ignoreCase = true) ||
                bookmark.content.contains(query, ignoreCase = true) ||
                bookmark.tags.contains(query, ignoreCase = true)
            
            val matchesCategory = currentCategory == "Tümü" || bookmark.category == currentCategory
            
            matchesQuery && matchesCategory
        }
        
        adapter.submitList(filtered)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun showBookmarkDetailDialog(bookmark: Bookmark) {
        AlertDialog.Builder(this)
            .setTitle(bookmark.title)
            .setMessage(bookmark.content)
            .setPositiveButton(R.string.copy_code) { _, _ ->
                copyToClipboard(bookmark.content)
            }
            .setNeutralButton(R.string.close, null)
            .show()
    }
    
    private fun showAddBookmarkDialog() {
        showBookmarkEditDialog(null)
    }
    
    private fun showEditBookmarkDialog(bookmark: Bookmark) {
        showBookmarkEditDialog(bookmark)
    }
    
    private fun showBookmarkEditDialog(bookmark: Bookmark?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_bookmark, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etBookmarkTitle)
        val etContent = dialogView.findViewById<EditText>(R.id.etBookmarkContent)
        val etCategory = dialogView.findViewById<EditText>(R.id.etBookmarkCategory)
        val etTags = dialogView.findViewById<EditText>(R.id.etBookmarkTags)
        
        bookmark?.let {
            etTitle.setText(it.title)
            etContent.setText(it.content)
            etCategory.setText(it.category)
            etTags.setText(it.tags)
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (bookmark == null) R.string.add_bookmark else R.string.edit_bookmark)
            .setView(dialogView)
            .setPositiveButton(R.string.save_snippet) { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()
                val category = etCategory.text.toString().trim().ifEmpty { "Genel" }
                val tags = etTags.text.toString().trim()
                
                if (title.isNotBlank() && content.isNotBlank()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val newBookmark = Bookmark(
                            id = bookmark?.id ?: 0,
                            title = title,
                            content = content,
                            category = category,
                            tags = tags,
                            createdAt = bookmark?.createdAt ?: System.currentTimeMillis()
                        )
                        db.bookmarkDao().insertBookmark(newBookmark)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@BookmarksActivity,
                                R.string.bookmark_saved,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.bookmark_title_content_required, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun confirmDelete(bookmark: Bookmark) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_bookmark_title)
            .setMessage(getString(R.string.delete_bookmark_confirm, bookmark.title))
            .setPositiveButton(R.string.delete_snippet) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.bookmarkDao().deleteBookmark(bookmark)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@BookmarksActivity,
                            R.string.bookmark_deleted,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bookmark", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    // Adapter for RecyclerView
    inner class BookmarkAdapter(
        private val onItemClick: (Bookmark) -> Unit,
        private val onCopyClick: (Bookmark) -> Unit,
        private val onDeleteClick: (Bookmark) -> Unit,
        private val onEditClick: (Bookmark) -> Unit
    ) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {
        
        private var bookmarks = listOf<Bookmark>()
        
        fun submitList(list: List<Bookmark>) {
            bookmarks = list
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bookmark, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(bookmarks[position])
        }
        
        override fun getItemCount() = bookmarks.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tvBookmarkTitle)
            private val tvContent: TextView = itemView.findViewById(R.id.tvBookmarkContent)
            private val tvCategory: TextView = itemView.findViewById(R.id.tvBookmarkCategory)
            private val tvTags: TextView = itemView.findViewById(R.id.tvBookmarkTags)
            private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopyBookmark)
            private val btnMore: ImageButton = itemView.findViewById(R.id.btnMoreBookmark)
            
            fun bind(bookmark: Bookmark) {
                tvTitle.text = bookmark.title
                tvContent.text = bookmark.content.take(200) + if (bookmark.content.length > 200) "..." else ""
                tvCategory.text = bookmark.category
                tvTags.text = bookmark.tags
                tvTags.visibility = if (bookmark.tags.isBlank()) View.GONE else View.VISIBLE
                
                itemView.setOnClickListener { onItemClick(bookmark) }
                btnCopy.setOnClickListener { onCopyClick(bookmark) }
                
                btnMore.setOnClickListener { view ->
                    val popup = PopupMenu(view.context, view)
                    popup.menuInflater.inflate(R.menu.menu_bookmark_options, popup.menu)
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_edit_bookmark -> {
                                onEditClick(bookmark)
                                true
                            }
                            R.id.action_delete_bookmark -> {
                                onDeleteClick(bookmark)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
            }
        }
    }
}
