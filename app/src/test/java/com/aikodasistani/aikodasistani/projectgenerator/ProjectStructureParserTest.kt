package com.aikodasistani.aikodasistani.projectgenerator

import com.aikodasistani.aikodasistani.projectgenerator.data.ProjectStructureParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ProjectStructureParser.
 * Tests parsing of AI output into ProjectStructure.
 */
class ProjectStructureParserTest {

    @Test
    fun `test parse empty input returns error`() {
        val result = ProjectStructureParser.parse("")
        assertTrue("Empty input should return error", result is ProjectStructureParser.ParseResult.Error)
    }

    @Test
    fun `test parse blank input returns error`() {
        val result = ProjectStructureParser.parse("   \n\t  ")
        assertTrue("Blank input should return error", result is ProjectStructureParser.ParseResult.Error)
    }

    @Test
    fun `test parse simple file structure`() {
        val input = """
            main.kt:
                fun main() {
                    println("Hello")
                }
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        assertEquals("Project root should match", "TestProject", structure.root)
        assertEquals("Should have 1 file", 1, structure.files.size)
        assertEquals("File path should be correct", "main.kt", structure.files[0].path)
        assertTrue("Content should contain println", structure.files[0].content.contains("println"))
    }

    @Test
    fun `test parse nested folder structure`() {
        val input = """
            /src/
            main.kt:
                fun main() {}
            
            /src/utils/
            helper.kt:
                object Helper {}
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        assertEquals("Should have 2 files", 2, structure.files.size)
        
        val mainFile = structure.files.find { it.filename == "main.kt" }
        assertNotNull("Should find main.kt", mainFile)
        assertEquals("main.kt path should include folder", "src/main.kt", mainFile?.path)
        
        val helperFile = structure.files.find { it.filename == "helper.kt" }
        assertNotNull("Should find helper.kt", helperFile)
        assertEquals("helper.kt path should include nested folder", "src/utils/helper.kt", helperFile?.path)
    }

    @Test
    fun `test parse multiple file types`() {
        val input = """
            app.kt:
                class App
            
            config.json:
                {"name": "test"}
            
            README.md:
                # Project
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        assertEquals("Should have 3 files", 3, structure.files.size)
        
        assertTrue("Should have .kt file", structure.files.any { it.extension == "kt" })
        assertTrue("Should have .json file", structure.files.any { it.extension == "json" })
        assertTrue("Should have .md file", structure.files.any { it.extension == "md" })
    }

    @Test
    fun `test parse preserves multiline content`() {
        val input = """
            script.py:
                def hello():
                    print("Hello")
                    print("World")
                
                def goodbye():
                    print("Bye")
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        val script = structure.files.find { it.filename == "script.py" }
        assertNotNull("Should find script.py", script)
        assertTrue("Content should contain both functions", 
            script!!.content.contains("def hello") && script.content.contains("def goodbye"))
    }

    @Test
    fun `test parse code block format`() {
        val input = """
            ```main.kt
            fun main() {
                println("Hello")
            }
            ```
            
            ```config.json
            {"key": "value"}
            ```
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse code block format", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        assertTrue("Should have at least 1 file", structure.files.isNotEmpty())
    }

    @Test
    fun `test parse handles complex Android project structure`() {
        val input = """
            /app/
            build.gradle.kts:
                plugins {
                    id("com.android.application")
                }
            
            /app/src/main/
            AndroidManifest.xml:
                <manifest package="com.example"/>
            
            /app/src/main/java/com/example/
            MainActivity.kt:
                class MainActivity : Activity()
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "AndroidProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        assertEquals("Should have 3 files", 3, structure.files.size)
        assertTrue("Should have build.gradle.kts", structure.files.any { it.filename == "build.gradle.kts" })
        assertTrue("Should have AndroidManifest.xml", structure.files.any { it.filename == "AndroidManifest.xml" })
        assertTrue("Should have MainActivity.kt", structure.files.any { it.filename == "MainActivity.kt" })
    }

    @Test
    fun `test validatePath rejects path traversal`() {
        assertFalse("Should reject ..", ProjectStructureParser.validatePath("../secret.txt"))
        assertFalse("Should reject leading slash", ProjectStructureParser.validatePath("/etc/passwd"))
    }

    @Test
    fun `test validatePath accepts valid paths`() {
        assertTrue("Should accept simple filename", ProjectStructureParser.validatePath("main.kt"))
        assertTrue("Should accept path with folder", ProjectStructureParser.validatePath("src/main.kt"))
        assertTrue("Should accept nested path", ProjectStructureParser.validatePath("src/main/java/App.kt"))
    }

    @Test
    fun `test parse removes common indentation`() {
        val input = """
            main.kt:
                    fun main() {
                        println("Hello")
                    }
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        val content = structure.files[0].content
        // Content should start with 'fun' not with spaces
        assertTrue("Content should start without excessive indentation", 
            content.trimStart().startsWith("fun"))
    }

    @Test
    fun `test metadata is correctly populated`() {
        val input = """
            file1.txt:
                Content 1
            
            file2.txt:
                Content 2
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        assertEquals("File count should be 2", 2, structure.metadata.totalFiles)
        assertTrue("Total size should be positive", structure.metadata.totalSize > 0)
    }

    @Test
    fun `test parse handles special characters in content`() {
        val input = """
            test.json:
                {"message": "Hello, \"World\"!", "symbols": "<>&"}
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val structure = (result as ProjectStructureParser.ParseResult.Success).structure
        
        val content = structure.files[0].content
        assertTrue("Should preserve quotes", content.contains("\""))
        assertTrue("Should preserve special chars", content.contains("<>&"))
    }

    @Test
    fun `test ProjectFile properties`() {
        val input = """
            /src/main/java/
            App.kt:
                class App
        """.trimIndent()
        
        val result = ProjectStructureParser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ProjectStructureParser.ParseResult.Success)
        val file = (result as ProjectStructureParser.ParseResult.Success).structure.files[0]
        
        assertEquals("Directory should be extracted", "src/main/java", file.directory)
        assertEquals("Filename should be extracted", "App.kt", file.filename)
        assertEquals("Extension should be extracted", "kt", file.extension)
    }
}
