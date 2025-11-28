package com.aikodasistani.aikodasistani.projectgen

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enterprise-Grade Project Generation Orchestrator
 * 
 * Coordinates the full project generation pipeline:
 * 1. Send prompt to AI Provider
 * 2. Parse AI output into ProjectStructure
 * 3. Write files to disk
 * 4. Create ZIP archive
 * 5. Return result with URI
 * 
 * Key Design Principles:
 * - Provider-agnostic (provider comes from ViewModel)
 * - No hardcoded model names
 * - Robust error handling
 * - MVVM + Clean Architecture compliant
 */
class ProjectGenerationOrchestrator(private val context: Context) {

    companion object {
        private const val TAG = "ProjectOrchestrator"
    }

    private val parser = ProjectStructureParser()
    private val fileWriter = ProjectFileWriter()
    private val zipEngine = ProjectZipEngine(context)

    /**
     * Generate a project using AI.
     * 
     * @param aiProvider The AI provider implementation (from ViewModel)
     * @param request The project generation request
     * @return ProjectGenerationOutput with success or detailed error
     */
    suspend fun generateProject(
        aiProvider: AiProvider,
        request: AiProjectRequest
    ): ProjectGenerationOutput = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "Starting project generation: ${request.projectName}")
            
            // Step 1: Build prompt and call AI
            val prompt = ProjectPromptBuilder.buildPrompt(request.prompt, request.projectName)
            
            Log.d(TAG, "Calling AI provider...")
            val aiResult = aiProvider.execute(prompt, request.option)
            
            val rawOutput = when (aiResult) {
                is AiProviderResult.Success -> aiResult.rawOutput
                is AiProviderResult.Error -> {
                    Log.e(TAG, "AI provider error: ${aiResult.message}")
                    return@withContext ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.PROVIDER_UNREACHABLE,
                        message = aiResult.message,
                        details = aiResult.cause?.message
                    )
                }
            }
            
            if (rawOutput.isBlank()) {
                return@withContext ProjectGenerationOutput.Error(
                    errorType = ProjectGenerationOutput.ErrorType.EMPTY_AI_RESPONSE,
                    message = "AI returned empty response"
                )
            }
            
            Log.d(TAG, "AI response received: ${rawOutput.length} chars")
            
            // Step 2: Parse AI output
            val parseResult = parser.parse(rawOutput, request.projectName)
            
            val structure = when (parseResult) {
                is ParserResult.Success -> parseResult.structure
                is ParserResult.Error -> {
                    Log.e(TAG, "Parse error: ${parseResult.message}")
                    return@withContext ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.MALFORMED_OUTPUT,
                        message = "Failed to parse AI output: ${parseResult.message}",
                        details = parseResult.rawOutput
                    )
                }
            }
            
            Log.d(TAG, "Parsed ${structure.files.size} files")
            
            // Step 3: Create ZIP directly from structure (more efficient)
            val outputDir = getOutputDirectory()
            
            val zipResult = zipEngine.createZipFromStructure(structure, outputDir)
            
            when (zipResult) {
                is ZipResult.Success -> {
                    val generationTime = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Project generated successfully in ${generationTime}ms")
                    
                    return@withContext ProjectGenerationOutput.Success(
                        projectName = request.projectName,
                        structure = structure,
                        zipUri = zipResult.zipUri,
                        zipPath = zipResult.zipPath,
                        totalFiles = structure.files.size,
                        totalSize = zipResult.sizeBytes,
                        generationTimeMs = generationTime
                    )
                }
                is ZipResult.Error -> {
                    Log.e(TAG, "ZIP error: ${zipResult.message}")
                    return@withContext ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.ZIP_CREATION_FAILURE,
                        message = zipResult.message
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Orchestrator error", e)
            return@withContext ProjectGenerationOutput.Error(
                errorType = ProjectGenerationOutput.ErrorType.UNKNOWN,
                message = "Unexpected error: ${e.message}",
                details = e.stackTraceToString().take(500)
            )
        }
    }

    /**
     * Generate a project from pre-built prompt (for typed project generation).
     * 
     * @param aiProvider The AI provider implementation
     * @param projectType The project type (e.g., "Android Kotlin")
     * @param projectName The project name
     * @param packageName Optional package name (for Android/Java)
     * @param description Optional description
     * @param option The provider option
     * @return ProjectGenerationOutput
     */
    suspend fun generateTypedProject(
        aiProvider: AiProvider,
        projectType: String,
        projectName: String,
        packageName: String?,
        description: String?,
        option: ProviderOption
    ): ProjectGenerationOutput = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "Starting typed project generation: $projectType - $projectName")
            
            // Build typed prompt
            val prompt = ProjectPromptBuilder.buildTypedPrompt(
                projectType = projectType,
                projectName = projectName,
                packageName = packageName,
                description = description
            )
            
            Log.d(TAG, "Calling AI provider for typed project...")
            val aiResult = aiProvider.execute(prompt, option)
            
            val rawOutput = when (aiResult) {
                is AiProviderResult.Success -> aiResult.rawOutput
                is AiProviderResult.Error -> {
                    return@withContext ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.PROVIDER_UNREACHABLE,
                        message = aiResult.message
                    )
                }
            }
            
            if (rawOutput.isBlank()) {
                return@withContext ProjectGenerationOutput.Error(
                    errorType = ProjectGenerationOutput.ErrorType.EMPTY_AI_RESPONSE,
                    message = "AI returned empty response"
                )
            }
            
            // Parse output
            val parseResult = parser.parse(rawOutput, projectName)
            
            val structure = when (parseResult) {
                is ParserResult.Success -> parseResult.structure
                is ParserResult.Error -> {
                    return@withContext ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.MALFORMED_OUTPUT,
                        message = parseResult.message
                    )
                }
            }
            
            // Create ZIP
            val outputDir = getOutputDirectory()
            val zipResult = zipEngine.createZipFromStructure(structure, outputDir)
            
            when (zipResult) {
                is ZipResult.Success -> {
                    val generationTime = System.currentTimeMillis() - startTime
                    
                    return@withContext ProjectGenerationOutput.Success(
                        projectName = projectName,
                        structure = structure,
                        zipUri = zipResult.zipUri,
                        zipPath = zipResult.zipPath,
                        totalFiles = structure.files.size,
                        totalSize = zipResult.sizeBytes,
                        generationTimeMs = generationTime
                    )
                }
                is ZipResult.Error -> {
                    return@withContext ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.ZIP_CREATION_FAILURE,
                        message = zipResult.message
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Typed generation error", e)
            return@withContext ProjectGenerationOutput.Error(
                errorType = ProjectGenerationOutput.ErrorType.UNKNOWN,
                message = "Error: ${e.message}"
            )
        }
    }

    /**
     * Get the output directory for generated projects.
     */
    private fun getOutputDirectory(): java.io.File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
    }

    /**
     * Clean up old generated files (optional).
     */
    fun cleanupOldGenerations(maxAgeDays: Int = 7) {
        try {
            val outputDir = getOutputDirectory()
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
            
            outputDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".zip") && file.lastModified() < cutoffTime) {
                    file.delete()
                    Log.d(TAG, "Cleaned up old file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old files", e)
        }
    }
}
