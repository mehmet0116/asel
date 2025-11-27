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
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class HttpStatusCodesActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var statusCodesRecyclerView: RecyclerView
    private lateinit var statusCodesAdapter: StatusCodeAdapter

    private val allStatusCodes = listOf(
        // 1xx Informational
        StatusCode(100, "Continue", "1xx", "İstemci isteğe devam edebilir"),
        StatusCode(101, "Switching Protocols", "1xx", "Sunucu protokol değiştirmeyi kabul etti"),
        StatusCode(102, "Processing", "1xx", "Sunucu isteği işliyor"),
        StatusCode(103, "Early Hints", "1xx", "Ön yükleme için ipuçları"),
        
        // 2xx Success
        StatusCode(200, "OK", "2xx", "İstek başarılı"),
        StatusCode(201, "Created", "2xx", "Yeni kaynak oluşturuldu"),
        StatusCode(202, "Accepted", "2xx", "İstek kabul edildi, işleniyor"),
        StatusCode(203, "Non-Authoritative Information", "2xx", "Bilgi yetkili olmayan kaynaktan"),
        StatusCode(204, "No Content", "2xx", "İstek başarılı, içerik yok"),
        StatusCode(205, "Reset Content", "2xx", "İstemci görünümü sıfırlamalı"),
        StatusCode(206, "Partial Content", "2xx", "Kısmi içerik döndürüldü"),
        StatusCode(207, "Multi-Status", "2xx", "Birden fazla durum kodu"),
        StatusCode(208, "Already Reported", "2xx", "Zaten raporlandı"),
        StatusCode(226, "IM Used", "2xx", "Delta encoding kullanıldı"),
        
        // 3xx Redirection
        StatusCode(300, "Multiple Choices", "3xx", "Birden fazla seçenek mevcut"),
        StatusCode(301, "Moved Permanently", "3xx", "Kalıcı olarak taşındı"),
        StatusCode(302, "Found", "3xx", "Geçici olarak başka yerde"),
        StatusCode(303, "See Other", "3xx", "Başka bir URI\'ye bak"),
        StatusCode(304, "Not Modified", "3xx", "Değişiklik yok, önbellek kullanılabilir"),
        StatusCode(305, "Use Proxy", "3xx", "Proxy kullanılmalı (kullanımdan kaldırıldı)"),
        StatusCode(307, "Temporary Redirect", "3xx", "Geçici yönlendirme"),
        StatusCode(308, "Permanent Redirect", "3xx", "Kalıcı yönlendirme"),
        
        // 4xx Client Errors
        StatusCode(400, "Bad Request", "4xx", "Geçersiz istek sözdizimi"),
        StatusCode(401, "Unauthorized", "4xx", "Kimlik doğrulama gerekli"),
        StatusCode(402, "Payment Required", "4xx", "Ödeme gerekli (gelecek kullanım için)"),
        StatusCode(403, "Forbidden", "4xx", "Erişim yasaklandı"),
        StatusCode(404, "Not Found", "4xx", "Kaynak bulunamadı"),
        StatusCode(405, "Method Not Allowed", "4xx", "HTTP metodu izin verilmiyor"),
        StatusCode(406, "Not Acceptable", "4xx", "Kabul edilebilir içerik yok"),
        StatusCode(407, "Proxy Authentication Required", "4xx", "Proxy kimlik doğrulama gerekli"),
        StatusCode(408, "Request Timeout", "4xx", "İstek zaman aşımına uğradı"),
        StatusCode(409, "Conflict", "4xx", "Kaynak çakışması"),
        StatusCode(410, "Gone", "4xx", "Kaynak kalıcı olarak silindi"),
        StatusCode(411, "Length Required", "4xx", "Content-Length gerekli"),
        StatusCode(412, "Precondition Failed", "4xx", "Ön koşul başarısız"),
        StatusCode(413, "Payload Too Large", "4xx", "İstek gövdesi çok büyük"),
        StatusCode(414, "URI Too Long", "4xx", "URI çok uzun"),
        StatusCode(415, "Unsupported Media Type", "4xx", "Desteklenmeyen medya türü"),
        StatusCode(416, "Range Not Satisfiable", "4xx", "Aralık karşılanamıyor"),
        StatusCode(417, "Expectation Failed", "4xx", "Beklenti başarısız"),
        StatusCode(418, "I\'m a teapot", "4xx", "Ben bir çaydanlığım (şaka)"),
        StatusCode(421, "Misdirected Request", "4xx", "Yanlış yönlendirilmiş istek"),
        StatusCode(422, "Unprocessable Entity", "4xx", "İşlenemeyen varlık"),
        StatusCode(423, "Locked", "4xx", "Kaynak kilitli"),
        StatusCode(424, "Failed Dependency", "4xx", "Bağımlılık başarısız"),
        StatusCode(425, "Too Early", "4xx", "Çok erken"),
        StatusCode(426, "Upgrade Required", "4xx", "Yükseltme gerekli"),
        StatusCode(428, "Precondition Required", "4xx", "Ön koşul gerekli"),
        StatusCode(429, "Too Many Requests", "4xx", "Çok fazla istek"),
        StatusCode(431, "Request Header Fields Too Large", "4xx", "İstek başlıkları çok büyük"),
        StatusCode(451, "Unavailable For Legal Reasons", "4xx", "Yasal nedenlerle kullanılamaz"),
        
        // 5xx Server Errors
        StatusCode(500, "Internal Server Error", "5xx", "Sunucu iç hatası"),
        StatusCode(501, "Not Implemented", "5xx", "Uygulanmadı"),
        StatusCode(502, "Bad Gateway", "5xx", "Geçersiz ağ geçidi yanıtı"),
        StatusCode(503, "Service Unavailable", "5xx", "Hizmet kullanılamıyor"),
        StatusCode(504, "Gateway Timeout", "5xx", "Ağ geçidi zaman aşımı"),
        StatusCode(505, "HTTP Version Not Supported", "5xx", "HTTP sürümü desteklenmiyor"),
        StatusCode(506, "Variant Also Negotiates", "5xx", "Varyant da müzakere ediyor"),
        StatusCode(507, "Insufficient Storage", "5xx", "Yetersiz depolama alanı"),
        StatusCode(508, "Loop Detected", "5xx", "Döngü algılandı"),
        StatusCode(510, "Not Extended", "5xx", "Genişletilmedi"),
        StatusCode(511, "Network Authentication Required", "5xx", "Ağ kimlik doğrulaması gerekli")
    )

    private var filteredStatusCodes = allStatusCodes.toMutableList()
    private var currentCategory = "Tümü"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_http_status_codes)

        setupToolbar()
        initViews()
        setupSearch()
        setupCategoryChips()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.http_status_codes)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        searchInput = findViewById(R.id.searchInput)
        categoryChipGroup = findViewById(R.id.categoryChipGroup)
        statusCodesRecyclerView = findViewById(R.id.statusCodesRecyclerView)
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterStatusCodes()
            }
        })
    }

    private fun setupCategoryChips() {
        val categories = listOf("Tümü", "1xx", "2xx", "3xx", "4xx", "5xx")
        
        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = when (category) {
                    "Tümü" -> "Tümü"
                    "1xx" -> "1xx Bilgi"
                    "2xx" -> "2xx Başarı"
                    "3xx" -> "3xx Yönlendirme"
                    "4xx" -> "4xx İstemci Hatası"
                    "5xx" -> "5xx Sunucu Hatası"
                    else -> category
                }
                isCheckable = true
                isChecked = category == "Tümü"
                setOnClickListener {
                    currentCategory = category
                    updateChipSelection()
                    filterStatusCodes()
                }
            }
            categoryChipGroup.addView(chip)
        }
    }

    private fun updateChipSelection() {
        val categories = listOf("Tümü", "1xx", "2xx", "3xx", "4xx", "5xx")
        for (i in 0 until categoryChipGroup.childCount) {
            val chip = categoryChipGroup.getChildAt(i) as Chip
            chip.isChecked = categories[i] == currentCategory
        }
    }

    private fun setupRecyclerView() {
        statusCodesAdapter = StatusCodeAdapter(filteredStatusCodes) { statusCode ->
            copyStatusCode(statusCode)
        }
        statusCodesRecyclerView.layoutManager = LinearLayoutManager(this)
        statusCodesRecyclerView.adapter = statusCodesAdapter
    }

    private fun filterStatusCodes() {
        val searchQuery = searchInput.text.toString().lowercase()
        
        filteredStatusCodes.clear()
        filteredStatusCodes.addAll(allStatusCodes.filter { statusCode ->
            val matchesCategory = currentCategory == "Tümü" || statusCode.category == currentCategory
            val matchesSearch = searchQuery.isEmpty() || 
                statusCode.code.toString().contains(searchQuery) ||
                statusCode.name.lowercase().contains(searchQuery) ||
                statusCode.description.lowercase().contains(searchQuery)
            
            matchesCategory && matchesSearch
        })
        
        statusCodesAdapter.notifyDataSetChanged()
    }

    private fun copyStatusCode(statusCode: StatusCode) {
        val text = "${statusCode.code} ${statusCode.name}"
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("HTTP Status Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    data class StatusCode(
        val code: Int,
        val name: String,
        val category: String,
        val description: String
    )

    inner class StatusCodeAdapter(
        private val statusCodes: List<StatusCode>,
        private val onCopyClick: (StatusCode) -> Unit
    ) : RecyclerView.Adapter<StatusCodeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val codeText: TextView = view.findViewById(R.id.statusCodeText)
            val nameText: TextView = view.findViewById(R.id.statusNameText)
            val descriptionText: TextView = view.findViewById(R.id.statusDescriptionText)
            val copyButton: ImageButton = view.findViewById(R.id.copyButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_http_status, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val statusCode = statusCodes[position]
            holder.codeText.text = statusCode.code.toString()
            holder.nameText.text = statusCode.name
            holder.descriptionText.text = statusCode.description
            
            // Set color based on category
            val colorRes = when (statusCode.category) {
                "1xx" -> R.color.status_1xx
                "2xx" -> R.color.status_2xx
                "3xx" -> R.color.status_3xx
                "4xx" -> R.color.status_4xx
                "5xx" -> R.color.status_5xx
                else -> R.color.status_1xx
            }
            holder.codeText.setTextColor(resources.getColor(colorRes, theme))
            
            holder.copyButton.setOnClickListener { onCopyClick(statusCode) }
            holder.itemView.setOnClickListener { onCopyClick(statusCode) }
        }

        override fun getItemCount() = statusCodes.size
    }
}
