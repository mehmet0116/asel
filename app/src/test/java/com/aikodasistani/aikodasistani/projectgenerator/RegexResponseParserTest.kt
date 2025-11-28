package com.aikodasistani.aikodasistani.projectgenerator

import com.aikodasistani.aikodasistani.projectgenerator.data.RegexResponseParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RegexResponseParser.
 * Tests the strict >>> FILE: format parsing.
 */
class RegexResponseParserTest {

    @Before
    fun setUp() {
        // Set a no-op logger to avoid Android Log issues in tests
        RegexResponseParser.setLogger(object : RegexResponseParser.Logger {
            override fun d(tag: String, message: String) {
                // No-op for tests
            }
        })
    }

    @Test
    fun `test parse empty input returns error`() {
        val result = RegexResponseParser.parse("")
        assertTrue("Empty input should return error", result is RegexResponseParser.ParseResult.Error)
        val error = result as RegexResponseParser.ParseResult.Error
        assertTrue("Error should mention empty", error.message.contains("Empty"))
    }

    @Test
    fun `test parse blank input returns error`() {
        val result = RegexResponseParser.parse("   \n\t  ")
        assertTrue("Blank input should return error", result is RegexResponseParser.ParseResult.Error)
    }

    @Test
    fun `test parse input without file markers returns error`() {
        val input = """
            This is just some text without any file markers.
            It should fail to parse.
        """.trimIndent()
        
        val result = RegexResponseParser.parse(input)
        assertTrue("Input without markers should return error", result is RegexResponseParser.ParseResult.Error)
        val error = result as RegexResponseParser.ParseResult.Error
        assertTrue("Error should mention no markers", error.message.contains("No file markers"))
    }

    @Test
    fun `test parse single file`() {
        val input = """
>>> FILE: main.kt
fun main() {
    println("Hello World")
}
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        assertEquals("Project root should match", "TestProject", structure.root)
        assertEquals("Should have 1 file", 1, structure.files.size)
        assertEquals("File path should be correct", "main.kt", structure.files[0].path)
        assertTrue("Content should contain println", structure.files[0].content.contains("println"))
    }

    @Test
    fun `test parse multiple files`() {
        val input = """
>>> FILE: src/main.kt
fun main() {
    println("Hello")
}
>>> FILE: src/utils/Helper.kt
object Helper {
    fun help() {}
}
>>> FILE: README.md
# My Project
Description here
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        assertEquals("Should have 3 files", 3, structure.files.size)
        assertTrue("Should have main.kt", structure.files.any { it.path == "src/main.kt" })
        assertTrue("Should have Helper.kt", structure.files.any { it.path == "src/utils/Helper.kt" })
        assertTrue("Should have README.md", structure.files.any { it.path == "README.md" })
    }

    @Test
    fun `test parse preserves multiline content`() {
        val input = """
>>> FILE: script.py
def hello():
    print("Hello")
    print("World")

def goodbye():
    print("Bye")
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        val script = structure.files.find { it.filename == "script.py" }
        assertNotNull("Should find script.py", script)
        assertTrue("Content should contain both functions",
            script!!.content.contains("def hello") && script.content.contains("def goodbye"))
    }

    @Test
    fun `test parse handles Android project structure`() {
        val input = """
>>> FILE: settings.gradle.kts
rootProject.name = "MyApp"
include(":app")
>>> FILE: build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
}
>>> FILE: app/build.gradle.kts
plugins {
    id("com.android.application")
}
android {
    namespace = "com.example.myapp"
}
>>> FILE: app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="MyApp"/>
</manifest>
>>> FILE: app/src/main/java/com/example/myapp/MainActivity.kt
package com.example.myapp
class MainActivity : Activity()
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "AndroidProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        assertEquals("Should have 5 files", 5, structure.files.size)
        assertTrue("Should have settings.gradle.kts", structure.files.any { it.filename == "settings.gradle.kts" })
        assertTrue("Should have AndroidManifest.xml", structure.files.any { it.filename == "AndroidManifest.xml" })
        assertTrue("Should have MainActivity.kt", structure.files.any { it.filename == "MainActivity.kt" })
    }

    @Test
    fun `test parse rejects path traversal`() {
        val input = """
>>> FILE: ../../../etc/passwd
malicious content
>>> FILE: normal.txt
safe content
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        // Should only have the safe file
        assertEquals("Should have 1 file (path traversal rejected)", 1, structure.files.size)
        assertEquals("Should only have normal.txt", "normal.txt", structure.files[0].path)
    }

    @Test
    fun `test parse rejects absolute paths`() {
        val input = """
>>> FILE: /etc/passwd
dangerous
>>> FILE: C:\Windows\System32\config
also dangerous
>>> FILE: src/safe.kt
safe content
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        assertEquals("Should have 1 file", 1, structure.files.size)
        assertEquals("Should only have safe.kt", "src/safe.kt", structure.files[0].path)
    }

    @Test
    fun `test parse detects duplicate paths`() {
        val input = """
>>> FILE: main.kt
content 1
>>> FILE: main.kt
content 2
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Duplicate paths should return error", result is RegexResponseParser.ParseResult.Error)
        val error = result as RegexResponseParser.ParseResult.Error
        assertTrue("Error should mention duplicate", error.message.contains("Duplicate"))
    }

    @Test
    fun `test parse skips empty files`() {
        val input = """
>>> FILE: empty.txt

>>> FILE: notempty.txt
has content
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        assertEquals("Should have 1 file (empty skipped)", 1, structure.files.size)
        assertEquals("Should only have notempty.txt", "notempty.txt", structure.files[0].path)
    }

    @Test
    fun `test parse handles special characters in content`() {
        val input = """
>>> FILE: test.json
{"message": "Hello, \"World\"!", "symbols": "<>&", "unicode": "日本語"}
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        val content = structure.files[0].content
        assertTrue("Should preserve quotes", content.contains("\\\""))
        assertTrue("Should preserve special chars", content.contains("<>&"))
        assertTrue("Should preserve unicode", content.contains("日本語"))
    }

    @Test
    fun `test parse with extra whitespace in marker`() {
        val input = """
>>> FILE:   src/main.kt  
fun main() {}
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        assertEquals("Path should be trimmed", "src/main.kt", structure.files[0].path)
    }

    @Test
    fun `test validatePath rejects various invalid paths`() {
        assertFalse("Should reject ..", RegexResponseParser.validatePath("../secret.txt"))
        assertFalse("Should reject leading slash", RegexResponseParser.validatePath("/etc/passwd"))
        assertFalse("Should reject Windows drive", RegexResponseParser.validatePath("C:\\Windows\\test.txt"))
        assertFalse("Should reject dotfile", RegexResponseParser.validatePath(".gitignore")) // no extension after dot
        assertFalse("Should reject no extension", RegexResponseParser.validatePath("noextension"))
        assertFalse("Should reject blank", RegexResponseParser.validatePath(""))
    }

    @Test
    fun `test validatePath accepts valid paths`() {
        assertTrue("Should accept simple filename", RegexResponseParser.validatePath("main.kt"))
        assertTrue("Should accept path with folder", RegexResponseParser.validatePath("src/main.kt"))
        assertTrue("Should accept nested path", RegexResponseParser.validatePath("src/main/java/App.kt"))
        assertTrue("Should accept multiple dots", RegexResponseParser.validatePath("config.min.js"))
    }

    @Test
    fun `test metadata is correctly populated`() {
        val input = """
>>> FILE: file1.txt
Content 1
>>> FILE: file2.txt
Content 2
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val structure = (result as RegexResponseParser.ParseResult.Success).structure

        assertEquals("File count should be 2", 2, structure.metadata.totalFiles)
        assertTrue("Total size should be positive", structure.metadata.totalSize > 0)
    }

    @Test
    fun `test ProjectFile properties are extracted correctly`() {
        val input = """
>>> FILE: src/main/java/com/example/App.kt
class App
        """.trimIndent()

        val result = RegexResponseParser.parse(input, "TestProject")

        assertTrue("Should parse successfully", result is RegexResponseParser.ParseResult.Success)
        val file = (result as RegexResponseParser.ParseResult.Success).structure.files[0]

        assertEquals("Directory should be extracted", "src/main/java/com/example", file.directory)
        assertEquals("Filename should be extracted", "App.kt", file.filename)
        assertEquals("Extension should be extracted", "kt", file.extension)
    }
}
