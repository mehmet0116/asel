package com.aikodasistani.aikodasistani

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AI Provider utility functions
 * Tests for Issue #50: Multi-Provider AI Integration Bug
 */
class AIProviderUtilsTest {

    /**
     * Helper function that mirrors the logic in MainActivity.requiresMaxCompletionTokens()
     * This allows testing the logic without Android dependencies.
     */
    private fun requiresMaxCompletionTokens(model: String): Boolean {
        return when {
            model.startsWith("gpt-4.1") -> true
            model.startsWith("o1") -> true
            model.startsWith("o3") -> true
            model.startsWith("gpt-4o-2024-11") -> true
            model.startsWith("gpt-4o-2024-12") -> true
            model.startsWith("gpt-4o-2025") -> true
            model == "chatgpt-4o-latest" -> true
            model.startsWith("gpt-3.5") -> false
            model.startsWith("gpt-4-turbo") -> false
            model == "gpt-4" -> false
            model.startsWith("gpt-4-0") -> false
            else -> false
        }
    }

    // ==================== OpenAI Token Parameter Tests ====================

    @Test
    fun `test old gpt-3_5-turbo uses max_tokens`() {
        assertFalse(
            "gpt-3.5-turbo should use max_tokens (not max_completion_tokens)",
            requiresMaxCompletionTokens("gpt-3.5-turbo")
        )
    }

    @Test
    fun `test old gpt-4-turbo uses max_tokens`() {
        assertFalse(
            "gpt-4-turbo should use max_tokens (not max_completion_tokens)",
            requiresMaxCompletionTokens("gpt-4-turbo")
        )
    }

    @Test
    fun `test gpt-4 uses max_tokens`() {
        assertFalse(
            "gpt-4 should use max_tokens (not max_completion_tokens)",
            requiresMaxCompletionTokens("gpt-4")
        )
    }

    @Test
    fun `test gpt-4-0613 uses max_tokens`() {
        assertFalse(
            "gpt-4-0613 should use max_tokens (not max_completion_tokens)",
            requiresMaxCompletionTokens("gpt-4-0613")
        )
    }

    @Test
    fun `test gpt-4o-mini uses max_tokens for backward compatibility`() {
        assertFalse(
            "gpt-4o-mini should use max_tokens for backward compatibility",
            requiresMaxCompletionTokens("gpt-4o-mini")
        )
    }

    @Test
    fun `test gpt-4o uses max_tokens for backward compatibility`() {
        assertFalse(
            "gpt-4o should use max_tokens for backward compatibility",
            requiresMaxCompletionTokens("gpt-4o")
        )
    }

    @Test
    fun `test new gpt-4_1 requires max_completion_tokens`() {
        assertTrue(
            "gpt-4.1 should require max_completion_tokens",
            requiresMaxCompletionTokens("gpt-4.1")
        )
    }

    @Test
    fun `test gpt-4_1-mini requires max_completion_tokens`() {
        assertTrue(
            "gpt-4.1-mini should require max_completion_tokens",
            requiresMaxCompletionTokens("gpt-4.1-mini")
        )
    }

    @Test
    fun `test gpt-4_1-nano requires max_completion_tokens`() {
        assertTrue(
            "gpt-4.1-nano should require max_completion_tokens",
            requiresMaxCompletionTokens("gpt-4.1-nano")
        )
    }

    @Test
    fun `test o1 model requires max_completion_tokens`() {
        assertTrue(
            "o1 should require max_completion_tokens",
            requiresMaxCompletionTokens("o1")
        )
    }

    @Test
    fun `test o1-preview requires max_completion_tokens`() {
        assertTrue(
            "o1-preview should require max_completion_tokens",
            requiresMaxCompletionTokens("o1-preview")
        )
    }

    @Test
    fun `test o1-mini requires max_completion_tokens`() {
        assertTrue(
            "o1-mini should require max_completion_tokens",
            requiresMaxCompletionTokens("o1-mini")
        )
    }

    @Test
    fun `test o3 model requires max_completion_tokens`() {
        assertTrue(
            "o3 should require max_completion_tokens",
            requiresMaxCompletionTokens("o3")
        )
    }

    @Test
    fun `test o3-mini requires max_completion_tokens`() {
        assertTrue(
            "o3-mini should require max_completion_tokens",
            requiresMaxCompletionTokens("o3-mini")
        )
    }

    @Test
    fun `test chatgpt-4o-latest requires max_completion_tokens`() {
        assertTrue(
            "chatgpt-4o-latest should require max_completion_tokens",
            requiresMaxCompletionTokens("chatgpt-4o-latest")
        )
    }

    @Test
    fun `test November 2024 gpt-4o variant requires max_completion_tokens`() {
        assertTrue(
            "gpt-4o-2024-11-20 should require max_completion_tokens",
            requiresMaxCompletionTokens("gpt-4o-2024-11-20")
        )
    }

    @Test
    fun `test December 2024 gpt-4o variant requires max_completion_tokens`() {
        assertTrue(
            "gpt-4o-2024-12-17 should require max_completion_tokens",
            requiresMaxCompletionTokens("gpt-4o-2024-12-17")
        )
    }

    @Test
    fun `test 2025 gpt-4o variant requires max_completion_tokens`() {
        assertTrue(
            "gpt-4o-2025-01-01 should require max_completion_tokens",
            requiresMaxCompletionTokens("gpt-4o-2025-01-01")
        )
    }

    // ==================== DeepSeek Response Parsing Tests ====================

    /**
     * Helper function to simulate DeepSeek response content extraction
     * Returns content if available, otherwise reasoning_content, otherwise null
     */
    private fun extractDeepSeekContent(content: String?, reasoningContent: String?): String? {
        return content?.takeIf { it.isNotEmpty() }
            ?: reasoningContent?.takeIf { it.isNotEmpty() }
    }

    @Test
    fun `test DeepSeek content is preferred over reasoning_content`() {
        val result = extractDeepSeekContent("Final answer", "Step by step reasoning")
        assertEquals("Final answer", result)
    }

    @Test
    fun `test DeepSeek falls back to reasoning_content when content is null`() {
        val result = extractDeepSeekContent(null, "Step by step reasoning")
        assertEquals("Step by step reasoning", result)
    }

    @Test
    fun `test DeepSeek falls back to reasoning_content when content is empty`() {
        val result = extractDeepSeekContent("", "Step by step reasoning")
        assertEquals("Step by step reasoning", result)
    }

    @Test
    fun `test DeepSeek returns null when both are null`() {
        val result = extractDeepSeekContent(null, null)
        assertNull(result)
    }

    @Test
    fun `test DeepSeek returns null when both are empty`() {
        val result = extractDeepSeekContent("", "")
        assertNull(result)
    }

    @Test
    fun `test DeepSeek non-empty content is not replaced with null reasoning`() {
        val result = extractDeepSeekContent("Valid content", null)
        assertEquals("Valid content", result)
    }

    // ==================== Provider URL Tests ====================

    @Test
    fun `test OpenAI base URL format`() {
        val baseUrl = "https://api.openai.com"
        val endpoint = "$baseUrl/v1/chat/completions"
        assertEquals("https://api.openai.com/v1/chat/completions", endpoint)
    }

    @Test
    fun `test DeepSeek base URL format`() {
        val baseUrl = "https://api.deepseek.com"
        val endpoint = "$baseUrl/v1/chat/completions"
        assertEquals("https://api.deepseek.com/v1/chat/completions", endpoint)
    }

    @Test
    fun `test Qwen base URL format`() {
        val baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode"
        val endpoint = "$baseUrl/v1/chat/completions"
        assertEquals("https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions", endpoint)
    }

    @Test
    fun `test trailing slash handling`() {
        val baseUrl = "https://api.openai.com/"
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val endpoint = "${safeBaseUrl}v1/chat/completions"
        assertEquals("https://api.openai.com/v1/chat/completions", endpoint)
    }

    @Test
    fun `test no trailing slash handling`() {
        val baseUrl = "https://api.openai.com"
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val endpoint = "${safeBaseUrl}v1/chat/completions"
        assertEquals("https://api.openai.com/v1/chat/completions", endpoint)
    }
}
