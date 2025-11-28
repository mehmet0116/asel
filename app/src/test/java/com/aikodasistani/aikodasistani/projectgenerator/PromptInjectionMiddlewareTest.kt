package com.aikodasistani.aikodasistani.projectgenerator

import com.aikodasistani.aikodasistani.projectgenerator.data.PromptInjectionMiddleware
import com.aikodasistani.aikodasistani.projectgenerator.domain.prompts.SystemPrompts
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PromptInjectionMiddleware.
 * Tests system prompt injection before user queries.
 */
class PromptInjectionMiddlewareTest {

    @Before
    fun setUp() {
        // Reset to default config before each test
        PromptInjectionMiddleware.reset()
    }

    @After
    fun tearDown() {
        // Reset after each test
        PromptInjectionMiddleware.reset()
    }

    @Test
    fun `test inject adds system prompt before user query`() {
        val userQuery = "Create a todo list app"

        val result = PromptInjectionMiddleware.inject(userQuery)

        assertTrue("Should be injected", result.wasInjected)
        assertEquals("User query should be preserved", userQuery, result.userQuery)
        assertTrue("Combined prompt should contain system prompt content", 
            result.combinedPrompt.contains(result.systemPrompt.trim()))
        assertTrue("Combined prompt should contain user query", 
            result.combinedPrompt.contains(userQuery))
        assertTrue("System prompt should contain FILE format instruction",
            result.systemPrompt.contains(">>> FILE:"))
        // Verify order: system prompt content should come before user query
        val systemPromptIdx = result.combinedPrompt.indexOf("OUTPUT FORMAT")
        val userQueryIdx = result.combinedPrompt.indexOf(userQuery)
        assertTrue("System prompt should come before user query", systemPromptIdx < userQueryIdx)
    }

    @Test
    fun `test inject uses default Android scaffold prompt`() {
        val userQuery = "Create a weather app"

        val result = PromptInjectionMiddleware.inject(userQuery)

        assertTrue("Should use Android scaffold prompt",
            result.systemPrompt.contains("Android"))
        assertTrue("Should contain OUTPUT FORMAT section",
            result.systemPrompt.contains("OUTPUT FORMAT"))
        assertTrue("Should contain REQUIRED PROJECT STRUCTURE",
            result.systemPrompt.contains("REQUIRED PROJECT STRUCTURE"))
    }

    @Test
    fun `test inject prevents double injection`() {
        // Create a query that already contains the system prompt markers
        val userQueryWithSystem = """
            >>> FILE: test.kt
            Some code
            
            OUTPUT FORMAT instructions here
            
            Create a todo app
        """.trimIndent()

        val result = PromptInjectionMiddleware.inject(userQueryWithSystem)

        assertFalse("Should not inject when markers already present", result.wasInjected)
        assertEquals("Combined prompt should equal original query", 
            userQueryWithSystem, result.combinedPrompt)
        assertTrue("System prompt should be empty", result.systemPrompt.isEmpty())
    }

    @Test
    fun `test injectAndGetPrompt returns combined string`() {
        val userQuery = "Create a calculator app"

        val combinedPrompt = PromptInjectionMiddleware.injectAndGetPrompt(userQuery)

        assertTrue("Should contain system prompt content", 
            combinedPrompt.contains(">>> FILE:"))
        assertTrue("Should contain user query", 
            combinedPrompt.contains(userQuery))
    }

    @Test
    fun `test inject with custom configuration`() {
        val customPrompt = "Custom system prompt for testing >>> FILE:"
        val config = PromptInjectionMiddleware.Config(
            customSystemPrompt = customPrompt,
            separator = "\n---\n"
        )
        val userQuery = "Create an app"

        val result = PromptInjectionMiddleware.inject(userQuery, config)

        assertEquals("Should use custom system prompt", customPrompt, result.systemPrompt)
        assertTrue("Should use custom separator", 
            result.combinedPrompt.contains("---"))
    }

    @Test
    fun `test configure updates default config`() {
        val customPrompt = "Global custom prompt >>> FILE:"
        PromptInjectionMiddleware.configure(
            PromptInjectionMiddleware.Config(customSystemPrompt = customPrompt)
        )

        val result = PromptInjectionMiddleware.inject("User query")

        assertEquals("Should use configured system prompt", customPrompt, result.systemPrompt)
    }

    @Test
    fun `test reset restores default config`() {
        PromptInjectionMiddleware.configure(
            PromptInjectionMiddleware.Config(customSystemPrompt = "Custom >>> FILE:")
        )
        
        PromptInjectionMiddleware.reset()

        val result = PromptInjectionMiddleware.inject("User query")

        assertTrue("Should use default Android scaffold prompt after reset",
            result.systemPrompt.contains("Android"))
    }

    @Test
    fun `test injectForProject customizes prompt with project details`() {
        val userQuery = "Create a notes app with sync"
        val projectName = "MyNotesApp"
        val packageName = "com.company.notes"

        val result = PromptInjectionMiddleware.injectForProject(
            userQuery = userQuery,
            projectName = projectName,
            packageName = packageName
        )

        assertTrue("Should be injected", result.wasInjected)
        assertTrue("System prompt should contain project name",
            result.systemPrompt.contains(projectName))
        assertTrue("System prompt should contain package name",
            result.systemPrompt.contains(packageName))
        assertTrue("Combined prompt should contain user query",
            result.combinedPrompt.contains(userQuery))
    }

    @Test
    fun `test combined prompt has correct structure`() {
        val userQuery = "Build a photo gallery app"

        val result = PromptInjectionMiddleware.inject(userQuery)

        // The combined prompt should have: system prompt + separator + user query
        val expectedSeparator = "## USER REQUEST:"
        assertTrue("Combined prompt should contain separator",
            result.combinedPrompt.contains(expectedSeparator))
        
        // User query should come after the separator
        val separatorIndex = result.combinedPrompt.indexOf(expectedSeparator)
        val userQueryIndex = result.combinedPrompt.indexOf(userQuery)
        assertTrue("User query should come after separator",
            userQueryIndex > separatorIndex)
    }

    @Test
    fun `test config with useAndroidScaffold false`() {
        val config = PromptInjectionMiddleware.Config(
            useAndroidScaffold = false
        )

        val result = PromptInjectionMiddleware.inject("Create an app", config)

        // Should use minimal prompt instead
        assertTrue("System prompt should contain FILE format",
            result.systemPrompt.contains(">>> FILE:"))
        // But should be shorter than the full Android scaffold
        assertTrue("System prompt should be shorter",
            result.systemPrompt.length < SystemPrompts.ANDROID_FULL_SCAFFOLD_PROMPT.length)
    }

    @Test
    fun `test inject preserves user query exactly`() {
        val userQuery = """
            Create a complex app with:
            - Feature A
            - Feature B
            
            Special characters: <>&"'
            Unicode: 日本語
        """.trimIndent()

        val result = PromptInjectionMiddleware.inject(userQuery)

        assertEquals("User query should be exactly preserved", userQuery, result.userQuery)
        assertTrue("Combined prompt should contain exact user query",
            result.combinedPrompt.contains(userQuery))
    }

    @Test
    fun `test empty user query still gets system prompt`() {
        val result = PromptInjectionMiddleware.inject("")

        assertTrue("Should still inject", result.wasInjected)
        assertTrue("System prompt should still be present",
            result.systemPrompt.isNotBlank())
        assertTrue("Combined prompt should contain system prompt",
            result.combinedPrompt.contains(">>> FILE:"))
    }
}
