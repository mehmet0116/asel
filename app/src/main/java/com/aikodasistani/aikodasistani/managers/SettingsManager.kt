package com.aikodasistani.aikodasistani.managers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.aikodasistani.aikodasistani.data.ModelConfig
import com.aikodasistani.aikodasistani.models.ThinkingLevel
import com.aikodasistani.aikodasistani.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import android.util.Log

/**
 * Manages application settings, preferences, and API configurations
 * Handles theme, provider/model selection, API keys, and thinking levels
 */
class SettingsManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Thinking levels configuration
    val thinkingLevels = listOf(
        ThinkingLevel(0, "Kapalı", R.color.purple_500, "Normal mod", 0, 1.0),
        ThinkingLevel(1, "Hafif", R.color.green, "Hızlı analiz", 2000, 1.3),
        ThinkingLevel(2, "Orta", R.color.orange, "Dengeli analiz", 4000, 1.7),
        ThinkingLevel(3, "Derin", R.color.deep_orange, "Kapsamlı analiz", 7000, 2.2),
        ThinkingLevel(4, "Çok Derin", R.color.red, "Çok kapsamlı analiz", 10000, 3.0)
    )

    var modelConfig: Map<String, List<String>> = emptyMap()
        private set

    // Custom models added by user for each provider
    private var customModels: MutableMap<String, MutableList<String>> = mutableMapOf()

    var currentProvider: String = "OPENAI"
        private set

    var currentModel: String = ""
        private set

    var openAiApiKey: String = ""
        private set

    var geminiApiKey: String = ""
        private set

    var deepseekApiKey: String = ""
        private set

    var dashscopeApiKey: String = ""
        private set

    var currentThinkingLevel: Int = 0
        private set

    /**
     * Initialize settings manager - load API keys and provider/model configuration
     */
    suspend fun initialize() {
        loadApiKeys()
        loadCustomModels()
        fetchModelConfig()
        loadProviderAndModel()
    }

    /**
     * Load model configuration from assets/models.json
     */
    suspend fun fetchModelConfig() {
        try {
            val jsonString = withContext(Dispatchers.IO) {
                context.assets.open("models.json").bufferedReader().use { it.readText() }
            }
            val json = Json { ignoreUnknownKeys = true }
            val config = json.decodeFromString<ModelConfig>(jsonString)
            modelConfig = config.providers.associate { it.provider to it.models }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Failed to load model config from assets", e)
            modelConfig = mapOf(
                "OPENAI" to listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"),
                "GEMINI" to listOf("gemini-2.5-flash", "gemini-2.5-pro"),
                "DEEPSEEK" to listOf("deepseek-chat", "deepseek-coder"),
                "QWEN" to listOf("qwen-turbo", "qwen-plus", "qwen-max")
            )
        }
    }

    /**
     * Toggle between light and dark theme
     */
    fun toggleTheme() {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val newNightMode =
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        sharedPreferences.edit().putInt("night_mode", newNightMode).apply()
        AppCompatDelegate.setDefaultNightMode(newNightMode)
    }

    /**
     * Set the current AI provider (OPENAI, GEMINI, etc.)
     */
    fun setProvider(provider: String) {
        currentProvider = provider
        sharedPreferences.edit().putString("current_provider", provider).apply()
        setModel(modelConfig[provider]?.firstOrNull() ?: "")
    }

    /**
     * Set the current model for the selected provider
     */
    fun setModel(model: String) {
        currentModel = model
        sharedPreferences.edit().putString("current_model", model).apply()
    }

    /**
     * Load provider and model from shared preferences
     */
    fun loadProviderAndModel() {
        currentProvider = sharedPreferences.getString("current_provider", "OPENAI") ?: "OPENAI"
        if (modelConfig.isNotEmpty()) {
            val defaultModel = modelConfig[currentProvider]?.firstOrNull() ?: ""
            currentModel = sharedPreferences.getString("current_model", defaultModel) ?: defaultModel
        }
    }

    /**
     * Save API keys to shared preferences
     */
    fun saveApiKeys(openAI: String, gemini: String, deepSeek: String, dashScope: String) {
        sharedPreferences.edit().apply {
            putString("user_openai_api_key", openAI)
            putString("user_gemini_api_key", gemini)
            putString("user_deepseek_api_key", deepSeek)
            putString("user_dashscope_api_key", dashScope)
            apply()
        }
        loadApiKeys()
    }

    /**
     * Load API keys from shared preferences
     */
    fun loadApiKeys() {
        openAiApiKey = sharedPreferences.getString("user_openai_api_key", "") ?: ""
        geminiApiKey = sharedPreferences.getString("user_gemini_api_key", "") ?: ""
        deepseekApiKey = sharedPreferences.getString("user_deepseek_api_key", "") ?: ""
        dashscopeApiKey = sharedPreferences.getString("user_dashscope_api_key", "") ?: ""

        if (openAiApiKey.isEmpty()) openAiApiKey = ""
        if (geminiApiKey.isEmpty()) geminiApiKey = ""
        if (deepseekApiKey.isEmpty()) deepseekApiKey = ""
        if (dashscopeApiKey.isEmpty()) dashscopeApiKey = ""
    }

    /**
     * Set thinking level (0-4)
     */
    fun setThinkingLevel(level: Int) {
        currentThinkingLevel = level
        sharedPreferences.edit().putInt("thinking_level", level).apply()
    }

    /**
     * Load thinking level from shared preferences
     */
    fun loadThinkingLevel(): Int {
        currentThinkingLevel = sharedPreferences.getInt("thinking_level", 0)
        return currentThinkingLevel
    }

    /**
     * Get the current thinking level configuration
     */
    fun getCurrentThinkingLevel(): ThinkingLevel {
        return thinkingLevels[currentThinkingLevel]
    }

    /**
     * Get all models for a provider (default + custom)
     */
    fun getModelsForProvider(provider: String): List<String> {
        val defaultModels = modelConfig[provider] ?: emptyList()
        val userCustomModels = customModels[provider] ?: emptyList()
        // Combine default and custom models, avoiding duplicates
        return (defaultModels + userCustomModels).distinct()
    }

    /**
     * Add a custom model for the current provider
     */
    fun addCustomModel(provider: String, modelName: String): Boolean {
        if (modelName.isBlank()) return false
        
        val trimmedName = modelName.trim()
        val existingModels = getModelsForProvider(provider)
        
        // Check if model already exists
        if (existingModels.contains(trimmedName)) {
            return false
        }
        
        // Add to custom models
        if (customModels[provider] == null) {
            customModels[provider] = mutableListOf()
        }
        customModels[provider]!!.add(trimmedName)
        
        // Save to SharedPreferences
        saveCustomModels()
        
        return true
    }

    /**
     * Remove a custom model
     */
    fun removeCustomModel(provider: String, modelName: String): Boolean {
        val removed = customModels[provider]?.remove(modelName) ?: false
        if (removed) {
            saveCustomModels()
        }
        return removed
    }

    /**
     * Check if a model is a custom (user-added) model
     */
    fun isCustomModel(provider: String, modelName: String): Boolean {
        return customModels[provider]?.contains(modelName) ?: false
    }

    /**
     * Get only custom models for a provider
     */
    fun getCustomModelsForProvider(provider: String): List<String> {
        return customModels[provider]?.toList() ?: emptyList()
    }

    /**
     * Save custom models to SharedPreferences
     */
    private fun saveCustomModels() {
        val json = Json { ignoreUnknownKeys = true }
        val serializedMap = customModels.mapValues { it.value.toList() }
        val jsonString = json.encodeToString(
            kotlinx.serialization.serializer<Map<String, List<String>>>(),
            serializedMap
        )
        sharedPreferences.edit().putString("custom_models", jsonString).apply()
        Log.d("SettingsManager", "Saved custom models: $jsonString")
    }

    /**
     * Load custom models from SharedPreferences
     */
    private fun loadCustomModels() {
        try {
            val jsonString = sharedPreferences.getString("custom_models", null)
            if (jsonString != null) {
                val json = Json { ignoreUnknownKeys = true }
                val loadedMap = json.decodeFromString<Map<String, List<String>>>(jsonString)
                customModels = loadedMap.mapValues { it.value.toMutableList() }.toMutableMap()
                Log.d("SettingsManager", "Loaded custom models: $customModels")
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Failed to load custom models", e)
            customModels = mutableMapOf()
        }
    }
}
