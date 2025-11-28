package com.aikodasistani.aikodasistani.projectgenerator.data

import android.util.Log
import com.aikodasistani.aikodasistani.managers.SettingsManager
import com.aikodasistani.aikodasistani.projectgenerator.domain.AiProvider
import com.aikodasistani.aikodasistani.projectgenerator.domain.AiProviderErrorType
import com.aikodasistani.aikodasistani.projectgenerator.domain.AiProviderException
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProviderIdentifier
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProviderOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Adapter that bridges the existing app's AI infrastructure to the AiProvider interface.
 * This adapter does NOT contain any project generation logic - it only provides raw AI text responses.
 * 
 * Provider and option selection comes from UI/ViewModel - this adapter just executes requests.
 */
class AiProviderAdapter(
    private val settingsManager: SettingsManager
) : AiProvider {
    
    companion object {
        private const val TAG = "AiProviderAdapter"
        
        /**
         * Maximum tokens for project generation responses.
         * This is set high to support large project structures with many files.
         */
        private const val MAX_OUTPUT_TOKENS = 16000
        
        /**
         * Temperature for project generation.
         * Set to 0.7 for a balance between creativity and consistency.
         */
        private const val GENERATION_TEMPERATURE = 0.7
    }
    
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override val identifier: ProviderIdentifier
        get() = ProviderIdentifier(settingsManager.currentProvider)
    
    override val availableOptions: List<ProviderOption>
        get() = settingsManager.getModelsForProvider(settingsManager.currentProvider).map { model ->
            ProviderOption(
                id = model,
                displayName = model,
                description = "AI model for generation"
            )
        }
    
    /**
     * Executes a prompt against the current AI provider and returns raw text.
     * This method does NOT interpret the response - it returns exactly what the AI outputs.
     */
    override suspend fun execute(prompt: String, option: ProviderOption): String = withContext(Dispatchers.IO) {
        val provider = settingsManager.currentProvider
        val model = option.id
        
        Log.d(TAG, "Executing prompt on $provider/$model")
        
        try {
            when (provider) {
                "GEMINI" -> executeGemini(prompt, model)
                else -> executeOpenAICompatible(prompt, model, provider)
            }
        } catch (e: AiProviderException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI execution failed", e)
            throw AiProviderException(
                "AI execution failed: ${e.message}",
                AiProviderErrorType.UNKNOWN,
                e
            )
        }
    }
    
    /**
     * Executes a request on OpenAI-compatible APIs (OpenAI, DeepSeek, Qwen, custom providers).
     */
    private suspend fun executeOpenAICompatible(
        prompt: String, 
        model: String, 
        provider: String
    ): String = suspendCancellableCoroutine { continuation ->
        val baseUrl = settingsManager.getProviderBaseUrl(provider)
        val apiKey = getApiKeyForProvider(provider)
        
        if (apiKey.isBlank()) {
            continuation.resumeWithException(
                AiProviderException(
                    "API key not configured for $provider",
                    AiProviderErrorType.AUTHENTICATION_ERROR
                )
            )
            return@suspendCancellableCoroutine
        }
        
        val url = "$baseUrl/v1/chat/completions"
        
        // Build request body
        val requestBody = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", getSystemPromptForProjectGeneration())
                }
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            // Use correct token parameter based on model
            if (requiresMaxCompletionTokens(model)) {
                put("max_completion_tokens", MAX_OUTPUT_TOKENS)
            } else {
                put("max_tokens", MAX_OUTPUT_TOKENS)
            }
            put("temperature", GENERATION_TEMPERATURE)
        }.toString()
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val call = httpClient.newCall(request)
        
        continuation.invokeOnCancellation {
            call.cancel()
        }
        
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(
                        AiProviderException(
                            "Network error: ${e.message}",
                            AiProviderErrorType.NETWORK_ERROR,
                            e
                        )
                    )
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        val errorType = when (response.code) {
                            401 -> AiProviderErrorType.AUTHENTICATION_ERROR
                            429 -> AiProviderErrorType.RATE_LIMITED
                            400 -> AiProviderErrorType.INVALID_REQUEST
                            else -> AiProviderErrorType.PROVIDER_ERROR
                        }
                        continuation.resumeWithException(
                            AiProviderException(
                                "API error ${response.code}: $errorBody",
                                errorType
                            )
                        )
                        return
                    }
                    
                    val responseBody = response.body?.string() ?: ""
                    if (responseBody.isBlank()) {
                        continuation.resumeWithException(
                            AiProviderException(
                                "Empty response from AI",
                                AiProviderErrorType.EMPTY_RESPONSE
                            )
                        )
                        return
                    }
                    
                    try {
                        val content = extractOpenAIContent(responseBody)
                        if (content.isBlank()) {
                            continuation.resumeWithException(
                                AiProviderException(
                                    "No content in AI response",
                                    AiProviderErrorType.EMPTY_RESPONSE
                                )
                            )
                        } else {
                            continuation.resume(content)
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(
                            AiProviderException(
                                "Failed to parse response: ${e.message}",
                                AiProviderErrorType.PROVIDER_ERROR,
                                e
                            )
                        )
                    }
                }
            }
        })
    }
    
    /**
     * Executes a request on Gemini API.
     */
    private suspend fun executeGemini(prompt: String, model: String): String = suspendCancellableCoroutine { continuation ->
        val apiKey = settingsManager.geminiApiKey
        
        if (apiKey.isBlank()) {
            continuation.resumeWithException(
                AiProviderException(
                    "Gemini API key not configured",
                    AiProviderErrorType.AUTHENTICATION_ERROR
                )
            )
            return@suspendCancellableCoroutine
        }
        
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", "${getSystemPromptForProjectGeneration()}\n\n$prompt")
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", MAX_OUTPUT_TOKENS)
                put("temperature", GENERATION_TEMPERATURE)
            }
        }.toString()
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val call = httpClient.newCall(request)
        
        continuation.invokeOnCancellation {
            call.cancel()
        }
        
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(
                        AiProviderException(
                            "Network error: ${e.message}",
                            AiProviderErrorType.NETWORK_ERROR,
                            e
                        )
                    )
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        continuation.resumeWithException(
                            AiProviderException(
                                "Gemini API error ${response.code}: $errorBody",
                                AiProviderErrorType.PROVIDER_ERROR
                            )
                        )
                        return
                    }
                    
                    val responseBody = response.body?.string() ?: ""
                    try {
                        val content = extractGeminiContent(responseBody)
                        if (content.isBlank()) {
                            continuation.resumeWithException(
                                AiProviderException(
                                    "No content in Gemini response",
                                    AiProviderErrorType.EMPTY_RESPONSE
                                )
                            )
                        } else {
                            continuation.resume(content)
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(
                            AiProviderException(
                                "Failed to parse Gemini response: ${e.message}",
                                AiProviderErrorType.PROVIDER_ERROR,
                                e
                            )
                        )
                    }
                }
            }
        })
    }
    
    /**
     * Extracts content from OpenAI-compatible response.
     * Handles both standard content and reasoning_content (for DeepSeek).
     */
    private fun extractOpenAIContent(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val choices = root["choices"]?.jsonArray ?: return ""
        if (choices.isEmpty()) return ""
        
        val message = choices[0].jsonObject["message"]?.jsonObject ?: return ""
        
        // Try content first
        val content = message["content"]?.jsonPrimitive?.contentOrNull
        if (!content.isNullOrBlank() && content != "null") {
            return content
        }
        
        // Fall back to reasoning_content (DeepSeek)
        val reasoningContent = message["reasoning_content"]?.jsonPrimitive?.contentOrNull
        if (!reasoningContent.isNullOrBlank() && reasoningContent != "null") {
            return reasoningContent
        }
        
        return ""
    }
    
    /**
     * Extracts content from Gemini response.
     */
    private fun extractGeminiContent(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val candidates = root["candidates"]?.jsonArray ?: return ""
        if (candidates.isEmpty()) return ""
        
        val content = candidates[0].jsonObject["content"]?.jsonObject ?: return ""
        val parts = content["parts"]?.jsonArray ?: return ""
        if (parts.isEmpty()) return ""
        
        return parts[0].jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
    }
    
    /**
     * Gets API key for a provider.
     */
    private fun getApiKeyForProvider(provider: String): String {
        return when (provider) {
            "OPENAI" -> settingsManager.openAiApiKey
            "DEEPSEEK" -> settingsManager.deepseekApiKey
            "QWEN" -> settingsManager.dashscopeApiKey
            "GEMINI" -> settingsManager.geminiApiKey
            else -> settingsManager.getCustomProviderApiKey(provider)
        }
    }
    
    /**
     * Determines if a model requires max_completion_tokens instead of max_tokens.
     */
    private fun requiresMaxCompletionTokens(model: String): Boolean {
        return when {
            model.startsWith("gpt-5") -> true
            model.startsWith("gpt-4.1") -> true
            model.startsWith("o1") -> true
            model.startsWith("o3") -> true
            model.startsWith("gpt-4o-2024-11") -> true
            model.startsWith("gpt-4o-2024-12") -> true
            model.startsWith("gpt-4o-2025") -> true
            model == "chatgpt-4o-latest" -> true
            else -> false
        }
    }
    
    /**
     * Gets the system prompt for project generation.
     * This instructs the AI to output in the expected format.
     */
    private fun getSystemPromptForProjectGeneration(): String {
        return """
You are an expert software project generator. When asked to create a project, you MUST output the complete project structure and file contents in the following EXACT format:

FORMAT RULES:
1. Folder paths start and end with '/'
2. File names end with ':'
3. File content is indented below the filename
4. Include ALL necessary files for a complete, working project

EXAMPLE FORMAT:
/src/
main.kt:
    fun main() {
        println("Hello World")
    }

/src/utils/
helper.kt:
    object Helper {
        fun doSomething() {}
    }

build.gradle.kts:
    plugins {
        kotlin("jvm") version "1.9.0"
    }

README.md:
    # Project Name
    Description here

CRITICAL REQUIREMENTS:
- Generate COMPLETE, REAL, WORKING code
- Include ALL configuration files (build scripts, manifests, package.json, etc.)
- Include proper folder structure for the framework/language requested
- Use current best practices and latest stable versions
- Make the project immediately runnable after extraction
- DO NOT include explanatory text outside the file structure
- Start your response directly with the project structure
        """.trimIndent()
    }
}
