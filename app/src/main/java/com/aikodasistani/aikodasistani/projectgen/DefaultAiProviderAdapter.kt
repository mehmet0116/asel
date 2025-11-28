package com.aikodasistani.aikodasistani.projectgen

import android.content.Context
import android.util.Log
import com.aikodasistani.aikodasistani.BuildConfig
import com.aikodasistani.aikodasistani.managers.SettingsManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import java.util.concurrent.TimeUnit

/**
 * Default AI Provider Adapter
 * 
 * Connects the project generator to the existing AI infrastructure.
 * This adapter is provider-agnostic - it uses the provider/model from
 * the ViewModel (UI selection) to determine how to make the API call.
 * 
 * Supports:
 * - OpenAI (and compatible APIs like DeepSeek, Qwen)
 * - Gemini
 * - Custom providers (OpenAI-compatible)
 * 
 * Key Design:
 * - NO hardcoded model names
 * - Provider comes from SettingsManager (UI-driven)
 * - Model comes from SettingsManager (UI-driven)
 */
class DefaultAiProviderAdapter(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val provider: String,
    private val model: String
) : AiProvider {

    companion object {
        private const val TAG = "AiProviderAdapter"
        private const val TIMEOUT_SECONDS = 180L
        private const val MAX_TOKENS = 8000
        // Default model used when no model is specified
        // This is only a fallback - primary model selection comes from UI
        private const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
    }

    private val http by lazy {
        OkHttpClient.Builder()
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun execute(prompt: String, option: ProviderOption): AiProviderResult = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Executing AI call: provider=$provider, model=$model")
                
                when {
                    provider == "GEMINI" -> executeGemini(prompt)
                    else -> executeOpenAICompatible(prompt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI execution error", e)
                AiProviderResult.Error(
                    message = "AI çağrısı başarısız: ${e.message}",
                    cause = e
                )
            }
        }

    /**
     * Execute with Gemini SDK
     */
    private suspend fun executeGemini(prompt: String): AiProviderResult {
        val apiKey = settingsManager.geminiApiKey
        
        if (apiKey.isBlank()) {
            return AiProviderResult.Error("Gemini API anahtarı bulunamadı")
        }

        return try {
            val generativeModel = GenerativeModel(
                modelName = model.ifBlank { DEFAULT_GEMINI_MODEL },
                apiKey = apiKey
            )

            val inputContent = content {
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val text = response.text

            if (text.isNullOrBlank()) {
                AiProviderResult.Error("Gemini boş yanıt döndürdü")
            } else {
                Log.d(TAG, "Gemini response: ${text.length} chars")
                AiProviderResult.Success(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini error", e)
            AiProviderResult.Error("Gemini hatası: ${e.message}", e)
        }
    }

    /**
     * Execute with OpenAI-compatible API (OpenAI, DeepSeek, Qwen, custom)
     */
    private suspend fun executeOpenAICompatible(prompt: String): AiProviderResult {
        val apiKey = getApiKey()
        val baseUrl = settingsManager.getProviderBaseUrl(provider)
        
        if (apiKey.isBlank()) {
            return AiProviderResult.Error("$provider API anahtarı bulunamadı")
        }

        val url = "$baseUrl/v1/chat/completions"
        
        Log.d(TAG, "OpenAI-compatible call to: $url with model: $model")

        val messagesJson = buildJsonArray {
            add(buildJsonObject {
                put("role", JsonPrimitive("system"))
                put("content", JsonPrimitive(
                    """You are an expert software architect and code generator. 
                    |Generate complete, production-ready project structures.
                    |Output each file with its path and full content.
                    |Use proper formatting and include all necessary configuration files.""".trimMargin()
                ))
            })
            add(buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("content", JsonPrimitive(prompt))
            })
        }

        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", messagesJson)
            put("max_tokens", JsonPrimitive(MAX_TOKENS))
            put("temperature", JsonPrimitive(0.7))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            val response = http.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "API error: ${response.code} - $errorBody")
                return AiProviderResult.Error(
                    "API hatası (${response.code}): ${parseErrorMessage(errorBody)}"
                )
            }

            val responseBody = response.body?.string() ?: ""
            
            val content = try {
                val root = json.parseToJsonElement(responseBody).jsonObject
                root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.content
            } catch (e: Exception) {
                Log.e(TAG, "Response parse error", e)
                null
            }

            if (content.isNullOrBlank()) {
                AiProviderResult.Error("API boş yanıt döndürdü")
            } else {
                Log.d(TAG, "OpenAI-compatible response: ${content.length} chars")
                AiProviderResult.Success(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP error", e)
            AiProviderResult.Error("Bağlantı hatası: ${e.message}", e)
        }
    }

    /**
     * Get API key for current provider
     */
    private fun getApiKey(): String {
        return when (provider.uppercase()) {
            "OPENAI" -> settingsManager.openAiApiKey.ifBlank {
                // Try BuildConfig as fallback
                try { BuildConfig.OPENAI_API_KEY } catch (e: Exception) { "" }
            }
            "DEEPSEEK" -> settingsManager.deepseekApiKey.ifBlank {
                try { BuildConfig.DEEPSEEK_API_KEY } catch (e: Exception) { "" }
            }
            "QWEN" -> settingsManager.dashscopeApiKey.ifBlank {
                try { BuildConfig.DASHSCOPE_API_KEY } catch (e: Exception) { "" }
            }
            else -> {
                // Check for custom provider
                if (settingsManager.isCustomProvider(provider)) {
                    settingsManager.getCustomProviderApiKey(provider)
                } else {
                    ""
                }
            }
        }
    }

    /**
     * Parse error message from API response
     */
    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val root = json.parseToJsonElement(errorBody).jsonObject
            root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                ?: errorBody.take(200)
        } catch (e: Exception) {
            errorBody.take(200)
        }
    }
}
