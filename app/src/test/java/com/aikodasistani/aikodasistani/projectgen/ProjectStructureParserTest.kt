package com.aikodasistani.aikodasistani.projectgen

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests for ProjectStructureParser
 * 
 * Tests the parser's ability to handle:
 * - Basic file/folder structures
 * - Nested directories
 * - Multiple file types
 * - Edge cases and error conditions
 */
class ProjectStructureParserTest {

    private lateinit var parser: ProjectStructureParser

    @Before
    fun setUp() {
        parser = ProjectStructureParser()
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test parse empty input returns error`() {
        val result = parser.parse("", "TestProject")
        assertTrue("Empty input should return error", result is ParserResult.Error)
    }

    @Test
    fun `test parse blank input returns error`() {
        val result = parser.parse("   \n\n   ", "TestProject")
        assertTrue("Blank input should return error", result is ParserResult.Error)
    }

    @Test
    fun `test parse simple file structure`() {
        val input = """
            /TestProject/
            main.kt:
                fun main() {
                    println("Hello")
                }
        """.trimIndent()

        val result = parser.parse(input, "TestProject")
        
        assertTrue("Should parse successfully", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        assertEquals("TestProject", structure.root)
        assertTrue("Should have at least one file", structure.files.isNotEmpty())
    }

    @Test
    fun `test parse multiple files`() {
        val input = """
            /MyProject/
            /src/
            Main.kt:
                package com.example
                
                fun main() {}
            
            /resources/
            config.json:
                {
                    "name": "test"
                }
        """.trimIndent()

        val result = parser.parse(input, "MyProject")
        
        assertTrue("Should parse successfully", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        assertTrue("Should have multiple files", structure.files.size >= 2)
    }

    // ==================== Code Block Parsing Tests ====================

    @Test
    fun `test parse markdown code blocks with filename`() {
        val input = """
            Here's the project structure:
            
            ```kotlin Main.kt
            fun main() {
                println("Hello World")
            }
            ```
            
            ```json config.json
            {
                "version": "1.0"
            }
            ```
        """.trimIndent()

        val result = parser.parse(input, "TestProject")
        
        assertTrue("Should parse code blocks", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        assertTrue("Should extract files from code blocks", structure.files.isNotEmpty())
    }

    @Test
    fun `test parse code blocks without explicit filename`() {
        val input = """
            Here's some code:
            
            ```kotlin
            fun hello() {
                println("Hello")
            }
            ```
        """.trimIndent()

        val result = parser.parse(input, "TestProject")
        
        assertTrue("Should parse unnamed code blocks", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        assertTrue("Should create file from unnamed code block", structure.files.isNotEmpty())
    }

    // ==================== Nested Directory Tests ====================

    @Test
    fun `test parse deeply nested directories`() {
        val input = """
            /MyApp/
            /src/main/kotlin/com/example/
            Application.kt:
                package com.example
                
                class Application
        """.trimIndent()

        val result = parser.parse(input, "MyApp")
        
        assertTrue("Should parse nested directories", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        val hasNestedPath = structure.files.any { 
            it.path.contains("src") && it.path.contains("main") 
        }
        assertTrue("Should preserve nested path structure", hasNestedPath)
    }

    // ==================== Content Preservation Tests ====================

    @Test
    fun `test preserves whitespace in content`() {
        val input = """
            /Project/
            script.py:
                def hello():
                    if True:
                        print("Hello")
                    return
        """.trimIndent()

        val result = parser.parse(input, "Project")
        
        assertTrue("Should parse successfully", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        val pythonFile = structure.files.find { it.path.endsWith(".py") }
        assertNotNull("Should find Python file", pythonFile)
        assertTrue("Should preserve indentation", 
            pythonFile!!.content.contains("    ") || pythonFile.content.contains("\t"))
    }

    // ==================== File Extension Tests ====================

    @Test
    fun `test handles various file extensions`() {
        val extensions = listOf(
            "file.kt", "file.java", "file.py", "file.js", 
            "file.ts", "file.json", "file.yaml", "file.xml",
            "file.html", "file.css", "file.md", "file.gradle"
        )

        for (ext in extensions) {
            val input = """
                /Project/
                $ext:
                    content for $ext
            """.trimIndent()

            val result = parser.parse(input, "Project")
            assertTrue("Should handle $ext", result is ParserResult.Success)
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test handles special characters in content`() {
        val input = """
            /Project/
            special.txt:
                Special chars: äöü ñ 中文 🚀 <>&"'
        """.trimIndent()

        val result = parser.parse(input, "Project")
        
        assertTrue("Should handle special characters", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        val file = structure.files.firstOrNull()
        assertNotNull("Should have file", file)
        assertTrue("Should preserve special chars", 
            file!!.content.contains("🚀") || file.content.contains("äöü"))
    }

    @Test
    fun `test sanitizes project name`() {
        val result = parser.parse("/test/\nfile.txt:\n    content", "My<Project>Name")
        
        assertTrue("Should parse with sanitized name", result is ParserResult.Success)
        val structure = (result as ParserResult.Success).structure
        
        assertFalse("Should not contain < in root", structure.root.contains("<"))
        assertFalse("Should not contain > in root", structure.root.contains(">"))
    }

    @Test
    fun `test handles large content`() {
        val largeContent = "x".repeat(100000)
        val input = """
            /Project/
            large.txt:
                $largeContent
        """.trimIndent()

        val result = parser.parse(input, "Project")
        
        assertTrue("Should handle large content", result is ParserResult.Success)
    }

    // ==================== ProjectStructure/ProjectFile Tests ====================

    @Test
    fun `test ProjectFile extension property`() {
        val file = ProjectFile("project/src/Main.kt", "content")
        assertEquals("kt", file.extension)
    }

    @Test
    fun `test ProjectFile fileName property`() {
        val file = ProjectFile("project/src/Main.kt", "content")
        assertEquals("Main.kt", file.fileName)
    }

    @Test
    fun `test ProjectFile directory property`() {
        val file = ProjectFile("project/src/Main.kt", "content")
        assertEquals("project/src", file.directory)
    }

    @Test
    fun `test ProjectStructure totalSize`() {
        val structure = ProjectStructure(
            root = "test",
            files = listOf(
                ProjectFile("test/a.txt", "12345"),
                ProjectFile("test/b.txt", "67890")
            )
        )
        assertEquals(10, structure.totalSize)
    }

    @Test
    fun `test ProjectStructure totalFiles`() {
        val structure = ProjectStructure(
            root = "test",
            files = listOf(
                ProjectFile("test/a.txt", "a"),
                ProjectFile("test/b.txt", "b"),
                ProjectFile("test/c.txt", "c")
            )
        )
        assertEquals(3, structure.totalFiles)
    }
}
