package com.aikodasistani.aikodasistani.projectgenerator.domain

/**
 * Provider-agnostic interface for AI text generation.
 * 
 * Providers MUST:
 * - Only return raw text output from the AI model
 * - NOT know anything about project generation
 * - NOT interpret project structures
 * - NOT contain formatting logic
 * 
 * All AI provider implementations must follow these rules to ensure
 * the system remains provider-agnostic and future-proof.
 */
interface AiProvider {
    /**
     * Executes a prompt against the AI provider and returns the raw text response.
     * 
     * @param prompt The user's prompt/request
     * @param option The provider option (capability variant) to use
     * @return Raw text output from the AI - no interpretation or processing
     * @throws AiProviderException if the provider cannot complete the request
     */
    suspend fun execute(prompt: String, option: ProviderOption): String
    
    /**
     * Returns the unique identifier for this provider.
     */
    val identifier: ProviderIdentifier
    
    /**
     * Returns available options for this provider.
     */
    val availableOptions: List<ProviderOption>
}

/**
 * Exception thrown by AI providers when they cannot complete a request.
 */
class AiProviderException(
    message: String,
    val errorType: AiProviderErrorType,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Types of errors that can occur in AI provider operations.
 */
enum class AiProviderErrorType {
    NETWORK_ERROR,
    AUTHENTICATION_ERROR,
    RATE_LIMITED,
    INVALID_REQUEST,
    PROVIDER_ERROR,
    TIMEOUT,
    EMPTY_RESPONSE,
    UNKNOWN
}
