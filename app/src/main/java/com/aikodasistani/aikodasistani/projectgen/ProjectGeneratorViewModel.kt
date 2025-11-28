package com.aikodasistani.aikodasistani.projectgen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aikodasistani.aikodasistani.managers.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Project Generator ViewModel
 * 
 * Manages the UI state and business logic for AI-driven project generation.
 * 
 * Key Design Principles:
 * - Provider selection comes from UI (no hardcoded values)
 * - Provider options are UI-driven configuration
 * - Clean separation of concerns (MVVM)
 * - Reactive state management with StateFlow
 */
class ProjectGeneratorViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ProjectGeneratorVM"
    }

    private val settingsManager = SettingsManager(application)
    private val orchestrator = ProjectGenerationOrchestrator(application)

    // ==================== UI State ====================
    
    /**
     * Current generation state
     */
    sealed class GenerationState {
        object Idle : GenerationState()
        data class Generating(val message: String) : GenerationState()
        data class Success(val result: ProjectGenerationOutput.Success) : GenerationState()
        data class Error(val error: ProjectGenerationOutput.Error) : GenerationState()
    }

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    /**
     * Available providers (from SettingsManager)
     */
    private val _providers = MutableStateFlow<List<String>>(emptyList())
    val providers: StateFlow<List<String>> = _providers.asStateFlow()

    /**
     * Currently selected provider
     */
    private val _selectedProvider = MutableStateFlow("")
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()

    /**
     * Available options (models) for current provider
     */
    private val _providerOptions = MutableStateFlow<List<String>>(emptyList())
    val providerOptions: StateFlow<List<String>> = _providerOptions.asStateFlow()

    /**
     * Currently selected option
     */
    private val _selectedOption = MutableStateFlow("")
    val selectedOption: StateFlow<String> = _selectedOption.asStateFlow()

    init {
        loadProviders()
    }

    /**
     * Load available providers from SettingsManager
     */
    private fun loadProviders() {
        viewModelScope.launch {
            try {
                settingsManager.initialize()
                
                val allProviders = settingsManager.getAllProviders()
                _providers.value = allProviders
                
                // Set current provider from settings
                val currentProvider = settingsManager.currentProvider
                if (currentProvider.isNotBlank()) {
                    selectProvider(currentProvider)
                } else if (allProviders.isNotEmpty()) {
                    selectProvider(allProviders.first())
                }
                
                Log.d(TAG, "Loaded ${allProviders.size} providers")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading providers", e)
                // Fallback to defaults
                _providers.value = listOf("OPENAI", "GEMINI", "DEEPSEEK")
                selectProvider("OPENAI")
            }
        }
    }

    /**
     * Select a provider and load its options
     */
    fun selectProvider(provider: String) {
        _selectedProvider.value = provider
        
        val options = settingsManager.getModelsForProvider(provider)
        _providerOptions.value = options
        
        // Select current model from settings if it matches provider
        val currentModel = settingsManager.currentModel
        if (options.contains(currentModel)) {
            _selectedOption.value = currentModel
        } else if (options.isNotEmpty()) {
            _selectedOption.value = options.first()
        }
        
        Log.d(TAG, "Selected provider: $provider, options: ${options.size}")
    }

    /**
     * Select a provider option (model)
     */
    fun selectOption(option: String) {
        _selectedOption.value = option
        Log.d(TAG, "Selected option: $option")
    }

    /**
     * Generate project from natural language prompt
     */
    fun generateFromPrompt(prompt: String, projectName: String) {
        if (prompt.isBlank()) {
            _generationState.value = GenerationState.Error(
                ProjectGenerationOutput.Error(
                    errorType = ProjectGenerationOutput.ErrorType.UNKNOWN,
                    message = "Lütfen bir proje açıklaması girin"
                )
            )
            return
        }

        viewModelScope.launch {
            _generationState.value = GenerationState.Generating("AI ile proje oluşturuluyor...")
            
            try {
                val provider = createProviderIdentifier()
                val option = createProviderOption()
                
                val request = AiProjectRequest(
                    provider = provider,
                    option = option,
                    prompt = prompt,
                    projectName = projectName.ifBlank { extractProjectName(prompt) }
                )
                
                // Create AI provider adapter
                val aiProvider = createAiProviderAdapter()
                
                val result = orchestrator.generateProject(aiProvider, request)
                
                when (result) {
                    is ProjectGenerationOutput.Success -> {
                        _generationState.value = GenerationState.Success(result)
                    }
                    is ProjectGenerationOutput.Error -> {
                        _generationState.value = GenerationState.Error(result)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Generation error", e)
                _generationState.value = GenerationState.Error(
                    ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.UNKNOWN,
                        message = "Hata: ${e.message}"
                    )
                )
            }
        }
    }

    /**
     * Generate project from typed request (template-based)
     */
    fun generateTypedProject(
        projectType: String,
        projectName: String,
        packageName: String?,
        description: String?
    ) {
        if (projectName.isBlank()) {
            _generationState.value = GenerationState.Error(
                ProjectGenerationOutput.Error(
                    errorType = ProjectGenerationOutput.ErrorType.UNKNOWN,
                    message = "Lütfen proje adı girin"
                )
            )
            return
        }

        viewModelScope.launch {
            _generationState.value = GenerationState.Generating(
                "$projectType projesi oluşturuluyor..."
            )
            
            try {
                val option = createProviderOption()
                val aiProvider = createAiProviderAdapter()
                
                val result = orchestrator.generateTypedProject(
                    aiProvider = aiProvider,
                    projectType = projectType,
                    projectName = projectName,
                    packageName = packageName,
                    description = description,
                    option = option
                )
                
                when (result) {
                    is ProjectGenerationOutput.Success -> {
                        _generationState.value = GenerationState.Success(result)
                    }
                    is ProjectGenerationOutput.Error -> {
                        _generationState.value = GenerationState.Error(result)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Typed generation error", e)
                _generationState.value = GenerationState.Error(
                    ProjectGenerationOutput.Error(
                        errorType = ProjectGenerationOutput.ErrorType.UNKNOWN,
                        message = "Hata: ${e.message}"
                    )
                )
            }
        }
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _generationState.value = GenerationState.Idle
    }

    /**
     * Create ProviderIdentifier from current selection
     */
    private fun createProviderIdentifier(): ProviderIdentifier {
        val provider = _selectedProvider.value
        val baseUrl = settingsManager.getProviderBaseUrl(provider)
        return ProviderIdentifier(name = provider, baseUrl = baseUrl)
    }

    /**
     * Create ProviderOption from current selection
     */
    private fun createProviderOption(): ProviderOption {
        val option = _selectedOption.value
        return ProviderOption(id = option, displayName = option)
    }

    /**
     * Create an AI provider adapter that connects to the existing AI infrastructure
     */
    private fun createAiProviderAdapter(): AiProvider {
        return DefaultAiProviderAdapter(
            context = getApplication(),
            settingsManager = settingsManager,
            provider = _selectedProvider.value,
            model = _selectedOption.value
        )
    }

    /**
     * Extract project name from prompt
     */
    private fun extractProjectName(prompt: String): String {
        // Try to find quoted name
        val quotedMatch = Regex("\"([^\"]+)\"").find(prompt)
        if (quotedMatch != null) {
            return quotedMatch.groupValues[1]
        }
        
        // Try to find "named X" or "called X"
        val namedMatch = Regex("(?:named|called)\\s+(\\w+)", RegexOption.IGNORE_CASE).find(prompt)
        if (namedMatch != null) {
            return namedMatch.groupValues[1]
        }
        
        // Try to find project type and use it as base
        val typeMatch = Regex("(android|flutter|react|node|python|web|api)", RegexOption.IGNORE_CASE).find(prompt)
        if (typeMatch != null) {
            return "My${typeMatch.groupValues[1].replaceFirstChar { it.uppercase() }}Project"
        }
        
        return "MyProject"
    }
}
