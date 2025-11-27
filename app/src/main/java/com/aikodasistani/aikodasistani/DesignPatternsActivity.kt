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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class DesignPatternsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var categoryChips: ChipGroup
    
    private var allPatterns = listOf<DesignPattern>()
    private var filteredPatterns = listOf<DesignPattern>()
    private var currentCategory = "Tümü"
    
    data class DesignPattern(
        val name: String,
        val category: String,
        val intent: String,
        val problem: String,
        val solution: String,
        val codeExample: String,
        val useCase: List<String>
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_design_patterns)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tasarım Kalıpları"
        
        initViews()
        loadPatterns()
        setupSearch()
        setupCategories()
        updateList()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerPatterns)
        searchInput = findViewById(R.id.searchInput)
        categoryChips = findViewById(R.id.categoryChips)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun loadPatterns() {
        allPatterns = listOf(
            // Creational Patterns
            DesignPattern(
                "Singleton",
                "Creational",
                "Bir sınıfın sadece bir örneğinin olmasını ve global erişim noktası sağlamayı garanti eder.",
                "Uygulama genelinde tek bir paylaşılan kaynak gerektiğinde.",
                "Özel constructor ve static metod ile tek örnek oluşturulur.",
                """object DatabaseConnection {
    private var connection: Connection? = null
    
    fun getConnection(): Connection {
        if (connection == null) {
            connection = createConnection()
        }
        return connection!!
    }
}""",
                listOf("Veritabanı bağlantıları", "Logger", "Konfigürasyon yöneticisi")
            ),
            DesignPattern(
                "Factory Method",
                "Creational",
                "Nesne oluşturma mantığını alt sınıflara bırakır.",
                "Oluşturulacak nesnenin türü çalışma zamanında belirlenmeli.",
                "Abstract factory metodu ile alt sınıflar kendi nesnelerini oluşturur.",
                """interface Product {
    fun operation(): String
}

abstract class Creator {
    abstract fun createProduct(): Product
}

class ConcreteCreatorA : Creator() {
    override fun createProduct() = ConcreteProductA()
}""",
                listOf("UI bileşenleri", "Belge oluşturucular")
            ),
            DesignPattern(
                "Builder",
                "Creational",
                "Karmaşık nesnelerin adım adım oluşturulmasını sağlar.",
                "Nesne çok sayıda isteğe bağlı parametreye sahip olduğunda.",
                "Builder sınıfı ile ayrı ayrı adımlarla nesne oluşturulur.",
                """class UserBuilder {
    private var name: String = ""
    private var email: String = ""
    
    fun name(name: String) = apply { this.name = name }
    fun email(email: String) = apply { this.email = email }
    fun build() = User(name, email)
}

// Kullanım
val user = UserBuilder().name("Ali").email("ali@mail.com").build()""",
                listOf("Kompleks nesneler", "HTTP istekleri", "Dialog builder")
            ),
            
            // Structural Patterns
            DesignPattern(
                "Adapter",
                "Structural",
                "Uyumsuz arayüzlerin birlikte çalışmasını sağlar.",
                "Mevcut bir sınıfı beklenen arayüze uydurmak gerektiğinde.",
                "Wrapper sınıfı ile kaynak arayüz hedef arayüze dönüştürülür.",
                """interface ModernPrinter {
    fun print(document: Document)
}

class PrinterAdapter(private val oldPrinter: OldPrinter) : ModernPrinter {
    override fun print(document: Document) {
        oldPrinter.printText(document.content)
    }
}""",
                listOf("Legacy kod entegrasyonu", "3rd party kütüphaneler")
            ),
            DesignPattern(
                "Decorator",
                "Structural",
                "Nesnelere dinamik olarak yeni davranışlar ekler.",
                "Sınıf hiyerarşisi olmadan işlevsellik eklemek gerektiğinde.",
                "Wrapper nesneler ile orijinal nesnenin davranışı genişletilir.",
                """interface Coffee {
    fun cost(): Double
}

class MilkDecorator(private val coffee: Coffee) : Coffee {
    override fun cost() = coffee.cost() + 2.0
}

// Kullanım
val coffee = MilkDecorator(SimpleCoffee())""",
                listOf("I/O streams", "GUI bileşenleri", "Logging")
            ),
            DesignPattern(
                "Facade",
                "Structural",
                "Karmaşık bir alt sisteme basit bir arayüz sağlar.",
                "Karmaşık işlemleri basitleştirmek gerektiğinde.",
                "Tek bir sınıf ile alt sistem çağrıları kapsüllenir.",
                """class VideoConverter {
    fun convert(filename: String, format: String): File {
        val file = VideoFile(filename)
        val codec = CodecFactory.extract(file)
        return AudioMixer.fix(BitrateReader.convert(file, format))
    }
}""",
                listOf("API basitleştirme", "Microservices")
            ),
            
            // Behavioral Patterns
            DesignPattern(
                "Observer",
                "Behavioral",
                "Bir nesnenin durumu değiştiğinde bağımlı nesnelerin bilgilendirilmesini sağlar.",
                "Bir-çok bağımlılık ve olay tabanlı sistemlerde.",
                "Subject ve Observer arayüzleri ile abonelik sistemi kurulur.",
                """interface Observer {
    fun update(message: String)
}

class NewsAgency {
    private val observers = mutableListOf<Observer>()
    
    fun subscribe(observer: Observer) = observers.add(observer)
    
    fun notifyObservers(news: String) {
        observers.forEach { it.update(news) }
    }
}""",
                listOf("Event handling", "MVC/MVP", "Pub/Sub sistemleri")
            ),
            DesignPattern(
                "Strategy",
                "Behavioral",
                "Algoritmaları kapsülleyip değiştirilebilir hale getirir.",
                "Çalışma zamanında algoritma seçimi gerektiğinde.",
                "Strateji arayüzü ile farklı algoritmalar değiştirilebilir.",
                """interface PaymentStrategy {
    fun pay(amount: Double)
}

class CreditCardPayment : PaymentStrategy {
    override fun pay(amount: Double) {
        println("Kredi kartı ile ödeme")
    }
}

class ShoppingCart {
    var paymentStrategy: PaymentStrategy? = null
    fun checkout(amount: Double) = paymentStrategy?.pay(amount)
}""",
                listOf("Ödeme sistemleri", "Sıralama algoritmaları")
            ),
            DesignPattern(
                "Command",
                "Behavioral",
                "İstekleri nesne olarak kapsülleyerek geri alma imkanı sağlar.",
                "Undo/redo veya kuyruk sistemi gerektiğinde.",
                "Command nesneleri işlemleri ve gerekli verileri taşır.",
                """interface Command {
    fun execute()
    fun undo()
}

class LightOnCommand(private val light: Light) : Command {
    override fun execute() = light.on()
    override fun undo() = light.off()
}""",
                listOf("Undo/Redo", "Makrolar", "İşlem kuyruğu")
            ),
            DesignPattern(
                "State",
                "Behavioral",
                "Nesnenin iç durumu değiştiğinde davranışının değişmesini sağlar.",
                "Nesne davranışı duruma bağlı olduğunda.",
                "Durum nesneleri farklı davranışları kapsüller.",
                """interface State {
    fun handle(context: Document)
}

class DraftState : State {
    override fun handle(context: Document) {
        context.state = ReviewState()
    }
}

class Document {
    var state: State = DraftState()
    fun publish() = state.handle(this)
}""",
                listOf("Durum makineleri", "UI durumları")
            )
        )
        
        filteredPatterns = allPatterns
    }
    
    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPatterns()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupCategories() {
        val categories = listOf("Tümü", "Creational", "Structural", "Behavioral")
        
        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = when (category) {
                    "Creational" -> "Oluşturucu"
                    "Structural" -> "Yapısal"
                    "Behavioral" -> "Davranışsal"
                    else -> category
                }
                isCheckable = true
                isChecked = category == "Tümü"
                setOnClickListener {
                    currentCategory = category
                    for (i in 0 until categoryChips.childCount) {
                        (categoryChips.getChildAt(i) as? Chip)?.isChecked = false
                    }
                    this.isChecked = true
                    filterPatterns()
                }
            }
            categoryChips.addView(chip)
        }
    }
    
    private fun filterPatterns() {
        val searchQuery = searchInput.text.toString().lowercase()
        
        filteredPatterns = allPatterns.filter { pattern ->
            val matchesCategory = currentCategory == "Tümü" || pattern.category == currentCategory
            val matchesSearch = searchQuery.isEmpty() || 
                pattern.name.lowercase().contains(searchQuery) ||
                pattern.intent.lowercase().contains(searchQuery)
            matchesCategory && matchesSearch
        }
        
        updateList()
    }
    
    private fun updateList() {
        recyclerView.adapter = PatternAdapter(filteredPatterns)
    }
    
    inner class PatternAdapter(private val patterns: List<DesignPattern>) : 
        RecyclerView.Adapter<PatternAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPatternName)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvIntent: TextView = view.findViewById(R.id.tvIntent)
            val tvUseCases: TextView = view.findViewById(R.id.tvUseCases)
            val btnDetails: Button = view.findViewById(R.id.btnDetails)
            val btnCopyCode: Button = view.findViewById(R.id.btnCopyCode)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_design_pattern, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pattern = patterns[position]
            
            holder.tvName.text = pattern.name
            holder.tvCategory.text = when (pattern.category) {
                "Creational" -> "🔨 Oluşturucu"
                "Structural" -> "🏗️ Yapısal"
                "Behavioral" -> "🎭 Davranışsal"
                else -> pattern.category
            }
            holder.tvIntent.text = pattern.intent
            holder.tvUseCases.text = "Kullanım: ${pattern.useCase.joinToString(", ")}"
            
            holder.btnDetails.setOnClickListener {
                showPatternDetails(pattern)
            }
            
            holder.btnCopyCode.setOnClickListener {
                copyToClipboard(pattern.codeExample)
                Toast.makeText(this@DesignPatternsActivity, "Kod kopyalandı", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun getItemCount() = patterns.size
    }
    
    private fun showPatternDetails(pattern: DesignPattern) {
        val message = """
📝 PROBLEM:
${pattern.problem}

💡 ÇÖZÜM:
${pattern.solution}

📌 KULLANIM ALANLARI:
${pattern.useCase.joinToString("\n") { "• $it" }}

💻 KOD ÖRNEĞİ:
${pattern.codeExample}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("${pattern.name} Pattern")
            .setMessage(message)
            .setPositiveButton("Kodu Kopyala") { _, _ ->
                copyToClipboard(pattern.codeExample)
                Toast.makeText(this, "Kod kopyalandı", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Kapat", null)
            .show()
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("code", text)
        clipboard.setPrimaryClip(clip)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
