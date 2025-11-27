package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.Project
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProjectManagerActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var chipGroup: ChipGroup
    
    private var allProjects = listOf<Project>()
    private var selectedLanguage: String? = null
    
    private val languages = listOf("Tümü", "Kotlin", "Java", "Python", "JavaScript", "TypeScript", "Swift", "Go", "Rust")
    private val projectColors = listOf("#4CAF50", "#2196F3", "#9C27B0", "#F44336", "#FF9800", "#00BCD4", "#E91E63", "#795548")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_manager)
        
        database = AppDatabase.getDatabase(this)
        
        setupViews()
        setupChips()
        loadProjects()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        recyclerView = findViewById(R.id.recyclerProjects)
        emptyView = findViewById(R.id.emptyView)
        chipGroup = findViewById(R.id.chipGroupLanguages)
        
        // FAB
        findViewById<FloatingActionButton>(R.id.fabAddProject).setOnClickListener {
            showAddProjectDialog()
        }
        
        // Setup RecyclerView
        projectAdapter = ProjectAdapter(
            onItemClick = { project -> openProject(project) },
            onEditClick = { project -> showEditProjectDialog(project) },
            onDeleteClick = { project -> confirmDeleteProject(project) }
        )
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@ProjectManagerActivity, 2)
            adapter = projectAdapter
        }
    }
    
    private fun setupChips() {
        languages.forEach { lang ->
            val chip = Chip(this).apply {
                text = lang
                isCheckable = true
                isChecked = lang == "Tümü"
                setOnClickListener {
                    selectedLanguage = if (lang == "Tümü") null else lang.lowercase()
                    filterProjects()
                }
            }
            chipGroup.addView(chip)
        }
    }
    
    private fun loadProjects() {
        lifecycleScope.launch {
            database.projectDao().getAllProjects().collectLatest { projects ->
                allProjects = projects
                filterProjects()
            }
        }
    }
    
    private fun filterProjects() {
        val filtered = if (selectedLanguage == null) {
            allProjects
        } else {
            allProjects.filter { it.language.equals(selectedLanguage, ignoreCase = true) }
        }
        
        projectAdapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty())
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
    
    private fun showAddProjectDialog(project: Project? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_project, null)
        
        val etName = dialogView.findViewById<EditText>(R.id.etProjectName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etProjectDescription)
        val languageChipGroup = dialogView.findViewById<ChipGroup>(R.id.chipGroupProjectLanguage)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.colorContainer)
        
        var selectedColor = project?.color ?: projectColors[0]
        var selectedLang = project?.language ?: "kotlin"
        
        // Pre-fill if editing
        project?.let {
            etName.setText(it.name)
            etDescription.setText(it.description)
            selectedColor = it.color
            selectedLang = it.language
        }
        
        // Add language chips
        listOf("Kotlin", "Java", "Python", "JavaScript", "TypeScript", "Swift", "Go", "Rust").forEach { lang ->
            val chip = Chip(this).apply {
                text = lang
                isCheckable = true
                isChecked = lang.lowercase() == selectedLang
                setOnClickListener {
                    selectedLang = lang.lowercase()
                }
            }
            languageChipGroup.addView(chip)
        }
        
        // Add color selection
        projectColors.forEach { color ->
            val colorView = CardView(this).apply {
                val size = (40 * resources.displayMetrics.density).toInt()
                val margin = (4 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                radius = size / 2f
                cardElevation = 4f
                setCardBackgroundColor(android.graphics.Color.parseColor(color))
                setOnClickListener {
                    selectedColor = color
                    updateColorSelection(colorContainer, color)
                }
            }
            colorContainer.addView(colorView)
        }
        updateColorSelection(colorContainer, selectedColor)
        
        AlertDialog.Builder(this)
            .setTitle(if (project == null) getString(R.string.add_project) else getString(R.string.edit_project))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save_template)) { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val newProject = Project(
                            id = project?.id ?: 0,
                            name = name,
                            description = description,
                            language = selectedLang,
                            color = selectedColor,
                            filesCount = project?.filesCount ?: 0,
                            snippetsCount = project?.snippetsCount ?: 0,
                            templatesCount = project?.templatesCount ?: 0,
                            notesCount = project?.notesCount ?: 0,
                            createdAt = project?.createdAt ?: System.currentTimeMillis()
                        )
                        database.projectDao().insertProject(newProject)
                        Toast.makeText(this@ProjectManagerActivity, 
                            getString(R.string.project_saved), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.project_name_required), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showEditProjectDialog(project: Project) {
        showAddProjectDialog(project)
    }
    
    private fun updateColorSelection(container: LinearLayout, selectedColor: String) {
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i) as? CardView
            view?.let {
                val scale = if (projectColors.getOrNull(i) == selectedColor) 1.3f else 1f
                it.scaleX = scale
                it.scaleY = scale
            }
        }
    }
    
    private fun openProject(project: Project) {
        // Update last accessed
        lifecycleScope.launch {
            database.projectDao().updateLastAccessed(project.id, System.currentTimeMillis())
        }
        
        // Show project details dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_project_details, null)
        
        dialogView.findViewById<TextView>(R.id.tvProjectName).text = project.name
        dialogView.findViewById<TextView>(R.id.tvProjectDescription).text = 
            project.description.ifEmpty { getString(R.string.no_description) }
        dialogView.findViewById<TextView>(R.id.tvProjectLanguage).text = project.language.uppercase()
        dialogView.findViewById<TextView>(R.id.tvSnippetsCount).text = "${project.snippetsCount}"
        dialogView.findViewById<TextView>(R.id.tvTemplatesCount).text = "${project.templatesCount}"
        dialogView.findViewById<TextView>(R.id.tvNotesCount).text = "${project.notesCount}"
        
        AlertDialog.Builder(this)
            .setTitle("📂 ${project.name}")
            .setView(dialogView)
            .setPositiveButton(getString(R.string.close), null)
            .setNeutralButton(getString(R.string.edit_snippet)) { _, _ ->
                showEditProjectDialog(project)
            }
            .show()
    }
    
    private fun confirmDeleteProject(project: Project) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_project_title))
            .setMessage(getString(R.string.delete_project_confirm, project.name))
            .setPositiveButton(getString(R.string.delete_snippet_button)) { _, _ ->
                lifecycleScope.launch {
                    database.projectDao().deleteProject(project)
                    Toast.makeText(this@ProjectManagerActivity, 
                        getString(R.string.project_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    // Inner adapter class
    inner class ProjectAdapter(
        private val onItemClick: (Project) -> Unit,
        private val onEditClick: (Project) -> Unit,
        private val onDeleteClick: (Project) -> Unit
    ) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {
        
        private var projects = listOf<Project>()
        
        fun submitList(newList: List<Project>) {
            projects = newList
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ProjectViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
            return ProjectViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
            holder.bind(projects[position])
        }
        
        override fun getItemCount() = projects.size
        
        inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView = itemView.findViewById<CardView>(R.id.cardProject)
            private val tvName = itemView.findViewById<TextView>(R.id.tvProjectName)
            private val tvDescription = itemView.findViewById<TextView>(R.id.tvProjectDescription)
            private val tvLanguage = itemView.findViewById<TextView>(R.id.tvProjectLanguage)
            private val tvStats = itemView.findViewById<TextView>(R.id.tvProjectStats)
            private val colorIndicator = itemView.findViewById<View>(R.id.colorIndicator)
            
            fun bind(project: Project) {
                tvName.text = project.name
                tvDescription.text = project.description.ifEmpty { getString(R.string.no_description) }
                tvLanguage.text = project.language.uppercase()
                tvStats.text = "${project.snippetsCount} snippets • ${project.notesCount} notes"
                colorIndicator.setBackgroundColor(android.graphics.Color.parseColor(project.color))
                
                cardView.setOnClickListener { onItemClick(project) }
                cardView.setOnLongClickListener {
                    showProjectOptions(project)
                    true
                }
            }
            
            private fun showProjectOptions(project: Project) {
                AlertDialog.Builder(itemView.context)
                    .setTitle(project.name)
                    .setItems(arrayOf(
                        getString(R.string.edit_snippet),
                        getString(R.string.delete_snippet)
                    )) { _, which ->
                        when (which) {
                            0 -> onEditClick(project)
                            1 -> onDeleteClick(project)
                        }
                    }
                    .show()
            }
        }
    }
}
