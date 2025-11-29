package com.aikodasistani.aikodasistani.projectgenerator.canvas

import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectFile
import com.aikodasistani.aikodasistani.projectgenerator.presentation.canvas.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for canvas visualization models and utilities.
 */
class CanvasVisualizationModelsTest {

    @Test
    fun `test GeneratedFileState properties`() {
        val fileState = GeneratedFileState(
            path = "src/main/kotlin/App.kt",
            displayName = "App.kt",
            extension = "kt",
            status = FileStatus.COMPLETED,
            content = "fun main() {}"
        )
        
        assertEquals("App.kt", fileState.filename)
        assertEquals("src/main/kotlin", fileState.directory)
        assertEquals("kotlin", fileState.languageHint)
    }
    
    @Test
    fun `test GeneratedFileState language hints`() {
        val kotlinFile = GeneratedFileState(
            path = "test.kt", displayName = "test.kt", extension = "kt"
        )
        assertEquals("kotlin", kotlinFile.languageHint)
        
        val javaFile = GeneratedFileState(
            path = "Test.java", displayName = "Test.java", extension = "java"
        )
        assertEquals("java", javaFile.languageHint)
        
        val pythonFile = GeneratedFileState(
            path = "script.py", displayName = "script.py", extension = "py"
        )
        assertEquals("python", pythonFile.languageHint)
        
        val jsFile = GeneratedFileState(
            path = "app.js", displayName = "app.js", extension = "js"
        )
        assertEquals("javascript", jsFile.languageHint)
        
        val tsFile = GeneratedFileState(
            path = "app.tsx", displayName = "app.tsx", extension = "tsx"
        )
        assertEquals("typescript", tsFile.languageHint)
        
        val unknownFile = GeneratedFileState(
            path = "data.xyz", displayName = "data.xyz", extension = "xyz"
        )
        assertEquals("text", unknownFile.languageHint)
    }
    
    @Test
    fun `test FileTreeNode buildTree with empty list`() {
        val tree = FileTreeNode.buildTree(emptyList(), "TestProject")
        
        assertEquals("TestProject", tree.name)
        assertTrue(tree.isDirectory)
        assertTrue(tree.isExpanded)
        assertTrue(tree.children.isEmpty())
    }
    
    @Test
    fun `test FileTreeNode buildTree with single file`() {
        val files = listOf(
            GeneratedFileState(
                path = "README.md",
                displayName = "README.md",
                extension = "md"
            )
        )
        
        val tree = FileTreeNode.buildTree(files, "TestProject")
        
        assertEquals("TestProject", tree.name)
        assertEquals(1, tree.children.size)
        assertEquals("README.md", tree.children[0].name)
        assertFalse(tree.children[0].isDirectory)
    }
    
    @Test
    fun `test FileTreeNode buildTree with nested directories`() {
        val files = listOf(
            GeneratedFileState(
                path = "src/main/kotlin/App.kt",
                displayName = "App.kt",
                extension = "kt",
                parentPath = "src/main/kotlin"
            ),
            GeneratedFileState(
                path = "src/main/kotlin/Utils.kt",
                displayName = "Utils.kt",
                extension = "kt",
                parentPath = "src/main/kotlin"
            ),
            GeneratedFileState(
                path = "build.gradle.kts",
                displayName = "build.gradle.kts",
                extension = "kts",
                parentPath = ""
            )
        )
        
        val tree = FileTreeNode.buildTree(files, "TestProject")
        
        assertEquals("TestProject", tree.name)
        // Should have 'src' directory and 'build.gradle.kts' file at root level
        assertTrue(tree.children.isNotEmpty())
        
        // Check that directories come before files
        val directoryChildren = tree.children.filter { it.isDirectory }
        val fileChildren = tree.children.filter { !it.isDirectory }
        
        assertTrue(directoryChildren.isNotEmpty())
        assertTrue(fileChildren.isNotEmpty())
    }
    
    @Test
    fun `test ProjectFile toGeneratedFileState conversion`() {
        val projectFile = ProjectFile(
            path = "src/main/App.kt",
            content = "class App {}"
        )
        
        val fileState = projectFile.toGeneratedFileState(0, FileStatus.COMPLETED)
        
        assertEquals("src/main/App.kt", fileState.path)
        assertEquals("App.kt", fileState.displayName)
        assertEquals("kt", fileState.extension)
        assertEquals(FileStatus.COMPLETED, fileState.status)
        assertEquals("class App {}", fileState.content)
    }
    
    @Test
    fun `test CanvasVisualizationState initial state`() {
        val state = CanvasVisualizationState()
        
        assertFalse(state.isVisible)
        assertEquals("", state.projectName)
        assertTrue(state.files.isEmpty())
        assertEquals(-1, state.selectedFileIndex)
        assertEquals(0f, state.overallProgress, 0.001f)
        assertEquals(GenerationPhase.IDLE, state.currentPhase)
        assertEquals(PlaybackState.PLAYING, state.playbackState)
        assertNull(state.errorState)
    }
    
    @Test
    fun `test GenerationEvent creation`() {
        val event = GenerationEvent(
            type = EventType.FILE_COMPLETED,
            message = "File written: App.kt",
            fileIndex = 0
        )
        
        assertEquals(EventType.FILE_COMPLETED, event.type)
        assertEquals("File written: App.kt", event.message)
        assertEquals(0, event.fileIndex)
        assertTrue(event.timestamp > 0)
    }
    
    @Test
    fun `test GenerationMetadata defaults`() {
        val metadata = GenerationMetadata()
        
        assertEquals("", metadata.provider)
        assertEquals("", metadata.model)
        assertEquals(0, metadata.totalFiles)
        assertEquals(0L, metadata.totalSize)
        assertEquals(0L, metadata.durationMs)
        assertNull(metadata.tokensUsed)
    }
    
    @Test
    fun `test ErrorState with recoverable flag`() {
        val recoverableError = ErrorState(
            message = "Network timeout",
            recoverable = true
        )
        assertTrue(recoverableError.recoverable)
        
        val fatalError = ErrorState(
            message = "Invalid request",
            recoverable = false
        )
        assertFalse(fatalError.recoverable)
    }
    
    @Test
    fun `test FileStatus enum values`() {
        assertEquals(5, FileStatus.entries.size)
        assertTrue(FileStatus.entries.contains(FileStatus.PENDING))
        assertTrue(FileStatus.entries.contains(FileStatus.GENERATING))
        assertTrue(FileStatus.entries.contains(FileStatus.COMPLETED))
        assertTrue(FileStatus.entries.contains(FileStatus.ERROR))
        assertTrue(FileStatus.entries.contains(FileStatus.SKIPPED))
    }
    
    @Test
    fun `test GenerationPhase enum values`() {
        assertEquals(8, GenerationPhase.entries.size)
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.IDLE))
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.PREPARING))
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.CALLING_AI))
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.PARSING))
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.WRITING_FILES))
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.CREATING_ZIP))
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.COMPLETED))
        assertTrue(GenerationPhase.entries.contains(GenerationPhase.FAILED))
    }
    
    @Test
    fun `test PlaybackState enum values`() {
        assertEquals(4, PlaybackState.entries.size)
        assertTrue(PlaybackState.entries.contains(PlaybackState.PLAYING))
        assertTrue(PlaybackState.entries.contains(PlaybackState.PAUSED))
        assertTrue(PlaybackState.entries.contains(PlaybackState.FAST_FORWARD))
        assertTrue(PlaybackState.entries.contains(PlaybackState.REPLAYING))
    }
    
    @Test
    fun `test EventType enum values`() {
        assertEquals(9, EventType.entries.size)
        assertTrue(EventType.entries.contains(EventType.PHASE_CHANGE))
        assertTrue(EventType.entries.contains(EventType.FILE_STARTED))
        assertTrue(EventType.entries.contains(EventType.FILE_COMPLETED))
        assertTrue(EventType.entries.contains(EventType.FILE_ERROR))
        assertTrue(EventType.entries.contains(EventType.PROGRESS_UPDATE))
        assertTrue(EventType.entries.contains(EventType.AI_RESPONSE))
        assertTrue(EventType.entries.contains(EventType.WARNING))
        assertTrue(EventType.entries.contains(EventType.ERROR))
        assertTrue(EventType.entries.contains(EventType.INFO))
    }
}
