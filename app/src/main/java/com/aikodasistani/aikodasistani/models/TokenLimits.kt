package com.aikodasistani.aikodasistani.models

/**
 * Token limits configuration for AI models
 * @param maxTokens Maximum tokens for the response
 * @param maxContext Maximum context window size
 * @param historyMessages Number of history messages to include
 */
data class TokenLimits(
    val maxTokens: Int,
    val maxContext: Int,
    val historyMessages: Int
)
