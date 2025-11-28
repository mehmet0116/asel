package com.aikodasistani.aikodasistani.projectgenerator.data

import android.content.Context
import android.util.Log
import com.aikodasistani.aikodasistani.projectgenerator.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Orchestrator for the AI Project Generation pipeline.
 * 
 * This class coordinates the entire flow:
 * 1. Receive request from ViewModel (provider + option come from UI)
 * 2. Call AI Provider to get raw response
 * 3. Parse response into ProjectStructure
 * 4. Write files to disk
 * 5. Create ZIP archive
 * 6. Return result
 * 
 * All provider/model information comes from outside (ViewModel).
 * This class does NOT know about specific providers or models.
 */
class AIProjectGeneratorOrchestrator(
    private val context: Context,
    private val aiProvider: AiProvider
) {
    
    private val TAG = "AIProjectOrchestrator"
    
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()
    
    /**
     * Generates a project based on the given request.
     * Provider and option are specified by the caller (from ViewModel/UI).
     * 
     * @param request The generation request containing prompt, provider, option, and project name
     * @return AIProjectGenerationResult indicating success or failure
     */
    suspend fun generateProject(request: AIProjectGenerationRequest): AIProjectGenerationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting project generation for: ${request.projectName}")
            Log.d(TAG, "Provider: ${request.provider}, Option: ${request.option.id}")
            
            // Step 1: Preparing
            updateState(GenerationState.Preparing)
            
            // Build the full prompt
            val fullPrompt = buildProjectPrompt(request)
            
            // Step 2: Call AI
            updateState(GenerationState.CallingAI("Requesting project from AI..."))
            
            val rawAiOutput = try {
                aiProvider.execute(fullPrompt, request.option)
            } catch (e: AiProviderException) {
                Log.e(TAG, "AI provider error", e)
                return@withContext createError(
                    when (e.errorType) {
                        AiProviderErrorType.NETWORK_ERROR -> GenerationErrorType.PROVIDER_UNREACHABLE
                        AiProviderErrorType.AUTHENTICATION_ERROR -> GenerationErrorType.INVALID_REQUEST
                        AiProviderErrorType.EMPTY_RESPONSE -> GenerationErrorType.EMPTY_AI_RESPONSE
                        else -> GenerationErrorType.UNKNOWN_ERROR
                    },
                    e.message ?: "AI provider error"
                )
            }
            
            if (rawAiOutput.isBlank()) {
                return@withContext createError(
                    GenerationErrorType.EMPTY_AI_RESPONSE,
                    "AI returned an empty response"
                )
            }
            
            Log.d(TAG, "Received AI response: ${rawAiOutput.take(500)}...")
            
            // Step 3: Parse AI output
            updateState(GenerationState.Parsing("Parsing project structure..."))
            
            val parseResult = ProjectStructureParser.parse(rawAiOutput, request.projectName)
            
            val projectStructure = when (parseResult) {
                is ProjectStructureParser.ParseResult.Success -> parseResult.structure
                is ProjectStructureParser.ParseResult.Error -> {
                    Log.e(TAG, "Parse error: ${parseResult.message}")
                    return@withContext createError(
                        GenerationErrorType.MALFORMED_STRUCTURAL_OUTPUT,
                        parseResult.message,
                        parseResult.details
                    )
                }
            }
            
            Log.d(TAG, "Parsed ${projectStructure.files.size} files")
            
            // Step 4: Write files to disk
            updateState(GenerationState.WritingFiles(0, projectStructure.files.size))
            
            val writeResult = FileWriterEngine.writeProject(
                context = context,
                structure = projectStructure,
                progressCallback = { current, total, _ ->
                    updateState(GenerationState.WritingFiles(current, total))
                }
            )
            
            val outputDir = when (writeResult) {
                is FileWriterEngine.WriteResult.Success -> writeResult.outputDirectory
                is FileWriterEngine.WriteResult.Error -> {
                    Log.e(TAG, "Write error: ${writeResult.message}")
                    return@withContext createError(
                        GenerationErrorType.FILE_SYSTEM_ERROR,
                        writeResult.message,
                        writeResult.failedFile
                    )
                }
            }
            
            Log.d(TAG, "Files written to: ${outputDir.absolutePath}")
            
            // Step 5: Create ZIP
            updateState(GenerationState.CreatingZip("Creating ZIP archive..."))
            
            val zipResult = ZipEngine.createZip(
                context = context,
                projectDir = outputDir,
                outputName = request.projectName
            )
            
            when (zipResult) {
                is ZipEngine.ZipResult.Success -> {
                    Log.d(TAG, "ZIP created: ${zipResult.zipFile.absolutePath}")
                    
                    // Update project structure metadata
                    val finalStructure = projectStructure.copy(
                        metadata = projectStructure.metadata.copy(
                            providerUsed = request.provider.value,
                            optionUsed = request.option.id
                        )
                    )
                    
                    val result = AIProjectGenerationResult.Success(
                        projectStructure = finalStructure,
                        zipUri = zipResult.uri,
                        zipPath = zipResult.zipFile.absolutePath,
                        message = "Generated ${zipResult.filesZipped} files " +
                                "(${formatBytes(zipResult.compressedSize)} compressed)"
                    )
                    
                    updateState(GenerationState.Completed(result))
                    
                    return@withContext result
                }
                is ZipEngine.ZipResult.Error -> {
                    Log.e(TAG, "ZIP error: ${zipResult.message}")
                    return@withContext createError(
                        GenerationErrorType.ZIP_CREATION_ERROR,
                        zipResult.message
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            createError(
                GenerationErrorType.UNKNOWN_ERROR,
                "Generation failed: ${e.message}",
                e.stackTraceToString()
            )
        }
    }
    
    /**
     * Generates a project directly from AI without intermediate file writing.
     * More efficient for smaller projects.
     */
    suspend fun generateProjectDirect(request: AIProjectGenerationRequest): AIProjectGenerationResult = withContext(Dispatchers.IO) {
        try {
            updateState(GenerationState.Preparing)
            
            val fullPrompt = buildProjectPrompt(request)
            
            updateState(GenerationState.CallingAI("Requesting project from AI..."))
            
            val rawAiOutput = try {
                aiProvider.execute(fullPrompt, request.option)
            } catch (e: AiProviderException) {
                return@withContext createError(
                    GenerationErrorType.PROVIDER_UNREACHABLE,
                    e.message ?: "AI provider error"
                )
            }
            
            if (rawAiOutput.isBlank()) {
                return@withContext createError(
                    GenerationErrorType.EMPTY_AI_RESPONSE,
                    "AI returned an empty response"
                )
            }
            
            updateState(GenerationState.Parsing("Parsing project structure..."))
            
            val parseResult = ProjectStructureParser.parse(rawAiOutput, request.projectName)
            
            val projectStructure = when (parseResult) {
                is ProjectStructureParser.ParseResult.Success -> parseResult.structure
                is ProjectStructureParser.ParseResult.Error -> {
                    return@withContext createError(
                        GenerationErrorType.MALFORMED_STRUCTURAL_OUTPUT,
                        parseResult.message,
                        parseResult.details
                    )
                }
            }
            
            updateState(GenerationState.CreatingZip("Creating ZIP archive..."))
            
            // Create ZIP directly from parsed files (skipping disk write)
            val zipResult = ZipEngine.createZipFromFiles(
                context = context,
                projectName = request.projectName,
                files = projectStructure.files
            )
            
            when (zipResult) {
                is ZipEngine.ZipResult.Success -> {
                    val finalStructure = projectStructure.copy(
                        metadata = projectStructure.metadata.copy(
                            providerUsed = request.provider.value,
                            optionUsed = request.option.id
                        )
                    )
                    
                    val result = AIProjectGenerationResult.Success(
                        projectStructure = finalStructure,
                        zipUri = zipResult.uri,
                        zipPath = zipResult.zipFile.absolutePath,
                        message = "Generated ${zipResult.filesZipped} files"
                    )
                    
                    updateState(GenerationState.Completed(result))
                    return@withContext result
                }
                is ZipEngine.ZipResult.Error -> {
                    return@withContext createError(
                        GenerationErrorType.ZIP_CREATION_ERROR,
                        zipResult.message
                    )
                }
            }
            
        } catch (e: Exception) {
            createError(
                GenerationErrorType.UNKNOWN_ERROR,
                "Generation failed: ${e.message}"
            )
        }
    }
    
    /**
     * Resets the orchestrator to idle state.
     */
    fun reset() {
        _generationState.value = GenerationState.Idle
    }
    
    /**
     * Builds the full prompt for project generation.
     */
    private fun buildProjectPrompt(request: AIProjectGenerationRequest): String {
        val sb = StringBuilder()
        
        sb.appendLine("Generate a complete, production-ready ${request.projectName} project.")
        sb.appendLine()
        sb.appendLine("PROJECT REQUIREMENTS:")
        sb.appendLine(request.prompt)
        
        if (!request.additionalContext.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("ADDITIONAL CONTEXT:")
            sb.appendLine(request.additionalContext)
        }
        
        sb.appendLine()
        sb.appendLine("IMPORTANT: Generate the COMPLETE project with ALL necessary files.")
        sb.appendLine("Include proper folder structure, configuration files, and working source code.")
        sb.appendLine("Make sure the project can run immediately after extraction.")
        
        return sb.toString()
    }
    
    /**
     * Updates the generation state.
     */
    private fun updateState(state: GenerationState) {
        _generationState.value = state
    }
    
    /**
     * Creates an error result and updates state.
     */
    private fun createError(
        errorType: GenerationErrorType,
        message: String,
        details: String? = null
    ): AIProjectGenerationResult.Error {
        val error = AIProjectGenerationResult.Error(
            errorType = errorType,
            message = message,
            details = details
        )
        updateState(GenerationState.Failed(error))
        return error
    }
    
    /**
     * Formats byte count to human-readable string.
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
