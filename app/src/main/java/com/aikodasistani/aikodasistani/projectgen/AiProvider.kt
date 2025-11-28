package com.aikodasistani.aikodasistani.projectgen

/**
 * Provider-Agnostic AI Provider Interface
 * 
 * This interface defines the contract for AI providers in the project generator.
 * 
 * Key Design Principles:
 * - Providers MUST NOT know anything about project generation
 * - Providers MUST NOT interpret project structures
 * - Providers MUST NOT contain formatting logic
 * - Providers MUST only return raw text output
 * 
 * The implementation of this interface is delegated to existing AI infrastructure
 * in the app (MainActivity's provider system). This ensures:
 * - No provider-specific logic in the project generator
 * - No model names hardcoded
 * - Full reuse of existing provider infrastructure
 */
interface AiProvider {
    /**
     * Execute a prompt against the AI provider and return raw text output.
     * 
     * @param prompt The prompt to send to the AI
     * @param option The provider option (capability variant from UI)
     * @return AiProviderResult with raw output or error
     */
    suspend fun execute(prompt: String, option: ProviderOption): AiProviderResult
}

/**
 * Prompt Builder for Project Generation
 * 
 * Builds optimized prompts for AI project generation.
 * Instructs the AI to output in the expected hierarchical format.
 */
object ProjectPromptBuilder {

    /**
     * Build the full prompt for project generation.
     * 
     * @param userRequest The user's natural language request
     * @param projectName The name for the project
     * @return Complete prompt string
     */
    fun buildPrompt(userRequest: String, projectName: String): String {
        return """
You are an expert software architect and code generator. Generate a complete, production-ready project structure based on the user's request.

USER REQUEST: $userRequest
PROJECT NAME: $projectName

IMPORTANT OUTPUT FORMAT RULES:
1. Output the project structure in this EXACT format
2. For each folder, write the path starting with / and ending with /
3. For each file, write the filename followed by a colon (:)
4. Write the file content with proper indentation below the filename
5. Include ALL necessary files for a working project

OUTPUT FORMAT EXAMPLE:
/$projectName/
/src/main/kotlin/
MainActivity.kt:
    package com.example.app
    
    class MainActivity {
        fun main() {
            println("Hello World")
        }
    }

/src/main/resources/
config.json:
    {
        "name": "$projectName",
        "version": "1.0.0"
    }

build.gradle.kts:
    plugins {
        kotlin("jvm")
    }

README.md:
    # $projectName
    
    Generated project description here.

REQUIREMENTS:
- Generate a COMPLETE, WORKING project structure
- Include all configuration files (build files, package.json, pubspec.yaml, etc.)
- Include proper package structure
- Include README with instructions
- Include .gitignore if appropriate
- Use industry best practices and modern conventions
- Generate REAL, FUNCTIONAL code - not placeholders

Now generate the complete project for: $userRequest
        """.trimIndent()
    }

    /**
     * Build a prompt for a specific project type.
     */
    fun buildTypedPrompt(
        projectType: String,
        projectName: String,
        packageName: String?,
        description: String?
    ): String {
        val basePrompt = buildPrompt(
            userRequest = "Create a $projectType project" + 
                (if (description != null) " with: $description" else ""),
            projectName = projectName
        )

        val additionalInstructions = when {
            projectType.contains("android", ignoreCase = true) -> """
                
Additional Android Requirements:
- Use Kotlin as the primary language
- Include Jetpack Compose for UI
- Package name: ${packageName ?: "com.example.$projectName"}
- Include proper Android manifest
- Include build.gradle.kts files
- Include proper project structure (app/src/main/...)
            """.trimIndent()

            projectType.contains("flutter", ignoreCase = true) -> """
                
Additional Flutter Requirements:
- Use Dart language
- Include pubspec.yaml with dependencies
- Include proper lib/ structure
- Include main.dart with material app
            """.trimIndent()

            projectType.contains("node", ignoreCase = true) || 
            projectType.contains("express", ignoreCase = true) -> """
                
Additional Node.js Requirements:
- Include package.json with proper scripts
- Use modern ES modules or CommonJS as appropriate
- Include proper src/ structure
- Include .gitignore for node_modules
            """.trimIndent()

            projectType.contains("python", ignoreCase = true) -> """
                
Additional Python Requirements:
- Include requirements.txt
- Include proper package structure
- Include __init__.py files
- Include main entry point
            """.trimIndent()

            projectType.contains("react", ignoreCase = true) -> """
                
Additional React Requirements:
- Include package.json with React dependencies
- Include src/ with components
- Include proper build configuration
            """.trimIndent()

            else -> ""
        }

        return basePrompt + additionalInstructions
    }
}
