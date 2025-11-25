package com.aikodasistani.aikodasistani.managers

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.aikodasistani.aikodasistani.R

/**
 * Manages all dialog operations including settings, provider/model selection,
 * loading overlays, and user input dialogs
 */
class DialogManager(private val activity: Activity) {

    private var loadingOverlay: FrameLayout? = null
    private var loadingText: TextView? = null

    /**
     * Initialize loading overlay and text view
     */
    fun initializeLoadingViews(overlay: FrameLayout, text: TextView) {
        loadingOverlay = overlay
        loadingText = text
    }

    /**
     * Show loading overlay with message
     * @param message The loading message to display
     * @param allowCancel Whether to allow canceling the operation
     */
    fun showLoading(message: String, allowCancel: Boolean = false) {
        loadingText?.text = message
        loadingOverlay?.isVisible = true
        loadingOverlay?.isClickable = !allowCancel
        loadingOverlay?.isFocusable = !allowCancel
    }

    /**
     * Hide loading overlay
     */
    fun hideLoading() {
        loadingOverlay?.isVisible = false
    }

    /**
     * Show thinking level selection dialog
     */
    fun showThinkingLevelDialog(
        levels: List<String>,
        onLevelSelected: (Int) -> Unit
    ) {
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("🧠 Derin Düşünme Seviyesi Seçin")
            .setItems(levels.toTypedArray()) { _, which ->
                onLevelSelected(which)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /**
     * Show provider selection dialog
     */
    fun showProviderSelectionDialog(
        providers: Array<String>,
        onProviderSelected: (String) -> Unit
    ) {
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("🔌 Sağlayıcı Seç")
            .setItems(providers) { _, which ->
                onProviderSelected(providers[which])
            }
            .show()
    }

    /**
     * Show model selection dialog
     */
    fun showModelSelectionDialog(
        models: Array<String>,
        onModelSelected: (String) -> Unit
    ) {
        if (models.isEmpty()) {
            Toast.makeText(activity, "Bu sağlayıcı için model bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("🤖 Model Seç")
            .setItems(models) { _, which ->
                onModelSelected(models[which])
            }
            .show()
    }

    /**
     * Show settings dialog for API keys
     */
    fun showSettingsDialog(
        currentOpenAiKey: String,
        currentGeminiKey: String,
        currentDeepSeekKey: String,
        currentDashScopeKey: String,
        onSave: (String, String, String, String) -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_settings, null)
        val editTextOpenAi = dialogView.findViewById<EditText>(R.id.editTextOpenAiKey)
        val editTextGemini = dialogView.findViewById<EditText>(R.id.editTextGeminiKey)
        val editTextDeepSeek = dialogView.findViewById<EditText>(R.id.editTextDeepSeekKey)
        val editTextDashScope = dialogView.findViewById<EditText>(R.id.editTextDashScopeKey)

        editTextOpenAi.setText(currentOpenAiKey)
        editTextGemini.setText(currentGeminiKey)
        editTextDeepSeek.setText(currentDeepSeekKey)
        editTextDashScope.setText(currentDashScopeKey)

        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .setPositiveButton("💾 Kaydet") { _, _ ->
                val newOpenAiKey = editTextOpenAi.text.toString().trim()
                val newGeminiKey = editTextGemini.text.toString().trim()
                val newDeepSeekKey = editTextDeepSeek.text.toString().trim()
                val newDashScopeKey = editTextDashScope.text.toString().trim()
                onSave(newOpenAiKey, newGeminiKey, newDeepSeekKey, newDashScopeKey)
                Toast.makeText(activity, "API Anahtarları kaydedildi.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /**
     * Show URL input dialog
     */
    fun showUrlInputDialog(onUrlEntered: (String) -> Unit) {
        val builder = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
        builder.setTitle("🌐 Web Sitesi URL'sini Girin")

        val input = EditText(activity)
        input.hint = "https://ornek.com"
        builder.setView(input)

        builder.setPositiveButton("Tamam") { dialog, _ ->
            val url = input.text.toString().trim()
            if (url.isNotBlank()) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    onUrlEntered(url)
                } else {
                    Toast.makeText(activity, "Geçerli bir URL girin (http:// veya https://)", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    /**
     * Show new chat confirmation dialog
     */
    fun showNewChatConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("💬 Yeni Sohbet")
            .setMessage("Yeni bir sohbet başlatmak istediğinizden emin misiniz? Mevcut sohbet kaydedilecek.")
            .setPositiveButton("Evet") { _, _ -> onConfirm() }
            .setNegativeButton("Hayır", null)
            .show()
    }

    /**
     * Show attachment options dialog
     */
    fun showAttachmentOptionsDialog(
        onCameraSelected: () -> Unit,
        onGallerySelected: () -> Unit,
        onFileSelected: () -> Unit,
        onVideoSelected: () -> Unit,
        onUrlSelected: () -> Unit
    ) {
        val options = arrayOf(
            "📷 Fotoğraf Çek",
            "🖼️ Galeriden Seç",
            "📁 Dosya Seç",
            "🎥 Video Seç",
            "🌐 URL'den İçerik Al"
        )

        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("📎 Dosya Ekle")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onCameraSelected()
                    1 -> onGallerySelected()
                    2 -> onFileSelected()
                    3 -> onVideoSelected()
                    4 -> onUrlSelected()
                }
            }
            .show()
    }

    /**
     * Show code fix suggestion dialog
     */
    fun showCodeFixSuggestionDialog(
        errorDescription: String,
        onAnalyzeClick: () -> Unit
    ) {
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("🔧 Kod Hatası Tespit Edildi")
            .setMessage(errorDescription)
            .setPositiveButton("🔍 Analiz Et ve Düzelt") { _, _ ->
                onAnalyzeClick()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /**
     * Show fixed code dialog
     */
    fun showFixedCodeDialog(
        fixedCode: String,
        onApplyClick: (String) -> Unit
    ) {
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("✅ Düzeltilmiş Kod")
            .setMessage(fixedCode)
            .setPositiveButton("✔️ Uygula") { _, _ ->
                onApplyClick(fixedCode)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /**
     * Show add feature dialog
     */
    fun showAddFeatureDialog(onFeatureEntered: (String) -> Unit) {
        val builder = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
        builder.setTitle("✨ Yeni Özellik Ekle")
        builder.setMessage("Eklemek istediğiniz özelliği detaylı açıklayın:")

        val input = EditText(activity)
        input.hint = "Örn: Kullanıcı profil sayfası ekle"
        builder.setView(input)

        builder.setPositiveButton("Ekle") { dialog, _ ->
            val feature = input.text.toString().trim()
            if (feature.isNotBlank()) {
                onFeatureEntered(feature)
            } else {
                Toast.makeText(activity, "Lütfen bir özellik açıklaması girin", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    /**
     * Show long video confirmation dialog
     */
    fun showLongVideoDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Uzun Video")
            .setMessage("Video 30 saniyeden uzun. Sadece ilk 30 saniyesi analiz edilecek. Devam etmek istiyor musunuz?")
            .setPositiveButton("Evet") { _, _ -> onConfirm() }
            .setNegativeButton("Hayır", null)
            .show()
    }
}
