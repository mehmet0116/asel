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
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.QuickNote
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class QuickNotesActivity : AppCompatActivity() {
    
    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoteAdapter
    private lateinit var searchEditText: EditText
    private lateinit var emptyView: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    
    private var allNotes = listOf<QuickNote>()
    
    private val noteColors = listOf(
        "#FFFFFF", "#FFF59D", "#FFCC80", "#EF9A9A", 
        "#CE93D8", "#90CAF9", "#80DEEA", "#A5D6A7"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_notes)
        
        supportActionBar?.apply {
            title = getString(R.string.quick_notes_title)
            setDisplayHomeAsUpEnabled(true)
        }
        
        db = AppDatabase.getDatabase(this)
        
        initViews()
        setupSearch()
        loadNotes()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewNotes)
        searchEditText = findViewById(R.id.etSearchNotes)
        emptyView = findViewById(R.id.emptyView)
        fabAdd = findViewById(R.id.fabAddNote)
        
        adapter = NoteAdapter(
            onItemClick = { note -> showEditNoteDialog(note) },
            onPinClick = { note -> togglePin(note) },
            onDeleteClick = { note -> confirmDelete(note) },
            onCopyClick = { note -> copyToClipboard(note.content) }
        )
        
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter
        
        fabAdd.setOnClickListener { showAddNoteDialog() }
    }
    
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterNotes(s?.toString() ?: "")
            }
        })
    }
    
    private fun loadNotes() {
        lifecycleScope.launch {
            db.quickNoteDao().getAllNotes().collect { notes ->
                allNotes = notes
                filterNotes(searchEditText.text?.toString() ?: "")
            }
        }
    }
    
    private fun filterNotes(query: String) {
        val filtered = if (query.isEmpty()) {
            allNotes
        } else {
            allNotes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
            }
        }
        
        adapter.submitList(filtered)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun showAddNoteDialog() {
        showNoteEditDialog(null)
    }
    
    private fun showEditNoteDialog(note: QuickNote) {
        showNoteEditDialog(note)
    }
    
    private fun showNoteEditDialog(note: QuickNote?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etNoteTitle)
        val etContent = dialogView.findViewById<EditText>(R.id.etNoteContent)
        val spinnerLanguage = dialogView.findViewById<Spinner>(R.id.spinnerNoteLanguage)
        val colorGroup = dialogView.findViewById<LinearLayout>(R.id.colorPalette)
        
        var selectedColor = note?.color ?: "#FFFFFF"
        
        // Setup color palette
        noteColors.forEach { color ->
            val colorView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    marginEnd = 16
                }
                setBackgroundColor(android.graphics.Color.parseColor(color))
                setOnClickListener {
                    selectedColor = color
                    // Update selection indicator
                    for (i in 0 until colorGroup.childCount) {
                        colorGroup.getChildAt(i).alpha = if ((colorGroup.getChildAt(i).tag as? String) == color) 1f else 0.5f
                    }
                }
                tag = color
                alpha = if (color == selectedColor) 1f else 0.5f
            }
            colorGroup.addView(colorView)
        }
        
        // Setup language spinner
        val languages = listOf("Yok", "Kotlin", "Java", "Python", "JavaScript", "TypeScript", "HTML", "CSS", "SQL", "Swift", "Go", "Rust")
        spinnerLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        
        note?.let {
            etTitle.setText(it.title)
            etContent.setText(it.content)
            it.language?.let { lang ->
                val index = languages.indexOf(lang)
                if (index >= 0) spinnerLanguage.setSelection(index)
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (note == null) R.string.add_note else R.string.edit_note)
            .setView(dialogView)
            .setPositiveButton(R.string.save_snippet) { _, _ ->
                val title = etTitle.text.toString().trim().ifEmpty { "Adsız Not" }
                val content = etContent.text.toString().trim()
                val language = spinnerLanguage.selectedItem.toString().let { if (it == "Yok") null else it }
                
                if (content.isNotBlank()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val newNote = QuickNote(
                            id = note?.id ?: 0,
                            title = title,
                            content = content,
                            language = language,
                            color = selectedColor,
                            isPinned = note?.isPinned ?: false,
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        db.quickNoteDao().insertNote(newNote)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@QuickNotesActivity,
                                R.string.note_saved,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.note_content_required, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun togglePin(note: QuickNote) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.quickNoteDao().setPinned(note.id, !note.isPinned)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@QuickNotesActivity,
                    if (!note.isPinned) R.string.note_pinned else R.string.note_unpinned,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun confirmDelete(note: QuickNote) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_note_title)
            .setMessage(getString(R.string.delete_note_confirm, note.title))
            .setPositiveButton(R.string.delete_snippet) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.quickNoteDao().deleteNote(note)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@QuickNotesActivity,
                            R.string.note_deleted,
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
        val clip = ClipData.newPlainText("Note", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    // Adapter for RecyclerView
    inner class NoteAdapter(
        private val onItemClick: (QuickNote) -> Unit,
        private val onPinClick: (QuickNote) -> Unit,
        private val onDeleteClick: (QuickNote) -> Unit,
        private val onCopyClick: (QuickNote) -> Unit
    ) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {
        
        private var notes = listOf<QuickNote>()
        private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
        
        fun submitList(list: List<QuickNote>) {
            notes = list
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_note, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(notes[position])
        }
        
        override fun getItemCount() = notes.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: CardView = itemView.findViewById(R.id.cardNote)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
            private val tvContent: TextView = itemView.findViewById(R.id.tvNoteContent)
            private val tvLanguage: TextView = itemView.findViewById(R.id.tvNoteLanguage)
            private val tvDate: TextView = itemView.findViewById(R.id.tvNoteDate)
            private val ivPin: ImageView = itemView.findViewById(R.id.ivPinNote)
            private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopyNote)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteNote)
            
            fun bind(note: QuickNote) {
                tvTitle.text = note.title
                tvContent.text = note.content
                tvDate.text = dateFormat.format(Date(note.updatedAt))
                
                try {
                    cardView.setCardBackgroundColor(android.graphics.Color.parseColor(note.color))
                } catch (_: Exception) {
                    cardView.setCardBackgroundColor(android.graphics.Color.WHITE)
                }
                
                ivPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE
                
                note.language?.let {
                    tvLanguage.text = it
                    tvLanguage.visibility = View.VISIBLE
                } ?: run {
                    tvLanguage.visibility = View.GONE
                }
                
                itemView.setOnClickListener { onItemClick(note) }
                ivPin.setOnClickListener { onPinClick(note) }
                cardView.setOnLongClickListener { 
                    onPinClick(note)
                    true
                }
                btnCopy.setOnClickListener { onCopyClick(note) }
                btnDelete.setOnClickListener { onDeleteClick(note) }
            }
        }
    }
}
