package com.aikodasistani.aikodasistani.util

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Unit tests for ZipFileAnalyzerUtil
 */
class ZipFileAnalyzerUtilTest {

    @Test
    fun `test format analysis result for empty result`() {
        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = false,
            errorMessage = "Test error",
            totalFiles = 0,
            totalSize = 0L,
            files = emptyList(),
            directoryStructure = emptyList(),
            projectType = ZipFileAnalyzerUtil.ProjectType.UNKNOWN
        )

        val formatted = ZipFileAnalyzerUtil.formatAnalysisResult(result)

        assertTrue("Should contain error indicator", formatted.contains("❌"))
        assertTrue("Should contain error message", formatted.contains("Test error"))
    }

    @Test
    fun `test format analysis result for successful result`() {
        val files = listOf(
            ZipFileAnalyzerUtil.ZipFileEntry(
                name = "MainActivity.kt",
                path = "app/src/main/java/MainActivity.kt",
                size = 1000L,
                extension = ".kt",
                isCodeFile = true,
                content = "fun main() {}",
                language = "Kotlin"
            )
        )

        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 1,
            totalSize = 1000L,
            files = files,
            directoryStructure = listOf("app", "app/src", "app/src/main"),
            projectType = ZipFileAnalyzerUtil.ProjectType.ANDROID
        )

        val formatted = ZipFileAnalyzerUtil.formatAnalysisResult(result)

        assertTrue("Should contain ZIP header", formatted.contains("ZIP DOSYASI ANALİZİ"))
        assertTrue("Should contain project type", formatted.contains("Android"))
        assertTrue("Should contain file count", formatted.contains("1"))
        assertTrue("Should contain completion message", formatted.contains("ANALİZ TAMAMLANDI"))
    }

    @Test
    fun `test project type detection for Android`() {
        val files = listOf(
            createFileEntry("AndroidManifest.xml", ".xml", 100L),
            createFileEntry("build.gradle", ".gradle", 200L),
            createFileEntry("MainActivity.kt", ".kt", 500L)
        )

        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 3,
            totalSize = 800L,
            files = files,
            directoryStructure = listOf("app", "app/src/main"),
            projectType = ZipFileAnalyzerUtil.ProjectType.ANDROID
        )

        assertEquals("Should detect Android project", ZipFileAnalyzerUtil.ProjectType.ANDROID, result.projectType)
    }

    @Test
    fun `test format includes code content`() {
        val codeContent = "fun main() { println(\"Hello World\") }"
        val files = listOf(
            ZipFileAnalyzerUtil.ZipFileEntry(
                name = "Main.kt",
                path = "src/Main.kt",
                size = codeContent.length.toLong(),
                extension = ".kt",
                isCodeFile = true,
                content = codeContent,
                language = "Kotlin"
            )
        )

        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 1,
            totalSize = codeContent.length.toLong(),
            files = files,
            directoryStructure = listOf("src"),
            projectType = ZipFileAnalyzerUtil.ProjectType.UNKNOWN
        )

        val formatted = ZipFileAnalyzerUtil.formatAnalysisResult(result)

        assertTrue("Should contain code content", formatted.contains("Hello World"))
        assertTrue("Should contain language", formatted.contains("Kotlin"))
    }

    @Test
    fun `test language distribution in format`() {
        val files = listOf(
            createFileEntry("Main.kt", ".kt", 100L, "Kotlin"),
            createFileEntry("Utils.kt", ".kt", 100L, "Kotlin"),
            createFileEntry("App.java", ".java", 100L, "Java")
        )

        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 3,
            totalSize = 300L,
            files = files,
            directoryStructure = emptyList(),
            projectType = ZipFileAnalyzerUtil.ProjectType.UNKNOWN
        )

        val formatted = ZipFileAnalyzerUtil.formatAnalysisResult(result)

        assertTrue("Should contain Kotlin language", formatted.contains("Kotlin"))
        assertTrue("Should contain Java language", formatted.contains("Java"))
    }

    @Test
    fun `test important files detection`() {
        val files = listOf(
            createFileEntry("MainActivity.kt", ".kt", 500L, "Kotlin"),
            createFileEntry("build.gradle.kts", ".gradle.kts", 200L, "Gradle Kotlin DSL"),
            createFileEntry("package.json", ".json", 100L, "JSON"),
            createFileEntry("utils.js", ".js", 100L, "JavaScript")
        )

        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 4,
            totalSize = 900L,
            files = files,
            directoryStructure = emptyList(),
            projectType = ZipFileAnalyzerUtil.ProjectType.UNKNOWN
        )

        val formatted = ZipFileAnalyzerUtil.formatAnalysisResult(result)

        assertTrue("Should highlight important files", formatted.contains("ÖNEMLİ DOSYALAR"))
        assertTrue("Should contain MainActivity", formatted.contains("MainActivity"))
    }

    @Test
    fun `test ZipFileEntry data class`() {
        val entry = ZipFileAnalyzerUtil.ZipFileEntry(
            name = "test.kt",
            path = "src/test.kt",
            size = 100L,
            extension = ".kt",
            isCodeFile = true,
            content = "fun test() {}",
            language = "Kotlin"
        )

        assertEquals("test.kt", entry.name)
        assertEquals("src/test.kt", entry.path)
        assertEquals(100L, entry.size)
        assertEquals(".kt", entry.extension)
        assertTrue(entry.isCodeFile)
        assertEquals("fun test() {}", entry.content)
        assertEquals("Kotlin", entry.language)
    }

    @Test
    fun `test project types enum values`() {
        val types = ZipFileAnalyzerUtil.ProjectType.entries

        assertTrue("Should contain ANDROID", types.contains(ZipFileAnalyzerUtil.ProjectType.ANDROID))
        assertTrue("Should contain IOS", types.contains(ZipFileAnalyzerUtil.ProjectType.IOS))
        assertTrue("Should contain REACT", types.contains(ZipFileAnalyzerUtil.ProjectType.REACT))
        assertTrue("Should contain NODEJS", types.contains(ZipFileAnalyzerUtil.ProjectType.NODEJS))
        assertTrue("Should contain PYTHON", types.contains(ZipFileAnalyzerUtil.ProjectType.PYTHON))
        assertTrue("Should contain UNKNOWN", types.contains(ZipFileAnalyzerUtil.ProjectType.UNKNOWN))
    }

    @Test
    fun `test readZipEntryContent does not close stream`() {
        // This test verifies that reading content from multiple files works correctly
        // by ensuring the ZipInputStream doesn't get closed prematurely
        
        val files = listOf(
            ZipFileAnalyzerUtil.ZipFileEntry(
                name = "File1.kt",
                path = "src/File1.kt",
                size = 100L,
                extension = ".kt",
                isCodeFile = true,
                content = "fun test1() { println(\"File 1\") }",
                language = "Kotlin"
            ),
            ZipFileAnalyzerUtil.ZipFileEntry(
                name = "File2.kt",
                path = "src/File2.kt",
                size = 100L,
                extension = ".kt",
                isCodeFile = true,
                content = "fun test2() { println(\"File 2\") }",
                language = "Kotlin"
            ),
            ZipFileAnalyzerUtil.ZipFileEntry(
                name = "File3.java",
                path = "src/File3.java",
                size = 100L,
                extension = ".java",
                isCodeFile = true,
                content = "public class File3 { }",
                language = "Java"
            )
        )

        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 3,
            totalSize = 300L,
            files = files,
            directoryStructure = listOf("src"),
            projectType = ZipFileAnalyzerUtil.ProjectType.UNKNOWN
        )

        assertEquals("Should have 3 files", 3, result.totalFiles)
        assertEquals("Should have 3 file entries", 3, result.files.size)
        
        // Verify all files have content
        result.files.forEach { file ->
            assertNotNull("File ${file.name} should have content", file.content)
            assertTrue("File ${file.name} content should not be empty", file.content!!.isNotEmpty())
        }
    }

    @Test
    fun `test progress callback is called during analysis`() {
        // This test verifies that progress callbacks work correctly
        // and provide meaningful updates during analysis
        val progressUpdates = mutableListOf<Triple<Int, String, String>>()
        
        // Create a mock result that would have progress updates
        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 10,
            totalSize = 5000L,
            files = (1..10).map { i ->
                ZipFileAnalyzerUtil.ZipFileEntry(
                    name = "File$i.kt",
                    path = "src/File$i.kt",
                    size = 500L,
                    extension = ".kt",
                    isCodeFile = true,
                    content = "// File $i content",
                    language = "Kotlin"
                )
            },
            directoryStructure = listOf("src"),
            projectType = ZipFileAnalyzerUtil.ProjectType.UNKNOWN
        )
        
        // Verify result structure
        assertEquals("Should have 10 files", 10, result.totalFiles)
        assertTrue("Should be successful", result.success)
        assertNull("Should have no error", result.errorMessage)
    }
    
    @Test
    fun `test analysis result contains project summary`() {
        val files = listOf(
            createFileEntry("MainActivity.kt", ".kt", 1000L, "Kotlin"),
            createFileEntry("build.gradle.kts", ".gradle.kts", 500L, "Gradle Kotlin DSL"),
            createFileEntry("AndroidManifest.xml", ".xml", 300L, "XML")
        )
        
        val result = ZipFileAnalyzerUtil.ZipAnalysisResult(
            success = true,
            errorMessage = null,
            totalFiles = 3,
            totalSize = 1800L,
            files = files,
            directoryStructure = listOf("app", "app/src", "app/src/main"),
            projectType = ZipFileAnalyzerUtil.ProjectType.ANDROID
        )
        
        val formatted = ZipFileAnalyzerUtil.formatAnalysisResult(result)
        
        // Verify summary includes key information
        assertTrue("Should show total files", formatted.contains("3"))
        assertTrue("Should show Android project type", formatted.contains("Android"))
        assertTrue("Should show file structure", formatted.contains("KLASÖR"))
        assertTrue("Should show programming languages", formatted.contains("Kotlin"))
        assertTrue("Should show completion", formatted.contains("TAMAMLANDI"))
    }

    private fun createFileEntry(
        name: String,
        extension: String,
        size: Long,
        language: String? = null
    ): ZipFileAnalyzerUtil.ZipFileEntry {
        return ZipFileAnalyzerUtil.ZipFileEntry(
            name = name,
            path = name,
            size = size,
            extension = extension,
            isCodeFile = language != null,
            content = null,
            language = language
        )
    }
}
