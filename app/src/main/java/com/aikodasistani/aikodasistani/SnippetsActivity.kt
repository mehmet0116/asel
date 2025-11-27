package com.aikodasistani.aikodasistani

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.Snippet
import com.aikodasistani.aikodasistani.ui.SnippetAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for managing code snippets.
 * Users can save, search, and organize their frequently used code blocks.
 */
class SnippetsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipFavorites: Chip
    private lateinit var chipMostUsed: Chip
    private lateinit var fabAddSnippet: FloatingActionButton
    private lateinit var snippetAdapter: SnippetAdapter

    private var currentFilter = FilterType.ALL
    private var currentSearchQuery = ""

    enum class FilterType {
        ALL, FAVORITES, MOST_USED
    }

    companion object {
        const val EXTRA_CODE_TO_SAVE = "code_to_save"
        const val EXTRA_LANGUAGE = "language"
        const val RESULT_SNIPPET_SELECTED = "snippet_selected"
        const val RESULT_CODE = "code"
        
        private val SUPPORTED_LANGUAGES = listOf(
            "Kotlin", "Java", "Python", "JavaScript", "TypeScript",
            "HTML", "CSS", "JSON", "XML", "SQL",
            "Swift", "Go", "Rust", "C", "C++",
            "PHP", "Ruby", "Dart", "Shell", "Other"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snippets)

        db = AppDatabase.getDatabase(this)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        setupFab()

        // Check if we're adding a new snippet from code
        handleIncomingIntent()

        // Load snippets
        loadSnippets()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewSnippets)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        searchEditText = findViewById(R.id.searchEditText)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        chipAll = findViewById(R.id.chipAll)
        chipFavorites = findViewById(R.id.chipFavorites)
        chipMostUsed = findViewById(R.id.chipMostUsed)
        fabAddSnippet = findViewById(R.id.fabAddSnippet)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        snippetAdapter = SnippetAdapter(
            onSnippetClick = { snippet -> showSnippetDetails(snippet) },
            onUseClick = { snippet -> useSnippet(snippet) },
            onCopyClick = { snippet -> copySnippet(snippet) },
            onFavoriteClick = { snippet -> toggleFavorite(snippet) },
            onEditClick = { snippet -> showEditSnippetDialog(snippet) },
            onDeleteClick = { snippet -> confirmDeleteSnippet(snippet) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SnippetsActivity)
            adapter = snippetAdapter
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString() ?: ""
                loadSnippets()
            }
        })
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipFavorites) -> FilterType.FAVORITES
                checkedIds.contains(R.id.chipMostUsed) -> FilterType.MOST_USED
                else -> FilterType.ALL
            }
            loadSnippets()
        }
    }

    private fun setupFab() {
        fabAddSnippet.setOnClickListener {
            showAddSnippetDialog()
        }
    }

    private fun handleIncomingIntent() {
        val codeToSave = intent.getStringExtra(EXTRA_CODE_TO_SAVE)
        val language = intent.getStringExtra(EXTRA_LANGUAGE)

        if (!codeToSave.isNullOrBlank()) {
            showAddSnippetDialog(codeToSave, language)
        }
    }

    private fun loadSnippets() {
        lifecycleScope.launch {
            val snippetsFlow = when {
                currentSearchQuery.isNotBlank() -> db.snippetDao().searchSnippets(currentSearchQuery)
                currentFilter == FilterType.FAVORITES -> db.snippetDao().getFavoriteSnippets()
                currentFilter == FilterType.MOST_USED -> db.snippetDao().getMostUsedSnippets()
                else -> db.snippetDao().getAllSnippets()
            }

            snippetsFlow.collectLatest { snippets ->
                snippetAdapter.submitList(snippets)
                updateEmptyState(snippets.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateLayout.isVisible = isEmpty
        recyclerView.isVisible = !isEmpty
    }

    private fun showSnippetDetails(snippet: Snippet) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_code_preview, null)
        val codeTextView = dialogView.findViewById<android.widget.TextView>(R.id.tvCodePreview)
        codeTextView.text = snippet.code

        AlertDialog.Builder(this)
            .setTitle(snippet.title)
            .setView(dialogView)
            .setPositiveButton(R.string.use_snippet) { _, _ -> useSnippet(snippet) }
            .setNeutralButton(R.string.copy_code) { _, _ -> copySnippet(snippet) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun useSnippet(snippet: Snippet) {
        // Increment usage count
        lifecycleScope.launch(Dispatchers.IO) {
            db.snippetDao().incrementUsageCount(snippet.id)
        }

        // Return the code to MainActivity
        val resultIntent = Intent().apply {
            putExtra(RESULT_SNIPPET_SELECTED, true)
            putExtra(RESULT_CODE, snippet.code)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun copySnippet(snippet: Snippet) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Code Snippet", snippet.code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.snippet_copied, Toast.LENGTH_SHORT).show()

        // Increment usage count
        lifecycleScope.launch(Dispatchers.IO) {
            db.snippetDao().incrementUsageCount(snippet.id)
        }
    }

    private fun toggleFavorite(snippet: Snippet) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.snippetDao().updateFavoriteStatus(snippet.id, !snippet.isFavorite)
        }
    }

    private fun showAddSnippetDialog(code: String? = null, language: String? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_snippet, null)

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val actvLanguage = dialogView.findViewById<AutoCompleteTextView>(R.id.actvLanguage)
        val etCode = dialogView.findViewById<TextInputEditText>(R.id.etCode)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val etTags = dialogView.findViewById<TextInputEditText>(R.id.etTags)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Setup language dropdown
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SUPPORTED_LANGUAGES)
        actvLanguage.setAdapter(languageAdapter)

        // Pre-fill if coming from message
        code?.let { etCode.setText(it) }
        language?.let { actvLanguage.setText(it, false) }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            val snippetCode = etCode.text?.toString() ?: ""
            val snippetLanguage = actvLanguage.text?.toString()?.trim()
            val description = etDescription.text?.toString()?.trim()
            val tags = etTags.text?.toString()?.trim()

            if (title.isBlank()) {
                Toast.makeText(this, R.string.snippet_title_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (snippetCode.isBlank()) {
                Toast.makeText(this, R.string.snippet_code_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val snippet = Snippet(
                title = title,
                code = snippetCode,
                language = snippetLanguage?.takeIf { it.isNotBlank() },
                description = description?.takeIf { it.isNotBlank() },
                tags = tags?.takeIf { it.isNotBlank() }
            )

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    db.snippetDao().insertSnippet(snippet)
                }
                Toast.makeText(this@SnippetsActivity, R.string.snippet_saved, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditSnippetDialog(snippet: Snippet) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_snippet, null)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val actvLanguage = dialogView.findViewById<AutoCompleteTextView>(R.id.actvLanguage)
        val etCode = dialogView.findViewById<TextInputEditText>(R.id.etCode)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val etTags = dialogView.findViewById<TextInputEditText>(R.id.etTags)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Change title to "Edit"
        tvTitle.setText(R.string.edit_snippet_title)

        // Setup language dropdown
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SUPPORTED_LANGUAGES)
        actvLanguage.setAdapter(languageAdapter)

        // Pre-fill with existing values
        etTitle.setText(snippet.title)
        actvLanguage.setText(snippet.language ?: "", false)
        etCode.setText(snippet.code)
        etDescription.setText(snippet.description ?: "")
        etTags.setText(snippet.tags ?: "")

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            val snippetCode = etCode.text?.toString() ?: ""
            val snippetLanguage = actvLanguage.text?.toString()?.trim()
            val description = etDescription.text?.toString()?.trim()
            val tags = etTags.text?.toString()?.trim()

            if (title.isBlank()) {
                Toast.makeText(this, R.string.snippet_title_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (snippetCode.isBlank()) {
                Toast.makeText(this, R.string.snippet_code_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedSnippet = snippet.copy(
                title = title,
                code = snippetCode,
                language = snippetLanguage?.takeIf { it.isNotBlank() },
                description = description?.takeIf { it.isNotBlank() },
                tags = tags?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    db.snippetDao().updateSnippet(updatedSnippet)
                }
                Toast.makeText(this@SnippetsActivity, R.string.snippet_saved, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun confirmDeleteSnippet(snippet: Snippet) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_snippet)
            .setMessage(getString(R.string.delete_snippet_confirm, snippet.title))
            .setPositiveButton(R.string.delete_snippet) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.snippetDao().deleteSnippet(snippet)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SnippetsActivity, R.string.snippet_deleted, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
