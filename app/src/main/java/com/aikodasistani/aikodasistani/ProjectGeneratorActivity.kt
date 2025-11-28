package com.aikodasistani.aikodasistani

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.managers.SettingsManager
import com.aikodasistani.aikodasistani.models.ProjectGenerationRequest
import com.aikodasistani.aikodasistani.models.ProjectGenerationResult
import com.aikodasistani.aikodasistani.models.ProjectType
import com.aikodasistani.aikodasistani.models.TemplateCategories
import com.aikodasistani.aikodasistani.projectgenerator.domain.GenerationState
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProviderIdentifier
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProviderOption
import com.aikodasistani.aikodasistani.projectgenerator.presentation.AIProjectGeneratorViewModel
import com.aikodasistani.aikodasistani.util.ProjectGeneratorUtil
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class ProjectGeneratorActivity : AppCompatActivity() {
    
    private lateinit var etProjectName: EditText
    private lateinit var etPackageName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etNaturalLanguage: EditText
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvProjectTypes: RecyclerView
    private lateinit var btnGenerate: Button
    private lateinit var btnGenerateFromText: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var resultContainer: CardView
    private lateinit var tvResultInfo: TextView
    private lateinit var btnOpenZip: Button
    private lateinit var btnShareZip: Button
    
    // Provider/Model selection UI (optional - may not exist in layout)
    private var tvSelectedProvider: TextView? = null
    private var tvSelectedModel: TextView? = null
    private var btnSelectProvider: Button? = null
    private var btnSelectModel: Button? = null
    
    private var selectedProjectType: ProjectType = ProjectType.ANDROID_KOTLIN
    private var generatedFilePath: String? = null
    private var generatedFileUri: android.net.Uri? = null
    
    // Settings manager for provider/model info
    private lateinit var settingsManager: SettingsManager
    
    // ViewModel for AI-driven generation
    private val viewModel: AIProjectGeneratorViewModel by viewModels {
        AIProjectGeneratorViewModel.Factory(applicationContext, settingsManager)
    }
    
    // Flag to use AI generation vs template generation
    private var useAIGeneration = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_generator)
        
        // Initialize settings manager first
        settingsManager = SettingsManager(this)
        lifecycleScope.launch {
            settingsManager.initialize()
            setupProviderModelUI()
        }
        
        setupViews()
        setupProjectTypeGrid()
        setupCategoryChips()
        setupButtons()
        observeViewModel()
    }
    
    private fun setupViews() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        etProjectName = findViewById(R.id.etProjectName)
        etPackageName = findViewById(R.id.etPackageName)
        etDescription = findViewById(R.id.etDescription)
        etNaturalLanguage = findViewById(R.id.etNaturalLanguage)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        rvProjectTypes = findViewById(R.id.rvProjectTypes)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnGenerateFromText = findViewById(R.id.btnGenerateFromText)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        resultContainer = findViewById(R.id.resultContainer)
        tvResultInfo = findViewById(R.id.tvResultInfo)
        btnOpenZip = findViewById(R.id.btnOpenZip)
        btnShareZip = findViewById(R.id.btnShareZip)
        
        // Initialize provider/model selection views (optional - may be null if not in layout)
        tvSelectedProvider = findViewById(R.id.tvSelectedProvider)
        tvSelectedModel = findViewById(R.id.tvSelectedModel)
        btnSelectProvider = findViewById(R.id.btnSelectProvider)
        btnSelectModel = findViewById(R.id.btnSelectModel)
    }
    
    private fun setupProviderModelUI() {
        // Update UI with current provider/model
        viewModel.loadProvidersAndOptions()
        
        btnSelectProvider?.setOnClickListener {
            showProviderSelectionDialog()
        }
        
        btnSelectModel?.setOnClickListener {
            showModelSelectionDialog()
        }
        
        // Update display if views exist
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedProvider.collect { provider ->
                        tvSelectedProvider?.text = provider?.value ?: "Not selected"
                    }
                }
                launch {
                    viewModel.selectedOption.collect { option ->
                        tvSelectedModel?.text = option?.displayName ?: "Not selected"
                    }
                }
            }
        }
    }
    
    private fun showProviderSelectionDialog() {
        val providers = viewModel.availableProviders.value
        if (providers.isEmpty()) {
            Toast.makeText(this, "No providers available", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = providers.map { it.value }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select AI Provider")
            .setItems(items) { _, which ->
                viewModel.selectProvider(providers[which])
            }
            .show()
    }
    
    private fun showModelSelectionDialog() {
        val options = viewModel.availableOptions.value
        if (options.isEmpty()) {
            Toast.makeText(this, "No models available", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = options.map { it.displayName }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select AI Model")
            .setItems(items) { _, which ->
                viewModel.selectOption(options[which])
            }
            .show()
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AIProjectGeneratorViewModel.UIState.Idle -> {
                            showLoading(false)
                        }
                        is AIProjectGeneratorViewModel.UIState.Loading -> {
                            showLoading(true)
                            tvStatus.text = "🔄 Preparing..."
                        }
                        is AIProjectGeneratorViewModel.UIState.Generating -> {
                            showLoading(true)
                            tvStatus.text = when (val genState = state.state) {
                                is GenerationState.Preparing -> "🔄 Preparing..."
                                is GenerationState.CallingAI -> "🤖 ${genState.message}"
                                is GenerationState.Parsing -> "📝 ${genState.message}"
                                is GenerationState.WritingFiles -> "📁 Writing files (${genState.progress}/${genState.total})..."
                                is GenerationState.CreatingZip -> "📦 ${genState.message}"
                                else -> "🔄 Processing..."
                            }
                        }
                        is AIProjectGeneratorViewModel.UIState.Success -> {
                            showLoading(false)
                            val result = state.result
                            generatedFilePath = result.zipPath
                            generatedFileUri = result.zipUri
                            
                            tvStatus.text = "✅ Project generated successfully!"
                            tvResultInfo.text = """
                                📁 Project: ${result.projectStructure.root}
                                📄 Files: ${result.projectStructure.files.size}
                                📦 ${result.message ?: ""}
                            """.trimIndent()
                            
                            resultContainer.visibility = View.VISIBLE
                        }
                        is AIProjectGeneratorViewModel.UIState.Error -> {
                            showLoading(false)
                            tvStatus.text = "❌ ${viewModel.getErrorMessage(state.error)}"
                            resultContainer.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
    
    private fun setupCategoryChips() {
        TemplateCategories.categories.forEach { category ->
            val chip = Chip(this).apply {
                text = "${category.icon} ${category.name}"
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        filterProjectTypes(category.templates)
                    }
                }
            }
            chipGroupCategories.addView(chip)
        }
        
        // Add "All" chip
        val allChip = Chip(this).apply {
            text = "📋 Tümü"
            isCheckable = true
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    filterProjectTypes(ProjectType.entries)
                }
            }
        }
        chipGroupCategories.addView(allChip, 0)
    }
    
    private fun setupProjectTypeGrid() {
        val adapter = ProjectTypeAdapter(ProjectType.entries) { projectType ->
            selectedProjectType = projectType
            etProjectName.hint = "Örn: My${projectType.displayName.replace(" ", "").replace("(", "").replace(")", "")}"
            
            // Update package name hint based on project type
            when (projectType) {
                ProjectType.ANDROID_KOTLIN, ProjectType.ANDROID_JAVA -> {
                    etPackageName.visibility = View.VISIBLE
                    etPackageName.hint = "com.example.myapp"
                }
                else -> {
                    etPackageName.visibility = View.GONE
                }
            }
            
            Toast.makeText(this, "${projectType.displayName} seçildi", Toast.LENGTH_SHORT).show()
        }
        
        rvProjectTypes.layoutManager = GridLayoutManager(this, 2)
        rvProjectTypes.adapter = adapter
    }
    
    private fun filterProjectTypes(types: List<ProjectType>) {
        val adapter = rvProjectTypes.adapter as? ProjectTypeAdapter
        adapter?.updateList(types)
    }
    
    private fun setupButtons() {
        btnGenerate.setOnClickListener {
            val projectName = etProjectName.text.toString().trim()
            if (projectName.isEmpty()) {
                etProjectName.error = getString(R.string.project_name_required)
                return@setOnClickListener
            }
            
            if (useAIGeneration) {
                // Use AI-driven generation
                val prompt = buildProjectPrompt(
                    projectType = selectedProjectType,
                    projectName = projectName,
                    packageName = etPackageName.text.toString().trim().ifEmpty { null },
                    description = etDescription.text.toString().trim().ifEmpty { null }
                )
                viewModel.generateProject(projectName, prompt)
            } else {
                // Use template-based generation (fallback)
                generateProjectFromTemplate(
                    ProjectGenerationRequest(
                        projectType = selectedProjectType,
                        projectName = projectName,
                        packageName = etPackageName.text.toString().trim().ifEmpty { null },
                        description = etDescription.text.toString().trim().ifEmpty { null }
                    )
                )
            }
        }
        
        btnGenerateFromText.setOnClickListener {
            val input = etNaturalLanguage.text.toString().trim()
            if (input.isEmpty()) {
                etNaturalLanguage.error = "Please enter a description"
                return@setOnClickListener
            }
            
            // Always use AI for natural language input
            val projectName = ProjectGeneratorUtil.extractProjectName(input)
            viewModel.generateProject(projectName, input)
        }
        
        btnOpenZip.setOnClickListener {
            generatedFileUri?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/zip")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No app found to open ZIP files", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        btnShareZip.setOnClickListener {
            generatedFileUri?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Project"))
            }
        }
    }
    
    private fun buildProjectPrompt(
        projectType: ProjectType,
        projectName: String,
        packageName: String?,
        description: String?
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("Create a ${projectType.displayName} project named '$projectName'.")
        sb.appendLine()
        sb.appendLine("Project Type: ${projectType.displayName}")
        sb.appendLine("Description: ${projectType.description}")
        
        if (!packageName.isNullOrBlank()) {
            sb.appendLine("Package Name: $packageName")
        }
        
        if (!description.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("Additional Requirements:")
            sb.appendLine(description)
        }
        
        sb.appendLine()
        sb.appendLine("Generate a complete, production-ready project with all necessary files.")
        
        return sb.toString()
    }
    
    private fun generateProjectFromTemplate(request: ProjectGenerationRequest) {
        lifecycleScope.launch {
            showLoading(true)
            tvStatus.text = "🔄 ${request.projectType.displayName} project is being created..."
            
            val result = ProjectGeneratorUtil.generateProject(this@ProjectGeneratorActivity, request)
            
            when (result) {
                is ProjectGenerationResult.Success -> {
                    generatedFilePath = result.outputPath
                    generatedFileUri = result.outputUri
                    
                    tvStatus.text = "✅ Project generated successfully!"
                    tvResultInfo.text = """
                        📁 Project: ${result.projectName}
                        📦 Type: ${result.projectType.displayName}
                        📄 Files: ${result.files.size}
                        ${result.description ?: ""}
                    """.trimIndent()
                    
                    resultContainer.visibility = View.VISIBLE
                }
                is ProjectGenerationResult.Error -> {
                    tvStatus.text = "❌ Error: ${result.message}"
                    resultContainer.visibility = View.GONE
                }
            }
            
            showLoading(false)
        }
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGenerate.isEnabled = !show
        btnGenerateFromText.isEnabled = !show
    }
    
    // Inner adapter class for project types
    inner class ProjectTypeAdapter(
        private var items: List<ProjectType>,
        private val onItemClick: (ProjectType) -> Unit
    ) : RecyclerView.Adapter<ProjectTypeAdapter.ViewHolder>() {
        
        private var selectedPosition = 0
        
        fun updateList(newItems: List<ProjectType>) {
            items = newItems
            selectedPosition = 0
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_project_type, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], position == selectedPosition)
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(R.id.tvProjectTypeName)
            private val tvDesc: TextView = itemView.findViewById(R.id.tvProjectTypeDesc)
            private val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardProjectType)
            
            fun bind(projectType: ProjectType, isSelected: Boolean) {
                val icon = when {
                    projectType.displayName.contains("Android") -> "📱"
                    projectType.displayName.contains("Flutter") -> "🦋"
                    projectType.displayName.contains("React") -> "⚛️"
                    projectType.displayName.contains("Node") -> "🟢"
                    projectType.displayName.contains("Python") -> "🐍"
                    projectType.displayName.contains("Vue") -> "💚"
                    projectType.displayName.contains("Angular") -> "🔴"
                    projectType.displayName.contains("Spring") -> "🍃"
                    projectType.displayName.contains(".NET") -> "💜"
                    projectType.displayName.contains("Go") -> "🔵"
                    projectType.displayName.contains("Rust") -> "🦀"
                    else -> "📁"
                }
                
                tvName.text = "$icon ${projectType.displayName}"
                tvDesc.text = projectType.description
                
                cardView.setCardBackgroundColor(
                    if (isSelected) getColor(R.color.primary_light) 
                    else getColor(R.color.surface)
                )
                
                itemView.setOnClickListener {
                    val oldPosition = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onItemClick(projectType)
                }
            }
        }
    }
}
