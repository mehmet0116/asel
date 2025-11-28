package com.aikodasistani.aikodasistani.projectgenerator.domain

import android.net.Uri

/**
 * Represents the structure of a generated project containing all files and metadata.
 * This is a provider-agnostic data class that captures the AI output after parsing.
 */
data class ProjectStructure(
    val root: String,
    val files: List<ProjectFile>,
    val metadata: ProjectMetadata = ProjectMetadata()
)

/**
 * Represents a single file within the generated project structure.
 * Contains the file path (relative to project root) and its content.
 */
data class ProjectFile(
    val path: String,
    val content: String
) {
    /**
     * Returns the directory portion of the path.
     */
    val directory: String
        get() = path.substringBeforeLast('/', "")

    /**
     * Returns just the filename.
     */
    val filename: String
        get() = path.substringAfterLast('/')

    /**
     * Returns the file extension (empty if no extension).
     */
    val extension: String
        get() = filename.substringAfterLast('.', "")
}

/**
 * Metadata about the generated project.
 */
data class ProjectMetadata(
    val totalFiles: Int = 0,
    val totalSize: Long = 0,
    val generatedAt: Long = System.currentTimeMillis(),
    val providerUsed: String? = null,
    val optionUsed: String? = null
)

/**
 * Identifier for an AI provider (e.g., OPENAI, GEMINI, DEEPSEEK, QWEN).
 * This is a value class to provide type safety without model-specific information.
 */
@JvmInline
value class ProviderIdentifier(val value: String) {
    override fun toString(): String = value
}

/**
 * Represents a provider-specific option/capability variant.
 * This is NOT a model name - it represents a UI-defined capability level.
 */
data class ProviderOption(
    val id: String,
    val displayName: String,
    val description: String = ""
)

/**
 * Request object for AI project generation.
 * Contains all necessary information to generate a project via AI.
 */
data class AIProjectGenerationRequest(
    val prompt: String,
    val provider: ProviderIdentifier,
    val option: ProviderOption,
    val projectName: String,
    val additionalContext: String? = null
)

/**
 * Result of an AI project generation operation.
 */
sealed class AIProjectGenerationResult {
    /**
     * Successful project generation.
     */
    data class Success(
        val projectStructure: ProjectStructure,
        val zipUri: Uri,
        val zipPath: String,
        val message: String? = null
    ) : AIProjectGenerationResult()

    /**
     * Generation failed with an error.
     */
    data class Error(
        val errorType: GenerationErrorType,
        val message: String,
        val details: String? = null,
        val cause: Throwable? = null
    ) : AIProjectGenerationResult()
}

/**
 * Types of errors that can occur during project generation.
 */
enum class GenerationErrorType {
    PROVIDER_UNREACHABLE,
    EMPTY_AI_RESPONSE,
    MALFORMED_STRUCTURAL_OUTPUT,
    PARSING_ERROR,
    FILE_SYSTEM_ERROR,
    STORAGE_QUOTA_EXCEEDED,
    ZIP_CREATION_ERROR,
    INVALID_REQUEST,
    UNKNOWN_ERROR
}

/**
 * State of the AI project generation process for UI observation.
 */
sealed class GenerationState {
    object Idle : GenerationState()
    object Preparing : GenerationState()
    data class CallingAI(val message: String) : GenerationState()
    data class Parsing(val message: String) : GenerationState()
    data class WritingFiles(val progress: Int, val total: Int) : GenerationState()
    data class CreatingZip(val message: String) : GenerationState()
    data class Completed(val result: AIProjectGenerationResult.Success) : GenerationState()
    data class Failed(val error: AIProjectGenerationResult.Error) : GenerationState()
}
