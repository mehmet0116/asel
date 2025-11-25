package com.aikodasistani.aikodasistani.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CodeAutoCompletionUtil
 */
class CodeAutoCompletionUtilTest {

    @Test
    fun `test Kotlin missing closing bracket detection`() {
        val code = """
            fun main() {
                println("Hello")
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "kotlin")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect missing bracket", 
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.MISSING_BRACKET })
    }

    @Test
    fun `test Kotlin correct code has no errors`() {
        val code = """
            fun main() {
                println("Hello")
            }
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "kotlin")

        assertFalse("Should not detect errors in correct code", result.hasErrors)
    }

    @Test
    fun `test Java missing semicolon detection`() {
        val code = """
            public class Main {
                public static void main(String[] args) {
                    int x = 5
                }
            }
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "java")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect missing semicolon",
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.MISSING_SEMICOLON })
    }

    @Test
    fun `test Python missing colon detection`() {
        val code = """
            def hello()
                print("Hello")
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "python")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect missing colon",
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.MISSING_COLON })
    }

    @Test
    fun `test Python correct code has no errors`() {
        val code = """
            def hello():
                print("Hello")
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "python")

        assertFalse("Should not detect errors in correct code", result.hasErrors)
    }

    @Test
    fun `test unclosed string detection`() {
        val code = """
            val message = "Hello world
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "kotlin")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect unclosed string",
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.UNCLOSED_STRING })
    }

    @Test
    fun `test JSON trailing comma detection`() {
        val code = """
            {
                "name": "test",
                "value": 123,
            }
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "json")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect syntax error (trailing comma)",
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.SYNTAX_ERROR })
    }

    @Test
    fun `test HTML unclosed tag detection`() {
        val code = """
            <html>
            <body>
            <div>Hello
            </body>
            </html>
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "html")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect unclosed tag",
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.MISSING_BRACKET })
    }

    @Test
    fun `test auto fix missing brackets`() {
        val code = """
            fun main() {
                println("Hello")
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "kotlin")

        assertNotNull("Should provide fixed code", result.fixedCode)
        assertTrue("Fixed code should contain closing bracket", result.fixedCode!!.contains("}"))
    }

    @Test
    fun `test auto fix missing parentheses`() {
        val code = """
            println("Hello"
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "kotlin")

        assertNotNull("Should provide fixed code", result.fixedCode)
        assertTrue("Fixed code should contain closing parenthesis", result.fixedCode!!.contains(")"))
    }

    @Test
    fun `test error summary generation`() {
        val code = """
            fun main() {
                println("Hello")
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "kotlin")
        val summary = CodeAutoCompletionUtil.generateErrorSummary(result)

        assertTrue("Summary should contain warning", summary.contains("⚠️"))
        assertTrue("Summary should mention errors", summary.contains("hata"))
    }

    @Test
    fun `test no error summary for correct code`() {
        val code = """
            fun main() {
                println("Hello")
            }
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "kotlin")
        val summary = CodeAutoCompletionUtil.generateErrorSummary(result)

        assertTrue("Summary should indicate no errors", summary.contains("✅"))
    }

    @Test
    fun `test shouldSuggestAutoFix returns true for fixable errors`() {
        val code = """
            fun main() {
                println("Hello")
        """.trimIndent()

        val shouldSuggest = CodeAutoCompletionUtil.shouldSuggestAutoFix(code)

        assertTrue("Should suggest auto fix for fixable errors", shouldSuggest)
    }

    @Test
    fun `test shouldSuggestAutoFix returns false for correct code`() {
        val code = """
            fun main() {
                println("Hello")
            }
        """.trimIndent()

        val shouldSuggest = CodeAutoCompletionUtil.shouldSuggestAutoFix(code)

        assertFalse("Should not suggest auto fix for correct code", shouldSuggest)
    }

    @Test
    fun `test JavaScript missing bracket detection`() {
        val code = """
            function hello() {
                console.log("Hello")
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "javascript")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect missing bracket",
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.MISSING_BRACKET })
    }

    @Test
    fun `test template literal detection`() {
        val code = """
            const message = `Hello world
        """.trimIndent()

        val result = CodeAutoCompletionUtil.analyzeCode(code, "javascript")

        assertTrue("Should detect errors", result.hasErrors)
        assertTrue("Should detect unclosed template literal",
            result.errors.any { it.errorType == CodeAutoCompletionUtil.ErrorType.UNCLOSED_STRING })
    }
}
