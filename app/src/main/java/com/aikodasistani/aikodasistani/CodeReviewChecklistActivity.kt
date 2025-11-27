package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout

class CodeReviewChecklistActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var progressText: TextView
    
    private val checkedItems = mutableSetOf<String>()
    private var currentCategory = "all"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_review_checklist)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.code_review_checklist_title)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        progressIndicator = findViewById(R.id.progressIndicator)
        progressText = findViewById(R.id.progressText)
        
        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("Tümü"))
        tabLayout.addTab(tabLayout.newTab().setText("Kod Kalitesi"))
        tabLayout.addTab(tabLayout.newTab().setText("Güvenlik"))
        tabLayout.addTab(tabLayout.newTab().setText("Performans"))
        tabLayout.addTab(tabLayout.newTab().setText("Test"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentCategory = when (tab?.position) {
                    0 -> "all"
                    1 -> "quality"
                    2 -> "security"
                    3 -> "performance"
                    4 -> "testing"
                    else -> "all"
                }
                updateList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        updateList()
    }
    
    private fun updateList() {
        val items = getChecklistItems().filter { 
            currentCategory == "all" || it.category == currentCategory 
        }
        recyclerView.adapter = ChecklistAdapter(items)
        updateProgress()
    }
    
    private fun updateProgress() {
        val allItems = getChecklistItems()
        val checkedCount = allItems.count { checkedItems.contains(it.id) }
        val totalCount = allItems.size
        val percentage = if (totalCount > 0) (checkedCount * 100 / totalCount) else 0
        
        progressIndicator.progress = percentage
        progressText.text = "$checkedCount / $totalCount tamamlandı (%$percentage)"
    }
    
    private fun getChecklistItems(): List<ChecklistItem> {
        return listOf(
            // Kod Kalitesi
            ChecklistItem("q1", "quality", "📝 Değişken isimleri açıklayıcı mı?", 
                "Değişkenler ne yaptığını anlatan isimler kullanmalı. Tek harfli isimlerden kaçının."),
            ChecklistItem("q2", "quality", "📝 Fonksiyonlar tek bir iş mi yapıyor?", 
                "Single Responsibility Principle - Her fonksiyon sadece bir işi yapmalı."),
            ChecklistItem("q3", "quality", "📝 Magic number/string kullanılmış mı?",
                "Sabit değerler const veya companion object içinde tanımlanmalı."),
            ChecklistItem("q4", "quality", "📝 Kod tekrarı var mı (DRY)?",
                "Don't Repeat Yourself - Aynı kod birden fazla yerde kullanılıyorsa refactor edilmeli."),
            ChecklistItem("q5", "quality", "📝 Yorum satırları gerekli ve güncel mi?",
                "Gereksiz yorumları kaldırın, karmaşık logic'i açıklayan yorumlar ekleyin."),
            ChecklistItem("q6", "quality", "📝 Kod formatı tutarlı mı?",
                "Proje genelinde aynı kod stili kullanılmalı (indentation, boşluklar)."),
            ChecklistItem("q7", "quality", "📝 Null safety sağlanmış mı?",
                "Kotlin'de !! kullanımından kaçının, null kontrollerini yapın."),
            ChecklistItem("q8", "quality", "📝 Error handling düzgün yapılmış mı?",
                "Exception'lar yakalanmalı ve anlamlı hata mesajları verilmeli."),
            ChecklistItem("q9", "quality", "📝 Karmaşık fonksiyonlar parçalanmış mı?",
                "Uzun fonksiyonlar daha küçük, yönetilebilir parçalara bölünmeli."),
            ChecklistItem("q10", "quality", "📝 Gereksiz kod (dead code) var mı?",
                "Kullanılmayan değişkenler, fonksiyonlar ve importlar kaldırılmalı."),
            
            // Güvenlik
            ChecklistItem("s1", "security", "🔒 API anahtarları hardcoded değil mi?",
                "Sensitive bilgiler local.properties veya environment variable'da olmalı."),
            ChecklistItem("s2", "security", "🔒 SQL Injection koruması var mı?",
                "Prepared statements veya parameterized queries kullanılmalı."),
            ChecklistItem("s3", "security", "🔒 Input validation yapılıyor mu?",
                "Kullanıcı girdileri doğrulanmalı ve sanitize edilmeli."),
            ChecklistItem("s4", "security", "🔒 Sensitive data loglanmıyor mu?",
                "Şifreler, tokenlar ve kişisel bilgiler loglara yazılmamalı."),
            ChecklistItem("s5", "security", "🔒 HTTPS kullanılıyor mu?",
                "Tüm network istekleri şifreli bağlantı üzerinden yapılmalı."),
            ChecklistItem("s6", "security", "🔒 File permissions doğru mu?",
                "Dosyalar sadece gerekli izinlerle oluşturulmalı."),
            ChecklistItem("s7", "security", "🔒 Authentication/Authorization kontrolleri var mı?",
                "Her endpoint ve işlem için yetki kontrolü yapılmalı."),
            ChecklistItem("s8", "security", "🔒 Dependency'lerde güvenlik açığı var mı?",
                "Kullanılan kütüphaneler güncel ve güvenli olmalı."),
            
            // Performans
            ChecklistItem("p1", "performance", "⚡ N+1 query problemi var mı?",
                "Döngü içinde database sorgusu yapılmamalı, batch işlemler tercih edilmeli."),
            ChecklistItem("p2", "performance", "⚡ Heavy işlemler main thread'de mi?",
                "Network, DB ve dosya işlemleri background thread'de yapılmalı."),
            ChecklistItem("p3", "performance", "⚡ Memory leak potansiyeli var mı?",
                "Context referansları, listener'lar ve callback'ler düzgün temizlenmeli."),
            ChecklistItem("p4", "performance", "⚡ Unnecessary object creation var mı?",
                "Döngü içinde gereksiz nesne oluşturmaktan kaçının."),
            ChecklistItem("p5", "performance", "⚡ RecyclerView ViewHolder pattern kullanılıyor mu?",
                "Listeler için RecyclerView ve proper ViewHolder kullanılmalı."),
            ChecklistItem("p6", "performance", "⚡ Image caching yapılıyor mu?",
                "Resimler Glide/Coil ile cache'lenmeli."),
            ChecklistItem("p7", "performance", "⚡ Lazy loading uygulanmış mı?",
                "Büyük veri setleri için pagination kullanılmalı."),
            ChecklistItem("p8", "performance", "⚡ Unnecessary recomposition var mı?",
                "Compose'da gereksiz recomposition'dan kaçının."),
            
            // Test
            ChecklistItem("t1", "testing", "🧪 Unit testler yazılmış mı?",
                "Critical iş mantığı için unit testler olmalı."),
            ChecklistItem("t2", "testing", "🧪 Edge case'ler test edilmiş mi?",
                "Boş liste, null değer, sınır değerler test edilmeli."),
            ChecklistItem("t3", "testing", "🧪 Mocking düzgün yapılmış mı?",
                "Bağımlılıklar mock'lanarak izole testler yazılmalı."),
            ChecklistItem("t4", "testing", "🧪 Test coverage yeterli mi?",
                "Kritik code path'ler test edilmeli, minimum %80 coverage hedefleyin."),
            ChecklistItem("t5", "testing", "🧪 Integration testler var mı?",
                "Farklı modüllerin birlikte çalışması test edilmeli."),
            ChecklistItem("t6", "testing", "🧪 UI testleri yazılmış mı?",
                "Kritik kullanıcı flow'ları için Espresso/Compose testleri olmalı."),
            ChecklistItem("t7", "testing", "🧪 Error senaryoları test edilmiş mi?",
                "Network hatası, timeout gibi hata durumları test edilmeli."),
            ChecklistItem("t8", "testing", "🧪 Testler bağımsız mı?",
                "Her test diğerlerinden bağımsız olarak çalışabilmeli.")
        )
    }
    
    data class ChecklistItem(
        val id: String,
        val category: String,
        val title: String,
        val description: String
    )
    
    inner class ChecklistAdapter(
        private val items: List<ChecklistItem>
    ) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.checkBox)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_checklist, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            
            holder.checkBox.text = item.title
            holder.checkBox.isChecked = checkedItems.contains(item.id)
            holder.descriptionText.text = item.description
            
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkedItems.add(item.id)
                } else {
                    checkedItems.remove(item.id)
                }
                updateProgress()
            }
            
            holder.itemView.setOnClickListener {
                holder.descriptionText.visibility = 
                    if (holder.descriptionText.visibility == View.VISIBLE) 
                        View.GONE 
                    else 
                        View.VISIBLE
            }
        }
        
        override fun getItemCount() = items.size
    }
}
