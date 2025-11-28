package com.aikodasistani.aikodasistani.projectgenerator.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aikodasistani.aikodasistani.managers.SettingsManager
import com.aikodasistani.aikodasistani.projectgenerator.data.AIProjectGeneratorOrchestrator
import com.aikodasistani.aikodasistani.projectgenerator.data.AiProviderAdapter
import com.aikodasistani.aikodasistani.projectgenerator.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AI Project Generation.
 * 
 * This ViewModel:
 * - Receives provider and option selection from UI
 * - Manages generation state for UI observation
 * - Coordinates the generation process via the orchestrator
 * - Does NOT contain any provider-specific logic
 * 
 * All provider/model information flows from UI → ViewModel → Orchestrator
 */
class AIProjectGeneratorViewModel(
    private val context: Context,
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    // AI Provider Adapter (bridges existing infrastructure)
    private val aiProvider = AiProviderAdapter(settingsManager)
    
    // Orchestrator for the generation pipeline
    private val orchestrator = AIProjectGeneratorOrchestrator(context, aiProvider)
    
    // State flows for UI observation
    private val _uiState = MutableStateFlow<UIState>(UIState.Idle)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    
    private val _selectedProvider = MutableStateFlow<ProviderIdentifier?>(null)
    val selectedProvider: StateFlow<ProviderIdentifier?> = _selectedProvider.asStateFlow()
    
    private val _selectedOption = MutableStateFlow<ProviderOption?>(null)
    val selectedOption: StateFlow<ProviderOption?> = _selectedOption.asStateFlow()
    
    private val _availableProviders = MutableStateFlow<List<ProviderIdentifier>>(emptyList())
    val availableProviders: StateFlow<List<ProviderIdentifier>> = _availableProviders.asStateFlow()
    
    private val _availableOptions = MutableStateFlow<List<ProviderOption>>(emptyList())
    val availableOptions: StateFlow<List<ProviderOption>> = _availableOptions.asStateFlow()
    
    private val _lastResult = MutableStateFlow<AIProjectGenerationResult?>(null)
    val lastResult: StateFlow<AIProjectGenerationResult?> = _lastResult.asStateFlow()
    
    /**
     * UI state for the project generator screen.
     */
    sealed class UIState {
        object Idle : UIState()
        object Loading : UIState()
        data class Generating(val state: GenerationState) : UIState()
        data class Success(val result: AIProjectGenerationResult.Success) : UIState()
        data class Error(val error: AIProjectGenerationResult.Error) : UIState()
    }
    
    init {
        // Load initial state from settings
        loadProvidersAndOptions()
        
        // Observe orchestrator state
        viewModelScope.launch {
            orchestrator.generationState.collect { state ->
                when (state) {
                    is GenerationState.Completed -> {
                        _uiState.value = UIState.Success(state.result)
                        _lastResult.value = state.result
                    }
                    is GenerationState.Failed -> {
                        _uiState.value = UIState.Error(state.error)
                        _lastResult.value = state.error
                    }
                    is GenerationState.Idle -> {
                        if (_uiState.value is UIState.Generating) {
                            _uiState.value = UIState.Idle
                        }
                    }
                    else -> {
                        _uiState.value = UIState.Generating(state)
                    }
                }
            }
        }
    }
    
    /**
     * Loads available providers and options from settings.
     */
    fun loadProvidersAndOptions() {
        viewModelScope.launch {
            // Get all providers from settings
            val providers = settingsManager.getAllProviders().map { ProviderIdentifier(it) }
            _availableProviders.value = providers
            
            // Set current provider
            val currentProviderId = ProviderIdentifier(settingsManager.currentProvider)
            _selectedProvider.value = currentProviderId
            
            // Load options for current provider
            updateOptionsForProvider(currentProviderId)
        }
    }
    
    /**
     * Selects a provider (called from UI).
     */
    fun selectProvider(provider: ProviderIdentifier) {
        _selectedProvider.value = provider
        settingsManager.setProvider(provider.value)
        updateOptionsForProvider(provider)
    }
    
    /**
     * Selects an option/model (called from UI).
     */
    fun selectOption(option: ProviderOption) {
        _selectedOption.value = option
        settingsManager.setModel(option.id)
    }
    
    /**
     * Updates available options when provider changes.
     */
    private fun updateOptionsForProvider(provider: ProviderIdentifier) {
        val models = settingsManager.getModelsForProvider(provider.value)
        val options = models.map { model ->
            ProviderOption(
                id = model,
                displayName = model,
                description = "AI model"
            )
        }
        _availableOptions.value = options
        
        // Select first option or current model
        val currentModel = settingsManager.currentModel
        val selectedOpt = options.find { it.id == currentModel } ?: options.firstOrNull()
        _selectedOption.value = selectedOpt
    }
    
    /**
     * Generates a project with the given prompt and settings.
     * Provider and option must be selected from UI before calling this.
     * 
     * @param projectName Name for the generated project
     * @param prompt User's project description/requirements
     * @param additionalContext Optional additional context
     */
    fun generateProject(
        projectName: String,
        prompt: String,
        additionalContext: String? = null
    ) {
        val provider = _selectedProvider.value
        val option = _selectedOption.value
        
        if (provider == null) {
            _uiState.value = UIState.Error(
                AIProjectGenerationResult.Error(
                    GenerationErrorType.INVALID_REQUEST,
                    "No provider selected"
                )
            )
            return
        }
        
        if (option == null) {
            _uiState.value = UIState.Error(
                AIProjectGenerationResult.Error(
                    GenerationErrorType.INVALID_REQUEST,
                    "No model selected"
                )
            )
            return
        }
        
        if (projectName.isBlank()) {
            _uiState.value = UIState.Error(
                AIProjectGenerationResult.Error(
                    GenerationErrorType.INVALID_REQUEST,
                    "Project name is required"
                )
            )
            return
        }
        
        if (prompt.isBlank()) {
            _uiState.value = UIState.Error(
                AIProjectGenerationResult.Error(
                    GenerationErrorType.INVALID_REQUEST,
                    "Project description is required"
                )
            )
            return
        }
        
        _uiState.value = UIState.Loading
        
        viewModelScope.launch {
            val request = AIProjectGenerationRequest(
                prompt = prompt,
                provider = provider,
                option = option,
                projectName = sanitizeProjectName(projectName),
                additionalContext = additionalContext
            )
            
            orchestrator.generateProject(request)
        }
    }
    
    /**
     * Resets the ViewModel to idle state.
     */
    fun reset() {
        orchestrator.reset()
        _uiState.value = UIState.Idle
        _lastResult.value = null
    }
    
    /**
     * Sanitizes a project name for use in file paths.
     */
    private fun sanitizeProjectName(name: String): String {
        return name
            .trim()
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .take(50)
            .ifEmpty { "MyProject" }
    }
    
    /**
     * Gets user-friendly error message for display.
     */
    fun getErrorMessage(error: AIProjectGenerationResult.Error): String {
        return when (error.errorType) {
            GenerationErrorType.PROVIDER_UNREACHABLE -> "Cannot reach AI provider. Check your internet connection."
            GenerationErrorType.EMPTY_AI_RESPONSE -> "AI returned an empty response. Please try again."
            GenerationErrorType.MALFORMED_STRUCTURAL_OUTPUT -> "Could not parse AI response. The format was invalid."
            GenerationErrorType.PARSING_ERROR -> "Error parsing project structure: ${error.message}"
            GenerationErrorType.FILE_SYSTEM_ERROR -> "File system error: ${error.message}"
            GenerationErrorType.STORAGE_QUOTA_EXCEEDED -> "Not enough storage space available."
            GenerationErrorType.ZIP_CREATION_ERROR -> "Failed to create ZIP file: ${error.message}"
            GenerationErrorType.INVALID_REQUEST -> error.message
            GenerationErrorType.UNKNOWN_ERROR -> "An unexpected error occurred: ${error.message}"
        }
    }
    
    /**
     * Factory for creating AIProjectGeneratorViewModel.
     */
    class Factory(
        private val context: Context,
        private val settingsManager: SettingsManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AIProjectGeneratorViewModel::class.java)) {
                return AIProjectGeneratorViewModel(context.applicationContext, settingsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
