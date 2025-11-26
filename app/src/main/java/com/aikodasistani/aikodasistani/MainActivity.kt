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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.aikodasistani.aikodasistani.managers.AIPromptManager
import com.aikodasistani.aikodasistani.managers.DialogManager
import com.aikodasistani.aikodasistani.managers.ImageManager
import com.aikodasistani.aikodasistani.managers.MessageManager
import com.aikodasistani.aikodasistani.managers.SettingsManager
import com.aikodasistani.aikodasistani.models.Message
import com.aikodasistani.aikodasistani.models.ThinkingLevel
import com.aikodasistani.aikodasistani.models.TokenLimits
import com.aikodasistani.aikodasistani.ui.MessageAdapter
import com.aikodasistani.aikodasistani.util.CodeAutoCompletionUtil
import com.aikodasistani.aikodasistani.util.CodeDetectionUtil
import com.aikodasistani.aikodasistani.util.FileDownloadUtil
import com.aikodasistani.aikodasistani.util.VideoProcessingUtil
import com.aikodasistani.aikodasistani.util.ZipFileAnalyzerUtil
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
import kotlin.math.min

// Office dosyaları için import'lar
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.opencsv.CSVReader

// PDF okuma için
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor

// Data classes moved to models package

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val THUMBNAIL_MAX_SIZE = 240
        private const val MAX_OCR_IMAGES = 3
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonAttachment: ImageButton
    private lateinit var buttonDeepThink: ImageButton
    private lateinit var cancelSendButton: Button
    private lateinit var attachmentPreviewContainer: View
    private lateinit var imagePreviewList: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: AppDatabase
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var loadingText: TextView

    // Manager instances for clean architecture
    private lateinit var settingsManager: SettingsManager
    private lateinit var aiPromptManager: AIPromptManager
    private lateinit var dialogManager: DialogManager
    private lateinit var imageManager: ImageManager
    private lateinit var messageManager: MessageManager

    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    // Properties (some delegated to managers but kept here for backward compatibility during refactor)
    private var modelConfig: Map<String, List<String>> = emptyMap()
    private var currentProvider: String = ""
    private var currentModel: String = ""
    private var photoURI: Uri? = null
    private val pendingImageBase64List = mutableListOf<String>()
    private var currentSessionId: Long = -1
    private var openAiApiKey = ""
    private var deepseekApiKey = ""
    private var geminiApiKey = ""
    private var dashscopeApiKey = ""
    private var currentThinkingLevel = 0
    
    // Thinking levels now accessed from SettingsManager
    private val thinkingLevels get() = settingsManager.thinkingLevels

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
                val data = result.data
                val uris = mutableListOf<Uri>()

                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { uris.add(it) }
                    }
                }

                data?.data?.let { uri ->
                    if (uris.isEmpty()) {
                        uris.add(uri)
                    }
                }

                if (uris.isNotEmpty()) {
                    handleSelectedImages(uris)
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
        val levels = thinkingLevels.map { "${it.name} - ${it.description}" }
        dialogManager.showThinkingLevelDialog(levels) { index ->
            setThinkingLevel(index)
        }
    }

    // YENİ: Düşünme seviyesini ayarla
    private fun setThinkingLevel(level: Int) {
        settingsManager.setThinkingLevel(level)
        syncFromSettingsManager()
        val selectedLevel = thinkingLevels[level]

        // Buton rengini güncelle
        buttonDeepThink.setColorFilter(ContextCompat.getColor(this, selectedLevel.color))

        // Tooltip göster
        Toast.makeText(
            this@MainActivity,
            "🧠 ${selectedLevel.name} Mod: ${selectedLevel.description}",
            Toast.LENGTH_LONG
        ).show()
    }

    // YENİ: Kaydedilmiş düşünme seviyesini yükle
    private fun loadThinkingLevel() {
        currentThinkingLevel = settingsManager.loadThinkingLevel()
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

    private fun getImageOnlySystemPrompt(imageCount: Int): String {
        val countNote = if (imageCount > 1) {
            "Birden fazla görseli 1), 2), 3) diye numaralandır."
        } else {
            "Tek görseli net ve kısa açıkla."
        }

        return """
        📷 GÖRSEL BETİMLEME MODU (DERİN DÜŞÜNME KAPALI)

        TALİMATLAR:
        - Sadece görselde GÖZÜKENİ Türkçe ve kısa anlat.
        - ❌ Öneri, yorum, tahmin, çözüm, aksiyon verme.
        - ❌ "İstersen" veya "öneririm" gibi yönlendirmeler yapma.
        - ✅ Nesneleri, ortamı, metinleri olduğu gibi aktar.
        - ✅ Emin değilsen "emin değilim" de, uydurma.
        - $countNote
        """.trimIndent()
    }

    private fun buildVisionUserPrompt(userMessage: String?, imageCount: Int): String {
        val baseInstruction = "Bu görsellerde ne görüyorsan SADECE onu anlat. Öneri veya yorum ekleme."
        val numbering = if (imageCount > 1) {
            "Her görseli 1), 2), 3) diye numaralandır ve ayrı ayrı betimle."
        } else {
            "Tek görseli kısa ve net tarif et."
        }

        val userNote = userMessage?.takeIf { it.isNotBlank() }?.let { "Kullanıcı isteği: $it" }

        return listOfNotNull(baseInstruction, numbering, userNote).joinToString("\n")
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

    // YENİ: Gerçek AI çağrısı için kademeli derin düşünme modu - CANLI DÜŞÜNME
    private fun getRealDeepThinkingResponse(userMessage: String?, base64Images: List<String>?) {
        val currentLevel = thinkingLevels[currentThinkingLevel]

        mainCoroutineScope.launch {
            try {
                // Düşünme seviyesi bildirimi - kısa toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "🧠 ${currentLevel.name} Modu Aktif",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Kısa bilgilendirme mesajı ekle
                val infoMessageId = withContext(Dispatchers.IO) {
                    db.sessionDao().insertMessage(
                        ArchivedMessage(
                            sessionId = currentSessionId,
                            text = "🧠 ${currentLevel.name} ile analiz ediliyor...",
                            isSentByUser = false
                        )
                    )
                }
                
                val infoMessage = Message(
                    text = "🧠 ${currentLevel.name} ile analiz ediliyor...",
                    isSentByUser = false,
                    id = infoMessageId
                )
                messageList.add(infoMessage)
                messageAdapter.notifyItemInserted(messageList.size - 1)
                recyclerView.scrollToPosition(messageList.size - 1)

                // Kısa bekleme
                delay(500)
                
                // Bilgilendirme mesajını kaldır
                withContext(Dispatchers.Main) {
                    val infoIndex = messageList.indexOf(infoMessage)
                    if (infoIndex != -1) {
                        messageList.removeAt(infoIndex)
                        messageAdapter.notifyItemRemoved(infoIndex)
                    }
                }

                // GERÇEK AI çağrısı - seviyeye özel prompt ile - CANLI STREAM
                val deepThinkingPrompt = getLeveledThinkingPrompt(userMessage, currentThinkingLevel)

                // ✅ DÜZELTME: Resim durumunu koru ve doğru parametreleri ilet
                getRealAiResponse(deepThinkingPrompt, base64Images, isDeepThinking = true)

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
    private fun getRealAiResponse(userMessage: String?, base64Images: List<String>?, isDeepThinking: Boolean = false) {
        // ✅ DÜZELTME: Video analiz hatası için validasyon
        val hasImages = !base64Images.isNullOrEmpty()
        val validatedMessage = when {
            userMessage.isNullOrBlank() && !hasImages -> {
                Log.e("AI_RESPONSE", "Hem mesaj hem görsel boş")
                appendChunkToLastMessage("\n❌ Hata: Gönderilecek içerik bulunamadı")
                hideLoading()
                return
            }
            userMessage.isNullOrBlank() -> "Bu görseli analiz et"
            else -> userMessage
        }

        Log.d(
            "AI_RESPONSE",
            "AI'ye gönderilen mesaj: ${validatedMessage.take(100)}..., görsel: $hasImages"
        )

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

                val visionOnlyMode = hasImages && !isDeepThinking

                // ✅ DÜZELTME: Video analiz için özel sistem prompt'u
                val systemPrompt = if (isDeepThinking) {
                    """
                    🧠 DERİN DÜŞÜNME MODU - CANLI DÜŞÜNME SÜRECİ:
                    
                    KRİTİK TALİMAT: Düşünme sürecini ADIM ADIM göster ve açıkla!
                    
                    NASIL CEVAP VERECEKSİN:
                    1. İlk olarak "🔍 PROBLEM ANALİZİ:" başlığı altında sorunu analiz et
                    2. Sonra "💭 DÜŞÜNME SÜRECİ:" başlığı altında düşüncelerini paylaş
                    3. Ardından "💡 ÇÖZÜMLERİ DEĞERLENDİRİYORUM:" diyerek alternatifleri sırala
                    4. "⚖️ KARŞILAŞTIRMA:" yaparak her seçeneğin artı/eksilerini listele
                    5. "🎯 EN İYİ ÇÖZÜM:" diyerek seçimini ve nedenini açıkla
                    6. Son olarak "📝 UYGULAMA PLANI:" ile detaylı adımları ver
                    
                    ÖNEMLİ: Her adımı düşünürken düşüncelerini paylaş, sanki sesli düşünüyormuş gibi!
                    Kullanıcı senin gerçek düşünme sürecini görsün. ChatGPT o1 gibi davran!
                    
                    SORU: ${userMessage ?: ""}
                    """.trimIndent()
                } else if (visionOnlyMode) {
                    getImageOnlySystemPrompt(base64Images?.size ?: 1)
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

                val (finalPrompt, finalImages) = when {
                    hasImages -> {
                        processImageForModel(base64Images!!, validatedMessage, currentProvider)
                    }
                    else -> Pair(validatedMessage, null)
                }

                if (finalPrompt.isBlank() && finalImages.isNullOrEmpty()) {
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
                            base64Images = finalImages,
                            base = baseUrl,
                            history = conversationHistory,
                            systemPrompt = systemPrompt
                        )
                    }

                    "GEMINI" -> {
                        if (geminiApiKey.isNotBlank()) {
                            val bmpList = finalImages?.mapNotNull { base64ToBitmap(it) }
                            callGeminiMultiTurn(
                                apiKey = geminiApiKey,
                                model = currentModel,
                                prompt = finalPrompt,
                                images = bmpList,
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
            
            // Initialize managers
            settingsManager = SettingsManager(this)
            aiPromptManager = AIPromptManager()
            dialogManager = DialogManager(this)
            Log.d("AttachmentDebug", "DialogManager initialized in MainActivity.onCreate")
            imageManager = ImageManager(this)
            messageManager = MessageManager()
            
            // Initialize settings manager asynchronously
            mainCoroutineScope.launch {
                settingsManager.initialize()
                // Sync local variables with manager state
                syncFromSettingsManager()
            }
            
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
        attachmentPreviewContainer = findViewById(R.id.attachmentPreviewContainer)
        imagePreviewList = findViewById(R.id.imagePreviewList)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingText = findViewById(R.id.loadingText)
        
        // Initialize dialog manager with loading views
        dialogManager.initializeLoadingViews(loadingOverlay, loadingText)
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
                // ✅ ISSUE #45: Use AttachmentProcessor for unified handling
                val attachment = com.aikodasistani.aikodasistani.util.AttachmentProcessor.processAttachment(this@MainActivity, uri)
                val emoji = com.aikodasistani.aikodasistani.util.AttachmentProcessor.getEmojiForType(attachment.type)
                val sizeStr = com.aikodasistani.aikodasistani.util.AttachmentProcessor.formatFileSize(attachment.sizeBytes)
                
                Log.d("FileReading", "Attachment type: ${attachment.type}, name: ${attachment.displayName}, MIME: ${attachment.mimeType}")

                // Handle based on unified attachment type
                when (attachment.type) {
                    com.aikodasistani.aikodasistani.models.AttachmentType.IMAGE -> {
                        withContext(Dispatchers.Main) {
                            showLoading("Görsel yükleniyor...")
                        }
                        processImageFile(uri)
                        return@launch
                    }
                    
                    com.aikodasistani.aikodasistani.models.AttachmentType.VIDEO -> {
                        withContext(Dispatchers.Main) {
                            showLoading("Video yükleniyor...")
                        }
                        processVideoFile(uri)
                        return@launch
                    }
                    
                    com.aikodasistani.aikodasistani.models.AttachmentType.ZIP -> {
                        // ZIP için loading gösterme, dialog kendi ilerlemesini gösterir
                        processZipFile(uri)
                        return@launch
                    }
                    
                    else -> {
                        // Diğer dosya türleri için loading göster
                        withContext(Dispatchers.Main) {
                            showLoading("Dosya okunuyor...")
                        }
                    }
                }

                // Read content based on attachment type
                val fileContent = when (attachment.type) {
                    com.aikodasistani.aikodasistani.models.AttachmentType.TEXT,
                    com.aikodasistani.aikodasistani.models.AttachmentType.CODE -> {
                        readTextFileSafe(uri)
                    }
                    com.aikodasistani.aikodasistani.models.AttachmentType.PDF -> {
                        readPdfContentSafe(uri)
                    }
                    com.aikodasistani.aikodasistani.models.AttachmentType.WORD -> {
                        readDocxContentSafe(uri)
                    }
                    com.aikodasistani.aikodasistani.models.AttachmentType.EXCEL -> {
                        readExcelContentSafe(uri)
                    }
                    com.aikodasistani.aikodasistani.models.AttachmentType.CSV -> {
                        readCsvContentSafe(uri)
                    }
                    else -> {
                        // Try to read as text, fallback to error message
                        try {
                            readTextFileSafe(uri)
                        } catch (e: Exception) {
                            "Desteklenmeyen dosya türü: ${attachment.mimeType}\nDosya: ${attachment.displayName}"
                        }
                    }
                }

                // PENDING DEĞİŞKENLERİNİ AYARLA
                pendingFileContent = fileContent
                pendingFileName = attachment.displayName

                Log.d("FILE_DEBUG", "Dosya okundu - İsim: ${attachment.displayName}, Boyut: ${fileContent.length} karakter")

                withContext(Dispatchers.Main) {
                    hideLoading()
                    // ✅ ISSUE #45: Show file bubble with unified format
                    setTextSafely(editTextMessage, "$emoji ${attachment.displayName} ($sizeStr)\n\nDosya okundu. Sorunuzu yazın veya Gönder'e basın.")
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

    private fun handleSelectedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return

        currentFileReadingJob?.cancel()

        currentFileReadingJob = fileReadingScope.launch {
            withContext(Dispatchers.Main) {
                showLoading("Görseller yükleniyor...")
            }

            var addedCount = 0

            uris.forEach { uri ->
                try {
                    if (processImageFile(uri, showStatusMessage = false)) {
                        addedCount++
                    }
                } catch (e: Exception) {
                    Log.e("FileReading", "Çoklu görsel yükleme hatası", e)
                }
            }

            withContext(Dispatchers.Main) {
                hideLoading()
                if (addedCount > 0) {
                    refreshAttachmentPreviewVisibility()
                    // Don't set automatic message text, just show a quick toast
                    Toast.makeText(
                        this@MainActivity,
                        "✓ ${pendingImageBase64List.size} görsel eklendi",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Hiçbir görsel eklenemedi",
                        Toast.LENGTH_SHORT
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

    private fun refreshAttachmentPreviewVisibility() {
        attachmentPreviewContainer.isVisible = pendingImageBase64List.isNotEmpty()
        if (pendingImageBase64List.isEmpty()) {
            imagePreviewList.removeAllViews()
        }
    }

    private fun clearPendingImages() {
        pendingImageBase64List.clear()
        imagePreviewList.removeAllViews()
        refreshAttachmentPreviewVisibility()
    }

    private fun addImagePreview(base64Image: String, previewBitmap: Bitmap) {
        val previewView = LayoutInflater.from(this).inflate(R.layout.item_image_preview, imagePreviewList, false)
        val imageView = previewView.findViewById<ImageView>(R.id.imagePreview)
        val removeButton = previewView.findViewById<ImageButton>(R.id.buttonRemovePreview)

        imageView.setImageBitmap(previewBitmap)

        removeButton.setOnClickListener {
            pendingImageBase64List.remove(base64Image)
            imagePreviewList.removeView(previewView)
            refreshAttachmentPreviewVisibility()
        }

        imagePreviewList.addView(previewView)
        refreshAttachmentPreviewVisibility()
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val scale = min(
            THUMBNAIL_MAX_SIZE.toFloat() / bitmap.width.toFloat(),
            THUMBNAIL_MAX_SIZE.toFloat() / bitmap.height.toFloat()
        ).coerceAtMost(1f)

        val targetWidth = maxOf(1, (bitmap.width * scale).toInt())
        val targetHeight = maxOf(1, (bitmap.height * scale).toInt())
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private suspend fun processImageFile(uri: Uri, showStatusMessage: Boolean = true): Boolean {
        return try {
            val bitmap = uriToBitmap(uri) ?: throw Exception("Resim bitmap'e dönüştürülemedi")

            val optimizedBitmap = optimizeBitmapSize(bitmap)
            val base64Image = bitmapToBase64(optimizedBitmap)
                ?: throw Exception("Resim base64'e dönüştürülemedi")

            val thumbnail = createThumbnail(optimizedBitmap)

            withContext(Dispatchers.Main) {
                pendingImageBase64List.add(base64Image)
                addImagePreview(base64Image, thumbnail)

                if (showStatusMessage) {
                    hideLoading()
                    // Don't set automatic message text, just show a quick toast
                    Toast.makeText(
                        this@MainActivity,
                        "✓ Görsel eklendi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            Log.d(
                "IMAGE_DEBUG",
                "Resim başarıyla yüklendi, boyut: ${base64Image.length} karakter, toplam: ${pendingImageBase64List.size}"
            )

            true
        } catch (e: Exception) {
            Log.e("FileReading", "Resim işleme hatası", e)
            withContext(Dispatchers.Main) {
                if (showStatusMessage) hideLoading()
                Toast.makeText(this@MainActivity, "❌ Resim yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
            }
            false
        }
    }

    // Video işleme fonksiyonu
    private suspend fun processVideoFile(uri: Uri) {
        showLoading("🎥 Video yükleniyor...")

        try {
            val videoInfo = VideoProcessingUtil.getVideoInfo(this, uri)
            val durationSeconds = videoInfo.durationMs / 1000

            // Show info about video duration, but proceed with full video
            withContext(Dispatchers.Main) {
                hideLoading()
                val message = if (durationSeconds > 60) {
                    "Video uzunluğu: ${durationSeconds}s. Tüm video analiz edilecek. Bu işlem zaman alabilir."
                } else {
                    "Video uzunluğu: ${durationSeconds}s. Analiz başlatılıyor."
                }
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Video Analizi")
                    .setMessage(message)
                    .setPositiveButton("Devam") { _, _ ->
                        mainCoroutineScope.launch { startVideoAnalysis(uri) }
                    }
                    .setNegativeButton("İptal", null)
                    .show()
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

    // ==================== PROFESYONEl ZIP İŞLEME ====================
    
    // ZIP analiz sonucu ve URI'yi sakla
    private var currentZipAnalysisResult: ZipFileAnalyzerUtil.ZipAnalysisResult? = null
    private var currentZipUri: Uri? = null
    private var zipAnalysisDialog: AlertDialog? = null
    private var isZipAnalysisComplete: Boolean = false
    
    private suspend fun processZipFile(uri: Uri) {
        currentZipUri = uri
        val fileName = getFileName(uri)
        
        withContext(Dispatchers.Main) {
            showProfessionalZipAnalysisDialog(fileName, uri)
        }
    }
    
    /**
     * Profesyonel ZIP Analiz Dialog'u - Otomatik analiz başlatır
     */
    private fun showProfessionalZipAnalysisDialog(fileName: String, uri: Uri) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_zip_analysis, null)
        
        // View bağlantıları
        val tvZipFileName = dialogView.findViewById<TextView>(R.id.tvZipFileName)
        val tvZipFileInfo = dialogView.findViewById<TextView>(R.id.tvZipFileInfo)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.progressBarZip)
        val tvProgressStatus = dialogView.findViewById<TextView>(R.id.tvProgressStatus)
        val tvLiveAnalysis = dialogView.findViewById<TextView>(R.id.tvLiveAnalysis)
        val statsSection = dialogView.findViewById<LinearLayout>(R.id.statsSection)
        val tvFileCount = dialogView.findViewById<TextView>(R.id.tvFileCount)
        val tvFolderCount = dialogView.findViewById<TextView>(R.id.tvFolderCount)
        val tvTotalSize = dialogView.findViewById<TextView>(R.id.tvTotalSize)
        val tvProjectType = dialogView.findViewById<TextView>(R.id.tvProjectType)
        val actionButtonsRow1 = dialogView.findViewById<LinearLayout>(R.id.actionButtonsRow1)
        val actionButtons = dialogView.findViewById<LinearLayout>(R.id.actionButtons)
        val btnSelectFiles = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectFiles)
        val btnFixErrors = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFixErrors)
        val btnAddFeature = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddFeature)
        val btnDownloadZip = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownloadZip)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnAnalyze = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAnalyze)
        
        // New views for project tree and collapsible analysis
        val projectTreeSection = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.projectTreeSection)
        val rvProjectTree = dialogView.findViewById<RecyclerView>(R.id.rvProjectTree)
        val liveAnalysisHeader = dialogView.findViewById<LinearLayout>(R.id.liveAnalysisHeader)
        val liveAnalysisContent = dialogView.findViewById<android.widget.ScrollView>(R.id.liveAnalysisContent)
        val ivAnalysisExpand = dialogView.findViewById<ImageView>(R.id.ivAnalysisExpand)
        val tvAnalysisToggle = dialogView.findViewById<TextView>(R.id.tvAnalysisToggle)
        
        // Setup project tree RecyclerView
        val projectTreeAdapter = com.aikodasistani.aikodasistani.ui.ProjectTreeAdapter()
        rvProjectTree.layoutManager = LinearLayoutManager(this)
        rvProjectTree.adapter = projectTreeAdapter
        
        // Setup collapsible live analysis
        var isAnalysisExpanded = true
        liveAnalysisHeader.setOnClickListener {
            isAnalysisExpanded = !isAnalysisExpanded
            liveAnalysisContent.visibility = if (isAnalysisExpanded) View.VISIBLE else View.GONE
            ivAnalysisExpand.setImageResource(
                if (isAnalysisExpanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
            )
            tvAnalysisToggle.setText(
                if (isAnalysisExpanded) R.string.zip_hide_analysis else R.string.zip_show_analysis
            )
        }
        
        // Dialog oluştur
        zipAnalysisDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        zipAnalysisDialog?.show()
        
        // Başlangıç değerleri - analiz otomatik başlayacak
        tvZipFileName.text = fileName
        // ✅ NEUTRAL MESSAGE (Issue #43): No "analyzing" - just "reading"
        tvZipFileInfo.text = "⏳ ZIP dosyası okunuyor..."
        tvProgressStatus.text = "Başlatılıyor..."
        tvLiveAnalysis.text = "📦 ZIP içeriği okunuyor...\n\n⏳ Lütfen bekleyin..."
        isZipAnalysisComplete = false
        
        // Canlı log stringbuilder
        val liveLog = StringBuilder()
        
        // Buton aksiyonları
        btnCancel.setOnClickListener {
            zipAnalysisDialog?.dismiss()
            currentZipAnalysisResult = null
            currentZipUri = null
            isZipAnalysisComplete = false
        }
        
        btnAnalyze.setOnClickListener {
            if (isZipAnalysisComplete) {
                // ✅ SILENT CODE READER (Issue #43): 
                // Only send raw code bundle when user explicitly clicks this button
                zipAnalysisDialog?.dismiss()
                currentZipAnalysisResult?.let { result ->
                    // Use raw code bundle instead of formatted analysis
                    val rawCodeBundle = ZipFileAnalyzerUtil.buildRawCodeBundle(result)
                    pendingFileContent = rawCodeBundle
                    pendingFileName = fileName
                    
                    // ✅ NEUTRAL MESSAGE (Issue #43): Don't auto-send, let user type their question
                    editTextMessage.setText("")
                    editTextMessage.hint = "Bu kodla ilgili sorunuzu yazın..."
                    Toast.makeText(this@MainActivity, "✅ Kodlar yüklendi. Sorunuzu yazın ve Gönder'e basın.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Analiz başarısız olmuş veya tamamlanmamış, tekrar dene
                liveLog.clear()
                performZipAnalysis(
                    uri, fileName, liveLog,
                    tvZipFileInfo, tvProgressStatus, progressBar, tvLiveAnalysis,
                    statsSection, actionButtonsRow1, actionButtons,
                    tvFileCount, tvFolderCount, tvTotalSize, tvProjectType,
                    btnAnalyze, btnCancel, projectTreeSection, projectTreeAdapter
                )
            }
        }
        
        btnFixErrors.setOnClickListener {
            zipAnalysisDialog?.dismiss()
            currentZipAnalysisResult?.let { result ->
                val errorFixPrompt = ZipFileAnalyzerUtil.generateErrorFixPrompt(result)
                pendingFileContent = errorFixPrompt
                pendingFileName = fileName
                
                // Otomatik gönder
                mainCoroutineScope.launch {
                    addMessage("🔧 ZIP Hata Düzeltme: $fileName", true)
                    if (currentThinkingLevel > 0) {
                        getRealDeepThinkingResponse(errorFixPrompt, null)
                    } else {
                        getRealAiResponse(errorFixPrompt, null, false)
                    }
                }
            }
        }
        
        btnAddFeature.setOnClickListener {
            zipAnalysisDialog?.dismiss()
            showAddFeatureDialog()
        }
        
        btnDownloadZip.setOnClickListener {
            currentZipAnalysisResult?.let { result ->
                downloadModifiedZip(result)
            }
        }
        
        btnSelectFiles.setOnClickListener {
            currentZipAnalysisResult?.let { result ->
                showFileSelectionDialog(result, fileName)
            }
        }
        
        // ✅ OTOMATİK ANALİZ BAŞLAT - Dialog açılır açılmaz analiz başlar
        performZipAnalysis(
            uri, fileName, liveLog,
            tvZipFileInfo, tvProgressStatus, progressBar, tvLiveAnalysis,
            statsSection, actionButtonsRow1, actionButtons,
            tvFileCount, tvFolderCount, tvTotalSize, tvProjectType,
            btnAnalyze, btnCancel, projectTreeSection, projectTreeAdapter
        )
    }
    
    /**
     * Özellik ekleme dialog'u
     */
    private fun showAddFeatureDialog() {
        val input = EditText(this).apply {
            hint = "Örn: Dark mode ekle, Login sayfası oluştur, API entegrasyonu yap..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 5
            setPadding(48, 32, 48, 32)
        }
        
        AlertDialog.Builder(this)
            .setTitle("➕ Hangi Özelliği Ekleyelim?")
            .setMessage("Projenize eklemek istediğiniz özelliği detaylı şekilde açıklayın:")
            .setView(input)
            .setPositiveButton("Özellik Ekle") { _, _ ->
                val featureRequest = input.text.toString().trim()
                if (featureRequest.isNotEmpty()) {
                    currentZipAnalysisResult?.let { result ->
                        val featurePrompt = ZipFileAnalyzerUtil.generateAddFeaturePrompt(result, featureRequest)
                        pendingFileContent = featurePrompt
                        // fileName is already saved during ZIP analysis
                        pendingFileName = pendingFileName ?: "project.zip"
                        setTextSafely(editTextMessage, "➕ Özellik ekleme modu aktif!\n\nİstek: $featureRequest")
                        
                        // Otomatik gönder
                        mainCoroutineScope.launch {
                            delay(500)
                            buttonSend.performClick()
                        }
                    }
                } else {
                    Toast.makeText(this, "Lütfen bir özellik açıklaması girin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    /**
     * Dosya seçim dialog'u - Kullanıcı istediği dosyaları seçip analiz edebilir
     */
    private fun showFileSelectionDialog(result: ZipFileAnalyzerUtil.ZipAnalysisResult, fileName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_file_selection, null)
        
        // View bağlantıları
        val tvSelectedCount = dialogView.findViewById<TextView>(R.id.tvSelectedCount)
        val etSearchFile = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchFile)
        val chipAllFiles = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipAllFiles)
        val chipCodeFiles = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipCodeFiles)
        val rvFileList = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFileList)
        val btnSelectAll = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAll)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnAnalyzeSelected = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAnalyzeSelected)
        
        // RecyclerView ayarla
        val adapter = com.aikodasistani.aikodasistani.ui.FileSelectionAdapter(result.files) { selectedCount ->
            tvSelectedCount.text = if (selectedCount > 0) {
                getString(R.string.files_selected, selectedCount)
            } else {
                getString(R.string.no_files_selected)
            }
            btnAnalyzeSelected.isEnabled = selectedCount > 0
        }
        
        rvFileList.adapter = adapter
        rvFileList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        // Dialog oluştur
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Arama işlevi
        etSearchFile.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                val codeFilesOnly = chipCodeFiles.isChecked
                adapter.filter(query, codeFilesOnly)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Chip filtreler
        chipAllFiles.setOnClickListener {
            chipCodeFiles.isChecked = false
            chipAllFiles.isChecked = true
            val query = etSearchFile.text?.toString() ?: ""
            adapter.filter(query, false)
        }
        
        chipCodeFiles.setOnClickListener {
            chipAllFiles.isChecked = false
            chipCodeFiles.isChecked = true
            val query = etSearchFile.text?.toString() ?: ""
            adapter.filter(query, true)
        }
        
        // Tümünü seç butonu
        btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }
        
        // İptal butonu
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Seçilenleri analiz et butonu
        btnAnalyzeSelected.setOnClickListener {
            val selectedFiles = adapter.getSelectedFiles()
            if (selectedFiles.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_files_selected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            dialog.dismiss()
            zipAnalysisDialog?.dismiss()
            
            // Seçili dosyaları AI'ye gönder
            val analysisText = ZipFileAnalyzerUtil.formatSelectedFilesAnalysis(selectedFiles, result.projectType)
            pendingFileContent = analysisText
            pendingFileName = fileName
            
            mainCoroutineScope.launch {
                addMessage("📂 Seçili Dosya Analizi: $fileName\n\n${selectedFiles.size} dosya seçildi ve analiz için gönderildi.", true)
                if (currentThinkingLevel > 0) {
                    getRealDeepThinkingResponse(analysisText, null)
                } else {
                    getRealAiResponse(analysisText, null, false)
                }
            }
        }
        
        // Dialog'u göster
        dialog.show()
    }
    
    /**
     * Düzenlenmiş ZIP'i indir
     */
    private fun downloadModifiedZip(result: ZipFileAnalyzerUtil.ZipAnalysisResult) {
        mainCoroutineScope.launch {
            showLoading("📥 ZIP oluşturuluyor...")
            
            try {
                val saveResult = ZipFileAnalyzerUtil.createModifiedZip(
                    context = this@MainActivity,
                    originalResult = result,
                    modifiedFiles = emptyMap(), // Şu an için değişiklik yok, orijinal içerik
                    outputFileName = "project_${System.currentTimeMillis()}.zip"
                )
                
                withContext(Dispatchers.Main) {
                    hideLoading()
                    
                    if (saveResult.success) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("✅ ZIP İndirildi!")
                            .setMessage("Dosya kaydedildi:\n${saveResult.filePath}")
                            .setPositiveButton("Tamam", null)
                            .setNeutralButton("Paylaş") { _, _ ->
                                shareZipFile(saveResult.filePath!!)
                            }
                            .show()
                    } else {
                        Toast.makeText(
                            this@MainActivity, 
                            "❌ ZIP oluşturulamadı: ${saveResult.errorMessage}", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(this@MainActivity, "❌ Hata: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * ZIP dosyasını paylaş
     */
    private fun shareZipFile(filePath: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "ZIP Dosyasını Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(this, "Paylaşım hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * ZIP dosyası analiz işlemini gerçekleştirir
     */
    private fun performZipAnalysis(
        uri: Uri,
        fileName: String,
        liveLog: StringBuilder,
        tvZipFileInfo: TextView,
        tvProgressStatus: TextView,
        progressBar: android.widget.ProgressBar,
        tvLiveAnalysis: TextView,
        statsSection: LinearLayout,
        actionButtonsRow1: LinearLayout,
        actionButtons: LinearLayout,
        tvFileCount: TextView,
        tvFolderCount: TextView,
        tvTotalSize: TextView,
        tvProjectType: TextView,
        btnAnalyze: com.google.android.material.button.MaterialButton,
        btnCancel: com.google.android.material.button.MaterialButton,
        projectTreeSection: androidx.cardview.widget.CardView? = null,
        projectTreeAdapter: com.aikodasistani.aikodasistani.ui.ProjectTreeAdapter? = null
    ) {
        // Butonları devre dışı bırak - analiz sırasında
        btnAnalyze.isEnabled = false
        btnAnalyze.text = "⏳ Analiz ediliyor..."
        btnCancel.text = "İptal Et"
        btnCancel.isEnabled = true
        
        tvZipFileInfo.text = "⏳ ZIP dosyası okunuyor..."
        tvProgressStatus.text = "Başlatılıyor..."
        liveLog.clear()
        liveLog.append("🔍 ZIP dosyası analiz ediliyor...\n\n")
        tvLiveAnalysis.text = liveLog.toString()
        
        mainCoroutineScope.launch {
            try {
                val analysisResult = ZipFileAnalyzerUtil.analyzeZipFile(
                    contentResolver, 
                    uri
                ) { progress, currentFile, status ->
                    // Canlı güncelleme - Main thread'de
                    runOnUiThread {
                        progressBar.progress = progress
                        tvProgressStatus.text = "$progress% - $status"
                        
                        // Canlı log'a ekle
                        if (status.isNotEmpty()) {
                            liveLog.append("$status\n")
                            tvLiveAnalysis.text = liveLog.toString()
                            
                            // Auto-scroll için parent'ı bul
                            (tvLiveAnalysis.parent as? android.widget.ScrollView)?.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }
                
                // Analiz tamamlandı
                currentZipAnalysisResult = analysisResult
                
                withContext(Dispatchers.Main) {
                    if (analysisResult.success) {
                        // ✅ NEUTRAL MESSAGE (Issue #43): No "send to AI" prompts
                        tvZipFileInfo.text = "✅ Dosyalar başarıyla okundu. Artık bu kodla ilgili sorular sorabilirsiniz."
                        statsSection.visibility = View.VISIBLE
                        actionButtonsRow1.visibility = View.VISIBLE
                        actionButtons.visibility = View.VISIBLE
                        
                        tvFileCount.text = analysisResult.totalFiles.toString()
                        tvFolderCount.text = analysisResult.directoryStructure.size.toString()
                        tvTotalSize.text = formatFileSizeSimple(analysisResult.totalSize)
                        tvProjectType.text = getProjectTypeEmoji(analysisResult.projectType)
                        
                        // Update project tree view
                        projectTreeSection?.visibility = View.VISIBLE
                        projectTreeAdapter?.setData(
                            analysisResult.directoryStructure,
                            analysisResult.files
                        )
                        
                        // Progress'i tamamlandı olarak güncelle
                        progressBar.progress = 100
                        // ✅ NEUTRAL MESSAGE (Issue #43): Simple completion status
                        tvProgressStatus.text = "✅ Okuma tamamlandı"
                        
                        // ✅ NEUTRAL LOG (Issue #43): Only technical info, no commentary
                        liveLog.append("\n" + "═".repeat(40) + "\n")
                        liveLog.append("✅ DOSYALAR BAŞARIYLA OKUNDU\n")
                        liveLog.append("📁 ${analysisResult.totalFiles} dosya\n")
                        liveLog.append("📂 ${analysisResult.directoryStructure.size} klasör\n")
                        liveLog.append("💾 ${formatFileSizeSimple(analysisResult.totalSize)}\n")
                        
                        val codeFilesCount = analysisResult.files.count { it.isCodeFile && it.content != null }
                        liveLog.append("📝 ${codeFilesCount} kod dosyası yüklendi\n")
                        
                        // Dil dağılımı - neutral info only
                        val languages = analysisResult.files
                            .filter { it.language != null }
                            .groupBy { it.language!! }
                            .mapValues { it.value.size }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)
                        
                        if (languages.isNotEmpty()) {
                            liveLog.append("\n💻 Programlama Dilleri:\n")
                            languages.forEach { (lang, count) ->
                                liveLog.append("  • $lang: $count dosya\n")
                            }
                        }
                        
                        // ✅ NEUTRAL MESSAGE (Issue #43): Ready for questions, not "send to AI"
                        liveLog.append("\n✅ Sorularınız için hazır.")
                        
                        tvLiveAnalysis.text = liveLog.toString()
                        
                        // ✅ SILENT CODE READER (Issue #43): 
                        // Use raw code bundle instead of formatted analysis
                        // This stores code content without commentary
                        pendingFileContent = ZipFileAnalyzerUtil.buildRawCodeBundle(analysisResult)
                        pendingFileName = fileName
                        
                        // Analiz tamamlandı durumuna geç
                        isZipAnalysisComplete = true
                        
                        // ✅ NEUTRAL BUTTON TEXT (Issue #43)
                        btnAnalyze.text = getString(R.string.zip_analyze_with_ai)
                        btnAnalyze.isEnabled = true
                        btnCancel.text = "Kapat"
                        btnCancel.isEnabled = true
                        
                    } else {
                        tvZipFileInfo.text = "❌ Hata: ${analysisResult.errorMessage}"
                        tvProgressStatus.text = "Okuma başarısız"
                        btnAnalyze.text = getString(R.string.zip_retry)
                        btnAnalyze.isEnabled = true
                        btnCancel.text = "Kapat"
                        btnCancel.isEnabled = true
                    }
                }
                
            } catch (e: Exception) {
                Log.e("ZipAnalysis", "Analiz hatası", e)
                withContext(Dispatchers.Main) {
                    tvZipFileInfo.text = "❌ Hata: ${e.message}"
                    tvProgressStatus.text = "Okuma başarısız"
                    btnAnalyze.text = getString(R.string.zip_retry)
                    btnAnalyze.isEnabled = true
                    btnCancel.text = "Kapat"
                    btnCancel.isEnabled = true
                }
            }
        }
    }
    
    /**
     * Proje tipi için emoji döndür
     */
    private fun getProjectTypeEmoji(type: ZipFileAnalyzerUtil.ProjectType): String {
        return when (type) {
            ZipFileAnalyzerUtil.ProjectType.ANDROID -> "📱 Android"
            ZipFileAnalyzerUtil.ProjectType.IOS -> "🍎 iOS"
            ZipFileAnalyzerUtil.ProjectType.REACT -> "⚛️ React"
            ZipFileAnalyzerUtil.ProjectType.NODEJS -> "🟢 Node.js"
            ZipFileAnalyzerUtil.ProjectType.PYTHON -> "🐍 Python"
            ZipFileAnalyzerUtil.ProjectType.JAVA_MAVEN -> "☕ Java"
            ZipFileAnalyzerUtil.ProjectType.GRADLE -> "🐘 Gradle"
            ZipFileAnalyzerUtil.ProjectType.DOTNET -> "💜 .NET"
            ZipFileAnalyzerUtil.ProjectType.FLUTTER -> "🦋 Flutter"
            ZipFileAnalyzerUtil.ProjectType.GO -> "🔵 Go"
            ZipFileAnalyzerUtil.ProjectType.RUST -> "🦀 Rust"
            ZipFileAnalyzerUtil.ProjectType.WEB -> "🌐 Web"
            ZipFileAnalyzerUtil.ProjectType.UNKNOWN -> "❓ Bilinmiyor"
        }
    }

    // Basit dosya boyutu formatı
    private fun formatFileSizeSimple(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
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
        settingsManager.toggleTheme()
    }

    private suspend fun fetchModelConfig() {
        settingsManager.fetchModelConfig()
        syncFromSettingsManager()
    }

    private fun showProviderSelectionDialog() {
        val providers = modelConfig.keys.toTypedArray()
        dialogManager.showProviderSelectionDialog(providers) { provider ->
            setProvider(provider)
        }
    }

    private fun showModelSelectionDialog() {
        val models = modelConfig[currentProvider]?.toTypedArray() ?: emptyArray()
        dialogManager.showModelSelectionDialog(models) { model ->
            setModel(model)
        }
    }

    private fun setProvider(provider: String) {
        settingsManager.setProvider(provider)
        syncFromSettingsManager()
        updateTitle()
    }

    private fun setModel(model: String) {
        settingsManager.setModel(model)
        syncFromSettingsManager()
        updateTitle()
    }

    private fun loadProviderAndModel() {
        settingsManager.loadProviderAndModel()
        syncFromSettingsManager()
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
        dialogManager.showSettingsDialog(
            currentOpenAiKey = openAiApiKey,
            currentGeminiKey = geminiApiKey,
            currentDeepSeekKey = deepseekApiKey,
            currentDashScopeKey = dashscopeApiKey
        ) { newOpenAiKey, newGeminiKey, newDeepSeekKey, newDashScopeKey ->
            saveApiKeys(newOpenAiKey, newGeminiKey, newDeepSeekKey, newDashScopeKey)
        }
    }

    private fun saveApiKeys(openAI: String, gemini: String, deepSeek: String, dashScope: String) {
        settingsManager.saveApiKeys(openAI, gemini, deepSeek, dashScope)
        syncFromSettingsManager()
    }

    private fun loadApiKeys() {
        settingsManager.loadApiKeys()
        syncFromSettingsManager()
    }
    
    /**
     * Sync local variables from SettingsManager
     */
    private fun syncFromSettingsManager() {
        modelConfig = settingsManager.modelConfig
        currentProvider = settingsManager.currentProvider
        currentModel = settingsManager.currentModel
        openAiApiKey = settingsManager.openAiApiKey
        geminiApiKey = settingsManager.geminiApiKey
        deepseekApiKey = settingsManager.deepseekApiKey
        dashscopeApiKey = settingsManager.dashscopeApiKey
        currentThinkingLevel = settingsManager.currentThinkingLevel
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            context = this,
            messages = messageList,
            markwon = markwon,
            onDownloadClick = { messageText ->
                FileDownloadUtil.showActionDialog(this, messageText, null)
            },
            onAnalyzeCodeClick = { code ->
                analyzeSelectedCode(code)
            }
        )
        recyclerView.adapter = messageAdapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        recyclerView.setHasFixedSize(false)
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.itemAnimator = null
    }

    // ✅ FIX: Updated send button - user text has priority over pendingFileContent (Issue #43)
    private fun setupSendButton() {
        buttonSend.setOnClickListener {
            val activeJob = currentResponseJob
            if (activeJob?.isActive == true) {
                cancelActiveSend(activeJob)
                return@setOnClickListener
            }

            val text = editTextMessage.text.toString().trim()

            // ✅ NEW PRIORITY ORDER (Issue #43):
            // 1. User's text message always has priority
            // 2. ZIP/file context is only used when there's no user text OR combined with user text
            
            if (text.isNotEmpty()) {
                // Priority 1: User's text message is primary
                val imagesToSend = pendingImageBase64List.toList()
                
                // If there's pending ZIP context, combine it with user question as background context
                val messageToSend = if (pendingFileContent != null && pendingFileContent!!.isNotBlank()) {
                    // Use raw code bundle as context, user question is primary
                    val zipContext = pendingFileContent!!
                    Log.d("SEND_DEBUG", "User question with ZIP context, question: ${text.take(50)}...")
                    
                    // Build combined message: user question first, then code context
                    """
                    |User Question: $text
                    |
                    |Code Context (from uploaded ZIP file):
                    |$zipContext
                    """.trimMargin()
                } else {
                    text
                }

                Log.d("SEND_DEBUG", "Sending user message: ${text.take(50)}...")

                // Show user's original question in chat (not the combined message)
                addMessage(text, true)

                // ✅ Send the combined message (or just user text if no pending content)
                if (currentThinkingLevel > 0) {
                    getRealDeepThinkingResponse(messageToSend, imagesToSend)
                } else {
                    getRealAiResponse(messageToSend, imagesToSend, false)
                }

                editTextMessage.text.clear()
                clearPendingImages()
                // Clear pending content after sending
                pendingFileContent = null
                pendingFileName = null
                
            } else if (pendingFileContent != null && pendingFileContent!!.isNotBlank()) {
                // Priority 2: Only if there is no user text, send pending file content
                // This is for when user explicitly wants to send the file content
                val contentToSend = pendingFileContent!!

                Log.d("SEND_DEBUG", "Sending file content (no user text), length: ${contentToSend.length}")

                addMessage(contentToSend, true)
                editTextMessage.text.clear()

                if (currentThinkingLevel > 0) {
                    getRealDeepThinkingResponse(contentToSend, null)
                } else {
                    getRealAiResponse(contentToSend, null, false)
                }

                // Clear after sending
                pendingFileContent = null
                pendingFileName = null

            } else if (pendingImageBase64List.isNotEmpty()) {
                // Priority 3: Images only (no text, no file content)
                val messageToSend = "Bu görseli analiz et"
                val imagesToSend = pendingImageBase64List.toList()

                Log.d("SEND_DEBUG", "Sending images only: ${imagesToSend.size} images")

                addMessage(messageToSend, true)

                if (currentThinkingLevel > 0) {
                    getRealDeepThinkingResponse(messageToSend, imagesToSend)
                } else {
                    getRealAiResponse(messageToSend, imagesToSend, false)
                }

                editTextMessage.text.clear()
                clearPendingImages()
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
        buttonAttachment.setOnClickListener {
            // Visible feedback to user and extra log to ensure click reaches app
            Toast.makeText(this@MainActivity, "Attachment button clicked", Toast.LENGTH_SHORT).show()
            Log.i("ATTACH_TEST", "attachment button clicked - showing toast")
            Log.d("AttachmentDebug", "attachment button clicked - invoking showAttachmentOptions")
            showAttachmentOptions()
        }

        // Fallback touch listener: some devices / overlays may prevent normal click delivery.
        // This will capture raw touch events and trigger the attachment options on ACTION_UP.
        buttonAttachment.setOnTouchListener { v, event ->
            try {
                val actionName = when (event.action) {
                    MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
                    MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
                    MotionEvent.ACTION_UP -> "ACTION_UP"
                    MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
                    else -> "ACTION_${event.action}"
                }
                Log.i("ATTACH_TEST", "buttonAttachment onTouch: $actionName")

                if (event.action == MotionEvent.ACTION_UP) {
                    // small visual feedback and call the same handler
                    v.performClick()
                    showAttachmentOptions()
                }
            } catch (e: Exception) {
                Log.e("AttachmentDebug", "onTouch error", e)
            }
            // Return true to indicate we've handled the touch; prevents duplicate click callbacks
            true
        }
    }

    // Add XML onClick handler to ensure clicks are received even if setOnClickListener fails
    fun onAttachmentButtonClicked(view: View) {
        Toast.makeText(this@MainActivity, "Attachment (XML onClick) clicked", Toast.LENGTH_SHORT).show()
        Log.i("ATTACH_TEST", "onAttachmentButtonClicked (XML) invoked")
        // Call existing handler to keep behavior consistent
        showAttachmentOptions()
    }

    private fun showAttachmentOptions() {
        Log.d("AttachmentDebug", "showAttachmentOptions() called")
         dialogManager.showAttachmentOptionsDialog(
             onCameraSelected = { handleCameraOption() },
             onGallerySelected = { openGallery() },
             onFileSelected = { openFiles() },
             onVideoSelected = { selectVideo() },
             onRecordVideoSelected = { recordVideo() },
             onUrlSelected = { showUrlInputDialog() }
         )
     }

    // Video çekme fonksiyonu:
    private fun recordVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            // No duration limit - support full video duration
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
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        galleryLauncher.launch(Intent.createChooser(intent, "Görselleri seç"))
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
            // Check for code errors and suggest auto-fix
            checkAndSuggestCodeFix(lastMessage.text)
            
            // ✅ ISSUE #45: Check for document generation requests in AI response
            checkAndHandleDocumentGeneration(lastMessage.text)
        }
    }
    
    /**
     * ✅ ISSUE #45: Check if AI response contains a document generation request
     * and generate the file if found.
     */
    private fun checkAndHandleDocumentGeneration(aiResponse: String) {
        val documentRequest = com.aikodasistani.aikodasistani.util.DocumentGenerator.parseDocumentRequest(aiResponse)
        
        if (documentRequest != null) {
            mainCoroutineScope.launch {
                try {
                    val result = com.aikodasistani.aikodasistani.util.DocumentGenerator.generateDocument(
                        this@MainActivity,
                        documentRequest
                    )
                    
                    when (result) {
                        is com.aikodasistani.aikodasistani.util.DocumentGenerator.GenerationResult.Success -> {
                            withContext(Dispatchers.Main) {
                                showGeneratedDocumentDialog(result.document)
                            }
                        }
                        is com.aikodasistani.aikodasistani.util.DocumentGenerator.GenerationResult.Error -> {
                            Log.e("DocumentGen", "Document generation failed: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DocumentGen", "Error generating document", e)
                }
            }
        }
    }
    
    /**
     * ✅ ISSUE #45: Show dialog for generated document with open/share options.
     */
    private fun showGeneratedDocumentDialog(document: com.aikodasistani.aikodasistani.models.GeneratedDocument) {
        val sizeStr = com.aikodasistani.aikodasistani.util.AttachmentProcessor.formatFileSize(document.sizeBytes)
        val emoji = when (document.fileType) {
            "xlsx" -> "📊"
            "csv" -> "📋"
            "md" -> "📝"
            else -> "📄"
        }
        
        AlertDialog.Builder(this)
            .setTitle("$emoji Dosya Oluşturuldu")
            .setMessage("${document.fileName}\nBoyut: $sizeStr")
            .setPositiveButton("Aç") { _, _ ->
                openGeneratedDocument(document)
            }
            .setNeutralButton("Paylaş") { _, _ ->
                shareGeneratedDocument(document)
            }
            .setNegativeButton("Kapat", null)
            .show()
    }
    
    /**
     * ✅ ISSUE #45: Open generated document with external app.
     */
    private fun openGeneratedDocument(document: com.aikodasistani.aikodasistani.models.GeneratedDocument) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(document.contentUri, document.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Bu dosya türünü açabilecek uygulama bulunamadı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("DocumentGen", "Error opening document", e)
            Toast.makeText(this, "Dosya açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * ✅ ISSUE #45: Share generated document via Android share sheet.
     */
    private fun shareGeneratedDocument(document: com.aikodasistani.aikodasistani.models.GeneratedDocument) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = document.mimeType
                putExtra(Intent.EXTRA_STREAM, document.contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Dosyayı Paylaş"))
        } catch (e: Exception) {
            Log.e("DocumentGen", "Error sharing document", e)
            Toast.makeText(this, "Paylaşım hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Analyzes AI-generated code for errors and suggests auto-fixes
     */
    private fun checkAndSuggestCodeFix(messageText: String) {
        // Extract code blocks from the message
        val codeBlockPattern = Regex("```(\\w+)?\\s*([\\s\\S]*?)```")
        val codeBlocks = codeBlockPattern.findAll(messageText).toList()

        if (codeBlocks.isEmpty()) return

        mainCoroutineScope.launch {
            codeBlocks.forEach { match ->
                val languageHint = match.groupValues[1].ifBlank { null }
                val codeContent = match.groupValues[2].trim()

                if (codeContent.isNotBlank()) {
                    val analysisResult = CodeAutoCompletionUtil.analyzeCode(codeContent, languageHint)

                    if (analysisResult.hasErrors) {
                        withContext(Dispatchers.Main) {
                            showCodeFixSuggestionDialog(analysisResult, codeContent)
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a dialog with code error analysis and auto-fix suggestion
     */
    private fun showCodeFixSuggestionDialog(
        analysisResult: CodeAutoCompletionUtil.CodeAnalysisResult,
        originalCode: String
    ) {
        val errorSummary = CodeAutoCompletionUtil.generateErrorSummary(analysisResult)

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("🔧 Kod Analizi")
            .setMessage(errorSummary)
            .setNegativeButton("Kapat", null)

        if (analysisResult.fixedCode != null) {
            dialogBuilder.setPositiveButton("Düzeltilmiş Kodu Göster") { _, _ ->
                showFixedCodeDialog(originalCode, analysisResult.fixedCode, analysisResult.language)
            }
        }

        dialogBuilder.show()
    }

    /**
     * Shows a dialog with the fixed code and options to copy or send to AI for review
     */
    private fun showFixedCodeDialog(
        originalCode: String,
        fixedCode: String,
        language: String?
    ) {
        val langLabel = language?.uppercase() ?: "CODE"
        val langFormatted = language ?: ""
        val formattedCode = "```$langFormatted\n$fixedCode\n```"

        AlertDialog.Builder(this)
            .setTitle("✨ Düzeltilmiş Kod ($langLabel)")
            .setMessage(fixedCode)
            .setPositiveButton("Kopyala") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Fixed Code", fixedCode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Düzeltilmiş kod panoya kopyalandı", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("AI'ye Gönder") { _, _ ->
                // Send fixed code to AI for verification
                val verificationPrompt = """
                    Bu düzeltilmiş kodu kontrol edebilir misin? 
                    Orijinal kodda tespit edilen hataları düzelttim.
                    
                    $formattedCode
                    
                    Kodda başka hata var mı? Derleme/çalıştırma için hazır mı?
                """.trimIndent()

                editTextMessage.setText(verificationPrompt)
                Toast.makeText(this, "Kod doğrulama için hazır. Gönder butonuna basın.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /**
     * Manually trigger code analysis on selected text
     */
    private fun analyzeSelectedCode(code: String) {
        mainCoroutineScope.launch {
            showLoading("Kod analiz ediliyor...")
            try {
                val result = CodeAutoCompletionUtil.analyzeCode(code)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    if (result.hasErrors) {
                        showCodeFixSuggestionDialog(result, code)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Kodda hata bulunamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(
                        this@MainActivity,
                        "Analiz hatası: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
        base64Images: List<String>,
        userMessage: String?,
        provider: String
    ): Pair<String, List<String>?> {
        if (base64Images.isEmpty()) return Pair(userMessage ?: "", null)

        return when (provider) {
            "OPENAI", "GEMINI" -> {
                // ✅ OpenAI & Gemini: Doğrudan çoklu görsel gönder
                val prompt = buildVisionUserPrompt(userMessage, base64Images.size)
                Pair(prompt, base64Images)
            }

            "DEEPSEEK", "QWEN" -> {
                // ✅ DeepSeek & Qwen: Önce OCR yap, metni gönder
                appendChunkToLastMessage("📷 Görseller metne çevriliyor...")

                val ocrTexts = base64Images.take(MAX_OCR_IMAGES).mapIndexed { index, image ->
                    val ocrText = simpleVisionToText(image)
                    "Görsel ${index + 1}: $ocrText"
                }

                val promptPrefix = buildVisionUserPrompt(userMessage, base64Images.size)
                val prompt = listOf(
                    promptPrefix,
                    "Görsel Analizi (sadece betimle):",
                    ocrTexts.joinToString("\n\n"),
                    "Kurallar: Öneri, yorum, çözüm veya tavsiye verme; sadece gördüğün detayları aktar."
                ).joinToString("\n\n")
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
        base64Images: List<String>?,
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

            // Build content array with text and images
            val currentUserContent = buildJsonArray {
                // Add text content first
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
                    !base64Images.isNullOrEmpty() -> buildVisionUserPrompt(null, base64Images.size)
                    else -> "Lütfen bir metin veya görsel paylaş."
                }
                
                add(buildJsonObject {
                    put("type", JsonPrimitive("text"))
                    put("text", JsonPrimitive(effectiveText))
                })
                
                // Add images
                base64Images?.forEach { image ->
                    add(buildJsonObject {
                        put("type", JsonPrimitive("image_url"))
                        put("image_url", buildJsonObject {
                            put("url", JsonPrimitive("data:image/jpeg;base64,$image"))
                        })
                    })
                }
            }

            // Add single user message with content array
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
        images: List<Bitmap>?,
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
            images?.forEach { bmp ->
                image(bmp)
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
        dialogManager.showNewChatConfirmation {
            mainCoroutineScope.launch {
                createNewSession()
                Toast.makeText(this@MainActivity, "Yeni sohbet başlatıldı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentFileReadingJob?.cancel()
        fileReadingScope.cancel()
        mainCoroutineScope.cancel()
    }
}
