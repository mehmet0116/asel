package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.aikodasistani.aikodasistani.managers.SettingsManager
import com.aikodasistani.aikodasistani.projectgenerator.presentation.AIProjectGeneratorViewModel
import com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas.CanvasVisualizationViewModel
import com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas.GenerationCanvasScreen
import com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas.PlaybackState
import com.aikodasistani.aikodasistani.ui.theme.AIKodAsistaniTheme
import kotlinx.coroutines.launch

/**
 * Activity for the canvas-based real-time visualization of AI code generation.
 * 
 * This activity provides:
 * - Real-time streaming visualization of generated files
 * - Interactive file tree navigation
 * - Syntax-highlighted code display with animations
 * - Progress tracking and metadata display
 * - Playback controls (pause, fast-forward, replay)
 * - AI query capability for code explanations
 */
class GenerationVisualizationActivity : ComponentActivity() {
    
    private lateinit var settingsManager: SettingsManager
    
    // ViewModel for AI generation
    private val generatorViewModel: AIProjectGeneratorViewModel by viewModels {
        AIProjectGeneratorViewModel.Factory(applicationContext, settingsManager)
    }
    
    // ViewModel for visualization
    private val visualizationViewModel: CanvasVisualizationViewModel by viewModels {
        CanvasVisualizationViewModel.Factory(
            applicationContext,
            settingsManager,
            generatorViewModel
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize settings manager
        settingsManager = SettingsManager(this)
        
        // Get project info from intent
        val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Project"
        val prompt = intent.getStringExtra(EXTRA_PROMPT) ?: ""
        val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START, false)
        
        // Initialize ViewModel with settings asynchronously
        lifecycleScope.launch {
            settingsManager.initialize()
            generatorViewModel.loadProvidersAndOptions()
            
            // Auto-start generation if requested
            if (autoStart && prompt.isNotEmpty()) {
                generatorViewModel.generateProject(projectName, prompt)
            }
        }
        
        // Show canvas with project name
        visualizationViewModel.showCanvas(projectName)
        
        setContent {
            AIKodAsistaniTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by visualizationViewModel.visualState.collectAsState()
                    
                    GenerationCanvasScreen(
                        state = state,
                        onFileSelect = { index ->
                            visualizationViewModel.selectFile(index)
                        },
                        onDirectoryToggle = { path ->
                            visualizationViewModel.toggleDirectory(path)
                        },
                        onPlaybackAction = { playbackState ->
                            visualizationViewModel.setPlaybackState(playbackState)
                        },
                        onCopyCode = { code ->
                            copyToClipboard(code)
                        },
                        onAskAI = { question, fileIndex ->
                            handleAIQuery(question, fileIndex)
                        },
                        onClose = {
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Copies code to the clipboard.
     */
    private fun copyToClipboard(code: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Generated Code", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Handles AI query about a file.
     */
    private fun handleAIQuery(question: String, fileIndex: Int) {
        visualizationViewModel.askAIAboutFile(question, fileIndex)
        
        // Optionally, you could launch an intent to the main chat activity
        // with the question as context
        Toast.makeText(
            this,
            "Question recorded. Use main chat for detailed AI responses.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    companion object {
        const val EXTRA_PROJECT_NAME = "project_name"
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_AUTO_START = "auto_start"
        
        /**
         * Creates an intent to launch the visualization activity.
         */
        fun createIntent(
            context: Context,
            projectName: String,
            prompt: String? = null,
            autoStart: Boolean = false
        ): Intent {
            return Intent(context, GenerationVisualizationActivity::class.java).apply {
                putExtra(EXTRA_PROJECT_NAME, projectName)
                if (prompt != null) {
                    putExtra(EXTRA_PROMPT, prompt)
                }
                putExtra(EXTRA_AUTO_START, autoStart)
            }
        }
    }
}
