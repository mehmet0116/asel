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
    
    // Custom providers added by user (provider name -> base URL)
    private var customProviders: MutableMap<String, CustomProviderConfig> = mutableMapOf()
    
    /**
     * Configuration for a custom provider
     */
    data class CustomProviderConfig(
        val name: String,
        val baseUrl: String,
        val defaultModels: List<String> = emptyList()
    )

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
        loadCustomProviders()
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
        val models = getModelsForProvider(provider)
        setModel(models.firstOrNull() ?: "")
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
        // Check default models from models.json
        val defaultModels = modelConfig[provider] ?: emptyList()
        // Check custom provider's default models
        val customProviderModels = customProviders[provider]?.defaultModels ?: emptyList()
        // Check user-added custom models
        val userCustomModels = customModels[provider] ?: emptyList()
        // Combine all, avoiding duplicates
        return (defaultModels + customProviderModels + userCustomModels).distinct()
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
    
    // ==================== Custom Provider Management ====================
    
    /**
     * Get all providers (default from models.json + custom providers)
     */
    fun getAllProviders(): List<String> {
        val defaultProviders = modelConfig.keys.toList()
        val customProviderNames = customProviders.keys.toList()
        return (defaultProviders + customProviderNames).distinct()
    }
    
    /**
     * Get only custom providers
     */
    fun getCustomProviders(): List<CustomProviderConfig> {
        return customProviders.values.toList()
    }
    
    /**
     * Check if a provider is a custom (user-added) provider
     */
    fun isCustomProvider(providerName: String): Boolean {
        return customProviders.containsKey(providerName)
    }
    
    /**
     * Get custom provider config by name
     */
    fun getCustomProviderConfig(providerName: String): CustomProviderConfig? {
        return customProviders[providerName]
    }
    
    /**
     * Add a custom provider
     * @param name The display name of the provider
     * @param baseUrl The base URL for API calls (e.g., https://api.example.com)
     * @param defaultModels Initial list of models for this provider
     * @return true if added successfully, false if provider already exists
     */
    fun addCustomProvider(name: String, baseUrl: String, defaultModels: List<String> = emptyList()): Boolean {
        if (name.isBlank() || baseUrl.isBlank()) return false
        
        val trimmedName = name.trim().uppercase()
        val trimmedUrl = baseUrl.trim()
        
        // Check if provider already exists (in default or custom)
        // modelConfig keys are uppercase, so comparison is case-insensitive
        if (modelConfig.keys.any { it.equals(trimmedName, ignoreCase = true) } || customProviders.containsKey(trimmedName)) {
            return false
        }
        
        // Add to custom providers
        customProviders[trimmedName] = CustomProviderConfig(
            name = trimmedName,
            baseUrl = trimmedUrl,
            defaultModels = defaultModels
        )
        
        // Also add models if provided
        if (defaultModels.isNotEmpty()) {
            customModels[trimmedName] = defaultModels.toMutableList()
        }
        
        saveCustomProviders()
        Log.d("SettingsManager", "Added custom provider: $trimmedName with URL: $trimmedUrl")
        
        return true
    }
    
    /**
     * Remove a custom provider
     * @param providerName The name of the provider to remove
     * @return true if removed successfully, false if provider doesn't exist or is not custom
     */
    fun removeCustomProvider(providerName: String): Boolean {
        if (!customProviders.containsKey(providerName)) {
            return false
        }
        
        customProviders.remove(providerName)
        customModels.remove(providerName) // Also remove associated models
        
        // If current provider was the removed one, switch to default
        if (currentProvider == providerName) {
            setProvider(modelConfig.keys.firstOrNull() ?: "OPENAI")
        }
        
        saveCustomProviders()
        Log.d("SettingsManager", "Removed custom provider: $providerName")
        
        return true
    }
    
    /**
     * Get the base URL for a provider
     * @param providerName The name of the provider
     * @return The base URL for API calls
     */
    fun getProviderBaseUrl(providerName: String): String {
        // First check custom providers
        customProviders[providerName]?.let { return it.baseUrl }
        
        // Then return default URLs for known providers
        return when (providerName) {
            "OPENAI" -> "https://api.openai.com"
            "GEMINI" -> "https://generativelanguage.googleapis.com"
            "DEEPSEEK" -> "https://api.deepseek.com"
            "QWEN" -> "https://dashscope-intl.aliyuncs.com/compatible-mode"
            else -> ""
        }
    }
    
    /**
     * Save custom providers to SharedPreferences
     */
    private fun saveCustomProviders() {
        val json = Json { ignoreUnknownKeys = true }
        val serializedMap = customProviders.mapValues { 
            mapOf(
                "name" to it.value.name,
                "baseUrl" to it.value.baseUrl,
                "defaultModels" to it.value.defaultModels.joinToString(",")
            )
        }
        val jsonString = json.encodeToString(serializedMap)
        sharedPreferences.edit().putString("custom_providers", jsonString).apply()
        Log.d("SettingsManager", "Saved custom providers: $jsonString")
    }
    
    /**
     * Load custom providers from SharedPreferences
     */
    private fun loadCustomProviders() {
        try {
            val jsonString = sharedPreferences.getString("custom_providers", null)
            if (jsonString != null) {
                val json = Json { ignoreUnknownKeys = true }
                val loadedMap = json.decodeFromString<Map<String, Map<String, String>>>(jsonString)
                customProviders = loadedMap.mapValues { entry ->
                    CustomProviderConfig(
                        name = entry.value["name"] ?: entry.key,
                        baseUrl = entry.value["baseUrl"] ?: "",
                        defaultModels = entry.value["defaultModels"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    )
                }.toMutableMap()
                Log.d("SettingsManager", "Loaded custom providers: $customProviders")
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Failed to load custom providers", e)
            customProviders = mutableMapOf()
        }
    }
    
    /**
     * Save API key for a custom provider
     */
    fun saveCustomProviderApiKey(providerName: String, apiKey: String) {
        sharedPreferences.edit()
            .putString("user_${providerName.lowercase()}_api_key", apiKey)
            .apply()
    }
    
    /**
     * Load API key for a custom provider
     */
    fun getCustomProviderApiKey(providerName: String): String {
        return sharedPreferences.getString("user_${providerName.lowercase()}_api_key", "") ?: ""
    }
}
