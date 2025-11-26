package com.aikodasistani.aikodasistani.managers

import android.app.Activity
import android.util.Log
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
import com.aikodasistani.aikodasistani.ui.AttachmentOptionsBottomSheet
import com.aikodasistani.aikodasistani.ui.createAttachmentComposeView

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
     * Show model selection dialog with option to add custom model
     */
    fun showModelSelectionDialog(
        models: Array<String>,
        onModelSelected: (String) -> Unit
    ) {
        showModelSelectionDialogWithCustom(models, null, onModelSelected, null, null)
    }

    /**
     * Show model selection dialog with custom model support
     */
    fun showModelSelectionDialogWithCustom(
        models: Array<String>,
        customModels: List<String>?,
        onModelSelected: (String) -> Unit,
        onAddCustomModel: ((String) -> Boolean)?,
        onRemoveCustomModel: ((String) -> Boolean)?
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_model_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewModels)
        
        recyclerView.layoutManager = LinearLayoutManager(activity)
        
        val dialog = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .create()
        
        val adapter = ModelAdapter(
            models = models,
            customModels = customModels ?: emptyList(),
            onItemClick = { model ->
                onModelSelected(model)
                dialog.dismiss()
            },
            onLongClick = { model, isCustom ->
                if (isCustom && onRemoveCustomModel != null) {
                    showRemoveCustomModelDialog(model) {
                        if (onRemoveCustomModel(model)) {
                            Toast.makeText(activity, "Model silindi: $model", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }
                    true
                } else {
                    false
                }
            }
        )
        recyclerView.adapter = adapter
        
        // Add custom model button if callback provided
        if (onAddCustomModel != null) {
            dialogView.findViewById<View>(R.id.btnAddCustomModel)?.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    showAddCustomModelDialog { modelName ->
                        if (onAddCustomModel(modelName)) {
                            Toast.makeText(activity, "Model eklendi: $modelName", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(activity, "Model eklenemedi. Zaten mevcut olabilir.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        dialog.show()
    }

    /**
     * Show dialog to add a custom model
     */
    fun showAddCustomModelDialog(onModelEntered: (String) -> Unit) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_custom_model, null)
        val editTextModelName = dialogView.findViewById<EditText>(R.id.editTextCustomModelName)
        
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setView(dialogView)
            .setPositiveButton("Ekle") { _, _ ->
                val modelName = editTextModelName.text.toString().trim()
                if (modelName.isNotBlank()) {
                    onModelEntered(modelName)
                } else {
                    Toast.makeText(activity, "Lütfen bir model adı girin.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /**
     * Show confirmation dialog to remove a custom model
     */
    private fun showRemoveCustomModelDialog(modelName: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("Özel Modeli Sil")
            .setMessage("\"$modelName\" modelini silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ -> onConfirm() }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    /**
     * Adapter for model items with custom model support
     */
    private class ModelAdapter(
        private val models: Array<String>,
        private val customModels: List<String>,
        private val onItemClick: (String) -> Unit,
        private val onLongClick: ((String, Boolean) -> Boolean)?
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
            val isCustom = customModels.contains(model)
            holder.name.text = if (isCustom) "$model ⭐" else model
            
            // Set icon and description based on model name
            when {
                isCustom -> {
                    holder.icon.text = "⭐"
                    holder.description.text = "Özel model (silmek için uzun bas)"
                }
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
            
            holder.card.setOnLongClickListener {
                onLongClick?.invoke(model, isCustom) ?: false
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
     * Show attachment options dialog using modern BottomSheet with AlertDialog fallback
     */
    fun showAttachmentOptionsDialog(
        onCameraSelected: () -> Unit,
        onGallerySelected: () -> Unit,
        onFileSelected: () -> Unit,
        onVideoSelected: () -> Unit,
        onRecordVideoSelected: () -> Unit,
        onUrlSelected: () -> Unit
    ) {
        Log.d("AttachmentDialog", "showAttachmentOptionsDialog: trying BottomSheet first")
        try {
            // Try to use modern BottomSheet first
            val bottomSheet = AttachmentOptionsBottomSheet(activity)
            bottomSheet.show(
                onCamera = {
                    Log.d("AttachmentDialog", "BottomSheet: optionCamera clicked")
                    onCameraSelected()
                },
                onGallery = {
                    Log.d("AttachmentDialog", "BottomSheet: optionGallery clicked")
                    onGallerySelected()
                },
                onFile = {
                    Log.d("AttachmentDialog", "BottomSheet: optionFile clicked")
                    onFileSelected()
                },
                onVideo = {
                    Log.d("AttachmentDialog", "BottomSheet: optionVideo clicked")
                    onVideoSelected()
                },
                onRecordVideo = {
                    Log.d("AttachmentDialog", "BottomSheet: optionRecordVideo clicked")
                    onRecordVideoSelected()
                },
                onUrl = {
                    Log.d("AttachmentDialog", "BottomSheet: optionUrl clicked")
                    onUrlSelected()
                }
            )
            Log.d("AttachmentDialog", "BottomSheet shown successfully")
        } catch (e: Exception) {
            Log.e("AttachmentDialog", "Failed to show BottomSheet, falling back to AlertDialog", e)
            // Fallback to simple AlertDialog list
            showAlertDialogFallback(
                onCameraSelected,
                onGallerySelected,
                onFileSelected,
                onVideoSelected,
                onRecordVideoSelected,
                onUrlSelected
            )
        }
    }
    
    /**
     * Fallback AlertDialog for attachment options
     */
    private fun showAlertDialogFallback(
        onCameraSelected: () -> Unit,
        onGallerySelected: () -> Unit,
        onFileSelected: () -> Unit,
        onVideoSelected: () -> Unit,
        onRecordVideoSelected: () -> Unit,
        onUrlSelected: () -> Unit
    ) {
        val items = arrayOf("Fotoğraf Çek", "Video Çek", "Galeriden Seç", "Dosya Seç", "Video Seç", "URL'den İçerik Al")
        AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
            .setTitle("Kaynak Seç")
            .setItems(items) { d, which ->
                when (which) {
                    0 -> onCameraSelected()
                    1 -> onRecordVideoSelected()
                    2 -> onGallerySelected()
                    3 -> onFileSelected()
                    4 -> onVideoSelected()
                    5 -> onUrlSelected()
                }
                d.dismiss()
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
        val builder = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
        builder.setTitle("🔧 Kod Hatası Tespit Edildi")
        builder.setMessage(errorDescription)
        builder.setPositiveButton("🔍 Analiz Et ve Düzelt") { _, _ -> onAnalyzeClick() }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }

    /**
     * Show fixed code dialog
     */
    fun showFixedCodeDialog(
        fixedCode: String,
        onApplyClick: (String) -> Unit
    ) {
        val builder = AlertDialog.Builder(activity, R.style.Theme_AIKodAsistani_Dialog)
        builder.setTitle("✅ Düzeltilmiş Kod")
        builder.setMessage(fixedCode)
        builder.setPositiveButton("✔️ Uygula") { _, _ -> onApplyClick(fixedCode) }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }
}
