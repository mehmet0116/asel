package com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aikodasistani.aikodasistani.managers.SettingsManager
import com.aikodasistani.aikodasistani.projectgenerator.domain.*
import com.aikodasistani.aikodasistani.projectgenerator.presentation.AIProjectGeneratorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the canvas-based visualization of code generation.
 * Transforms generation states into visualization states for the UI.
 */
class CanvasVisualizationViewModel(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val parentViewModel: AIProjectGeneratorViewModel
) : ViewModel() {
    
    companion object {
        /** Maximum number of events to keep in the event log */
        private const val MAX_EVENT_LOG_SIZE = 100
    }
    
    private val _visualState = MutableStateFlow(CanvasVisualizationState())
    val visualState: StateFlow<CanvasVisualizationState> = _visualState.asStateFlow()
    
    private val _expandedDirectories = MutableStateFlow<Set<String>>(setOf(""))
    
    // Track file streaming animation
    private var currentStreamingFileIndex = -1
    
    init {
        observeGenerationState()
    }
    
    /**
     * Observes the parent ViewModel's generation state and transforms it
     * into visualization state.
     */
    private fun observeGenerationState() {
        viewModelScope.launch {
            parentViewModel.uiState.collect { uiState ->
                when (uiState) {
                    is AIProjectGeneratorViewModel.UIState.Idle -> {
                        updateState { 
                            it.copy(
                                isVisible = false,
                                currentPhase = GenerationPhase.IDLE
                            )
                        }
                    }
                    is AIProjectGeneratorViewModel.UIState.Loading -> {
                        updateState {
                            it.copy(
                                isVisible = true,
                                currentPhase = GenerationPhase.PREPARING,
                                phaseMessage = "Preparing generation...",
                                startTimestamp = System.currentTimeMillis(),
                                files = emptyList(),
                                eventLog = listOf(
                                    GenerationEvent(
                                        type = EventType.PHASE_CHANGE,
                                        message = "Generation started"
                                    )
                                )
                            )
                        }
                    }
                    is AIProjectGeneratorViewModel.UIState.Generating -> {
                        handleGenerationState(uiState.state)
                    }
                    is AIProjectGeneratorViewModel.UIState.Success -> {
                        handleSuccess(uiState.result)
                    }
                    is AIProjectGeneratorViewModel.UIState.Error -> {
                        handleError(uiState.error)
                    }
                }
            }
        }
    }
    
    /**
     * Handles detailed generation state changes.
     */
    private suspend fun handleGenerationState(state: GenerationState) {
        when (state) {
            is GenerationState.Preparing -> {
                updateState { 
                    it.copy(
                        currentPhase = GenerationPhase.PREPARING,
                        phaseMessage = "Preparing request...",
                        overallProgress = 0.05f
                    )
                }
                addEvent(EventType.PHASE_CHANGE, "Preparing request")
            }
            is GenerationState.CallingAI -> {
                updateState { 
                    it.copy(
                        currentPhase = GenerationPhase.CALLING_AI,
                        phaseMessage = state.message,
                        overallProgress = 0.1f,
                        metadata = it.metadata.copy(
                            provider = parentViewModel.selectedProvider.value?.value ?: "",
                            model = parentViewModel.selectedOption.value?.displayName ?: ""
                        )
                    )
                }
                addEvent(EventType.AI_RESPONSE, state.message)
            }
            is GenerationState.Parsing -> {
                updateState { 
                    it.copy(
                        currentPhase = GenerationPhase.PARSING,
                        phaseMessage = state.message,
                        overallProgress = 0.3f
                    )
                }
                addEvent(EventType.PHASE_CHANGE, "Parsing AI response")
            }
            is GenerationState.WritingFiles -> {
                val progress = if (state.total > 0) {
                    0.3f + (0.5f * state.progress / state.total)
                } else 0.3f
                
                updateState { 
                    it.copy(
                        currentPhase = GenerationPhase.WRITING_FILES,
                        phaseMessage = "Writing file ${state.progress}/${state.total}",
                        overallProgress = progress
                    )
                }
                
                // Update file statuses
                if (state.progress > 0 && state.progress <= _visualState.value.files.size) {
                    updateFileStatus(state.progress - 1, FileStatus.COMPLETED)
                }
                if (state.progress < _visualState.value.files.size) {
                    updateFileStatus(state.progress, FileStatus.GENERATING)
                    if (currentStreamingFileIndex != state.progress) {
                        currentStreamingFileIndex = state.progress
                        selectFile(state.progress)
                    }
                }
            }
            is GenerationState.CreatingZip -> {
                updateState { 
                    it.copy(
                        currentPhase = GenerationPhase.CREATING_ZIP,
                        phaseMessage = state.message,
                        overallProgress = 0.9f
                    )
                }
                addEvent(EventType.PHASE_CHANGE, "Creating ZIP archive")
            }
            is GenerationState.Completed -> {
                handleSuccess(state.result)
            }
            is GenerationState.Failed -> {
                handleError(state.error)
            }
            is GenerationState.Idle -> {
                // No action needed
            }
        }
    }
    
    /**
     * Handles successful generation completion.
     */
    private suspend fun handleSuccess(result: AIProjectGenerationResult.Success) {
        val files = result.projectStructure.files.mapIndexed { index, file ->
            file.toGeneratedFileState(index, FileStatus.COMPLETED)
        }
        
        val duration = System.currentTimeMillis() - _visualState.value.startTimestamp
        
        updateState { 
            it.copy(
                currentPhase = GenerationPhase.COMPLETED,
                phaseMessage = "Generation complete!",
                overallProgress = 1f,
                files = files,
                projectName = result.projectStructure.root,
                metadata = it.metadata.copy(
                    totalFiles = files.size,
                    totalSize = result.projectStructure.metadata.totalSize,
                    durationMs = duration
                ),
                errorState = null
            )
        }
        
        addEvent(EventType.PHASE_CHANGE, "Generation completed successfully")
        addEvent(EventType.INFO, "${files.size} files generated")
        
        // Auto-select first file
        if (files.isNotEmpty() && _visualState.value.selectedFileIndex < 0) {
            selectFile(0)
        }
    }
    
    /**
     * Handles generation errors.
     */
    private suspend fun handleError(error: AIProjectGenerationResult.Error) {
        updateState { 
            it.copy(
                currentPhase = GenerationPhase.FAILED,
                phaseMessage = "Generation failed",
                errorState = ErrorState(
                    message = error.message,
                    details = error.details,
                    recoverable = error.errorType != GenerationErrorType.INVALID_REQUEST
                )
            )
        }
        
        addEvent(EventType.ERROR, error.message)
    }
    
    /**
     * Selects a file for viewing.
     */
    fun selectFile(index: Int) {
        viewModelScope.launch {
            updateState { it.copy(selectedFileIndex = index) }
            
            if (index >= 0 && index < _visualState.value.files.size) {
                addEvent(
                    EventType.INFO,
                    "Viewing: ${_visualState.value.files[index].path}"
                )
            }
        }
    }
    
    /**
     * Toggles directory expansion in the file tree.
     */
    fun toggleDirectory(path: String) {
        val current = _expandedDirectories.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _expandedDirectories.value = current
    }
    
    /**
     * Changes playback state.
     */
    fun setPlaybackState(state: PlaybackState) {
        viewModelScope.launch {
            updateState { it.copy(playbackState = state) }
            
            when (state) {
                PlaybackState.PAUSED -> addEvent(EventType.INFO, "Playback paused")
                PlaybackState.PLAYING -> addEvent(EventType.INFO, "Playback resumed")
                PlaybackState.FAST_FORWARD -> addEvent(EventType.INFO, "Fast forward enabled")
                PlaybackState.REPLAYING -> {
                    addEvent(EventType.INFO, "Replaying generation")
                    replayGeneration()
                }
            }
        }
    }
    
    /**
     * Replays the generation visualization.
     */
    private suspend fun replayGeneration() {
        val files = _visualState.value.files
        if (files.isEmpty()) return
        
        // Reset file statuses
        updateState { state ->
            state.copy(
                files = state.files.map { it.copy(status = FileStatus.PENDING) },
                currentPhase = GenerationPhase.WRITING_FILES,
                selectedFileIndex = 0,
                playbackState = PlaybackState.PLAYING
            )
        }
        
        // Animate through files
        for (index in files.indices) {
            if (_visualState.value.playbackState == PlaybackState.PAUSED) break
            
            val delayMs = when (_visualState.value.playbackState) {
                PlaybackState.FAST_FORWARD -> 100L
                else -> 500L
            }
            
            updateFileStatus(index, FileStatus.GENERATING)
            selectFile(index)
            
            delay(delayMs)
            
            updateFileStatus(index, FileStatus.COMPLETED)
        }
        
        updateState { it.copy(currentPhase = GenerationPhase.COMPLETED) }
    }
    
    /**
     * Submits a question to the AI about a file.
     */
    fun askAIAboutFile(question: String, fileIndex: Int) {
        viewModelScope.launch {
            val file = _visualState.value.files.getOrNull(fileIndex)
            val contextMessage = if (file != null) {
                "User asked about ${file.path}: $question"
            } else {
                "User asked: $question"
            }
            
            addEvent(EventType.AI_RESPONSE, "Question: $question")
            
            // Note: In a real implementation, this would call the AI provider
            // For now, we just log the question
            // The actual AI interaction would be handled by the parent activity
        }
    }
    
    /**
     * Shows the visualization canvas.
     */
    fun showCanvas(projectName: String) {
        viewModelScope.launch {
            updateState { 
                it.copy(
                    isVisible = true,
                    projectName = projectName,
                    startTimestamp = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * Hides the visualization canvas.
     */
    fun hideCanvas() {
        viewModelScope.launch {
            updateState { it.copy(isVisible = false) }
        }
    }
    
    /**
     * Initializes files from a project structure (for pre-populating).
     */
    fun initializeFiles(files: List<ProjectFile>) {
        viewModelScope.launch {
            val fileStates = files.mapIndexed { index, file ->
                file.toGeneratedFileState(index, FileStatus.PENDING)
            }
            updateState { 
                it.copy(
                    files = fileStates,
                    metadata = it.metadata.copy(totalFiles = files.size)
                )
            }
        }
    }
    
    // Helper functions
    
    private suspend fun updateState(update: (CanvasVisualizationState) -> CanvasVisualizationState) {
        _visualState.value = update(_visualState.value)
    }
    
    private suspend fun updateFileStatus(index: Int, status: FileStatus) {
        val files = _visualState.value.files.toMutableList()
        if (index in files.indices) {
            files[index] = files[index].copy(status = status)
            updateState { it.copy(files = files) }
            
            when (status) {
                FileStatus.GENERATING -> addEvent(
                    EventType.FILE_STARTED,
                    "Writing: ${files[index].path}",
                    fileIndex = index
                )
                FileStatus.COMPLETED -> addEvent(
                    EventType.FILE_COMPLETED,
                    "Completed: ${files[index].path}",
                    fileIndex = index
                )
                FileStatus.ERROR -> addEvent(
                    EventType.FILE_ERROR,
                    "Error: ${files[index].path}",
                    fileIndex = index
                )
                else -> {}
            }
        }
    }
    
    private suspend fun addEvent(
        type: EventType,
        message: String,
        details: String? = null,
        fileIndex: Int? = null
    ) {
        val event = GenerationEvent(
            type = type,
            message = message,
            details = details,
            fileIndex = fileIndex
        )
        val events = _visualState.value.eventLog.toMutableList()
        events.add(event)
        // Keep only last MAX_EVENT_LOG_SIZE events
        if (events.size > MAX_EVENT_LOG_SIZE) {
            events.removeAt(0)
        }
        updateState { it.copy(eventLog = events) }
    }
    
    /**
     * Factory for creating CanvasVisualizationViewModel.
     */
    class Factory(
        private val context: Context,
        private val settingsManager: SettingsManager,
        private val parentViewModel: AIProjectGeneratorViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CanvasVisualizationViewModel::class.java)) {
                return CanvasVisualizationViewModel(
                    context.applicationContext,
                    settingsManager,
                    parentViewModel
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
