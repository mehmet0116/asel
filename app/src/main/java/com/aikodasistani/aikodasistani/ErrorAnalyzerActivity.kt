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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ErrorAnalyzerActivity : AppCompatActivity() {

    private lateinit var editTextError: EditText
    private lateinit var btnAnalyze: Button
    private lateinit var resultContainer: LinearLayout
    private lateinit var tvErrorType: TextView
    private lateinit var tvExplanation: TextView
    private lateinit var tvSolution: TextView
    private lateinit var recyclerViewCommonErrors: RecyclerView
    private lateinit var chipGroupLanguages: ChipGroup
    
    private var selectedLanguage = "kotlin"
    private val commonErrors = mutableListOf<CommonError>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_analyzer)
        
        supportActionBar?.apply {
            title = getString(R.string.error_analyzer_title)
            setDisplayHomeAsUpEnabled(true)
        }
        
        initializeViews()
        setupLanguageChips()
        setupAnalyzeButton()
        loadCommonErrors()
        setupCommonErrorsAdapter()
    }
    
    private fun initializeViews() {
        editTextError = findViewById(R.id.editTextError)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        resultContainer = findViewById(R.id.resultContainer)
        tvErrorType = findViewById(R.id.tvErrorType)
        tvExplanation = findViewById(R.id.tvExplanation)
        tvSolution = findViewById(R.id.tvSolution)
        recyclerViewCommonErrors = findViewById(R.id.recyclerViewCommonErrors)
        chipGroupLanguages = findViewById(R.id.chipGroupLanguages)
    }
    
    private fun setupLanguageChips() {
        val languages = listOf(
            "Kotlin" to "kotlin",
            "Java" to "java",
            "Python" to "python",
            "JavaScript" to "javascript",
            "TypeScript" to "typescript",
            "C++" to "cpp",
            "Swift" to "swift"
        )
        
        languages.forEachIndexed { index, (name, lang) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = index == 0
                setOnClickListener {
                    selectedLanguage = lang
                    loadCommonErrors()
                    // Uncheck others
                    for (i in 0 until chipGroupLanguages.childCount) {
                        val c = chipGroupLanguages.getChildAt(i) as Chip
                        c.isChecked = c == this
                    }
                }
            }
            chipGroupLanguages.addView(chip)
        }
    }
    
    private fun setupAnalyzeButton() {
        btnAnalyze.setOnClickListener {
            val errorText = editTextError.text.toString().trim()
            if (errorText.isBlank()) {
                Toast.makeText(this, R.string.error_analyzer_empty_input, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            analyzeError(errorText)
        }
    }
    
    private fun analyzeError(errorText: String) {
        // Pattern matching for common error types
        val analysis = when {
            // Kotlin/Java NullPointerException
            errorText.contains("NullPointerException", ignoreCase = true) ||
            errorText.contains("null cannot be cast", ignoreCase = true) -> {
                ErrorAnalysis(
                    "NullPointerException",
                    "Null bir nesne üzerinde işlem yapmaya çalışıyorsunuz. Bu, bir değişkenin değer atanmadan kullanılması veya null dönen bir fonksiyonun sonucunun kullanılması nedeniyle olabilir.",
                    listOf(
                        "Null kontrolü ekleyin: if (variable != null) { ... }",
                        "Safe call operatörü kullanın: variable?.method()",
                        "Elvis operatörü kullanın: variable ?: defaultValue",
                        "let bloğu kullanın: variable?.let { ... }"
                    ),
                    "val name: String? = getName()\nval length = name?.length ?: 0"
                )
            }
            
            // IndexOutOfBoundsException
            errorText.contains("IndexOutOfBoundsException", ignoreCase = true) ||
            errorText.contains("index out of range", ignoreCase = true) ||
            errorText.contains("ArrayIndexOutOfBounds", ignoreCase = true) -> {
                ErrorAnalysis(
                    "IndexOutOfBoundsException",
                    "Bir liste veya dizinin geçersiz bir indeksine erişmeye çalışıyorsunuz. İndeks negatif veya listenin boyutundan büyük olamaz.",
                    listOf(
                        "İndeksi kontrol edin: if (index < list.size) { ... }",
                        "getOrNull kullanın: list.getOrNull(index)",
                        "getOrElse kullanın: list.getOrElse(index) { default }",
                        "firstOrNull/lastOrNull kullanın"
                    ),
                    "val item = list.getOrNull(index) ?: \"default\"\n// veya\nif (index in list.indices) { list[index] }"
                )
            }
            
            // ClassCastException
            errorText.contains("ClassCastException", ignoreCase = true) ||
            errorText.contains("cannot be cast to", ignoreCase = true) -> {
                ErrorAnalysis(
                    "ClassCastException",
                    "Bir nesneyi uyumsuz bir türe dönüştürmeye çalışıyorsunuz. Tip dönüşümü yalnızca nesne gerçekten o tipte ise yapılabilir.",
                    listOf(
                        "is kontrolü ekleyin: if (obj is TargetType) { ... }",
                        "Safe cast kullanın: obj as? TargetType",
                        "when ifadesi kullanın: when (obj) { is Type1 -> ... }",
                        "Jenerik tipleri kontrol edin"
                    ),
                    "val text = obj as? String ?: return\n// veya\nif (obj is String) { obj.length }"
                )
            }
            
            // StackOverflowError
            errorText.contains("StackOverflowError", ignoreCase = true) ||
            errorText.contains("stack overflow", ignoreCase = true) -> {
                ErrorAnalysis(
                    "StackOverflowError",
                    "Sonsuz özyineleme (recursive) döngüsü oluşmuş. Bir fonksiyon kendini çağırırken durma koşulu sağlanmıyor veya yanlış ayarlanmış.",
                    listOf(
                        "Temel durumu (base case) kontrol edin",
                        "Özyineleme koşulunu gözden geçirin",
                        "Özyineleme yerine döngü kullanmayı düşünün",
                        "Tail recursion kullanın (tailrec keyword)"
                    ),
                    "tailrec fun factorial(n: Int, acc: Int = 1): Int {\n    return if (n <= 1) acc\n    else factorial(n - 1, n * acc)\n}"
                )
            }
            
            // OutOfMemoryError
            errorText.contains("OutOfMemoryError", ignoreCase = true) ||
            errorText.contains("out of memory", ignoreCase = true) ||
            errorText.contains("heap space", ignoreCase = true) -> {
                ErrorAnalysis(
                    "OutOfMemoryError",
                    "Uygulama çok fazla bellek kullanıyor. Büyük nesneler, bellek sızıntıları veya verimsiz veri yapıları nedeniyle olabilir.",
                    listOf(
                        "Büyük resimleri optimize edin",
                        "Bellek sızıntılarını kontrol edin (LeakCanary kullanın)",
                        "Büyük listeleri parçalar halinde yükleyin",
                        "Bitmap'leri recycle edin veya Glide/Coil kullanın"
                    ),
                    "// Bitmap optimizasyonu\nval options = BitmapFactory.Options().apply {\n    inSampleSize = 4\n}\nval bitmap = BitmapFactory.decodeResource(resources, id, options)"
                )
            }
            
            // ConcurrentModificationException
            errorText.contains("ConcurrentModificationException", ignoreCase = true) -> {
                ErrorAnalysis(
                    "ConcurrentModificationException",
                    "Bir koleksiyon üzerinde iterasyon yaparken aynı anda koleksiyonu değiştirmeye çalışıyorsunuz.",
                    listOf(
                        "Iterator kullanın ve remove() ile silin",
                        "toMutableList() ile kopya oluşturun",
                        "removeIf kullanın",
                        "ConcurrentHashMap gibi thread-safe koleksiyonlar kullanın"
                    ),
                    "// Yanlış:\n// for (item in list) { list.remove(item) }\n\n// Doğru:\nlist.removeIf { it == targetItem }\n// veya\nval iterator = list.iterator()\nwhile (iterator.hasNext()) {\n    if (iterator.next() == target) iterator.remove()\n}"
                )
            }
            
            // Python specific errors
            errorText.contains("NameError", ignoreCase = true) ||
            errorText.contains("is not defined", ignoreCase = true) -> {
                ErrorAnalysis(
                    "NameError (Python)",
                    "Tanımlanmamış bir değişken veya fonksiyon kullanmaya çalışıyorsunuz.",
                    listOf(
                        "Değişken adının doğru yazıldığından emin olun",
                        "Değişkeni kullanmadan önce tanımlayın",
                        "Import ifadelerini kontrol edin",
                        "Scope (kapsam) sorunlarını kontrol edin"
                    ),
                    "# Yanlış:\nprint(my_var)  # NameError\n\n# Doğru:\nmy_var = \"Hello\"\nprint(my_var)"
                )
            }
            
            errorText.contains("IndentationError", ignoreCase = true) ||
            errorText.contains("unexpected indent", ignoreCase = true) -> {
                ErrorAnalysis(
                    "IndentationError (Python)",
                    "Python'da girinti (indentation) hatalı. Python boşluklara duyarlıdır ve tutarsız girintiler hataya neden olur.",
                    listOf(
                        "Tab yerine 4 boşluk kullanın (tutarlı olun)",
                        "IDE'nin boşluk/tab gösterimini açın",
                        "Tüm kodda aynı girinti stilini kullanın",
                        "Editörünüzü Python için yapılandırın"
                    ),
                    "# Yanlış:\nif True:\nprint(\"Hello\")  # IndentationError\n\n# Doğru:\nif True:\n    print(\"Hello\")"
                )
            }
            
            // JavaScript specific
            errorText.contains("TypeError", ignoreCase = true) ||
            errorText.contains("undefined is not a function", ignoreCase = true) ||
            errorText.contains("is not a function", ignoreCase = true) -> {
                ErrorAnalysis(
                    "TypeError (JavaScript)",
                    "Bir değer beklenenden farklı tipte. Genellikle undefined veya null üzerinde işlem yapmaya çalışmaktan kaynaklanır.",
                    listOf(
                        "Optional chaining kullanın: obj?.method?.()",
                        "Nullish coalescing kullanın: value ?? defaultValue",
                        "typeof kontrolü ekleyin",
                        "Async işlemlerde await kullanın"
                    ),
                    "// Optional chaining\nconst result = obj?.method?.();\n\n// Nullish coalescing\nconst value = input ?? 'default';"
                )
            }
            
            errorText.contains("SyntaxError", ignoreCase = true) -> {
                ErrorAnalysis(
                    "SyntaxError",
                    "Kod sözdizimi hatalı. Eksik parantez, virgül, noktalı virgül veya yanlış keyword kullanımı olabilir.",
                    listOf(
                        "Tüm parantezlerin eşleştiğinden emin olun",
                        "String'lerin düzgün kapatıldığını kontrol edin",
                        "Virgül ve noktalı virgülleri kontrol edin",
                        "IDE'nin syntax highlighting'ini kullanın"
                    ),
                    "// Eksik parantez kontrolü\nfunction example() {\n    if (condition) {\n        // kod\n    } // <- bu parantez eksikse SyntaxError\n}"
                )
            }
            
            // Network errors
            errorText.contains("UnknownHostException", ignoreCase = true) ||
            errorText.contains("SocketTimeoutException", ignoreCase = true) ||
            errorText.contains("connection refused", ignoreCase = true) -> {
                ErrorAnalysis(
                    "Ağ Hatası",
                    "Sunucuya bağlanılamıyor. İnternet bağlantısı, sunucu durumu veya URL sorunu olabilir.",
                    listOf(
                        "İnternet bağlantısını kontrol edin",
                        "URL'nin doğru olduğundan emin olun",
                        "Timeout süresini artırın",
                        "Retry mekanizması ekleyin"
                    ),
                    "val client = OkHttpClient.Builder()\n    .connectTimeout(30, TimeUnit.SECONDS)\n    .readTimeout(30, TimeUnit.SECONDS)\n    .build()"
                )
            }
            
            // Android specific
            errorText.contains("NetworkOnMainThreadException", ignoreCase = true) -> {
                ErrorAnalysis(
                    "NetworkOnMainThreadException",
                    "Android'de ana thread'de ağ işlemi yapılamaz. Ağ işlemleri arka planda yapılmalıdır.",
                    listOf(
                        "Coroutine kullanın: withContext(Dispatchers.IO)",
                        "AsyncTask kullanın (deprecated ama çalışır)",
                        "RxJava observeOn/subscribeOn kullanın",
                        "WorkManager kullanın uzun işlemler için"
                    ),
                    "lifecycleScope.launch {\n    val result = withContext(Dispatchers.IO) {\n        // Ağ işlemi\n    }\n    // UI güncelleme\n}"
                )
            }
            
            errorText.contains("CalledFromWrongThreadException", ignoreCase = true) ||
            errorText.contains("Only the original thread", ignoreCase = true) -> {
                ErrorAnalysis(
                    "UI Thread Hatası",
                    "UI işlemleri sadece ana thread'den yapılabilir. Arka plan thread'inden UI güncellemesi yapılamaz.",
                    listOf(
                        "runOnUiThread { } kullanın",
                        "Handler(Looper.getMainLooper()) kullanın",
                        "withContext(Dispatchers.Main) kullanın",
                        "LiveData veya StateFlow kullanın"
                    ),
                    "// Coroutine ile\nwithContext(Dispatchers.Main) {\n    textView.text = \"Updated\"\n}\n\n// runOnUiThread ile\nrunOnUiThread {\n    textView.text = \"Updated\"\n}"
                )
            }
            
            // Default
            else -> {
                ErrorAnalysis(
                    "Bilinmeyen Hata",
                    "Bu hata otomatik olarak tanımlanamadı. Hata mesajını dikkatlice okuyun ve aşağıdaki genel önerileri takip edin.",
                    listOf(
                        "Hata mesajındaki satır numarasını kontrol edin",
                        "Stack trace'i baştan sona okuyun",
                        "Google'da hata mesajını arayın",
                        "AI asistana bu hatayı açıklayarak sorun",
                        "Değişiklikleri geri alarak hangi değişikliğin soruna neden olduğunu bulun"
                    ),
                    "// Hata mesajını kopyalayıp AI asistana sorabilirsiniz:\n// \"Bu hatayı nasıl çözebilirim: [hata mesajı]\""
                )
            }
        }
        
        showAnalysisResult(analysis)
    }
    
    private fun showAnalysisResult(analysis: ErrorAnalysis) {
        resultContainer.visibility = View.VISIBLE
        tvErrorType.text = analysis.errorType
        tvExplanation.text = analysis.explanation
        tvSolution.text = analysis.solutions.joinToString("\n\n• ", prefix = "• ")
        
        // Copy solution button
        findViewById<Button>(R.id.btnCopySolution).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Code", analysis.codeExample)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        
        // Show code example
        findViewById<TextView>(R.id.tvCodeExample).text = analysis.codeExample
    }
    
    private fun loadCommonErrors() {
        commonErrors.clear()
        
        when (selectedLanguage) {
            "kotlin", "java" -> {
                commonErrors.addAll(listOf(
                    CommonError("NullPointerException", "Null nesne üzerinde işlem"),
                    CommonError("IndexOutOfBoundsException", "Geçersiz dizi indeksi"),
                    CommonError("ClassCastException", "Uyumsuz tip dönüşümü"),
                    CommonError("IllegalArgumentException", "Geçersiz parametre"),
                    CommonError("IllegalStateException", "Geçersiz nesne durumu"),
                    CommonError("ConcurrentModificationException", "Eşzamanlı değişiklik"),
                    CommonError("StackOverflowError", "Sonsuz özyineleme"),
                    CommonError("OutOfMemoryError", "Yetersiz bellek")
                ))
            }
            "python" -> {
                commonErrors.addAll(listOf(
                    CommonError("NameError", "Tanımlanmamış değişken"),
                    CommonError("TypeError", "Uyumsuz tip işlemi"),
                    CommonError("IndexError", "Liste indeks hatası"),
                    CommonError("KeyError", "Sözlük anahtar hatası"),
                    CommonError("IndentationError", "Girinti hatası"),
                    CommonError("ImportError", "Modül bulunamadı"),
                    CommonError("AttributeError", "Nesne özelliği yok"),
                    CommonError("ValueError", "Geçersiz değer")
                ))
            }
            "javascript", "typescript" -> {
                commonErrors.addAll(listOf(
                    CommonError("TypeError", "Tip uyumsuzluğu"),
                    CommonError("ReferenceError", "Tanımsız referans"),
                    CommonError("SyntaxError", "Sözdizimi hatası"),
                    CommonError("RangeError", "Aralık dışı değer"),
                    CommonError("URIError", "URI kodlama hatası"),
                    CommonError("undefined is not a function", "Fonksiyon çağrı hatası"),
                    CommonError("Cannot read property of null", "Null erişim hatası")
                ))
            }
            else -> {
                commonErrors.addAll(listOf(
                    CommonError("Syntax Error", "Sözdizimi hatası"),
                    CommonError("Runtime Error", "Çalışma zamanı hatası"),
                    CommonError("Logic Error", "Mantık hatası"),
                    CommonError("Memory Error", "Bellek hatası")
                ))
            }
        }
        
        setupCommonErrorsAdapter()
    }
    
    private fun setupCommonErrorsAdapter() {
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val tvName: TextView = view.findViewById(R.id.tvErrorName)
                val tvDescription: TextView = view.findViewById(R.id.tvErrorDescription)
            }
            
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_common_error, parent, false)
                return ViewHolder(view)
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val error = commonErrors[position]
                (holder as ViewHolder).tvName.text = error.name
                holder.tvDescription.text = error.description
                
                holder.itemView.setOnClickListener {
                    editTextError.setText(error.name)
                    analyzeError(error.name)
                }
            }
            
            override fun getItemCount() = commonErrors.size
        }
        
        recyclerViewCommonErrors.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewCommonErrors.adapter = adapter
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    // Data classes
    data class ErrorAnalysis(
        val errorType: String,
        val explanation: String,
        val solutions: List<String>,
        val codeExample: String
    )
    
    data class CommonError(
        val name: String,
        val description: String
    )
}
