package com.aikodasistani.aikodasistani

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.ArchivedMessage
import com.aikodasistani.aikodasistani.data.ModelConfig
import com.aikodasistani.aikodasistani.data.Session
import com.aikodasistani.aikodasistani.util.CodeDetectionUtil
import com.aikodasistani.aikodasistani.util.FileDownloadUtil
import com.aikodasistani.aikodasistani.util.VideoProcessingUtil
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.navigation.NavigationView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Office dosyaları için import'lar
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.opencsv.CSVReader

// PDF okuma için
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor

// YENİ: Kademeli derin düşünme seviyeleri
data class ThinkingLevel(
    val level: Int,
    val name: String,
    val color: Int,
    val description: String,
    val thinkingTime: Long, // ms cinsinden
    val detailMultiplier: Double // Detay çarpanı
)

data class Message(var text: String, val isSentByUser: Boolean, var id: Long = 0, val isThinking: Boolean = false, val thinkingSteps: MutableList<String> = mutableListOf())

// Token limitleri için data class
data class TokenLimits(
    val maxTokens: Int,
    val maxContext: Int,
    val historyMessages: Int
)

class MessageAdapter(
    private val context: Context,
    private val messages: List<Message>,
    private val markwon: Markwon,
    private val onDownloadClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_THINKING = 3
    }

    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewMessage)
    }

    class AiMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewMessage)
        val cardView: CardView = view.findViewById(R.id.cardViewMessage)
        val downloadButton: ImageButton = view.findViewById(R.id.downloadButton)
    }

    class ThinkingMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewThinking)
        val progressBar: android.widget.ProgressBar = view.findViewById(R.id.progressThinking)
        val stepsContainer: android.widget.LinearLayout = view.findViewById(R.id.thinkingStepsContainer)

        fun bind(thinkingSteps: List<String>) {
            stepsContainer.removeAllViews()

            thinkingSteps.forEachIndexed { index, step ->
                val stepView = TextView(itemView.context).apply {
                    text = "• $step"
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.dark_gray))
                    textSize = 12f
                    setPadding(0, 4, 0, 4)

                    // Animasyon
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .setDuration(400L)
                        .setStartDelay((index * 300).toLong())
                        .start()
                }
                stepsContainer.addView(stepView)
            }

            // Progress bar'ı güncelle
            val progress = minOf(100, (thinkingSteps.size * 100) / 6)
            progressBar.progress = progress
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            messages[position].isThinking -> VIEW_TYPE_THINKING
            messages[position].isSentByUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_AI -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_ai, parent, false)
                AiMessageViewHolder(view).apply {
                    textView.movementMethod = LinkMovementMethod.getInstance()
                }
            }
            VIEW_TYPE_THINKING -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_thinking_message, parent, false)
                ThinkingMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> {
                markwon.setMarkdown(holder.textView, message.text)
            }
            is AiMessageViewHolder -> {
                markwon.setMarkdown(holder.textView, message.text)

                // Kod tespiti ve indir butonu görünürlüğü
                val showDownload = CodeDetectionUtil.shouldShowDownloadButton(message.text)
                holder.downloadButton.visibility = if (showDownload) View.VISIBLE else View.GONE

                holder.downloadButton.setOnClickListener {
                    onDownloadClick(message.text)
                }

                holder.cardView.setOnLongClickListener { view ->
                    val popup = PopupMenu(context, view)
                    popup.menu.add("Tümünü Kopyala")
                    val codeBlocks = extractCodeBlocks(message.text)
                    if (codeBlocks.isNotEmpty()) {
                        popup.menu.add("Kodu Kopyala")
                    }

                    popup.setOnMenuItemClickListener { menuItem ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val textToCopy = when {
                            menuItem.title == "Kodu Kopyala" && codeBlocks.isNotEmpty() -> codeBlocks.joinToString("\n\n")
                            else -> message.text
                        }
                        val clip = ClipData.newPlainText("Copied Text", textToCopy)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                        true
                    }
                    if (popup.menu.size() > 0) popup.show()
                    true
                }
            }
            is ThinkingMessageViewHolder -> {
                holder.textView.text = context.getString(R.string.thinking_started)
                holder.bind(message.thinkingSteps)
            }
        }
    }

    override fun getItemCount() = messages.size

    private fun extractCodeBlocks(text: String): List<String> {
        val pattern = Regex("```(?:\\w+)?\\s*([\\s\\S]*?)```")
        return pattern.findAll(text).map { it.groupValues[1].trim() }.toList()
    }
}

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonAttachment: ImageButton
    private lateinit var buttonDeepThink: ImageButton
    private lateinit var cancelSendButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: AppDatabase
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var loadingText: TextView

    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    private var modelConfig: Map<String, List<String>> = emptyMap()
    private var currentProvider: String = ""
    private var currentModel: String = ""
    private var photoURI: Uri? = null
    private var pendingImageBase64: String? = null

    private var currentSessionId: Long = -1
    private var openAiApiKey = ""
    private var deepseekApiKey = ""
    private var geminiApiKey = ""
    private var dashscopeApiKey = ""

    // YENİ: Kademeli derin düşünme seviyeleri
    private val thinkingLevels = listOf(
        ThinkingLevel(0, "Kapalı", R.color.purple_500, "Normal mod", 0, 1.0),
        ThinkingLevel(1, "Hafif", R.color.green, "Hızlı analiz", 2000, 1.3),
        ThinkingLevel(2, "Orta", R.color.orange, "Dengeli analiz", 4000, 1.7),
        ThinkingLevel(3, "Derin", R.color.deep_orange, "Kapsamlı analiz", 7000, 2.2),
        ThinkingLevel(4, "Çok Derin", R.color.red, "Çok kapsamlı analiz", 10000, 3.0)
    )

    private var currentThinkingLevel = 0

    // 🔄 Coroutine scopes
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val fileReadingScope = CoroutineScope(Dispatchers.IO + Job())
    private var currentFileReadingJob: Job? = null
    private var currentResponseJob: Job? = null

    // Yeni değişkenler - Akıllı dosya işleme için
    private var pendingFileContent: String? = null
    private var pendingFileName: String? = null

    // Yeni video değişkenleri
    private val recordVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                mainCoroutineScope.launch {
                    processVideoFile(uri)
                }
            } ?: run {
                Toast.makeText(this@MainActivity, "Video seçilemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val selectVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                mainCoroutineScope.launch {
                    processVideoFile(uri)
                }
            }
        }
    }

    // Video analiz manager
    private lateinit var videoAnalysisManager: VideoAnalysisManager

    private data class VideoAnalysisConfig(
        val name: String,
        val frameIntervalMs: Long,
        val maxFrames: Int,
        val description: String
    )

    // ✅ DÜZELTİLDİ: registerForActivityResult doğru kullanımı
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) openCamera() else Toast.makeText(
                this@MainActivity,
                "Kamera izni olmadan bu özellik kullanılamaz.",
                Toast.LENGTH_SHORT
            ).show()
        }

    private val sessionsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data?.getBooleanExtra("new_session", false) == true) {
                    mainCoroutineScope.launch { createNewSession() }
                } else {
                    val sessionId = data?.getLongExtra("session_id", -1L) ?: -1L
                    if (sessionId != -1L) {
                        mainCoroutineScope.launch { loadSession(sessionId) }
                    }
                    val deletedSessionId = data?.getLongExtra("deleted_session_id", -1L) ?: -1L
                    if (deletedSessionId != -1L && deletedSessionId == currentSessionId) {
                        mainCoroutineScope.launch {
                            val sessionsList = withContext(Dispatchers.IO) {
                                db.sessionDao().getAllSessions().first()
                            }
                            val lastSession = sessionsList.firstOrNull()
                            if (lastSession != null) loadSession(lastSession.id) else createNewSession()
                        }
                    }
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                photoURI?.let { readContentFromUri(it, isImage = true) }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    readContentFromUri(uri, isImage = true)
                }
            }
        }

    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    readContentFromUri(uri, isImage = false)
                }
            }
        }

    private val markwon: Markwon by lazy {
        Markwon.builder(this)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    // YENİ: Düşünme seviyesi seçim dialog'u
    private fun showThinkingLevelDialog() {
        val levels = thinkingLevels.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🧠 Derin Düşünme Seviyesi Seçin")
            .setItems(levels) { _, which ->
                setThinkingLevel(which)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // YENİ: Düşünme seviyesini ayarla
    private fun setThinkingLevel(level: Int) {
        currentThinkingLevel = level
        val selectedLevel = thinkingLevels[level]

        // Buton rengini güncelle
        buttonDeepThink.setColorFilter(ContextCompat.getColor(this, selectedLevel.color))

        // Tooltip göster
        Toast.makeText(
            this@MainActivity,
            "🧠 ${selectedLevel.name} Mod: ${selectedLevel.description}",
            Toast.LENGTH_LONG
        ).show()

        sharedPreferences.edit().putInt("thinking_level", level).apply()
    }

    // YENİ: Kaydedilmiş düşünme seviyesini yükle
    private fun loadThinkingLevel() {
        currentThinkingLevel = sharedPreferences.getInt("thinking_level", 0)
        setThinkingLevel(currentThinkingLevel)
    }

    // Yeni fonksiyonlar - Model optimizasyonları için
    private fun getModelTokenLimits(provider: String, model: String): TokenLimits {
        return when {
            provider == "OPENAI" && model.startsWith("gpt-3.5") -> TokenLimits(
                maxTokens = 3500,
                maxContext = 4096,
                historyMessages = 6
            )
            provider == "OPENAI" && (model.startsWith("gpt-4") || model.startsWith("gpt-4o")) -> TokenLimits(
                maxTokens = 3500,
                maxContext = 4096,
                historyMessages = 8
            )
            provider == "GEMINI" -> TokenLimits(
                maxTokens = 12000,
                maxContext = 128000,
                historyMessages = 15
            )
            provider == "DEEPSEEK" -> TokenLimits(
                maxTokens = 6000,
                maxContext = 64000,
                historyMessages = 10
            )
            provider == "QWEN" -> TokenLimits(
                maxTokens = 6000,
                maxContext = 64000,
                historyMessages = 10
            )
            else -> TokenLimits(
                maxTokens = 3500,
                maxContext = 4096,
                historyMessages = 8
            )
        }
    }

    private fun getOptimizedHistory(history: List<Message>, provider: String): List<Message> {
        val tokenLimits = getModelTokenLimits(provider, currentModel)
        return history.takeLast(tokenLimits.historyMessages).map { message ->
            optimizeMessageForModel(message, provider)
        }
    }

    private fun optimizeMessageForModel(message: Message, provider: String): Message {
        val isCodeMessage = message.text.contains("```") ||
                CodeDetectionUtil.detectLanguageAndCode(message.text).first != null

        val truncateLimit = when {
            isCodeMessage -> when (provider) {
                "GEMINI" -> 8000
                "OPENAI" -> 3000
                else -> 5000
            }
            else -> 2000
        }

        return if (message.text.length > truncateLimit) {
            message.copy(text = message.text.take(truncateLimit) + "\n[...devamı var]")
        } else {
            message
        }
    }

    private fun getSystemPrompt(provider: String): String {
        val basePrompt = """
        KODLAMA ASİSTANI - GEÇMİŞİ HATIRLA:
        
        KRİTİK TALİMATLAR:
        1. ✅ TÜM geçmiş konuşmayı HATIRLA
        2. ✅ Önceki kod parçalarını TEKRAR KULLAN
        3. ✅ Proje bağlamını KORU
        4. ✅ Tekrarlanan sorularda ÖNCEKİ cevapları REFERANS al
        5. ❌ ASLA "hatırlamıyorum" deme!
        
        KODLAMA ÖZEL:
        - Önceki import'ları hatırla
        - Class/function tanımlarını koru
        - Proje yapısını sürdür
    """

        return when (provider) {
            "OPENAI" -> "$basePrompt\n\nOpenAI Model: Uzun context kullan, geçmişi unutma!"
            "GEMINI" -> "$basePrompt\n\nGemini Model: 128K token kapasiten var, tüm geçmişi kullan!"
            "DEEPSEEK" -> "$basePrompt\n\nDeepSeek Model: Geçmiş bağlamı koru, kod context'ini sürdür!"
            "QWEN" -> "$basePrompt\n\nQwen Model: Chinese ve English destekli, geçmişi hatırla!"
            else -> basePrompt
        }
    }

    // YENİ: Seviye bazlı düşünme prompt'ları
    private fun getLeveledThinkingPrompt(userMessage: String?, level: Int): String {
        return when (level) {
            1 -> """
                🧠 HAFİF DÜŞÜNME MODU
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %30 daha detaylı cevapla.
                - 2 farklı açıdan değerlendir
                - Pratik çözüm öner
                - Kısa ve öz ol
            """.trimIndent()

            2 -> """
                🧠 ORTA DÜŞÜNME MODU
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %70 daha detaylı cevapla.
                - 3 farklı açıdan değerlendir
                - Her çözümün artı/eksilerini listele
                - En iyi çözümü seç ve nedenini açıkla
                - Uygulama adımlarını sırala
            """.trimIndent()

            3 -> """
                🧠 DERİN DÜŞÜNME MODU
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %120 daha detaylı cevapla.
                - 4+ farklı açıdan kapsamlı analiz yap
                - Her çözümü 5 kriterde değerlendir
                - Best practices ve pattern'leri dahil et
                - Detaylı implementasyon planı sun
                - Olası riskleri ve çözümlerini belirt
            """.trimIndent()

            4 -> """
                🧠 ÇOK DERİN DÜŞÜNME MODU - AKADEMİK SEVİYE
                
                ORJİNAL SORU: $userMessage
                
                TALİMAT: Bu soruyu %200 daha detaylı cevapla.
                - 5+ farklı disipliner açıdan analiz et
                - Akademik referanslar ve case study'ler kullan
                - Endüstri standartlarını ve en iyi uygulamaları dahil et
                - Multiple senaryolar ve edge case'ler için çözüm üret
                - Detaylı ROI analizi ve optimizasyon önerileri sun
                - Uzun vadeli stratejik planlama yap
            """.trimIndent()

            else -> userMessage ?: ""
        }
    }

    // YENİ: Gerçek AI çağrısı için kademeli derin düşünme modu
    private fun getRealDeepThinkingResponse(userMessage: String?, base64Image: String?) {
        val currentLevel = thinkingLevels[currentThinkingLevel]

        val thinkingMessage = Message(
            text = "🧠 ${currentLevel.name} Düşünme Modu Başlatıldı...",
            isSentByUser = false,
            isThinking = true
        )

        mainCoroutineScope.launch {
            // Düşünme mesajını ekle
            val messageId = withContext(Dispatchers.IO) {
                db.sessionDao().insertMessage(
                    ArchivedMessage(
                        sessionId = currentSessionId,
                        text = thinkingMessage.text,
                        isSentByUser = false
                    )
                )
            }
            thinkingMessage.id = messageId
            messageList.add(thinkingMessage)
            messageAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.scrollToPosition(messageList.size - 1)

            try {
                // SEVİYE BAZLI DÜŞÜNME SÜRECİ
                when (currentThinkingLevel) {
                    1 -> startLightThinking(thinkingMessage) // Hafif
                    2 -> startMediumThinking(thinkingMessage) // Orta
                    3 -> startDeepThinking(thinkingMessage) // Derin
                    4 -> startVeryDeepThinking(thinkingMessage) // Çok Derin
                }

                // Düşünme mesajını kaldır ve GERÇEK AI çağrısını yap
                withContext(Dispatchers.Main) {
                    val thinkingIndex = messageList.indexOf(thinkingMessage)
                    if (thinkingIndex != -1) {
                        messageList.removeAt(thinkingIndex)
                        messageAdapter.notifyItemRemoved(thinkingIndex)
                    }

                    // GERÇEK AI çağrısı - seviyeye özel prompt ile
                    val deepThinkingPrompt = getLeveledThinkingPrompt(userMessage, currentThinkingLevel)

                    // ✅ DÜZELTME: Resim durumunu koru ve doğru parametreleri ilet
                    getRealAiResponse(deepThinkingPrompt, base64Image, isDeepThinking = true)
                }

            } catch (e: Exception) {
                Log.e("DeepThinking", "Düşünme sürecinde hata", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(this@MainActivity, "Düşünme sürecinde hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // YENİ: Seviye bazlı düşünme süreçleri
    private suspend fun startLightThinking(thinkingMessage: Message) {
        addThinkingStep(thinkingMessage, "🔍 Hızlı problem analizi...")
        delay(800)
        addThinkingStep(thinkingMessage, "💡 Temel çözüm araştırması...")
        delay(700)
        addThinkingStep(thinkingMessage, "⚡ Pratik çözüm önerisi...")
        delay(500)
    }

    private suspend fun startMediumThinking(thinkingMessage: Message) {
        addThinkingStep(thinkingMessage, "🔍 Problemi 2 açıdan analiz ediyorum...")
        delay(1000)
        addThinkingStep(thinkingMessage, "📚 İlgili bilgileri topluyorum...")
        delay(1000)
        addThinkingStep(thinkingMessage, "💡 2 alternatif çözüm geliştiriyorum...")
        delay(1000)
        addThinkingStep(thinkingMessage, "⚖️ Çözümleri karşılaştırıyorum...")
        delay(800)
        addThinkingStep(thinkingMessage, "🎯 En iyi çözümü seçiyorum...")
        delay(600)
    }

    private suspend fun startDeepThinking(thinkingMessage: Message) {
        addThinkingStep(thinkingMessage, "🔍 Problemi 3 boyutuyla analiz ediyorum...")
        delay(1200)
        addThinkingStep(thinkingMessage, "📚 Detaylı araştırma yapıyorum...")
        addThinkingStep(thinkingMessage, "   • Teknik gereksinimleri inceliyorum")
        delay(1000)
        addThinkingStep(thinkingMessage, "   • Best practices araştırıyorum")
        delay(1000)
        addThinkingStep(thinkingMessage, "💡 3+ alternatif çözüm geliştiriyorum...")
        delay(1200)
        addThinkingStep(thinkingMessage, "⚖️ Her çözümün artı/eksilerini listeliyorum...")
        delay(1000)
        addThinkingStep(thinkingMessage, "🎯 En optimize çözümü seçiyorum...")
        delay(800)
        addThinkingStep(thinkingMessage, "📝 Detaylı uygulama planı hazırlıyorum...")
        delay(600)
    }

    private suspend fun startVeryDeepThinking(thinkingMessage: Message) {
        addThinkingStep(thinkingMessage, "🔍 Problemi 5 farklı açıdan derinlemesine analiz...")
        delay(1500)
        addThinkingStep(thinkingMessage, "📚 Kapsamlı literatür taraması yapıyorum...")
        addThinkingStep(thinkingMessage, "   • Akademik kaynakları inceliyorum")
        delay(1200)
        addThinkingStep(thinkingMessage, "   • Endüstri standartlarını araştırıyorum")
        delay(1200)
        addThinkingStep(thinkingMessage, "   • Case study'leri değerlendiriyorum")
        delay(1200)
        addThinkingStep(thinkingMessage, "💡 5+ yenilikçi çözüm geliştiriyorum...")
        delay(1500)
        addThinkingStep(thinkingMessage, "⚖️ Her çözümü 5 kriterde değerlendiriyorum...")
        addThinkingStep(thinkingMessage, "   • Performans optimizasyonu")
        delay(1000)
        addThinkingStep(thinkingMessage, "   • Ölçeklenebilirlik")
        delay(1000)
        addThinkingStep(thinkingMessage, "   • Bakım kolaylığı")
        delay(1000)
        addThinkingStep(thinkingMessage, "   • Güvenlik")
        delay(1000)
        addThinkingStep(thinkingMessage, "   • Maliyet etkinliği")
        delay(1000)
        addThinkingStep(thinkingMessage, "🎯 En optimize çözüm kombinasyonunu seçiyorum...")
        delay(1000)
        addThinkingStep(thinkingMessage, "📝 Detaylı roadmap ve implementasyon planı...")
        delay(800)
    }

    // YENİ: Düşünme adımı ekleme fonksiyonu
    private suspend fun addThinkingStep(
        thinkingMessage: Message,
        step: String
    ) {
        withContext(Dispatchers.Main) {
            thinkingMessage.thinkingSteps.add(step)

            val position = messageList.indexOf(thinkingMessage)
            if (position != -1) {
                messageAdapter.notifyItemChanged(position)
                recyclerView.scrollToPosition(position)
            }
        }
    }

    // YENİ: Gerçek AI yanıtı (derin düşünme modu için)
    private fun getRealAiResponse(userMessage: String?, base64Image: String?, isDeepThinking: Boolean = false) {
        // ✅ DÜZELTME: Video analiz hatası için validasyon
        val validatedMessage = when {
            userMessage.isNullOrBlank() && base64Image.isNullOrBlank() -> {
                Log.e("AI_RESPONSE", "Hem mesaj hem görsel boş")
                appendChunkToLastMessage("\n❌ Hata: Gönderilecek içerik bulunamadı")
                hideLoading()
                return
            }
            userMessage.isNullOrBlank() -> "Bu görseli analiz et"
            else -> userMessage
        }

        Log.d("AI_RESPONSE", "AI'ye gönderilen mesaj: ${validatedMessage.take(100)}..., görsel: ${!base64Image.isNullOrBlank()}")

        addMessage("...", false)

        currentResponseJob?.cancel()

        val responseJob = mainCoroutineScope.launch {
            delay(100)
            showLoading(
                if (isDeepThinking) "Derin analiz yapılıyor..." else "Yanıt hazırlanıyor...",
                allowCancel = true
            )

            try {
                val conversationHistory = getOptimizedHistory(
                    messageList.dropLast(1),
                    currentProvider
                )

                // ✅ DÜZELTME: Video analiz için özel sistem prompt'u
                val systemPrompt = if (isDeepThinking) {
                    """
                    🧠 DERİN DÜŞÜNME MODU - DETAYLI ANALİZ TALİMATI:
                    
                    KRİTİK GÖREV: Aşağıdaki soruyu NORMALDEN %50 DAHA DETAYLI cevapla!
                    
                    DÜŞÜNME ADIMLARI:
                    1. 🔍 PROBLEM ANALİZİ: Sorunun kök nedenlerini araştır
                    2. 💡 ÇÖZÜM ALTERNATİFLERİ: En az 3 farklı yaklaşım sun
                    3. ⚖️ KARŞILAŞTIRMA: Her birinin artı/eksi yönlerini listele
                    4. 🎯 TAVSİYE: En iyi çözümü seç ve nedenini açıkla
                    5. 📝 UYGULAMA PLANI: Adım adım nasıl uygulanacağını anlat
                    
                    ÖNEMLİ: Normal yanıttan çok daha kapsamlı ve derinlemesine olmalı!
                    
                    SORU: ${userMessage ?: ""}
                    """.trimIndent()
                } else if (validatedMessage.contains("video analiz", ignoreCase = true) ||
                    validatedMessage.contains("video_analiz", ignoreCase = true)) {
                    // ✅ VIDEO ANALİZ İÇİN ÖZEL PROMPT - KOD ÖNERİSİ YAPMA!
                    """
                    📹 VİDEO ANALİZ MODU - SADECE ANALİZ YAP!
                    
                    KRİTİK TALİMATLAR:
                    1. ❌ KOD ÖNERME - SADECE ANALİZ ET
                    2. ❌ TEKNİK ÇÖZÜM ÖNERME - SADECE TESPİT ET
                    3. ✅ SADECE video içeriğini analiz et ve özetle
                    4. ✅ Görsel öğeleri, hareketleri, ortamı tarif et
                    5. ✅ Varsa metinleri oku ve aktar
                    6. ✅ Senaryoyu anlat ve olası anlamları yorumla
                    
                    ÖNEMLİ: SADECE ANALİZ! Kod, çözüm, öneri, teknik detay YOK!
                    """.trimIndent()
                } else {
                    getSystemPrompt(currentProvider)
                }

                val (finalPrompt, finalImage) = when {
                    !base64Image.isNullOrBlank() -> {
                        processImageForModel(base64Image, validatedMessage, currentProvider)
                    }
                    else -> Pair(validatedMessage, null)
                }

                if (finalPrompt.isBlank() && finalImage == null) {
                    throw Exception("Geçersiz istek: boş mesaj ve görsel")
                }

                // ✅ Model tipine göre işlem
                when (currentProvider) {
                    "OPENAI", "DEEPSEEK", "QWEN" -> {
                        val baseUrl = when (currentProvider) {
                            "OPENAI" -> "https://api.openai.com"
                            "DEEPSEEK" -> "https://api.deepseek.com"
                            "QWEN" -> "https://dashscope-intl.aliyuncs.com/compatible-mode"
                            else -> ""
                        }
                        val apiKey = when (currentProvider) {
                            "OPENAI" -> openAiApiKey
                            "DEEPSEEK" -> deepseekApiKey
                            "QWEN" -> dashscopeApiKey
                            else -> ""
                        }

                        callOpenAIMultiModal(
                            apiKey = apiKey,
                            model = currentModel,
                            prompt = finalPrompt,
                            base64Image = finalImage,
                            base = baseUrl,
                            history = conversationHistory,
                            systemPrompt = systemPrompt
                        )
                    }

                    "GEMINI" -> {
                        if (geminiApiKey.isNotBlank()) {
                            val bmp = if (finalImage != null) base64ToBitmap(finalImage) else null
                            callGeminiMultiTurn(
                                apiKey = geminiApiKey,
                                model = currentModel,
                                prompt = finalPrompt,
                                image = bmp,
                                history = conversationHistory,
                                systemPrompt = systemPrompt
                            )
                        } else {
                            throw Exception("Gemini API anahtarı boş")
                        }
                    }

                    else -> throw Exception("Bilinmeyen sağlayıcı: $currentProvider")
                }
            } catch (e: CancellationException) {
                appendCancellationNote()
                hideLoading()
                throw e
            } catch (e: Exception) {
                Log.e("MainActivity", "API Error", e)
                appendChunkToLastMessage("\n❌ Hata: ${e.message}")
                saveFinalAiResponse()
                hideLoading()
            } finally {
                currentResponseJob = null
            }
        }

        responseJob.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                mainCoroutineScope.launch { hideLoading() }
            }
        }

        currentResponseJob = responseJob
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val nightMode = sharedPreferences.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }

            db = AppDatabase.getDatabase(this)
            loadApiKeys()

            setContentView(R.layout.activity_main)

            // Video analiz manager'ı başlat
            initializeVideoAnalysis()

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        finish()
                    }
                }
            })

            initializeUI()
            setupRecyclerView()
            setupSendButton()
            setupCancelSendButton()
            setupAttachmentButton()
            setupDeepThinkButton()

            mainCoroutineScope.launch {
                showLoading("Modeller yükleniyor...")
                try {
                    fetchModelConfig()
                    loadProviderAndModel()
                    loadThinkingLevel() // YENİ: Düşünme seviyesini yükle
                    loadOrCreateSession()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Başlangıç hatası", e)
                    Toast.makeText(this@MainActivity, "Uygulama başlatılırken hata: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    hideLoading()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "OnCreate hatası", e)
            setContentView(R.layout.activity_main)
            Toast.makeText(this, "Uygulama başlatıldı, bazı özellikler kısıtlanmış olabilir", Toast.LENGTH_LONG).show()
        }
    }

    // YENİ: Gelişmiş derin düşünme butonu kurulumu
    private fun setupDeepThinkButton() {
        buttonDeepThink = findViewById(R.id.buttonDeepThink)

        // Kısa tıklama: Aç/Kapat
        buttonDeepThink.setOnClickListener {
            if (currentThinkingLevel == 0) {
                // Kapalıysa Orta seviyeye aç
                setThinkingLevel(2)
            } else {
                // Açıksa kapat
                setThinkingLevel(0)
            }
        }

        // Uzun tıklama: Seviye seçimi
        buttonDeepThink.setOnLongClickListener {
            showThinkingLevelDialog()
            true
        }
    }

    // Video analiz manager başlatma
    private fun initializeVideoAnalysis() {
        videoAnalysisManager = VideoAnalysisManager(this) { base64Image ->
            runBlocking { simpleVisionToText(base64Image) }
        }
    }

    private fun initializeUI() {
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        recyclerView = findViewById(R.id.recyclerView)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        buttonAttachment = findViewById(R.id.buttonAttachment)
        cancelSendButton = findViewById(R.id.buttonCancelSend)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingText = findViewById(R.id.loadingText)
    }

    private fun showLoading(message: String, allowCancel: Boolean = false) {
        runOnUiThread {
            loadingText.text = message
            loadingOverlay.visibility = View.VISIBLE

            // Gönder butonunu iptal moduna al
            if (allowCancel) {
                setSendButtonToCancelMode()
                loadingOverlay.isClickable = false
                loadingOverlay.isFocusable = false
            } else {
                setSendButtonToSendMode()
                buttonSend.isEnabled = false
                loadingOverlay.isClickable = true
                loadingOverlay.isFocusable = true
            }

            buttonAttachment.isEnabled = !allowCancel
            buttonDeepThink.isEnabled = !allowCancel
            editTextMessage.isEnabled = !allowCancel
            cancelSendButton.isVisible = allowCancel
            cancelSendButton.isEnabled = allowCancel
        }
    }

    private fun hideLoading() {
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
            // Butonları tekrar etkinleştir
            setSendButtonToSendMode()
            buttonSend.isEnabled = true
            buttonAttachment.isEnabled = true
            buttonDeepThink.isEnabled = true
            editTextMessage.isEnabled = true
            cancelSendButton.isVisible = false
            loadingOverlay.isClickable = true
            loadingOverlay.isFocusable = true
        }
    }

    private fun setSendButtonToCancelMode() {
        buttonSend.apply {
            isEnabled = true
            setImageResource(R.drawable.ic_close)
            contentDescription = getString(R.string.cancel_sending)
        }
    }

    private fun setSendButtonToSendMode() {
        buttonSend.apply {
            setImageResource(R.drawable.ic_send)
            contentDescription = getString(R.string.send_button_description)
        }
    }

    private suspend fun loadOrCreateSession() {
        try {
            val lastSessionId = sharedPreferences.getLong("last_session_id", -1)
            val sessionExists = lastSessionId != -1L && withContext(Dispatchers.IO) {
                db.sessionDao().getSessionById(lastSessionId) != null
            }
            if (sessionExists) {
                loadSession(lastSessionId)
            } else {
                createNewSession()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Session yükleme hatası", e)
            createNewSession()
        }
    }

    private suspend fun loadSession(sessionId: Long) {
        try {
            currentSessionId = sessionId
            messageList.clear()
            val archivedMessages = withContext(Dispatchers.IO) {
                db.sessionDao().getMessagesForSession(sessionId)
            }
            archivedMessages.forEach { msg ->
                messageList.add(Message(msg.text, msg.isSentByUser, msg.id))
            }
            withContext(Dispatchers.Main) {
                messageAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(if (messageList.isNotEmpty()) messageList.size - 1 else 0)
            }
            updateTitle()
            sharedPreferences.edit().putLong("last_session_id", sessionId).apply()
        } catch (e: Exception) {
            Log.e("MainActivity", "Session load hatası", e)
            throw e
        }
    }

    private suspend fun createNewSession() {
        try {
            val newSession = Session(name = "New Chat - ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
            val newSessionId = withContext(Dispatchers.IO) { db.sessionDao().insertSession(newSession) }
            loadSession(newSessionId)
        } catch (e: Exception) {
            Log.e("MainActivity", "Yeni session oluşturma hatası", e)
            throw e
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.nav_sessions -> {
                    val intent = Intent(this, SessionsActivity::class.java)
                    sessionsLauncher.launch(intent)
                }
                R.id.nav_change_provider -> showProviderSelectionDialog()
                R.id.nav_change_model -> showModelSelectionDialog()
                R.id.nav_new_chat -> showNewChatConfirmation()
                R.id.nav_toggle_theme -> toggleTheme()
                R.id.nav_settings -> showSettingsDialog()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation hatası", e)
            return false
        }
    }

    private fun readContentFromUri(uri: Uri, isImage: Boolean) {
        currentFileReadingJob?.cancel()

        currentFileReadingJob = fileReadingScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    showLoading("Dosya güvenli şekilde okunuyor...")
                }

                val mimeType = contentResolver.getType(uri) ?: ""
                Log.d("FileReading", "Dosya türü: $mimeType, URI: $uri")

                if (isImage || mimeType.startsWith("image/")) {
                    processImageFile(uri)
                    return@launch
                }

                // Video dosyalarını kontrol et
                if (mimeType.startsWith("video/")) {
                    processVideoFile(uri)
                    return@launch
                }

                val fileContent = when {
                    mimeType.startsWith("text/") ||
                            mimeType == "application/javascript" ||
                            mimeType == "application/json" -> {
                        readTextFileSafe(uri)
                    }
                    mimeType == "application/pdf" -> {
                        readPdfContentSafe(uri)
                    }
                    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                        readDocxContentSafe(uri)
                    }
                    mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> {
                        readExcelContentSafe(uri)
                    }
                    mimeType == "text/csv" -> {
                        readCsvContentSafe(uri)
                    }
                    else -> {
                        try {
                            readTextFileSafe(uri)
                        } catch (e: Exception) {
                            val fileName = try {
                                getFileName(uri)
                            } catch (e: Exception) {
                                "Bilinmeyen dosya"
                            }
                            "Desteklenmeyen dosya türü: $mimeType\nDosya: $fileName"
                        }
                    }
                }

                val fileName = getFileName(uri)

                // PENDING DEĞİŞKENLERİNİ AYARLA
                pendingFileContent = fileContent
                pendingFileName = fileName

                Log.d("FILE_DEBUG", "Dosya okundu - İsim: $fileName, Boyut: ${fileContent.length} karakter")

                withContext(Dispatchers.Main) {
                    hideLoading()
                    // Normal dosyalar için işlem
                    setTextSafely(editTextMessage, "📁 Dosya okundu: $fileName\n\nDosya işlenmeye hazır. Gönder butonuna basın.")
                }

            } catch (e: Exception) {
                Log.e("FileReading", "Dosya okuma hatası", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(
                        this@MainActivity,
                        "Dosya okunamadı: ${e.message ?: "Bilinmeyen hata"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun readTextFileSafe(uri: Uri): String {
        val stringBuilder = StringBuilder()
        var lineCount = 0
        val maxLines = 5000
        val maxFileSize = 2 * 1024 * 1024
        var totalSize = 0

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?

                    while (reader.readLine().also { line = it } != null && lineCount < maxLines) {
                        lineCount++
                        val lineSize = line?.length ?: 0
                        totalSize += lineSize

                        if (totalSize > maxFileSize) {
                            stringBuilder.append("\n\n--- UYARI: Dosya çok büyük, sadece ilk ${maxLines} satır gösteriliyor (${maxFileSize/1024}KB) ---")
                            break
                        }

                        stringBuilder.append(line).append('\n')

                        if (lineCount % 100 == 0) {
                            yield()
                        }
                    }
                }
            }

            return stringBuilder.toString()
        } catch (e: Exception) {
            Log.e("FileReading", "Metin dosyası okuma hatası", e)
            throw e
        }
    }

    private suspend fun readPdfContentSafe(uri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)
                val text = StringBuilder()
                val maxPages = 50
                val maxChars = 500000

                for (i in 1..minOf(pdfDocument.numberOfPages, maxPages)) {
                    val page = pdfDocument.getPage(i)
                    val pageText = PdfTextExtractor.getTextFromPage(page)
                    text.append(pageText).append("\n")

                    if (text.length > maxChars) {
                        text.append("\n--- UYARI: PDF çok büyük, sadece ilk ${maxChars} karakter gösteriliyor ---")
                        break
                    }

                    if (i % 10 == 0) yield()
                }

                pdfDocument.close()
                pdfReader.close()

                text.toString().ifBlank { "PDF boş veya okunamadı" }
            } ?: "PDF dosyası açılamadı"
        } catch (e: Exception) {
            Log.e("FileReading", "PDF okuma hatası", e)
            "PDF içeriği okunamadı: ${e.message}"
        }
    }

    private suspend fun readDocxContentSafe(uri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = XWPFDocument(inputStream)
                val text = document.paragraphs.joinToString("\n") { it.text }
                document.close()
                text.ifBlank { "Word dosyası boş" }
            } ?: "Word dosyası açılamadı"
        } catch (e: Exception) {
            Log.e("FileReading", "Word okuma hatası", e)
            "Word içeriği okunamadı: ${e.message}"
        }
    }

    private suspend fun readExcelContentSafe(uri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val text = StringBuilder()
                val maxSheets = 10

                for (sheetIndex in 0 until minOf(workbook.numberOfSheets, maxSheets)) {
                    val sheet = workbook.getSheetAt(sheetIndex)
                    text.append("--- Sayfa: ${sheet.sheetName} ---\n")

                    var rowCount = 0
                    val maxRows = 1000

                    for (row in sheet) {
                        if (rowCount++ >= maxRows) break

                        val rowData = StringBuilder()
                        for (cell in row) {
                            rowData.append(cell.toString()).append("\t")
                        }
                        if (rowData.isNotEmpty()) {
                            text.append(rowData.toString().trim()).append("\n")
                        }
                    }
                    text.append("\n")
                }

                workbook.close()
                text.toString().ifBlank { "Excel dosyası boş" }
            } ?: "Excel dosyası açılamadı"
        } catch (e: Exception) {
            Log.e("FileReading", "Excel okuma hatası", e)
            "Excel içeriği okunamadı: ${e.message}"
        }
    }

    private suspend fun readCsvContentSafe(uri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = CSVReader(InputStreamReader(inputStream))
                val text = StringBuilder()
                var lineCount = 0
                val maxLines = 10000

                var nextLine: Array<String>?
                while (reader.readNext().also { nextLine = it } != null && lineCount < maxLines) {
                    lineCount++
                    text.append(nextLine!!.joinToString(", ")).append("\n")

                    if (lineCount % 100 == 0) yield()
                }

                reader.close()
                text.toString().ifBlank { "CSV dosyası boş" }
            } ?: "CSV dosyası açılamadı"
        } catch (e: Exception) {
            Log.e("FileReading", "CSV okuma hatası", e)
            "CSV içeriği okunamadı: ${e.message}"
        }
    }

    private suspend fun processImageFile(uri: Uri) {
        try {
            val bitmap = uriToBitmap(uri)
            if (bitmap != null) {
                // Resim boyutunu optimize et
                val optimizedBitmap = optimizeBitmapSize(bitmap)
                pendingImageBase64 = bitmapToBase64(optimizedBitmap)

                if (pendingImageBase64.isNullOrEmpty()) {
                    throw Exception("Resim base64'e dönüştürülemedi")
                }

                withContext(Dispatchers.Main) {
                    hideLoading()
                    setTextSafely(editTextMessage, "🖼️ Resim yüklendi! Göndermek için mesaj yazın veya direkt gönder butonuna basın.")
                }

                Log.d("IMAGE_DEBUG", "Resim başarıyla yüklendi, boyut: ${pendingImageBase64!!.length} karakter")
            } else {
                throw Exception("Resim bitmap'e dönüştürülemedi")
            }
        } catch (e: Exception) {
            Log.e("FileReading", "Resim işleme hatası", e)
            withContext(Dispatchers.Main) {
                hideLoading()
                Toast.makeText(this@MainActivity, "❌ Resim yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                pendingImageBase64 = null
            }
        }
    }

    // Video işleme fonksiyonu
    private suspend fun processVideoFile(uri: Uri) {
        showLoading("🎥 Video yükleniyor...")

        try {
            val videoInfo = VideoProcessingUtil.getVideoInfo(this, uri)

            if (videoInfo.durationMs > 30000) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Uzun Video")
                        .setMessage("Video 30 saniyeden uzun. Sadece ilk 30 saniyesi analiz edilecek. Devam etmek istiyor musunuz?")
                        .setPositiveButton("Evet") { _, _ ->
                            mainCoroutineScope.launch { startVideoAnalysis(uri) }
                        }
                        .setNegativeButton("Hayır", null)
                        .show()
                }
            } else {
                startVideoAnalysis(uri)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Video info error", e)
            withContext(Dispatchers.Main) {
                hideLoading()
                Toast.makeText(this@MainActivity, "❌ Video yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun startVideoAnalysis(uri: Uri) {
        withContext(Dispatchers.Main) { hideLoading() }

        val analysisConfig = try {
            promptVideoAnalysisConfig()
        } catch (e: CancellationException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Video analizi iptal edildi", Toast.LENGTH_SHORT).show()
            }
            return
        }

        showLoading("🎬 ${analysisConfig.name} analiz başlıyor...")

        val result = videoAnalysisManager.analyzeVideo(
            videoUri = uri,
            frameIntervalMs = analysisConfig.frameIntervalMs,
            maxFrames = analysisConfig.maxFrames
        ) { progress: Int, totalFrames: Int, status: String ->
            runOnUiThread {
                val statusText = if (totalFrames > 0) {
                    "${analysisConfig.name} Analiz: %$progress - $status (${totalFrames} frame)"
                } else {
                    "${analysisConfig.name} Analiz: %$progress - $status"
                }
                loadingText.text = statusText
            }
        }

        // ✅ DÜZELTME: Video analiz sonrası state yönetimi
        when (result) {
            is VideoAnalysisManager.VideoAnalysisResult.Success -> {
                withContext(Dispatchers.Main) {
                    hideLoading()

                    // ✅ DÜZELTME: Video analiz sonucunu doğrudan mesaj olarak ekle
                    val analysisMessage = "✅ Video analiz tamamlandı!\n\n${result.analysis}"
                    setTextSafely(editTextMessage, analysisMessage)

                    // ✅ DÜZELTME: pendingFileContent'i güvenli şekilde ayarla
                    pendingFileContent = result.analysis
                    pendingFileName = "video_analiz_${System.currentTimeMillis()}.txt"

                    Log.d("VIDEO_DEBUG", "Video analiz tamamlandı, içerik uzunluğu: ${pendingFileContent?.length ?: 0}")
                }
            }
            is VideoAnalysisManager.VideoAnalysisResult.Error -> {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(this@MainActivity, "❌ Video analiz başarısız: ${result.message}", Toast.LENGTH_LONG).show()
                    pendingFileContent = null
                    pendingFileName = null
                }
            }
        }
    }

    private suspend fun promptVideoAnalysisConfig(): VideoAnalysisConfig {
        val configs = listOf(
            VideoAnalysisConfig(
                name = "Hızlı",
                frameIntervalMs = 8_000L,
                maxFrames = 4,
                description = "📉 Hızlı: Her 8 sn'de 1 frame, maksimum 4 frame"
            ),
            VideoAnalysisConfig(
                name = "Standart",
                frameIntervalMs = VideoAnalysisManager.DEFAULT_FRAME_INTERVAL_MS,
                maxFrames = VideoAnalysisManager.DEFAULT_MAX_FRAMES,
                description = "⚖️ Standart: Her 5 sn'de 1 frame, maksimum 10 frame"
            ),
            VideoAnalysisConfig(
                name = "Detaylı",
                frameIntervalMs = 2_000L,
                maxFrames = 15,
                description = "🔍 Detaylı: Her 2 sn'de 1 frame, maksimum 15 frame"
            )
        )

        var selectedIndex = 1

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Analiz Derinliği")
                    .setSingleChoiceItems(
                        configs.map { it.description }.toTypedArray(),
                        selectedIndex
                    ) { _, which ->
                        selectedIndex = which
                    }
                    .setPositiveButton("Başlat") { _, _ ->
                        continuation.resume(configs[selectedIndex])
                    }
                    .setNegativeButton("İptal") { _, _ ->
                        continuation.resumeWithException(CancellationException("Video analizi iptal edildi"))
                    }
                    .setOnCancelListener {
                        continuation.resumeWithException(CancellationException("Video analizi iptal edildi"))
                    }
                    .show()
            }
        }
    }

    // RESİM BOYUTU OPTİMİZASYON FONKSİYONU
    private fun optimizeBitmapSize(bitmap: Bitmap): Bitmap {
        var optimizedBitmap = bitmap
        val maxFileSize = 4 * 1024 * 1024 // 4MB - API limitleri için

        try {
            // Önce orijinal boyutu kontrol et
            var quality = 85
            var outputStream: ByteArrayOutputStream

            do {
                outputStream = ByteArrayOutputStream()
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val byteCount = outputStream.size()

                if (byteCount <= maxFileSize) {
                    break
                }

                quality -= 5
                if (quality < 60) {
                    // OCR için minimum 60 kalite koru, sadece boyut küçült
                    val scale = 0.75f
                    val newWidth = (optimizedBitmap.width * scale).toInt()
                    val newHeight = (optimizedBitmap.height * scale).toInt()
                    optimizedBitmap = Bitmap.createScaledBitmap(optimizedBitmap, newWidth, newHeight, true)
                    quality = 75
                }
            } while (quality > 50)

            outputStream.close()
            Log.d("IMAGE_OPTIMIZE", "OCR için optimize - Kalite: $quality%, Boyut: ${outputStream.size()} byte")

        } catch (e: Exception) {
            Log.e("IMAGE_OPTIMIZE", "Optimize hatası", e)
        }

        return optimizedBitmap
    }

    private fun setTextSafely(editText: EditText, text: String) {
        try {
            if (text.length > 100000) {
                val chunkSize = 50000
                var start = 0

                editText.setText("")

                while (start < text.length) {
                    val end = minOf(start + chunkSize, text.length)
                    val chunk = text.substring(start, end)
                    editText.append(chunk)
                    start = end

                    Thread.sleep(10)
                }
            } else {
                editText.setText(text)
            }
        } catch (e: Exception) {
            Log.e("UI", "Text set etme hatası", e)
            val safeText = if (text.length > 1000) text.take(1000) + "\n[...]" else text
            editText.setText(safeText)
        }
    }

    private suspend fun getFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (sizeIndex != -1) {
                        return@withContext it.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Dosya boyutu alınamadı", e)
        }
        return@withContext 0L
    }

    private suspend fun getFileName(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return@withContext it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Dosya adı alınamadı", e)
        }
        return@withContext "Bilinmeyen dosya"
    }

    private suspend fun readContentFromUrl(url: String) {
        showLoading("Web sitesi okunuyor...")
        try {
            val textContent = withContext(Dispatchers.IO) {
                Jsoup.connect(url)
                    .timeout(30000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get().text()
            }
            if (textContent.isNotBlank()) {
                val prompt = "Bu web sitesinin içeriğini analiz ve özetle:\n\n$textContent"
                addMessage(prompt, true)
            } else {
                Toast.makeText(this@MainActivity, "Web sitesinden içerik alınamadı.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "URL okuma hatası", e)
            Toast.makeText(this@MainActivity, "URL okunurken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            hideLoading()
        }
    }

    private fun showUrlInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Web Sitesi URL'sini Girin")

        val input = EditText(this)
        input.hint = "https://ornek.com"
        builder.setView(input)

        builder.setPositiveButton("Tamam") { dialog, _ ->
            val url = input.text.toString().trim()
            if (url.isNotBlank()) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    mainCoroutineScope.launch { readContentFromUrl(url) }
                } else {
                    Toast.makeText(this@MainActivity, "Geçerli bir URL girin (http:// veya https://)", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private suspend fun uriToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "URI to Bitmap conversion failed", e)
            null
        }
    }

    private fun base64ToBitmap(b64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e("MainActivity", "Base64 to Bitmap failed", e)
            null
        }
    }

    private suspend fun bitmapToBase64(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("MainActivity", "Bitmap to Base64 conversion failed", e)
            null
        }
    }

    private fun toggleTheme() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val newNightMode =
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        sharedPreferences.edit().putInt("night_mode", newNightMode).apply()
        AppCompatDelegate.setDefaultNightMode(newNightMode)
    }

    private suspend fun fetchModelConfig() {
        try {
            val jsonString = withContext(Dispatchers.IO) {
                assets.open("models.json").bufferedReader().use { it.readText() }
            }
            val json = Json { ignoreUnknownKeys = true }
            val config = json.decodeFromString<ModelConfig>(jsonString)
            modelConfig = config.providers.associate { it.provider to it.models }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load model config from assets", e)
            modelConfig = mapOf(
                "OPENAI" to listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"),
                "GEMINI" to listOf("gemini-2.5-flash", "gemini-2.5-pro"),
                "DEEPSEEK" to listOf("deepseek-chat", "deepseek-coder"),
                "QWEN" to listOf("qwen-turbo", "qwen-plus", "qwen-max")
            )
        }
    }

    private fun showProviderSelectionDialog() {
        val providers = modelConfig.keys.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Sağlayıcı Seç")
            .setItems(providers) { _, which -> setProvider(providers[which]) }
            .show()
    }

    private fun showModelSelectionDialog() {
        val models = modelConfig[currentProvider]?.toTypedArray() ?: emptyArray()
        if (models.isEmpty()) {
            Toast.makeText(this@MainActivity, "Bu sağlayıcı için model bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Model Seç")
            .setItems(models) { _, which -> setModel(models[which]) }
            .show()
    }

    private fun setProvider(provider: String) {
        currentProvider = provider
        sharedPreferences.edit().putString("current_provider", provider).apply()
        setModel(modelConfig[provider]?.firstOrNull() ?: "")
    }

    private fun setModel(model: String) {
        currentModel = model
        sharedPreferences.edit().putString("current_model", model).apply()
        updateTitle()
    }

    private fun loadProviderAndModel() {
        currentProvider = sharedPreferences.getString("current_provider", "OPENAI") ?: "OPENAI"
        if (modelConfig.isNotEmpty()) {
            val defaultModel = modelConfig[currentProvider]?.firstOrNull() ?: ""
            currentModel = sharedPreferences.getString("current_model", defaultModel) ?: defaultModel
        }
        updateTitle()
    }

    private fun updateTitle() {
        mainCoroutineScope.launch {
            val sessionName = if (currentSessionId != -1L) {
                withContext(Dispatchers.IO) { db.sessionDao().getSessionById(currentSessionId) }?.name
            } else null
            supportActionBar?.title =
                if (sessionName != null) "$sessionName - $currentModel" else currentModel
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val editTextOpenAi = dialogView.findViewById<EditText>(R.id.editTextOpenAiKey)
        val editTextGemini = dialogView.findViewById<EditText>(R.id.editTextGeminiKey)
        val editTextDeepSeek = dialogView.findViewById<EditText>(R.id.editTextDeepSeekKey)
        val editTextDashScope = dialogView.findViewById<EditText>(R.id.editTextDashScopeKey)

        editTextOpenAi.setText(openAiApiKey)
        editTextGemini.setText(geminiApiKey)
        editTextDeepSeek.setText(deepseekApiKey)
        editTextDashScope.setText(dashscopeApiKey)

        AlertDialog.Builder(this)
            .setTitle("API Anahtarlarını Ayarla")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val newOpenAiKey = editTextOpenAi.text.toString().trim()
                val newGeminiKey = editTextGemini.text.toString().trim()
                val newDeepSeekKey = editTextDeepSeek.text.toString().trim()
                val newDashScopeKey = editTextDashScope.text.toString().trim()
                saveApiKeys(newOpenAiKey, newGeminiKey, newDeepSeekKey, newDashScopeKey)
                Toast.makeText(this@MainActivity, "API Anahtarları kaydedildi.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun saveApiKeys(openAI: String, gemini: String, deepSeek: String, dashScope: String) {
        sharedPreferences.edit().apply {
            putString("user_openai_api_key", openAI)
            putString("user_gemini_api_key", gemini)
            putString("user_deepseek_api_key", deepSeek)
            putString("user_dashscope_api_key", dashScope)
            apply()
        }
        loadApiKeys()
    }

    private fun loadApiKeys() {
        openAiApiKey = sharedPreferences.getString("user_openai_api_key", "") ?: ""
        geminiApiKey = sharedPreferences.getString("user_gemini_api_key", "") ?: ""
        deepseekApiKey = sharedPreferences.getString("user_deepseek_api_key", "") ?: ""
        dashscopeApiKey = sharedPreferences.getString("user_dashscope_api_key", "") ?: ""

        if (openAiApiKey.isEmpty()) openAiApiKey = ""
        if (geminiApiKey.isEmpty()) geminiApiKey = ""
        if (deepseekApiKey.isEmpty()) deepseekApiKey = ""
        if (dashscopeApiKey.isEmpty()) dashscopeApiKey = ""
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(this, messageList, markwon) { messageText ->
            FileDownloadUtil.showActionDialog(this, messageText, null)
        }
        recyclerView.adapter = messageAdapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        recyclerView.setHasFixedSize(false)
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.itemAnimator = null
    }

    // ✅ DÜZELTME: Video analiz hatası için setupSendButton güncellendi
    private fun setupSendButton() {
        buttonSend.setOnClickListener {
            val activeJob = currentResponseJob
            if (activeJob?.isActive == true) {
                cancelActiveSend(activeJob)
                return@setOnClickListener
            }

            val text = editTextMessage.text.toString().trim()

            // ✅ DÜZELTME: Video analiz içeriği kontrolünü iyileştir
            if (pendingFileContent != null && pendingFileContent!!.isNotBlank()) {
                val contentToSend = pendingFileContent!!

                Log.d("SEND_DEBUG", "Video içeriği gönderiliyor, uzunluk: ${contentToSend.length}")

                addMessage(contentToSend, true)
                editTextMessage.text.clear()

                // ✅ KADEMELİ derin düşünme çağrısı
                if (currentThinkingLevel > 0) {
                    getRealDeepThinkingResponse(contentToSend, null)
                } else {
                    getRealAiResponse(contentToSend, null, false)
                }

                // Gönderdikten sonra temizle
                pendingFileContent = null
                pendingFileName = null

            } else if (text.isNotEmpty() || pendingImageBase64 != null) {
                val messageToSend = if (text.isNotEmpty()) text else "Bu görseli analiz et"

                Log.d("SEND_DEBUG", "Normal mesaj gönderiliyor: ${messageToSend.take(50)}...")

                addMessage(messageToSend, true)

                // ✅ KADEMELİ derin düşünme çağrısı
                if (currentThinkingLevel > 0) {
                    getRealDeepThinkingResponse(messageToSend, pendingImageBase64)
                } else {
                    getRealAiResponse(messageToSend, pendingImageBase64, false)
                }

                editTextMessage.text.clear()
                pendingImageBase64 = null
            } else {
                Toast.makeText(this@MainActivity, "❌ Lütfen bir mesaj yazın veya dosya/resim ekleyin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelActiveSend(activeJob: Job) {
        appendCancellationNote()
        activeJob.cancel(CancellationException("User cancelled send"))
        hideLoading()
        Toast.makeText(this@MainActivity, "Gönderim iptal edildi", Toast.LENGTH_SHORT).show()
    }

    private fun setupCancelSendButton() {
        cancelSendButton.setOnClickListener {
            val activeJob = currentResponseJob
            if (activeJob?.isActive == true) {
                cancelActiveSend(activeJob)
            }
        }
    }

    private fun setupAttachmentButton() {
        buttonAttachment.setOnClickListener { showAttachmentOptions() }
    }

    private fun showAttachmentOptions() {
        val options = arrayOf("Kamera", "Galeri", "Video Çek", "Video Seç", "Dosyalar", "Web Sitesi (URL)")
        AlertDialog.Builder(this)
            .setTitle("Kaynak Seç")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> handleCameraOption()
                    1 -> openGallery()
                    2 -> recordVideo()
                    3 -> selectVideo()
                    4 -> openFiles()
                    5 -> showUrlInputDialog()
                }
            }
            .show()
    }

    // Video çekme fonksiyonu:
    private fun recordVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30) // 30 saniye sınırı
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // Yüksek kalite
        }

        if (intent.resolveActivity(packageManager) != null) {
            recordVideoLauncher.launch(intent)
        } else {
            Toast.makeText(this@MainActivity, "Video çekme uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    // Video seçme fonksiyonu:
    private fun selectVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        selectVideoLauncher.launch(intent)
    }

    private fun handleCameraOption() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> openCamera()

            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    File.createTempFile(
                        "JPEG_",
                        ".jpg",
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    )
                } catch (_: IOException) {
                    null
                }
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun openFiles() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        fileLauncher.launch(intent)
    }

    private fun addMessage(text: String, isSentByUser: Boolean) {
        mainCoroutineScope.launch {
            // --- DÜZELTİLEN KISIM ---
            val result = CodeDetectionUtil.detectLanguageAndCode(text)
            val language = result.first
            val codeContent = result.second
            val isCode = language != null && codeContent != null
            // ------------------------

            val messageToSave = ArchivedMessage(
                sessionId = currentSessionId,
                text = text,
                isSentByUser = isSentByUser,
                language = language,
                isCode = isCode,
                codeContent = codeContent
            )

            val messageId = if (text.length < 100 * 1024) {
                withContext(Dispatchers.IO) { db.sessionDao().insertMessage(messageToSave) }
            } else {
                0L
            }

            val uiMessage = Message(text, isSentByUser, messageId)
            messageList.add(uiMessage)
            messageAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.scrollToPosition(messageList.size - 1)
        }
    }

    private fun appendCancellationNote() {
        if (messageList.isEmpty()) return
        val lastMessage = messageList.last()
        if (!lastMessage.isSentByUser) {
            val updatedText = if (lastMessage.text == "..." || lastMessage.text.isBlank()) {
                "❌ Mesaj iptal edildi"
            } else {
                "${lastMessage.text}\n❌ Mesaj iptal edildi"
            }

            messageList[messageList.size - 1] = lastMessage.copy(text = updatedText)
            mainCoroutineScope.launch {
                messageAdapter.notifyItemChanged(messageList.size - 1)
                recyclerView.scrollToPosition(messageList.size - 1)
                saveFinalAiResponse()
            }
        }
    }

    private fun appendChunkToLastMessage(chunk: String) {
        if (messageList.isEmpty()) return
        val lastMessage = messageList.last()
        if (!lastMessage.isSentByUser) {
            if (lastMessage.text == "...") {
                messageList[messageList.size - 1] = lastMessage.copy(text = chunk)
            } else {
                messageList[messageList.size - 1] =
                    lastMessage.copy(text = lastMessage.text + chunk)
            }
            messageAdapter.notifyItemChanged(messageList.size - 1)
        }
    }

    private suspend fun saveFinalAiResponse() {
        if (messageList.isEmpty()) return
        val lastMessage = messageList.last()
        if (!lastMessage.isSentByUser && lastMessage.id != 0L) {
            withContext(Dispatchers.IO) {
                val messageToUpdate = db.sessionDao().getMessageById(lastMessage.id)
                if (messageToUpdate != null) {
                    db.sessionDao().updateMessage(messageToUpdate.copy(text = lastMessage.text))
                }
            }
        }
    }

    private val http by lazy {
        OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(120, TimeUnit.SECONDS)
            .build()
    }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ✅ YENİ: Model-bazlı görsel işleme fonksiyonu
    private suspend fun processImageForModel(
        base64Image: String,
        userMessage: String?,
        provider: String
    ): Pair<String, String?> {
        return when (provider) {
            "OPENAI", "GEMINI" -> {
                // ✅ OpenAI & Gemini: Doğrudan görsel gönder
                val prompt = userMessage ?: "Bu görseli analiz et"
                Pair(prompt, base64Image)
            }

            "DEEPSEEK", "QWEN" -> {
                // ✅ DeepSeek & Qwen: Önce OCR yap, metni gönder
                appendChunkToLastMessage("📷 Görsel metne çevriliyor...")
                val ocrText = simpleVisionToText(base64Image)

                val prompt = if (!userMessage.isNullOrBlank()) {
                    "$userMessage\n\nGörsel Analizi: $ocrText"
                } else {
                    "Görsel Analizi: $ocrText\n\nLütfen bu görselde ne olduğunu detaylıca açıkla."
                }
                Pair(prompt, null) // Görsel yok, sadece metin
            }

            else -> Pair(userMessage ?: "", null)
        }
    }

    // ✅ YENİ: Basit OCR fonksiyonu - Gemini 2.5 flash'a güncellendi
    private suspend fun simpleVisionToText(base64Image: String): String {
        val simplePrompt = """
            Bu görselde ne görüyorsan SADECE tarif et. 
            Yorum yapma, öneride bulunma, kod yazma.
            Görselde ne varsa sadece onu açıkla.
            Metin varsa, metni olduğu gibi oku ve ver.
        """.trimIndent()

        return try {
            // Önce Gemini 2.5 flash dene
            if (geminiApiKey.isNotBlank()) {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = geminiApiKey
                )
                val bmp = base64ToBitmap(base64Image) ?: return "[Resim okunamadı]"
                val input = content {
                    image(bmp)
                    text(simplePrompt)
                }
                val result = generativeModel.generateContent(input)
                result.text ?: "[Gemini: cevap yok]"
            }
            // Sonra OpenAI
            else if (openAiApiKey.isNotBlank()) {
                callOpenAIVisionOnce(
                    baseUrl = "https://api.openai.com",
                    apiKey = openAiApiKey,
                    model = "gpt-4o-mini",
                    base64Image = base64Image,
                    prompt = simplePrompt
                )
            }
            else {
                "[OCR için API anahtarı gerekli]"
            }
        } catch (e: Exception) {
            Log.e("SimpleVision", "OCR hatası", e)
            "[Görsel analiz edilemedi: ${e.message}]"
        }
    }

    // ✅ BU KISIM DÜZELTİLDİ: URL HATASI GİDERİLDİ
    private suspend fun callOpenAIMultiModal(
        apiKey: String,
        model: String,
        prompt: String?,
        base64Image: String?,
        base: String,
        history: List<Message>,
        systemPrompt: String = ""
    ) {
        if (apiKey.isBlank()) throw Exception("API anahtarı boş ($base)")

        // ✅ DÜZELTME: Doğru URL oluşturma
        val url = "$base/v1/chat/completions"

        var isFirstChunk = true

        // Token limitlerini al
        val tokenLimits = getModelTokenLimits("OPENAI", model)

        val messagesJson = buildJsonArray {
            // ✅ SİSTEM PROMPT'U EKLE (derin düşünme veya normal)
            if (systemPrompt.isNotBlank()) {
                add(buildJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("content", JsonPrimitive(systemPrompt))
                })
            }

            // Optimize edilmiş geçmişi ekle
            history.forEach { message ->
                add(buildJsonObject {
                    put("role", JsonPrimitive(if (message.isSentByUser) "user" else "assistant"))
                    put("content", JsonPrimitive(message.text))
                })
            }

            val currentUserContent = buildJsonArray {
                if (!base64Image.isNullOrEmpty()) {
                    add(buildJsonObject {
                        put("type", JsonPrimitive("image_url"))
                        put("image_url", buildJsonObject {
                            put("url", JsonPrimitive("data:image/jpeg;base64,$base64Image"))
                        })
                    })
                }
                val effectiveText = when {
                    !prompt.isNullOrBlank() -> {
                        // Büyük dosya içeriğini optimize et
                        if (prompt.length > 6000) {
                            "Büyük bir dosya içeriği analiz etmem istediniz. " +
                                    "Dosya boyutu: ${prompt.length} karakter. " +
                                    "Önemli kısımları analiz edip sorunları ve iyileştirme önerilerini listeleyebilir misiniz? " +
                                    "İlk 5000 karakter: ${prompt.take(5000)}..."
                        } else {
                            prompt
                        }
                    }
                    !base64Image.isNullOrBlank() -> "Bu resmi analiz et ve Türkçe kısa özet ver."
                    else -> "Lütfen bir metin veya görsel paylaş."
                }

                add(buildJsonObject {
                    put("type", JsonPrimitive("text"))
                    put("text", JsonPrimitive(effectiveText))
                })
            }

            add(buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("content", currentUserContent)
            })
        }

        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", messagesJson)
            put("stream", JsonPrimitive(true))
            put("max_tokens", JsonPrimitive(tokenLimits.maxTokens))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val json = Json { ignoreUnknownKeys = true }

        withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("OpenAI_API", "Error: ${response.code} - $errorBody")
                    withContext(Dispatchers.Main) { hideLoading() }

                    // Token limit hatasını özel olarak handle et
                    if (response.code == 400 && errorBody?.contains("maximum context length") == true) {
                        throw Exception("Mesaj çok uzun. Lütfen daha kısa bir mesaj gönderin veya yeni bir sohbet başlatın.")
                    } else {
                        throw Exception("${response.code} ${response.message}: $errorBody")
                    }
                }
                val source = response.body!!.source()
                while (!source.exhausted()) {
                    coroutineContext.ensureActive()
                    val line = source.readUtf8Line() ?: continue
                    if (line.startsWith("data:")) {
                        if (isFirstChunk) {
                            withContext(Dispatchers.Main) { hideLoading() }
                            isFirstChunk = false
                        }
                        val data = line.substring(5).trim()
                        if (data == "[DONE]") break
                        try {
                            val root = json.parseToJsonElement(data).jsonObject
                            val delta =
                                root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject
                            val content = delta?.get("content")?.jsonPrimitive?.content
                            if (content != null) {
                                withContext(Dispatchers.Main) { appendChunkToLastMessage(content) }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        saveFinalAiResponse()
    }

    private suspend fun callGeminiMultiTurn(
        apiKey: String,
        model: String,
        prompt: String?,
        image: Bitmap?,
        history: List<Message>,
        systemPrompt: String = ""
    ) {
        val tokenLimits = getModelTokenLimits("GEMINI", model)

        val generativeModel = GenerativeModel(
            modelName = model,
            apiKey = apiKey
        )

        val geminiHistory = history.map {
            content(role = if (it.isSentByUser) "user" else "model") { text(it.text) }
        }

        val chat = generativeModel.startChat(
            history = geminiHistory
        )

        val inputContent = content {
            if (image != null) {
                image(image)
            }
            // ✅ SİSTEM PROMPT'U + USER PROMPT'u birleştir
            val fullPrompt = if (systemPrompt.isNotBlank()) {
                "$systemPrompt\n\nKullanıcı Sorusu: $prompt"
            } else {
                prompt ?: ""
            }
            text(fullPrompt)
        }

        try {
            chat.sendMessageStream(inputContent)
                .onEach { response ->
                    withContext(Dispatchers.Main) {
                        if (loadingOverlay.visibility == View.VISIBLE) {
                            hideLoading()
                        }
                        appendChunkToLastMessage(response.text ?: "")
                    }
                }
                .onCompletion { saveFinalAiResponse() }
                .collect()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { hideLoading() }
            throw e
        }
    }

    private suspend fun callOpenAIVisionOnce(
        baseUrl: String,
        apiKey: String,
        model: String,
        base64Image: String,
        prompt: String
    ): String {
        // ✅ DÜZELTME: URL oluşturma mantığı güvenli hale getirildi
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val url = "${safeBaseUrl}v1/chat/completions"

        val jsonBody = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", JsonPrimitive("text"))
                            put("text", JsonPrimitive(prompt))
                        })
                        add(buildJsonObject {
                            put("type", JsonPrimitive("image_url"))
                            put("image_url", buildJsonObject {
                                put("url", JsonPrimitive("data:image/jpeg;base64,$base64Image"))
                            })
                        })
                    })
                })
            })
            put("stream", JsonPrimitive(false))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        return withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@withContext "[Vision hata: HTTP ${response.code}] $body"
                try {
                    val root = Json.parseToJsonElement(body).jsonObject
                    root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("message")?.jsonObject
                        ?.get("content")?.jsonPrimitive?.content
                        ?: "[Vision: yanıt yok]"
                } catch (_: Exception) {
                    "[Vision: parse edilemedi]"
                }
            }
        }
    }

    private fun showNewChatConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Yeni Sohbet")
            .setMessage("Mevcut sohbet geçmişi temizlenerek yeni bir sohbet başlatılsın mı?")
            .setPositiveButton("Evet") { _, _ ->
                mainCoroutineScope.launch {
                    createNewSession()
                    Toast.makeText(this@MainActivity, "Yeni sohbet başlatıldı", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentFileReadingJob?.cancel()
        fileReadingScope.cancel()
        mainCoroutineScope.cancel()
    }
}