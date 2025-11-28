package com.aikodasistani.aikodasistani.projectgenerator.data

import com.aikodasistani.aikodasistani.projectgenerator.domain.prompts.SystemPrompts

/**
 * Middleware that ensures the system prompt is always injected before user queries.
 * 
 * This middleware sits between the UI/ViewModel layer and the AI provider,
 * guaranteeing that every project generation request includes the proper
 * system prompt that instructs the AI to use the `>>> FILE:` format.
 * 
 * Design principles:
 * 1. SINGLE RESPONSIBILITY: Only handles prompt injection, nothing else
 * 2. IMMUTABLE: Does not modify the original user prompt
 * 3. TESTABLE: Simple input/output with no side effects
 * 4. CONFIGURABLE: Allows customization of system prompts
 */
object PromptInjectionMiddleware {

    /**
     * Configuration for the middleware.
     */
    data class Config(
        /**
         * Whether to use the full scaffold prompt for Android projects.
         */
        val useAndroidScaffold: Boolean = true,

        /**
         * Custom system prompt to use instead of the default.
         * If null, uses the default ANDROID_FULL_SCAFFOLD_PROMPT.
         */
        val customSystemPrompt: String? = null,

        /**
         * Separator between system prompt and user query.
         */
        val separator: String = "\n\n## USER REQUEST:\n"
    )

    /**
     * The result of prompt injection containing both the system prompt and the combined prompt.
     */
    data class InjectedPrompt(
        /**
         * The system prompt that was injected.
         */
        val systemPrompt: String,

        /**
         * The original user query.
         */
        val userQuery: String,

        /**
         * The combined prompt (system + separator + user query) ready to send to AI.
         */
        val combinedPrompt: String,

        /**
         * Whether the system prompt was actually injected (false if user query already contained it).
         */
        val wasInjected: Boolean
    )

    /**
     * Default configuration using the Android full scaffold prompt.
     */
    private var defaultConfig = Config()

    /**
     * Updates the default configuration.
     * 
     * @param config The new configuration to use
     */
    fun configure(config: Config) {
        defaultConfig = config
    }

    /**
     * Resets to default configuration.
     */
    fun reset() {
        defaultConfig = Config()
    }

    /**
     * Injects the system prompt before the user query.
     * 
     * @param userQuery The user's prompt/request
     * @param config Optional configuration override (uses default if not provided)
     * @return InjectedPrompt containing the combined prompt ready for AI
     */
    fun inject(userQuery: String, config: Config = defaultConfig): InjectedPrompt {
        val systemPrompt = getSystemPrompt(config)

        // Check if the user query already contains the system prompt marker
        // to avoid double injection
        if (userQuery.contains(">>> FILE:") && userQuery.contains("OUTPUT FORMAT")) {
            return InjectedPrompt(
                systemPrompt = "",
                userQuery = userQuery,
                combinedPrompt = userQuery,
                wasInjected = false
            )
        }

        val combinedPrompt = buildString {
            append(systemPrompt)
            append(config.separator)
            append(userQuery)
        }

        return InjectedPrompt(
            systemPrompt = systemPrompt,
            userQuery = userQuery,
            combinedPrompt = combinedPrompt,
            wasInjected = true
        )
    }

    /**
     * Convenience method that just returns the combined prompt string.
     * 
     * @param userQuery The user's prompt/request
     * @param config Optional configuration override
     * @return The combined prompt string ready for AI
     */
    fun injectAndGetPrompt(userQuery: String, config: Config = defaultConfig): String {
        return inject(userQuery, config).combinedPrompt
    }

    /**
     * Creates a combined prompt with project-specific customization.
     * 
     * @param userQuery The user's request
     * @param projectName The name of the project to generate
     * @param packageName The base package name for the project
     * @return The combined prompt with customized system prompt
     */
    fun injectForProject(
        userQuery: String,
        projectName: String,
        packageName: String
    ): InjectedPrompt {
        val customizedSystemPrompt = SystemPrompts.getAndroidScaffoldPrompt(projectName, packageName)

        val combinedPrompt = buildString {
            append(customizedSystemPrompt)
            append(defaultConfig.separator)
            append(userQuery)
        }

        return InjectedPrompt(
            systemPrompt = customizedSystemPrompt,
            userQuery = userQuery,
            combinedPrompt = combinedPrompt,
            wasInjected = true
        )
    }

    /**
     * Gets the appropriate system prompt based on configuration.
     */
    private fun getSystemPrompt(config: Config): String {
        // If custom prompt is provided, use it
        config.customSystemPrompt?.let { return it }

        // Otherwise use the Android scaffold prompt
        return if (config.useAndroidScaffold) {
            SystemPrompts.ANDROID_FULL_SCAFFOLD_PROMPT
        } else {
            // Fallback minimal prompt
            MINIMAL_SYSTEM_PROMPT
        }
    }

    /**
     * A minimal system prompt for non-Android projects or simple cases.
     */
    private const val MINIMAL_SYSTEM_PROMPT = """
You are a code generation assistant. Generate complete, working code files.

OUTPUT FORMAT:
Each file must be prefixed with >>> FILE: followed by the file path.

Example:
>>> FILE: src/main.kt
fun main() {
    println("Hello")
}

>>> FILE: README.md
# Project Title
Description here

Start your response with >>> FILE: immediately. No explanations.
"""
}
