package com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas

import androidx.compose.runtime.Immutable
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectFile

/**
 * State for the canvas-based real-time visualization of code generation.
 */
@Immutable
data class CanvasVisualizationState(
    val isVisible: Boolean = false,
    val projectName: String = "",
    val files: List<GeneratedFileState> = emptyList(),
    val selectedFileIndex: Int = -1,
    val overallProgress: Float = 0f,
    val currentPhase: GenerationPhase = GenerationPhase.IDLE,
    val phaseMessage: String = "",
    val startTimestamp: Long = 0L,
    val eventLog: List<GenerationEvent> = emptyList(),
    val playbackState: PlaybackState = PlaybackState.PLAYING,
    val metadata: GenerationMetadata = GenerationMetadata(),
    val errorState: ErrorState? = null
)

/**
 * State of an individual generated file during the generation process.
 */
@Immutable
data class GeneratedFileState(
    val path: String,
    val displayName: String,
    val extension: String,
    val status: FileStatus = FileStatus.PENDING,
    val content: String = "",
    val visibleLines: Int = 0,
    val totalLines: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val isExpanded: Boolean = false,
    val parentPath: String = ""
) {
    val directory: String
        get() = path.substringBeforeLast('/', "")
    
    val filename: String
        get() = path.substringAfterLast('/')
    
    val languageHint: String
        get() = when (extension.lowercase()) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "xml" -> "xml"
            "json" -> "json"
            "gradle" -> "groovy"
            "py" -> "python"
            "js", "jsx" -> "javascript"
            "ts", "tsx" -> "typescript"
            "swift" -> "swift"
            "dart" -> "dart"
            "go" -> "go"
            "rs" -> "rust"
            "html" -> "html"
            "css", "scss", "sass" -> "css"
            "md" -> "markdown"
            "yaml", "yml" -> "yaml"
            "sh", "bash" -> "shell"
            else -> "text"
        }
}

/**
 * Status of a file during generation.
 */
enum class FileStatus {
    PENDING,
    GENERATING,
    COMPLETED,
    ERROR,
    SKIPPED
}

/**
 * Phases of the generation process.
 */
enum class GenerationPhase {
    IDLE,
    PREPARING,
    CALLING_AI,
    PARSING,
    WRITING_FILES,
    CREATING_ZIP,
    COMPLETED,
    FAILED
}

/**
 * Event log entry for audit and tracking.
 */
@Immutable
data class GenerationEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: EventType,
    val message: String,
    val details: String? = null,
    val fileIndex: Int? = null
)

/**
 * Types of generation events.
 */
enum class EventType {
    PHASE_CHANGE,
    FILE_STARTED,
    FILE_COMPLETED,
    FILE_ERROR,
    PROGRESS_UPDATE,
    AI_RESPONSE,
    WARNING,
    ERROR,
    INFO
}

/**
 * Playback control state.
 */
enum class PlaybackState {
    PLAYING,
    PAUSED,
    FAST_FORWARD,
    REPLAYING
}

/**
 * Metadata about the generation process.
 */
@Immutable
data class GenerationMetadata(
    val provider: String = "",
    val model: String = "",
    val prompt: String = "",
    val totalFiles: Int = 0,
    val totalSize: Long = 0L,
    val durationMs: Long = 0L,
    val tokensUsed: Int? = null
)

/**
 * Error state for visualization.
 */
@Immutable
data class ErrorState(
    val message: String,
    val details: String? = null,
    val recoverable: Boolean = false,
    val fileIndex: Int? = null
)

/**
 * Tree node for file structure visualization.
 */
@Immutable
data class FileTreeNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileTreeNode> = emptyList(),
    val fileIndex: Int = -1,
    val status: FileStatus = FileStatus.PENDING,
    val isExpanded: Boolean = false,
    val depth: Int = 0
) {
    companion object {
        /**
         * Builds a tree structure from a flat list of files.
         */
        fun buildTree(files: List<GeneratedFileState>, projectName: String): FileTreeNode {
            val root = FileTreeNode(
                name = projectName,
                path = "",
                isDirectory = true,
                isExpanded = true,
                depth = 0
            )
            
            if (files.isEmpty()) return root
            
            // Create directory structure
            val directories = mutableMapOf<String, MutableList<FileTreeNode>>()
            directories[""] = mutableListOf()
            
            files.forEachIndexed { index, file ->
                val parts = file.path.split("/")
                var currentPath = ""
                
                for (i in 0 until parts.size - 1) {
                    val parentPath = currentPath
                    currentPath = if (currentPath.isEmpty()) parts[i] else "$currentPath/${parts[i]}"
                    
                    if (!directories.containsKey(currentPath)) {
                        directories[currentPath] = mutableListOf()
                        val dirNode = FileTreeNode(
                            name = parts[i],
                            path = currentPath,
                            isDirectory = true,
                            depth = i + 1
                        )
                        directories.getOrPut(parentPath) { mutableListOf() }.add(dirNode)
                    }
                }
                
                // Add file to its parent directory
                val parentDir = file.directory
                val fileNode = FileTreeNode(
                    name = file.filename,
                    path = file.path,
                    isDirectory = false,
                    fileIndex = index,
                    status = file.status,
                    depth = parts.size
                )
                directories.getOrPut(parentDir) { mutableListOf() }.add(fileNode)
            }
            
            // Build tree recursively
            fun buildSubTree(path: String, depth: Int): List<FileTreeNode> {
                val children = directories[path] ?: return emptyList()
                return children.map { node ->
                    if (node.isDirectory) {
                        node.copy(children = buildSubTree(node.path, depth + 1))
                    } else {
                        node
                    }
                }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            }
            
            return root.copy(children = buildSubTree("", 1))
        }
    }
}

/**
 * Extension to convert ProjectFile to GeneratedFileState.
 */
fun ProjectFile.toGeneratedFileState(index: Int, status: FileStatus = FileStatus.COMPLETED): GeneratedFileState {
    val lines = content.lines()
    return GeneratedFileState(
        path = path,
        displayName = path.substringAfterLast('/'),
        extension = path.substringAfterLast('.', ""),
        status = status,
        content = content,
        visibleLines = lines.size,
        totalLines = lines.size,
        parentPath = path.substringBeforeLast('/', "")
    )
}
