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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class RegexPatternsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var categoryChips: ChipGroup
    private lateinit var testInput: EditText
    private lateinit var testPattern: EditText
    private lateinit var testResult: TextView
    private lateinit var btnTest: Button
    
    private var allPatterns = listOf<RegexPattern>()
    private var filteredPatterns = listOf<RegexPattern>()
    private var currentCategory = "Tümü"
    
    data class RegexPattern(
        val name: String,
        val pattern: String,
        val description: String,
        val category: String,
        val examples: List<String>
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_regex_patterns)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Regex Kütüphanesi"
        
        initViews()
        loadPatterns()
        setupSearch()
        setupCategories()
        setupTester()
        updateList()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerPatterns)
        searchInput = findViewById(R.id.searchInput)
        categoryChips = findViewById(R.id.categoryChips)
        testInput = findViewById(R.id.testInput)
        testPattern = findViewById(R.id.testPattern)
        testResult = findViewById(R.id.testResult)
        btnTest = findViewById(R.id.btnTest)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun loadPatterns() {
        allPatterns = listOf(
            // Doğrulama Patternleri
            RegexPattern(
                "E-posta",
                "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                "Geçerli e-posta adresini doğrular",
                "Doğrulama",
                listOf("user@example.com", "test.name@domain.co.uk")
            ),
            RegexPattern(
                "URL",
                "^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([\\/\\w .-]*)*\\/?$",
                "HTTP/HTTPS URL formatını doğrular",
                "Doğrulama",
                listOf("https://www.example.com", "http://test.org/page")
            ),
            RegexPattern(
                "Telefon (TR)",
                "^(\\+90|0)?[5][0-9]{9}$",
                "Türkiye cep telefonu numarasını doğrular",
                "Doğrulama",
                listOf("05551234567", "+905551234567")
            ),
            RegexPattern(
                "IP Adresi (IPv4)",
                "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
                "Geçerli IPv4 adresini doğrular",
                "Doğrulama",
                listOf("192.168.1.1", "10.0.0.255")
            ),
            RegexPattern(
                "IP Adresi (IPv6)",
                "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
                "Geçerli IPv6 adresini doğrular",
                "Doğrulama",
                listOf("2001:0db8:85a3:0000:0000:8a2e:0370:7334")
            ),
            RegexPattern(
                "TC Kimlik No",
                "^[1-9][0-9]{10}$",
                "11 haneli TC kimlik numarasını doğrular",
                "Doğrulama",
                listOf("12345678901")
            ),
            RegexPattern(
                "Kredi Kartı",
                "^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})$",
                "Visa, MasterCard, Amex kart numarasını doğrular",
                "Doğrulama",
                listOf("4111111111111111", "5500000000000004")
            ),
            RegexPattern(
                "IBAN (TR)",
                "^TR[0-9]{2}[0-9]{4}[0-9A-Z]{17}$",
                "Türkiye IBAN numarasını doğrular",
                "Doğrulama",
                listOf("TR330006100519786457841326")
            ),
            
            // Format Patternleri
            RegexPattern(
                "HEX Renk Kodu",
                "^#?([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$",
                "HEX renk kodunu doğrular",
                "Format",
                listOf("#FF5733", "#FFF", "A1B2C3")
            ),
            RegexPattern(
                "RGB Renk",
                "^rgb\\((\\d{1,3}),\\s*(\\d{1,3}),\\s*(\\d{1,3})\\)$",
                "RGB renk formatını doğrular",
                "Format",
                listOf("rgb(255, 128, 0)", "rgb(0, 255, 0)")
            ),
            RegexPattern(
                "Tarih (DD/MM/YYYY)",
                "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$",
                "Tarih formatını doğrular",
                "Format",
                listOf("25/12/2024", "01/01/2025")
            ),
            RegexPattern(
                "Tarih (YYYY-MM-DD)",
                "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$",
                "ISO tarih formatını doğrular",
                "Format",
                listOf("2024-12-25", "2025-01-01")
            ),
            RegexPattern(
                "Saat (HH:MM)",
                "^([01]?[0-9]|2[0-3]):[0-5][0-9]$",
                "24 saat formatını doğrular",
                "Format",
                listOf("14:30", "09:00", "23:59")
            ),
            RegexPattern(
                "UUID",
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                "UUID v4 formatını doğrular",
                "Format",
                listOf("550e8400-e29b-41d4-a716-446655440000")
            ),
            
            // Metin Patternleri
            RegexPattern(
                "Sadece Harfler",
                "^[a-zA-ZğüşöçİĞÜŞÖÇ]+$",
                "Sadece harfleri kabul eder (TR desteği)",
                "Metin",
                listOf("Merhaba", "Hello")
            ),
            RegexPattern(
                "Sadece Rakamlar",
                "^[0-9]+$",
                "Sadece rakamları kabul eder",
                "Metin",
                listOf("12345", "00001")
            ),
            RegexPattern(
                "Alfanümerik",
                "^[a-zA-Z0-9]+$",
                "Harf ve rakamları kabul eder",
                "Metin",
                listOf("abc123", "User01")
            ),
            RegexPattern(
                "Kullanıcı Adı",
                "^[a-zA-Z0-9_]{3,16}$",
                "3-16 karakter, harf, rakam ve alt çizgi",
                "Metin",
                listOf("user_name", "john123")
            ),
            RegexPattern(
                "Güçlü Şifre",
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$",
                "En az 8 karakter, büyük/küçük harf, rakam ve özel karakter",
                "Metin",
                listOf("Password1!", "Secure@123")
            ),
            RegexPattern(
                "Boşluk Temizle",
                "\\s+",
                "Fazla boşlukları yakalar",
                "Metin",
                listOf("çoklu   boşluklar")
            ),
            
            // Programlama Patternleri
            RegexPattern(
                "HTML Tag",
                "<([a-z]+)([^<]+)*(?:>(.*)<\\/\\1>|\\s+\\/>)",
                "HTML etiketlerini yakalar",
                "Programlama",
                listOf("<div>içerik</div>", "<img src=\"\" />")
            ),
            RegexPattern(
                "CSS Sınıf",
                "\\.[a-zA-Z_][a-zA-Z0-9_-]*",
                "CSS sınıf seçicilerini yakalar",
                "Programlama",
                listOf(".btn-primary", ".card_item")
            ),
            RegexPattern(
                "JavaScript Değişken",
                "\\b(var|let|const)\\s+([a-zA-Z_\$][a-zA-Z0-9_\$]*)\\b",
                "JS değişken tanımlarını yakalar",
                "Programlama",
                listOf("const name = 'test'", "let count = 0")
            ),
            RegexPattern(
                "Import Statement",
                "^import\\s+.*\\s+from\\s+['\"].*['\"];?$",
                "ES6 import ifadelerini yakalar",
                "Programlama",
                listOf("import React from 'react'")
            ),
            RegexPattern(
                "Yorum Satırı",
                "\\/\\/.*|\\/\\*[\\s\\S]*?\\*\\/",
                "Tek ve çoklu satır yorumlarını yakalar",
                "Programlama",
                listOf("// tek satır", "/* çoklu */")
            ),
            RegexPattern(
                "Fonksiyon Tanımı",
                "function\\s+([a-zA-Z_\$][a-zA-Z0-9_\$]*)\\s*\\(",
                "Fonksiyon tanımlarını yakalar",
                "Programlama",
                listOf("function myFunc()", "function calculate(a, b)")
            ),
            
            // Dosya Patternleri
            RegexPattern(
                "Dosya Uzantısı",
                "\\.([a-zA-Z0-9]+)$",
                "Dosya uzantısını yakalar",
                "Dosya",
                listOf("file.txt", "image.png")
            ),
            RegexPattern(
                "Resim Dosyası",
                "\\.(jpg|jpeg|png|gif|bmp|webp|svg)$",
                "Resim dosyalarını eşleştirir",
                "Dosya",
                listOf("photo.jpg", "icon.png")
            ),
            RegexPattern(
                "Kod Dosyası",
                "\\.(kt|java|py|js|ts|html|css|json|xml)$",
                "Kod dosyalarını eşleştirir",
                "Dosya",
                listOf("App.kt", "main.py")
            ),
            RegexPattern(
                "Dosya Yolu",
                "^(\\/[a-zA-Z0-9._-]+)+\\/?$",
                "Unix dosya yolunu doğrular",
                "Dosya",
                listOf("/home/user/file.txt", "/var/log/")
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
        val categories = listOf("Tümü") + allPatterns.map { it.category }.distinct()
        
        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                isChecked = category == "Tümü"
                setOnClickListener {
                    currentCategory = category
                    // Uncheck other chips
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
    
    private fun setupTester() {
        btnTest.setOnClickListener {
            val pattern = testPattern.text.toString()
            val input = testInput.text.toString()
            
            if (pattern.isEmpty()) {
                testResult.text = "Pattern girin"
                testResult.setTextColor(getColor(android.R.color.holo_orange_dark))
                return@setOnClickListener
            }
            
            try {
                val regex = Regex(pattern)
                val matches = regex.findAll(input).toList()
                
                if (matches.isEmpty()) {
                    testResult.text = "❌ Eşleşme yok"
                    testResult.setTextColor(getColor(android.R.color.holo_red_dark))
                } else {
                    val matchTexts = matches.map { it.value }
                    testResult.text = "✅ ${matches.size} eşleşme:\n${matchTexts.joinToString(", ")}"
                    testResult.setTextColor(getColor(android.R.color.holo_green_dark))
                }
            } catch (e: Exception) {
                testResult.text = "⚠️ Geçersiz pattern: ${e.message}"
                testResult.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }
    
    private fun filterPatterns() {
        val searchQuery = searchInput.text.toString().lowercase()
        
        filteredPatterns = allPatterns.filter { pattern ->
            val matchesCategory = currentCategory == "Tümü" || pattern.category == currentCategory
            val matchesSearch = searchQuery.isEmpty() || 
                pattern.name.lowercase().contains(searchQuery) ||
                pattern.description.lowercase().contains(searchQuery)
            matchesCategory && matchesSearch
        }
        
        updateList()
    }
    
    private fun updateList() {
        recyclerView.adapter = PatternAdapter(filteredPatterns)
    }
    
    inner class PatternAdapter(private val patterns: List<RegexPattern>) : 
        RecyclerView.Adapter<PatternAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPatternName)
            val tvPattern: TextView = view.findViewById(R.id.tvPattern)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvExamples: TextView = view.findViewById(R.id.tvExamples)
            val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
            val btnUse: Button = view.findViewById(R.id.btnUse)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_regex_pattern, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pattern = patterns[position]
            
            holder.tvName.text = pattern.name
            holder.tvPattern.text = pattern.pattern
            holder.tvDescription.text = pattern.description
            holder.tvCategory.text = pattern.category
            holder.tvExamples.text = "Örnekler: ${pattern.examples.joinToString(", ")}"
            
            holder.btnCopy.setOnClickListener {
                copyToClipboard(pattern.pattern)
                Toast.makeText(this@RegexPatternsActivity, "Pattern kopyalandı", Toast.LENGTH_SHORT).show()
            }
            
            holder.btnUse.setOnClickListener {
                testPattern.setText(pattern.pattern)
                Toast.makeText(this@RegexPatternsActivity, "Pattern test alanına eklendi", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun getItemCount() = patterns.size
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("regex", text)
        clipboard.setPrimaryClip(clip)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
