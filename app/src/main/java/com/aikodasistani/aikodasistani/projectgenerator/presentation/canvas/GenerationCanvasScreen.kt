package com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main canvas-based visualization screen for AI code generation.
 * Provides real-time, interactive view of the generation process.
 */
@Composable
fun GenerationCanvasScreen(
    state: CanvasVisualizationState,
    onFileSelect: (Int) -> Unit,
    onDirectoryToggle: (String) -> Unit,
    onPlaybackAction: (PlaybackState) -> Unit,
    onCopyCode: (String) -> Unit,
    onAskAI: (String, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileTree = remember(state.files, state.projectName) {
        FileTreeNode.buildTree(state.files, state.projectName.ifEmpty { "Project" })
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with progress and controls
        GenerationHeader(
            state = state,
            onPlaybackAction = onPlaybackAction,
            onClose = onClose
        )
        
        // Main content area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Left sidebar - File tree
            FileTreeSidebar(
                tree = fileTree,
                selectedFileIndex = state.selectedFileIndex,
                onFileClick = onFileSelect,
                onDirectoryToggle = onDirectoryToggle,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Center - Code viewer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CodeViewer(
                    file = state.files.getOrNull(state.selectedFileIndex),
                    isStreaming = state.currentPhase == GenerationPhase.WRITING_FILES,
                    playbackState = state.playbackState,
                    onCopyCode = onCopyCode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bottom panel - Event log and AI interaction
                BottomPanel(
                    state = state,
                    onAskAI = { question ->
                        onAskAI(question, state.selectedFileIndex)
                    },
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Header with phase indicator, progress, and playback controls.
 */
@Composable
private fun GenerationHeader(
    state: CanvasVisualizationState,
    onPlaybackAction: (PlaybackState) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhaseIndicator(phase = state.currentPhase)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = state.projectName.ifEmpty { "Project Generation" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.phaseMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Playback controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaybackControls(
                        currentState = state.playbackState,
                        isActive = state.currentPhase !in listOf(
                            GenerationPhase.IDLE,
                            GenerationPhase.COMPLETED,
                            GenerationPhase.FAILED
                        ),
                        onAction = onPlaybackAction
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Metadata chips
                    if (state.metadata.provider.isNotEmpty()) {
                        MetadataChip(
                            icon = "🤖",
                            text = state.metadata.provider
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (state.metadata.model.isNotEmpty()) {
                        MetadataChip(
                            icon = "⚙️",
                            text = state.metadata.model
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Close button
                    IconButton(onClick = onClose) {
                        Text(text = "✕", fontSize = 18.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            ProgressSection(
                progress = state.overallProgress,
                completedFiles = state.files.count { it.status == FileStatus.COMPLETED },
                totalFiles = state.files.size,
                duration = if (state.startTimestamp > 0) {
                    System.currentTimeMillis() - state.startTimestamp
                } else 0L
            )
        }
    }
}

/**
 * Phase indicator with animated icon.
 */
@Composable
private fun PhaseIndicator(phase: GenerationPhase) {
    val infiniteTransition = rememberInfiniteTransition(label = "phase_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val (icon, color, isAnimating) = when (phase) {
        GenerationPhase.IDLE -> Triple("⏸️", MaterialTheme.colorScheme.outline, false)
        GenerationPhase.PREPARING -> Triple("🔄", MaterialTheme.colorScheme.primary, true)
        GenerationPhase.CALLING_AI -> Triple("🤖", MaterialTheme.colorScheme.secondary, true)
        GenerationPhase.PARSING -> Triple("📝", MaterialTheme.colorScheme.tertiary, true)
        GenerationPhase.WRITING_FILES -> Triple("📁", MaterialTheme.colorScheme.primary, true)
        GenerationPhase.CREATING_ZIP -> Triple("📦", MaterialTheme.colorScheme.primary, true)
        GenerationPhase.COMPLETED -> Triple("✅", Color(0xFF4CAF50), false)
        GenerationPhase.FAILED -> Triple("❌", MaterialTheme.colorScheme.error, false)
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = icon,
                fontSize = (24 * if (isAnimating) scale else 1f).sp
            )
        }
    }
}

/**
 * Playback control buttons.
 */
@Composable
private fun PlaybackControls(
    currentState: PlaybackState,
    isActive: Boolean,
    onAction: (PlaybackState) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Pause/Play
        IconButton(
            onClick = {
                onAction(
                    if (currentState == PlaybackState.PAUSED) PlaybackState.PLAYING
                    else PlaybackState.PAUSED
                )
            },
            enabled = isActive
        ) {
            Text(
                text = if (currentState == PlaybackState.PAUSED) "▶️" else "⏸️",
                fontSize = 16.sp
            )
        }
        
        // Fast forward
        IconButton(
            onClick = { onAction(PlaybackState.FAST_FORWARD) },
            enabled = isActive && currentState != PlaybackState.FAST_FORWARD
        ) {
            Text(text = "⏩", fontSize = 16.sp)
        }
        
        // Replay (only when completed)
        IconButton(
            onClick = { onAction(PlaybackState.REPLAYING) },
            enabled = !isActive
        ) {
            Text(text = "🔄", fontSize = 16.sp)
        }
    }
}

/**
 * Metadata chip display.
 */
@Composable
private fun MetadataChip(
    icon: String,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Progress section with bar and stats.
 */
@Composable
private fun ProgressSection(
    progress: Float,
    completedFiles: Int,
    totalFiles: Int,
    duration: Long
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Progress: ${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "$completedFiles / $totalFiles files",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Bottom panel with event log and AI interaction.
 */
@Composable
private fun BottomPanel(
    state: CanvasVisualizationState,
    onAskAI: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var aiQuestion by remember { mutableStateOf("") }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📋 Event Log") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🤖 Ask AI") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ℹ️ Metadata") }
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                when (selectedTab) {
                    0 -> EventLogView(events = state.eventLog)
                    1 -> AIInteractionView(
                        question = aiQuestion,
                        onQuestionChange = { aiQuestion = it },
                        onAsk = { 
                            if (aiQuestion.isNotBlank()) {
                                onAskAI(aiQuestion)
                                aiQuestion = ""
                            }
                        },
                        selectedFile = state.files.getOrNull(state.selectedFileIndex)
                    )
                    2 -> MetadataView(metadata = state.metadata)
                }
            }
        }
    }
}

/**
 * Event log display.
 */
@Composable
private fun EventLogView(events: List<GenerationEvent>) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No events yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn {
            items(events.size) { index ->
                val event = events[events.size - 1 - index] // Newest first
                EventLogItem(event = event)
            }
        }
    }
}

@Composable
private fun EventLogItem(event: GenerationEvent) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val (icon, color) = when (event.type) {
        EventType.PHASE_CHANGE -> "🔄" to MaterialTheme.colorScheme.primary
        EventType.FILE_STARTED -> "📄" to MaterialTheme.colorScheme.secondary
        EventType.FILE_COMPLETED -> "✓" to Color(0xFF4CAF50)
        EventType.FILE_ERROR -> "❌" to MaterialTheme.colorScheme.error
        EventType.PROGRESS_UPDATE -> "📊" to MaterialTheme.colorScheme.tertiary
        EventType.AI_RESPONSE -> "🤖" to MaterialTheme.colorScheme.primary
        EventType.WARNING -> "⚠️" to Color(0xFFFF9800)
        EventType.ERROR -> "❌" to MaterialTheme.colorScheme.error
        EventType.INFO -> "ℹ️" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateFormat.format(Date(event.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(text = icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = event.message,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * AI interaction input.
 */
@Composable
private fun AIInteractionView(
    question: String,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
    selectedFile: GeneratedFileState?
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (selectedFile != null) {
            Text(
                text = "Ask about: ${selectedFile.path}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChange,
                placeholder = {
                    Text(
                        text = if (selectedFile != null) 
                            "Ask about this file..."
                        else 
                            "Ask about the project..."
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAsk,
                enabled = question.isNotBlank()
            ) {
                Text("Ask")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Quick questions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickQuestionChip("Explain this code") { onQuestionChange(it) }
            QuickQuestionChip("How can I improve this?") { onQuestionChange(it) }
            QuickQuestionChip("Add tests") { onQuestionChange(it) }
        }
    }
}

@Composable
private fun QuickQuestionChip(
    text: String,
    onClick: (String) -> Unit
) {
    AssistChip(
        onClick = { onClick(text) },
        label = { Text(text, style = MaterialTheme.typography.labelSmall) }
    )
}

/**
 * Metadata display.
 */
@Composable
private fun MetadataView(metadata: GenerationMetadata) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MetadataRow("Provider", metadata.provider.ifEmpty { "N/A" })
        MetadataRow("Model", metadata.model.ifEmpty { "N/A" })
        MetadataRow("Total Files", metadata.totalFiles.toString())
        MetadataRow("Total Size", formatBytes(metadata.totalSize))
        MetadataRow("Duration", formatDuration(metadata.durationMs))
        if (metadata.tokensUsed != null) {
            MetadataRow("Tokens Used", metadata.tokensUsed.toString())
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// Utility functions
private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0s"
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
}
