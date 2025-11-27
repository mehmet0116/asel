package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout

class CssLayoutGuideActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    
    private var flexboxProperties = listOf<CssProperty>()
    private var gridProperties = listOf<CssProperty>()
    private var currentTab = 0
    
    data class CssProperty(
        val name: String,
        val values: List<String>,
        val description: String,
        val example: String,
        val category: String
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_css_layout_guide)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "CSS Layout Rehberi"
        
        initViews()
        loadProperties()
        setupTabs()
        setupSearch()
        updateList()
    }
    
    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerProperties)
        searchInput = findViewById(R.id.searchInput)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun loadProperties() {
        flexboxProperties = listOf(
            // Container Properties
            CssProperty(
                "display: flex",
                listOf("flex", "inline-flex"),
                "Flex container oluşturur",
                ".container {\n  display: flex;\n}",
                "Container"
            ),
            CssProperty(
                "flex-direction",
                listOf("row", "row-reverse", "column", "column-reverse"),
                "Ana eksen yönünü belirler",
                ".container {\n  flex-direction: row;\n}",
                "Container"
            ),
            CssProperty(
                "flex-wrap",
                listOf("nowrap", "wrap", "wrap-reverse"),
                "Öğelerin satır değiştirmesini kontrol eder",
                ".container {\n  flex-wrap: wrap;\n}",
                "Container"
            ),
            CssProperty(
                "justify-content",
                listOf("flex-start", "flex-end", "center", "space-between", "space-around", "space-evenly"),
                "Ana eksende hizalamayı belirler",
                ".container {\n  justify-content: center;\n}",
                "Container"
            ),
            CssProperty(
                "align-items",
                listOf("stretch", "flex-start", "flex-end", "center", "baseline"),
                "Çapraz eksende hizalamayı belirler",
                ".container {\n  align-items: center;\n}",
                "Container"
            ),
            CssProperty(
                "align-content",
                listOf("stretch", "flex-start", "flex-end", "center", "space-between", "space-around"),
                "Çoklu satır hizalamasını belirler",
                ".container {\n  align-content: space-between;\n}",
                "Container"
            ),
            CssProperty(
                "gap",
                listOf("10px", "1rem", "10px 20px"),
                "Öğeler arası boşluğu belirler",
                ".container {\n  gap: 10px;\n}",
                "Container"
            ),
            
            // Item Properties
            CssProperty(
                "flex-grow",
                listOf("0", "1", "2"),
                "Öğenin büyüme oranını belirler",
                ".item {\n  flex-grow: 1;\n}",
                "Item"
            ),
            CssProperty(
                "flex-shrink",
                listOf("0", "1", "2"),
                "Öğenin küçülme oranını belirler",
                ".item {\n  flex-shrink: 0;\n}",
                "Item"
            ),
            CssProperty(
                "flex-basis",
                listOf("auto", "0", "100px", "50%"),
                "Öğenin başlangıç boyutunu belirler",
                ".item {\n  flex-basis: 200px;\n}",
                "Item"
            ),
            CssProperty(
                "flex",
                listOf("0 1 auto", "1", "1 1 0", "none"),
                "grow, shrink ve basis kısayolu",
                ".item {\n  flex: 1;\n}",
                "Item"
            ),
            CssProperty(
                "align-self",
                listOf("auto", "flex-start", "flex-end", "center", "baseline", "stretch"),
                "Tek öğeyi çapraz eksende hizalar",
                ".item {\n  align-self: center;\n}",
                "Item"
            ),
            CssProperty(
                "order",
                listOf("-1", "0", "1", "2"),
                "Öğenin sırasını belirler",
                ".item {\n  order: -1;\n}",
                "Item"
            )
        )
        
        gridProperties = listOf(
            // Container Properties
            CssProperty(
                "display: grid",
                listOf("grid", "inline-grid"),
                "Grid container oluşturur",
                ".container {\n  display: grid;\n}",
                "Container"
            ),
            CssProperty(
                "grid-template-columns",
                listOf("100px 200px", "1fr 2fr", "repeat(3, 1fr)", "auto-fill", "minmax(100px, 1fr)"),
                "Sütun yapısını tanımlar",
                ".container {\n  grid-template-columns: repeat(3, 1fr);\n}",
                "Container"
            ),
            CssProperty(
                "grid-template-rows",
                listOf("100px 200px", "1fr 2fr", "repeat(3, 1fr)", "auto", "minmax(100px, auto)"),
                "Satır yapısını tanımlar",
                ".container {\n  grid-template-rows: 100px auto 100px;\n}",
                "Container"
            ),
            CssProperty(
                "grid-template-areas",
                listOf("\"header header\" \"sidebar main\" \"footer footer\""),
                "İsimlendirilmiş alan tanımlar",
                ".container {\n  grid-template-areas:\n    \"header header\"\n    \"sidebar main\"\n    \"footer footer\";\n}",
                "Container"
            ),
            CssProperty(
                "gap / grid-gap",
                listOf("10px", "10px 20px", "1rem"),
                "Satır ve sütun arasındaki boşluk",
                ".container {\n  gap: 20px;\n}",
                "Container"
            ),
            CssProperty(
                "justify-items",
                listOf("start", "end", "center", "stretch"),
                "Grid hücrelerinde yatay hizalama",
                ".container {\n  justify-items: center;\n}",
                "Container"
            ),
            CssProperty(
                "align-items",
                listOf("start", "end", "center", "stretch"),
                "Grid hücrelerinde dikey hizalama",
                ".container {\n  align-items: center;\n}",
                "Container"
            ),
            CssProperty(
                "justify-content",
                listOf("start", "end", "center", "stretch", "space-around", "space-between", "space-evenly"),
                "Grid'i container içinde yatay konumlandırır",
                ".container {\n  justify-content: center;\n}",
                "Container"
            ),
            CssProperty(
                "align-content",
                listOf("start", "end", "center", "stretch", "space-around", "space-between", "space-evenly"),
                "Grid'i container içinde dikey konumlandırır",
                ".container {\n  align-content: center;\n}",
                "Container"
            ),
            CssProperty(
                "grid-auto-flow",
                listOf("row", "column", "row dense", "column dense"),
                "Otomatik yerleşim algoritması",
                ".container {\n  grid-auto-flow: dense;\n}",
                "Container"
            ),
            
            // Item Properties
            CssProperty(
                "grid-column",
                listOf("1", "1 / 3", "1 / span 2", "1 / -1"),
                "Öğenin sütun konumunu belirler",
                ".item {\n  grid-column: 1 / 3;\n}",
                "Item"
            ),
            CssProperty(
                "grid-row",
                listOf("1", "1 / 3", "1 / span 2", "1 / -1"),
                "Öğenin satır konumunu belirler",
                ".item {\n  grid-row: 1 / span 2;\n}",
                "Item"
            ),
            CssProperty(
                "grid-area",
                listOf("header", "sidebar", "1 / 1 / 3 / 3"),
                "Öğeyi isimli alana veya konuma yerleştirir",
                ".item {\n  grid-area: header;\n}",
                "Item"
            ),
            CssProperty(
                "justify-self",
                listOf("start", "end", "center", "stretch"),
                "Tek öğeyi hücrede yatay hizalar",
                ".item {\n  justify-self: center;\n}",
                "Item"
            ),
            CssProperty(
                "align-self",
                listOf("start", "end", "center", "stretch"),
                "Tek öğeyi hücrede dikey hizalar",
                ".item {\n  align-self: center;\n}",
                "Item"
            ),
            CssProperty(
                "place-self",
                listOf("center", "start end", "center stretch"),
                "align-self ve justify-self kısayolu",
                ".item {\n  place-self: center;\n}",
                "Item"
            )
        )
    }
    
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Flexbox"))
        tabLayout.addTab(tabLayout.newTab().setText("Grid"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun updateList() {
        val searchQuery = searchInput.text.toString().lowercase()
        val properties = if (currentTab == 0) flexboxProperties else gridProperties
        
        val filtered = if (searchQuery.isEmpty()) {
            properties
        } else {
            properties.filter { 
                it.name.lowercase().contains(searchQuery) ||
                it.description.lowercase().contains(searchQuery)
            }
        }
        
        recyclerView.adapter = PropertyAdapter(filtered)
    }
    
    inner class PropertyAdapter(private val properties: List<CssProperty>) : 
        RecyclerView.Adapter<PropertyAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPropertyName)
            val tvValues: TextView = view.findViewById(R.id.tvValues)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
            val tvExample: TextView = view.findViewById(R.id.tvExample)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_css_property, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val property = properties[position]
            
            holder.tvName.text = property.name
            holder.tvValues.text = "Değerler: ${property.values.joinToString(" | ")}"
            holder.tvDescription.text = property.description
            holder.tvExample.text = property.example
            holder.tvCategory.text = property.category
            
            holder.btnCopy.setOnClickListener {
                copyToClipboard(property.example)
                Toast.makeText(this@CssLayoutGuideActivity, "Kopyalandı", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun getItemCount() = properties.size
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("css", text)
        clipboard.setPrimaryClip(clip)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
