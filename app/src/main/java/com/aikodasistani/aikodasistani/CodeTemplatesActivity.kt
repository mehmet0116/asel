package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.CodeTemplate
import com.aikodasistani.aikodasistani.ui.TemplateAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CodeTemplatesActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var adapter: TemplateAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var emptyView: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    
    private var currentFilter = "all"
    private var currentCategory = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_templates)
        
        database = AppDatabase.getDatabase(this)
        
        setupViews()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        loadTemplates()
        insertDefaultTemplatesIfNeeded()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        searchEditText = findViewById(R.id.etSearch)
        filterChipGroup = findViewById(R.id.chipGroupFilter)
        recyclerView = findViewById(R.id.rvTemplates)
        emptyView = findViewById(R.id.emptyView)
        fabAdd = findViewById(R.id.fabAddTemplate)
        
        fabAdd.setOnClickListener {
            showAddTemplateDialog()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = TemplateAdapter(
            onUseClick = { template -> useTemplate(template) },
            onCopyClick = { template -> copyTemplate(template) },
            onFavoriteClick = { template -> toggleFavorite(template) },
            onEditClick = { template -> showEditTemplateDialog(template) },
            onDeleteClick = { template -> confirmDelete(template) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    searchTemplates(query)
                } else {
                    loadTemplates()
                }
            }
        })
    }
    
    private fun setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val chip = if (checkedIds.isNotEmpty()) {
                filterChipGroup.findViewById<Chip>(checkedIds[0])
            } else null
            
            currentFilter = when (chip?.id) {
                R.id.chipFavorites -> "favorites"
                R.id.chipAndroid -> { currentCategory = "android"; "category" }
                R.id.chipAlgorithm -> { currentCategory = "algorithm"; "category" }
                R.id.chipWeb -> { currentCategory = "web"; "category" }
                R.id.chipApi -> { currentCategory = "api"; "category" }
                else -> "all"
            }
            
            loadTemplates()
        }
    }
    
    private fun loadTemplates() {
        lifecycleScope.launch {
            when (currentFilter) {
                "favorites" -> {
                    val templates = database.codeTemplateDao().getFavorites()
                    adapter.submitList(templates)
                    emptyView.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (templates.isEmpty()) View.GONE else View.VISIBLE
                }
                "category" -> {
                    database.codeTemplateDao().getByCategory(currentCategory).collectLatest { templates ->
                        adapter.submitList(templates)
                        emptyView.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (templates.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                else -> {
                    database.codeTemplateDao().getAllTemplates().collectLatest { templates ->
                        adapter.submitList(templates)
                        emptyView.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (templates.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }
    
    private fun searchTemplates(query: String) {
        lifecycleScope.launch {
            database.codeTemplateDao().search(query).collectLatest { templates: List<CodeTemplate> ->
                adapter.submitList(templates)
                emptyView.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (templates.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
    
    private fun useTemplate(template: CodeTemplate) {
        lifecycleScope.launch {
            database.codeTemplateDao().incrementUsage(template.id)
        }
        
        // Check if there are variables to replace
        if (!template.variables.isNullOrEmpty()) {
            showVariableReplacementDialog(template)
        } else {
            sendTemplateToMain(template.code)
        }
    }
    
    private fun showVariableReplacementDialog(template: CodeTemplate) {
        // Parse variables and show replacement dialog
        try {
            val variablesJson = org.json.JSONArray(template.variables)
            val variableViews = mutableListOf<Pair<String, EditText>>()
            
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_template_variables, null)
            val container = dialogView.findViewById<LinearLayout>(R.id.variablesContainer)
            
            for (i in 0 until variablesJson.length()) {
                val varObj = variablesJson.getJSONObject(i)
                val name = varObj.getString("name")
                val placeholder = varObj.optString("placeholder", name)
                
                val itemView = LayoutInflater.from(this).inflate(R.layout.item_variable_input, container, false)
                val label = itemView.findViewById<TextView>(R.id.tvVariableName)
                val input = itemView.findViewById<EditText>(R.id.etVariableValue)
                
                label.text = name
                input.hint = placeholder
                
                container.addView(itemView)
                variableViews.add(name to input)
            }
            
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.replace_variables))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.use_template)) { _, _ ->
                    var code = template.code
                    variableViews.forEach { (name, editText) ->
                        val value = editText.text.toString().ifEmpty { name }
                        code = code.replace("{{$name}}", value)
                        code = code.replace("\${$name}", value)
                    }
                    sendTemplateToMain(code)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } catch (_: Exception) {
            sendTemplateToMain(template.code)
        }
    }
    
    private fun sendTemplateToMain(code: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("template_code", code)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        Toast.makeText(this, getString(R.string.template_applied), Toast.LENGTH_SHORT).show()
    }
    
    private fun copyTemplate(template: CodeTemplate) {
        lifecycleScope.launch {
            database.codeTemplateDao().incrementUsage(template.id)
        }
        
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Code Template", template.code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleFavorite(template: CodeTemplate) {
        lifecycleScope.launch {
            database.codeTemplateDao().setFavorite(template.id, !template.isFavorite)
        }
    }
    
    private fun showAddTemplateDialog() {
        showTemplateDialog(null)
    }
    
    private fun showEditTemplateDialog(template: CodeTemplate) {
        showTemplateDialog(template)
    }
    
    private fun showTemplateDialog(template: CodeTemplate?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_template, null)
        
        val etTitle = dialogView.findViewById<EditText>(R.id.etTemplateTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etTemplateDescription)
        val etCode = dialogView.findViewById<EditText>(R.id.etTemplateCode)
        val spinnerLanguage = dialogView.findViewById<Spinner>(R.id.spinnerLanguage)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val etTags = dialogView.findViewById<EditText>(R.id.etTemplateTags)
        
        // Setup language spinner
        val languages = arrayOf("Kotlin", "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Swift", "Go", "Rust", "SQL", "HTML", "CSS", "XML", "JSON")
        spinnerLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        
        // Setup category spinner
        val categories = arrayOf("android", "algorithm", "web", "api", "database", "utility", "other")
        val categoryNames = arrayOf("Android", "Algoritma", "Web", "API", "Veritabanı", "Yardımcı", "Diğer")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        
        // Fill existing data if editing
        template?.let {
            etTitle.setText(it.title)
            etDescription.setText(it.description)
            etCode.setText(it.code)
            etTags.setText(it.tags)
            
            languages.indexOfFirst { lang -> lang.equals(it.language, ignoreCase = true) }.let { idx ->
                if (idx >= 0) spinnerLanguage.setSelection(idx)
            }
            
            categories.indexOfFirst { cat -> cat == it.category }.let { idx ->
                if (idx >= 0) spinnerCategory.setSelection(idx)
            }
        }
        
        val title = if (template == null) getString(R.string.add_template) else getString(R.string.edit_template)
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save_template)) { _, _ ->
                val titleText = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val code = etCode.text.toString()
                val language = languages[spinnerLanguage.selectedItemPosition]
                val category = categories[spinnerCategory.selectedItemPosition]
                val tags = etTags.text.toString().trim()
                
                if (titleText.isEmpty() || code.isEmpty()) {
                    Toast.makeText(this, getString(R.string.template_title_code_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                lifecycleScope.launch {
                    val newTemplate = CodeTemplate(
                        id = template?.id ?: 0,
                        title = titleText,
                        description = description,
                        code = code,
                        language = language,
                        category = category,
                        tags = tags.ifEmpty { null },
                        isBuiltIn = false,
                        isFavorite = template?.isFavorite ?: false,
                        usageCount = template?.usageCount ?: 0,
                        createdAt = template?.createdAt ?: System.currentTimeMillis()
                    )
                    
                    if (template == null) {
                        database.codeTemplateDao().insert(newTemplate)
                        Toast.makeText(this@CodeTemplatesActivity, getString(R.string.template_saved), Toast.LENGTH_SHORT).show()
                    } else {
                        database.codeTemplateDao().update(newTemplate)
                        Toast.makeText(this@CodeTemplatesActivity, getString(R.string.template_updated), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun confirmDelete(template: CodeTemplate) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_template_title))
            .setMessage(getString(R.string.delete_template_confirm, template.title))
            .setPositiveButton(getString(R.string.delete_snippet)) { _, _ ->
                lifecycleScope.launch {
                    database.codeTemplateDao().delete(template)
                    Toast.makeText(this@CodeTemplatesActivity, getString(R.string.template_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun insertDefaultTemplatesIfNeeded() {
        lifecycleScope.launch {
            val count = database.codeTemplateDao().getCount()
            if (count == 0) {
                val defaultTemplates = getDefaultTemplates()
                database.codeTemplateDao().insertAll(defaultTemplates)
            }
        }
    }
    
    private fun getDefaultTemplates(): List<CodeTemplate> {
        val now = System.currentTimeMillis()
        return listOf(
            // Android Templates
            CodeTemplate(
                title = "RecyclerView Adapter",
                description = "Kotlin RecyclerView Adapter şablonu",
                code = """class {{AdapterName}}(
    private val items: List<{{ItemType}}>,
    private val onItemClick: ({{ItemType}}) -> Unit
) : RecyclerView.Adapter<{{AdapterName}}.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_{{layout_name}}, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount() = items.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: {{ItemType}}) {
            itemView.setOnClickListener { onItemClick(item) }
            // TODO: Bind views
        }
    }
}""",
                language = "Kotlin",
                category = "android",
                tags = "recyclerview, adapter, list",
                variables = """[{"name":"AdapterName","placeholder":"MyAdapter"},{"name":"ItemType","placeholder":"Item"},{"name":"layout_name","placeholder":"my_item"}]""",
                isBuiltIn = true,
                createdAt = now
            ),
            CodeTemplate(
                title = "ViewModel",
                description = "Android ViewModel şablonu",
                code = """class {{ViewModelName}} : ViewModel() {
    private val _state = MutableStateFlow({{StateName}}())
    val state = _state.asStateFlow()
    
    private val _events = Channel<{{EventName}}>()
    val events = _events.receiveAsFlow()
    
    fun onAction(action: {{ActionName}}) {
        when (action) {
            // Handle actions
        }
    }
    
    private fun updateState(update: ({{StateName}}) -> {{StateName}}) {
        _state.update(update)
    }
    
    private fun sendEvent(event: {{EventName}}) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
}

data class {{StateName}}(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class {{ActionName}} {
    // Define actions
}

sealed class {{EventName}} {
    // Define events
}""",
                language = "Kotlin",
                category = "android",
                tags = "viewmodel, mvvm, architecture",
                variables = """[{"name":"ViewModelName","placeholder":"MyViewModel"},{"name":"StateName","placeholder":"MyState"},{"name":"ActionName","placeholder":"MyAction"},{"name":"EventName","placeholder":"MyEvent"}]""",
                isBuiltIn = true,
                createdAt = now
            ),
            CodeTemplate(
                title = "Room Entity",
                description = "Room veritabanı entity şablonu",
                code = """@Entity(tableName = "{{table_name}}")
data class {{EntityName}}(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "{{column1}}")
    val {{field1}}: String,
    
    @ColumnInfo(name = "{{column2}}")
    val {{field2}}: Int = 0,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)""",
                language = "Kotlin",
                category = "database",
                tags = "room, entity, database",
                variables = """[{"name":"EntityName","placeholder":"MyEntity"},{"name":"table_name","placeholder":"my_table"},{"name":"column1","placeholder":"name"},{"name":"field1","placeholder":"name"},{"name":"column2","placeholder":"count"},{"name":"field2","placeholder":"count"}]""",
                isBuiltIn = true,
                createdAt = now
            ),
            
            // Algorithm Templates
            CodeTemplate(
                title = "Binary Search",
                description = "İkili arama algoritması",
                code = """fun binarySearch(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1
    
    while (left <= right) {
        val mid = left + (right - left) / 2
        
        when {
            arr[mid] == target -> return mid
            arr[mid] < target -> left = mid + 1
            else -> right = mid - 1
        }
    }
    
    return -1 // Not found
}""",
                language = "Kotlin",
                category = "algorithm",
                tags = "search, binary, sorting",
                isBuiltIn = true,
                createdAt = now
            ),
            CodeTemplate(
                title = "Quick Sort",
                description = "Hızlı sıralama algoritması",
                code = """fun quickSort(arr: IntArray, low: Int = 0, high: Int = arr.size - 1) {
    if (low < high) {
        val pivotIndex = partition(arr, low, high)
        quickSort(arr, low, pivotIndex - 1)
        quickSort(arr, pivotIndex + 1, high)
    }
}

private fun partition(arr: IntArray, low: Int, high: Int): Int {
    val pivot = arr[high]
    var i = low - 1
    
    for (j in low until high) {
        if (arr[j] <= pivot) {
            i++
            arr[i] = arr[j].also { arr[j] = arr[i] }
        }
    }
    
    arr[i + 1] = arr[high].also { arr[high] = arr[i + 1] }
    return i + 1
}""",
                language = "Kotlin",
                category = "algorithm",
                tags = "sort, quicksort, array",
                isBuiltIn = true,
                createdAt = now
            ),
            
            // API Templates
            CodeTemplate(
                title = "Retrofit Service",
                description = "Retrofit API servis interface'i",
                code = """interface {{ServiceName}} {
    @GET("{{endpoint}}")
    suspend fun getAll(): Response<List<{{ResponseType}}>>
    
    @GET("{{endpoint}}/{id}")
    suspend fun getById(@Path("id") id: Long): Response<{{ResponseType}}>
    
    @POST("{{endpoint}}")
    suspend fun create(@Body item: {{RequestType}}): Response<{{ResponseType}}>
    
    @PUT("{{endpoint}}/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body item: {{RequestType}}
    ): Response<{{ResponseType}}>
    
    @DELETE("{{endpoint}}/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}""",
                language = "Kotlin",
                category = "api",
                tags = "retrofit, api, network",
                variables = """[{"name":"ServiceName","placeholder":"MyApiService"},{"name":"endpoint","placeholder":"items"},{"name":"ResponseType","placeholder":"ItemResponse"},{"name":"RequestType","placeholder":"ItemRequest"}]""",
                isBuiltIn = true,
                createdAt = now
            ),
            
            // Web Templates
            CodeTemplate(
                title = "HTML Boilerplate",
                description = "Temel HTML şablonu",
                code = """<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{title}}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
    </style>
</head>
<body>
    <header>
        <h1>{{title}}</h1>
    </header>
    
    <main>
        <!-- Content here -->
    </main>
    
    <footer>
        <p>&copy; {{year}} {{author}}</p>
    </footer>
    
    <script>
        // JavaScript here
    </script>
</body>
</html>""",
                language = "HTML",
                category = "web",
                tags = "html, boilerplate, template",
                variables = """[{"name":"title","placeholder":"My Page"},{"name":"year","placeholder":"2024"},{"name":"author","placeholder":"Author Name"}]""",
                isBuiltIn = true,
                createdAt = now
            ),
            
            // Utility Templates
            CodeTemplate(
                title = "Singleton Pattern",
                description = "Kotlin Singleton (Object) pattern",
                code = """object {{SingletonName}} {
    private var initialized = false
    
    fun initialize() {
        if (initialized) return
        // Initialization logic
        initialized = true
    }
    
    fun doSomething(): {{ReturnType}} {
        // Implementation
    }
}""",
                language = "Kotlin",
                category = "utility",
                tags = "singleton, pattern, design",
                variables = """[{"name":"SingletonName","placeholder":"MySingleton"},{"name":"ReturnType","placeholder":"Unit"}]""",
                isBuiltIn = true,
                createdAt = now
            ),
            CodeTemplate(
                title = "Extension Function",
                description = "Kotlin extension function şablonu",
                code = """fun {{ReceiverType}}.{{functionName}}({{params}}): {{ReturnType}} {
    // Implementation
    return {{returnValue}}
}

// Usage:
// val result = {{receiverInstance}}.{{functionName}}()""",
                language = "Kotlin",
                category = "utility",
                tags = "extension, kotlin, utility",
                variables = """[{"name":"ReceiverType","placeholder":"String"},{"name":"functionName","placeholder":"myExtension"},{"name":"params","placeholder":""},{"name":"ReturnType","placeholder":"Boolean"},{"name":"returnValue","placeholder":"true"},{"name":"receiverInstance","placeholder":"myString"}]""",
                isBuiltIn = true,
                createdAt = now
            ),
            CodeTemplate(
                title = "Coroutine Flow",
                description = "Kotlin Flow pattern",
                code = """fun {{flowName}}(): Flow<{{DataType}}> = flow {
    try {
        // Emit loading state
        emit(Resource.Loading())
        
        // Fetch data
        val result = {{repository}}.getData()
        
        // Emit success
        emit(Resource.Success(result))
    } catch (e: Exception) {
        emit(Resource.Error(e.message ?: "Unknown error"))
    }
}.flowOn(Dispatchers.IO)

sealed class Resource<T> {
    class Loading<T> : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
}""",
                language = "Kotlin",
                category = "utility",
                tags = "flow, coroutine, async",
                variables = """[{"name":"flowName","placeholder":"getDataFlow"},{"name":"DataType","placeholder":"List<Item>"},{"name":"repository","placeholder":"repository"}]""",
                isBuiltIn = true,
                createdAt = now
            )
        )
    }
}
