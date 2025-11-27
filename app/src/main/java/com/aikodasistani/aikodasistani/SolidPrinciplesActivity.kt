package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class SolidPrinciplesActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solid_principles)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.solid_principles_title)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SolidPrinciplesAdapter(getSolidPrinciples())
    }
    
    private fun getSolidPrinciples(): List<SolidPrinciple> {
        return listOf(
            SolidPrinciple(
                letter = "S",
                name = "Single Responsibility Principle",
                nameTr = "Tek Sorumluluk Prensibi",
                description = "Bir sınıf yalnızca bir görevi yerine getirmeli ve değişmek için yalnızca bir nedeni olmalıdır.",
                problem = "Bir sınıf birden fazla sorumluluk taşıdığında, bir değişiklik diğer sorumlulukları da etkileyebilir.",
                solution = "Her sınıfı tek bir sorumluluğa odaklayın. Farklı görevleri ayrı sınıflara bölün.",
                badExample = """
// ❌ KÖTÜ: Birden fazla sorumluluk
class User {
    fun saveToDatabase() { /* DB işlemleri */ }
    fun sendEmail() { /* E-posta gönderimi */ }
    fun generateReport() { /* Rapor oluşturma */ }
    fun validateData() { /* Veri doğrulama */ }
}
                """.trimIndent(),
                goodExample = """
// ✅ İYİ: Tek sorumluluk
class User(val name: String, val email: String)

class UserRepository {
    fun save(user: User) { /* DB işlemleri */ }
}

class EmailService {
    fun sendEmail(user: User) { /* E-posta */ }
}

class ReportGenerator {
    fun generate(user: User) { /* Rapor */ }
}
                """.trimIndent(),
                benefits = listOf(
                    "Kod daha okunabilir ve anlaşılır",
                    "Test yazması kolay",
                    "Değişiklikler izole edilir",
                    "Yeniden kullanım kolaylaşır"
                )
            ),
            SolidPrinciple(
                letter = "O",
                name = "Open/Closed Principle",
                nameTr = "Açık/Kapalı Prensibi",
                description = "Yazılım varlıkları genişlemeye açık, değişikliğe kapalı olmalıdır.",
                problem = "Yeni özellikler eklemek için mevcut kodu değiştirmek gerekiyor.",
                solution = "Abstraction ve inheritance kullanarak genişletilebilir tasarım oluşturun.",
                badExample = """
// ❌ KÖTÜ: Değişikliğe açık
class DiscountCalculator {
    fun calculate(type: String, price: Double): Double {
        return when (type) {
            "REGULAR" -> price * 0.1
            "VIP" -> price * 0.2
            "PREMIUM" -> price * 0.3
            // Yeni tip için kodu değiştirmek gerekiyor
            else -> 0.0
        }
    }
}
                """.trimIndent(),
                goodExample = """
// ✅ İYİ: Genişlemeye açık
interface DiscountStrategy {
    fun calculate(price: Double): Double
}

class RegularDiscount : DiscountStrategy {
    override fun calculate(price: Double) = price * 0.1
}

class VIPDiscount : DiscountStrategy {
    override fun calculate(price: Double) = price * 0.2
}

// Yeni tip eklemek için sadece yeni sınıf oluştur
class StudentDiscount : DiscountStrategy {
    override fun calculate(price: Double) = price * 0.15
}
                """.trimIndent(),
                benefits = listOf(
                    "Mevcut kod değişmez",
                    "Yeni özellikler güvenle eklenir",
                    "Regresyon riski azalır",
                    "Modüler yapı sağlanır"
                )
            ),
            SolidPrinciple(
                letter = "L",
                name = "Liskov Substitution Principle",
                nameTr = "Liskov Yerine Geçme Prensibi",
                description = "Alt sınıflar, üst sınıfların yerine kullanılabilmeli ve programın doğruluğunu bozmamalıdır.",
                problem = "Alt sınıf, üst sınıfın davranışını beklenmedik şekilde değiştiriyor.",
                solution = "Alt sınıflar üst sınıfın sözleşmesine (contract) uymalıdır.",
                badExample = """
// ❌ KÖTÜ: LSP ihlali
open class Rectangle {
    open var width: Int = 0
    open var height: Int = 0
    
    fun area() = width * height
}

class Square : Rectangle() {
    override var width: Int
        get() = super.width
        set(value) {
            super.width = value
            super.height = value // Beklenmedik davranış!
        }
}

// Sorun:
val rect: Rectangle = Square()
rect.width = 5
rect.height = 4
// Beklenen: 20, Gerçek: 16
                """.trimIndent(),
                goodExample = """
// ✅ İYİ: LSP uyumlu
interface Shape {
    fun area(): Int
}

class Rectangle(
    private val width: Int,
    private val height: Int
) : Shape {
    override fun area() = width * height
}

class Square(private val side: Int) : Shape {
    override fun area() = side * side
}

// Her ikisi de Shape olarak güvenle kullanılabilir
fun printArea(shape: Shape) {
    println("Alan: \${shape.area()}")
}
                """.trimIndent(),
                benefits = listOf(
                    "Polimorfizm güvenle kullanılır",
                    "Beklenmedik hatalar önlenir",
                    "Kod güvenilirliği artar",
                    "Tasarım tutarlılığı sağlanır"
                )
            ),
            SolidPrinciple(
                letter = "I",
                name = "Interface Segregation Principle",
                nameTr = "Arayüz Ayrımı Prensibi",
                description = "İstemciler kullanmadıkları arayüzlere bağımlı olmaya zorlanmamalıdır.",
                problem = "Büyük arayüzler, gereksiz metotları implement etmeye zorluyor.",
                solution = "Büyük arayüzleri daha küçük, özel arayüzlere bölün.",
                badExample = """
// ❌ KÖTÜ: Şişman arayüz
interface Worker {
    fun work()
    fun eat()
    fun sleep()
    fun attendMeeting()
    fun submitReport()
}

class Robot : Worker {
    override fun work() { /* Çalış */ }
    override fun eat() { /* Robot yemek yemez! */ }
    override fun sleep() { /* Robot uyumaz! */ }
    override fun attendMeeting() { /* ??? */ }
    override fun submitReport() { /* ??? */ }
}
                """.trimIndent(),
                goodExample = """
// ✅ İYİ: Ayrılmış arayüzler
interface Workable {
    fun work()
}

interface Feedable {
    fun eat()
}

interface Sleepable {
    fun sleep()
}

interface Reportable {
    fun submitReport()
}

class Human : Workable, Feedable, Sleepable, Reportable {
    override fun work() { /* ... */ }
    override fun eat() { /* ... */ }
    override fun sleep() { /* ... */ }
    override fun submitReport() { /* ... */ }
}

class Robot : Workable {
    override fun work() { /* Sadece çalışır */ }
}
                """.trimIndent(),
                benefits = listOf(
                    "Gereksiz bağımlılıklar önlenir",
                    "Daha esnek tasarım",
                    "Daha kolay test",
                    "Daha az kod değişikliği"
                )
            ),
            SolidPrinciple(
                letter = "D",
                name = "Dependency Inversion Principle",
                nameTr = "Bağımlılık Tersine Çevirme Prensibi",
                description = "Yüksek seviyeli modüller düşük seviyeli modüllere bağımlı olmamalı. Her ikisi de soyutlamalara bağımlı olmalıdır.",
                problem = "Yüksek seviyeli modüller doğrudan düşük seviyeli modüllere bağımlı, değişiklik zor.",
                solution = "Abstraction (interface) kullanarak bağımlılıkları tersine çevirin.",
                badExample = """
// ❌ KÖTÜ: Doğrudan bağımlılık
class MySQLDatabase {
    fun save(data: String) { /* MySQL'e kaydet */ }
}

class UserService {
    private val database = MySQLDatabase() // Doğrudan bağımlılık
    
    fun saveUser(user: String) {
        database.save(user)
    }
}

// MySQL'den PostgreSQL'e geçmek zor!
                """.trimIndent(),
                goodExample = """
// ✅ İYİ: Soyutlamaya bağımlılık
interface Database {
    fun save(data: String)
}

class MySQLDatabase : Database {
    override fun save(data: String) { /* MySQL */ }
}

class PostgreSQLDatabase : Database {
    override fun save(data: String) { /* PostgreSQL */ }
}

class UserService(private val database: Database) {
    fun saveUser(user: String) {
        database.save(user)
    }
}

// Kullanım:
val service = UserService(MySQLDatabase())
// veya
val service2 = UserService(PostgreSQLDatabase())
                """.trimIndent(),
                benefits = listOf(
                    "Gevşek bağlılık (loose coupling)",
                    "Kolay test (mock kullanımı)",
                    "Kolay değiştirilebilirlik",
                    "Dependency Injection desteği"
                )
            )
        )
    }
    
    data class SolidPrinciple(
        val letter: String,
        val name: String,
        val nameTr: String,
        val description: String,
        val problem: String,
        val solution: String,
        val badExample: String,
        val goodExample: String,
        val benefits: List<String>
    )
    
    inner class SolidPrinciplesAdapter(
        private val principles: List<SolidPrinciple>
    ) : RecyclerView.Adapter<SolidPrinciplesAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val letterText: TextView = view.findViewById(R.id.letterText)
            val nameText: TextView = view.findViewById(R.id.nameText)
            val nameTrText: TextView = view.findViewById(R.id.nameTrText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
            val problemText: TextView = view.findViewById(R.id.problemText)
            val solutionText: TextView = view.findViewById(R.id.solutionText)
            val badExampleText: TextView = view.findViewById(R.id.badExampleText)
            val goodExampleText: TextView = view.findViewById(R.id.goodExampleText)
            val benefitsText: TextView = view.findViewById(R.id.benefitsText)
            val copyBadBtn: View = view.findViewById(R.id.copyBadBtn)
            val copyGoodBtn: View = view.findViewById(R.id.copyGoodBtn)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_solid_principle, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val principle = principles[position]
            
            holder.letterText.text = principle.letter
            holder.nameText.text = principle.name
            holder.nameTrText.text = principle.nameTr
            holder.descriptionText.text = principle.description
            holder.problemText.text = "⚠️ Problem: ${principle.problem}"
            holder.solutionText.text = "✅ Çözüm: ${principle.solution}"
            holder.badExampleText.text = principle.badExample
            holder.goodExampleText.text = principle.goodExample
            holder.benefitsText.text = principle.benefits.joinToString("\n") { "• $it" }
            
            holder.copyBadBtn.setOnClickListener {
                copyToClipboard(principle.badExample)
            }
            
            holder.copyGoodBtn.setOnClickListener {
                copyToClipboard(principle.goodExample)
            }
        }
        
        override fun getItemCount() = principles.size
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Kopyalandı", Toast.LENGTH_SHORT).show()
    }
}
