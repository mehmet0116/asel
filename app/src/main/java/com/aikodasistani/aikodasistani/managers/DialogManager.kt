package com.aikodasistani.aikodasistani.managers

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_thinking_level, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewLevels)
        
        recyclerView.layoutManager = LinearLayoutManager(activity)
        
        val dialog = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .create()
        
        val adapter = ThinkingLevelAdapter(levels) { position ->
            onLevelSelected(position)
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.show()
    }
    
    /**
     * Adapter for thinking level items
     */
    private class ThinkingLevelAdapter(
        private val levels: List<String>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ThinkingLevelAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view as CardView
            val levelNumber: TextView = view.findViewById(R.id.tvLevelNumber)
            val levelName: TextView = view.findViewById(R.id.tvLevelName)
            val levelDescription: TextView = view.findViewById(R.id.tvLevelDescription)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_thinking_level_card, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val levelText = levels[position]
            holder.levelNumber.text = (position + 1).toString()
            
            // Parse the level text to extract name and description
            val parts = levelText.split(" - ", limit = 2)
            holder.levelName.text = parts.getOrNull(0) ?: levelText
            holder.levelDescription.text = parts.getOrNull(1) ?: "Düşünme seviyesi ${position + 1}"
            
            holder.card.setOnClickListener {
                onItemClick(position)
            }
        }
        
        override fun getItemCount() = levels.size
    }

    /**
     * Show provider selection dialog
     */
    fun showProviderSelectionDialog(
        providers: Array<String>,
        onProviderSelected: (String) -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_provider_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewProviders)
        
        recyclerView.layoutManager = LinearLayoutManager(activity)
        
        val dialog = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .create()
        
        val adapter = ProviderAdapter(providers) { provider ->
            onProviderSelected(provider)
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.show()
    }
    
    /**
     * Adapter for provider items
     */
    private class ProviderAdapter(
        private val providers: Array<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<ProviderAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view as CardView
            val icon: TextView = view.findViewById(R.id.tvProviderIcon)
            val name: TextView = view.findViewById(R.id.tvProviderName)
            val description: TextView = view.findViewById(R.id.tvProviderDescription)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_provider_card, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val provider = providers[position]
            holder.name.text = provider
            
            // Set icon and description based on provider
            when (provider) {
                "OpenAI" -> {
                    holder.icon.text = "🤖"
                    holder.description.text = "GPT-4 ve GPT-3.5 modelleri"
                }
                "Gemini" -> {
                    holder.icon.text = "💎"
                    holder.description.text = "Google'ın AI modelleri"
                }
                "DeepSeek" -> {
                    holder.icon.text = "🔍"
                    holder.description.text = "Derin öğrenme modelleri"
                }
                "DashScope" -> {
                    holder.icon.text = "🌟"
                    holder.description.text = "Alibaba Cloud AI modelleri"
                }
                else -> {
                    holder.icon.text = "🤖"
                    holder.description.text = "AI sağlayıcısı"
                }
            }
            
            holder.card.setOnClickListener {
                onItemClick(provider)
            }
        }
        
        override fun getItemCount() = providers.size
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
        
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_model_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewModels)
        
        recyclerView.layoutManager = LinearLayoutManager(activity)
        
        val dialog = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .create()
        
        val adapter = ModelAdapter(models) { model ->
            onModelSelected(model)
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.show()
    }
    
    /**
     * Adapter for model items
     */
    private class ModelAdapter(
        private val models: Array<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view as CardView
            val icon: TextView = view.findViewById(R.id.tvModelIcon)
            val name: TextView = view.findViewById(R.id.tvModelName)
            val description: TextView = view.findViewById(R.id.tvModelDescription)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_model_card, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val model = models[position]
            holder.name.text = model
            
            // Set icon and description based on model name
            when {
                model.contains("gpt-4", ignoreCase = true) -> {
                    holder.icon.text = "🧠"
                    holder.description.text = "En gelişmiş AI modeli"
                }
                model.contains("gpt-3.5", ignoreCase = true) -> {
                    holder.icon.text = "💡"
                    holder.description.text = "Hızlı ve etkili model"
                }
                model.contains("gemini", ignoreCase = true) -> {
                    holder.icon.text = "💎"
                    holder.description.text = "Google'ın güçlü modeli"
                }
                model.contains("deepseek", ignoreCase = true) -> {
                    holder.icon.text = "🔍"
                    holder.description.text = "Derin analiz modeli"
                }
                model.contains("qwen", ignoreCase = true) -> {
                    holder.icon.text = "🌟"
                    holder.description.text = "Çok dilli model"
                }
                else -> {
                    holder.icon.text = "🤖"
                    holder.description.text = "AI modeli"
                }
            }
            
            holder.card.setOnClickListener {
                onItemClick(model)
            }
        }
        
        override fun getItemCount() = models.size
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
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_attachment_options, null)
        
        val dialog = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<CardView>(R.id.cardCamera).setOnClickListener {
            onCameraSelected()
            dialog.dismiss()
        }
        
        dialogView.findViewById<CardView>(R.id.cardGallery).setOnClickListener {
            onGallerySelected()
            dialog.dismiss()
        }
        
        dialogView.findViewById<CardView>(R.id.cardFile).setOnClickListener {
            onFileSelected()
            dialog.dismiss()
        }
        
        dialogView.findViewById<CardView>(R.id.cardVideo).setOnClickListener {
            onVideoSelected()
            dialog.dismiss()
        }
        
        dialogView.findViewById<CardView>(R.id.cardUrl).setOnClickListener {
            onUrlSelected()
            dialog.dismiss()
        }
        
        dialog.show()
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
