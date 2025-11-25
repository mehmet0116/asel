package com.aikodasistani.aikodasistani.util

/**
 * Utility class for detecting and fixing common code errors in AI-generated code.
 * Helps with auto-completion when Codex or other AI models produce code with errors.
 */
object CodeAutoCompletionUtil {

    /**
     * Result of code analysis containing detected errors and suggested fixes
     */
    data class CodeAnalysisResult(
        val hasErrors: Boolean,
        val errors: List<CodeError>,
        val fixedCode: String?,
        val language: String?
    )

    /**
     * Represents a code error with its description and fix
     */
    data class CodeError(
        val errorType: ErrorType,
        val description: String,
        val lineNumber: Int?,
        val suggestedFix: String?
    )

    /**
     * Types of common code errors
     */
    enum class ErrorType {
        MISSING_SEMICOLON,
        MISSING_BRACKET,
        MISSING_PARENTHESIS,
        MISSING_QUOTE,
        UNCLOSED_STRING,
        MISSING_IMPORT,
        SYNTAX_ERROR,
        INDENTATION_ERROR,
        MISSING_COLON,
        MISSING_RETURN_TYPE,
        INCOMPLETE_FUNCTION,
        INCOMPLETE_CLASS
    }

    /**
     * Analyzes code content and detects common errors
     */
    fun analyzeCode(code: String, language: String? = null): CodeAnalysisResult {
        val detectedLanguage = language ?: CodeDetectionUtil.detectLanguageAndCode(code).first
        val errors = mutableListOf<CodeError>()
        
        when (detectedLanguage?.lowercase()) {
            "kotlin" -> errors.addAll(analyzeKotlinCode(code))
            "java" -> errors.addAll(analyzeJavaCode(code))
            "python" -> errors.addAll(analyzePythonCode(code))
            "javascript" -> errors.addAll(analyzeJavaScriptCode(code))
            "html" -> errors.addAll(analyzeHtmlCode(code))
            "json" -> errors.addAll(analyzeJsonCode(code))
            "xml" -> errors.addAll(analyzeXmlCode(code))
            else -> errors.addAll(analyzeGenericCode(code))
        }

        val fixedCode = if (errors.isNotEmpty()) {
            autoFixCode(code, errors, detectedLanguage)
        } else null

        return CodeAnalysisResult(
            hasErrors = errors.isNotEmpty(),
            errors = errors,
            fixedCode = fixedCode,
            language = detectedLanguage
        )
    }

    /**
     * Attempts to automatically fix detected errors in the code
     */
    fun autoFixCode(code: String, errors: List<CodeError>, language: String?): String {
        var fixedCode = code

        errors.forEach { error ->
            when (error.errorType) {
                ErrorType.MISSING_SEMICOLON -> {
                    fixedCode = fixMissingSemicolons(fixedCode, language)
                }
                ErrorType.MISSING_BRACKET -> {
                    fixedCode = fixMissingBrackets(fixedCode)
                }
                ErrorType.MISSING_PARENTHESIS -> {
                    fixedCode = fixMissingParentheses(fixedCode)
                }
                ErrorType.UNCLOSED_STRING -> {
                    fixedCode = fixUnclosedStrings(fixedCode)
                }
                ErrorType.MISSING_COLON -> {
                    fixedCode = fixMissingColons(fixedCode, language)
                }
                else -> {
                    // For other error types, use the suggested fix if available
                    error.suggestedFix?.let {
                        // Apply the fix based on line number if available
                    }
                }
            }
        }

        return fixedCode
    }

    /**
     * Analyzes Kotlin code for common errors
     */
    private fun analyzeKotlinCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()

        // Check for unmatched brackets
        val bracketBalance = checkBracketBalance(code)
        if (bracketBalance.curlyBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.curlyBalance > 0) 
                    "Eksik kapatma parantezi '}' - ${bracketBalance.curlyBalance} adet"
                else 
                    "Fazla kapatma parantezi '}' - ${-bracketBalance.curlyBalance} adet",
                lineNumber = null,
                suggestedFix = if (bracketBalance.curlyBalance > 0) "}".repeat(bracketBalance.curlyBalance) else null
            ))
        }

        if (bracketBalance.parenBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_PARENTHESIS,
                description = if (bracketBalance.parenBalance > 0)
                    "Eksik kapatma parantezi ')' - ${bracketBalance.parenBalance} adet"
                else
                    "Fazla kapatma parantezi ')' - ${-bracketBalance.parenBalance} adet",
                lineNumber = null,
                suggestedFix = if (bracketBalance.parenBalance > 0) ")".repeat(bracketBalance.parenBalance) else null
            ))
        }

        // Check for unclosed strings
        lines.forEachIndexed { index, line ->
            if (hasUnclosedString(line)) {
                errors.add(CodeError(
                    errorType = ErrorType.UNCLOSED_STRING,
                    description = "Kapatılmamış string ifadesi",
                    lineNumber = index + 1,
                    suggestedFix = "\""
                ))
            }
        }

        // Check for incomplete function definitions
        val funcPattern = Regex("""fun\s+\w+\s*\([^)]*\)\s*(?::\s*\w+)?\s*$""")
        lines.forEachIndexed { index, line ->
            if (funcPattern.containsMatchIn(line.trim()) && 
                !line.trim().endsWith("{") && 
                (index + 1 >= lines.size || !lines[index + 1].trim().startsWith("{"))) {
                errors.add(CodeError(
                    errorType = ErrorType.INCOMPLETE_FUNCTION,
                    description = "Eksik fonksiyon gövdesi",
                    lineNumber = index + 1,
                    suggestedFix = " { }"
                ))
            }
        }

        return errors
    }

    /**
     * Analyzes Java code for common errors
     */
    private fun analyzeJavaCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()

        // Check for unmatched brackets
        val bracketBalance = checkBracketBalance(code)
        if (bracketBalance.curlyBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.curlyBalance > 0)
                    "Eksik kapatma parantezi '}' - ${bracketBalance.curlyBalance} adet"
                else
                    "Fazla kapatma parantezi '}' - ${-bracketBalance.curlyBalance} adet",
                lineNumber = null,
                suggestedFix = if (bracketBalance.curlyBalance > 0) "}".repeat(bracketBalance.curlyBalance) else null
            ))
        }

        // Check for missing semicolons in Java statements
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (needsSemicolon(trimmedLine, "java") && !trimmedLine.endsWith(";")) {
                errors.add(CodeError(
                    errorType = ErrorType.MISSING_SEMICOLON,
                    description = "Eksik noktalı virgül",
                    lineNumber = index + 1,
                    suggestedFix = ";"
                ))
            }
        }

        // Check for unclosed strings
        lines.forEachIndexed { index, line ->
            if (hasUnclosedString(line)) {
                errors.add(CodeError(
                    errorType = ErrorType.UNCLOSED_STRING,
                    description = "Kapatılmamış string ifadesi",
                    lineNumber = index + 1,
                    suggestedFix = "\""
                ))
            }
        }

        return errors
    }

    /**
     * Analyzes Python code for common errors
     */
    private fun analyzePythonCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()

        // Check for missing colons after def, if, for, while, class, etc.
        val colonPatterns = listOf(
            Regex("""^\s*def\s+\w+\s*\([^)]*\)\s*$"""),
            Regex("""^\s*if\s+.+\s*$"""),
            Regex("""^\s*elif\s+.+\s*$"""),
            Regex("""^\s*else\s*$"""),
            Regex("""^\s*for\s+.+\s*in\s+.+\s*$"""),
            Regex("""^\s*while\s+.+\s*$"""),
            Regex("""^\s*class\s+\w+.*\s*$"""),
            Regex("""^\s*try\s*$"""),
            Regex("""^\s*except.*\s*$"""),
            Regex("""^\s*finally\s*$"""),
            Regex("""^\s*with\s+.+\s*$""")
        )

        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (colonPatterns.any { it.matches(trimmedLine) } && !trimmedLine.endsWith(":")) {
                errors.add(CodeError(
                    errorType = ErrorType.MISSING_COLON,
                    description = "Python bloğu için eksik iki nokta ':'",
                    lineNumber = index + 1,
                    suggestedFix = ":"
                ))
            }
        }

        // Check for unclosed parentheses
        val bracketBalance = checkBracketBalance(code)
        if (bracketBalance.parenBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_PARENTHESIS,
                description = if (bracketBalance.parenBalance > 0)
                    "Eksik kapatma parantezi ')' - ${bracketBalance.parenBalance} adet"
                else
                    "Fazla kapatma parantezi ')' - ${-bracketBalance.parenBalance} adet",
                lineNumber = null,
                suggestedFix = if (bracketBalance.parenBalance > 0) ")".repeat(bracketBalance.parenBalance) else null
            ))
        }

        // Check for unclosed brackets
        if (bracketBalance.squareBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.squareBalance > 0)
                    "Eksik kapatma köşeli parantezi ']' - ${bracketBalance.squareBalance} adet"
                else
                    "Fazla kapatma köşeli parantezi ']' - ${-bracketBalance.squareBalance} adet",
                lineNumber = null,
                suggestedFix = if (bracketBalance.squareBalance > 0) "]".repeat(bracketBalance.squareBalance) else null
            ))
        }

        // Check for unclosed strings
        lines.forEachIndexed { index, line ->
            if (hasUnclosedString(line)) {
                errors.add(CodeError(
                    errorType = ErrorType.UNCLOSED_STRING,
                    description = "Kapatılmamış string ifadesi",
                    lineNumber = index + 1,
                    suggestedFix = "\""
                ))
            }
        }

        return errors
    }

    /**
     * Analyzes JavaScript code for common errors
     */
    private fun analyzeJavaScriptCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()
        val lines = code.lines()

        // Check for unmatched brackets
        val bracketBalance = checkBracketBalance(code)
        if (bracketBalance.curlyBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.curlyBalance > 0)
                    "Eksik kapatma parantezi '}' - ${bracketBalance.curlyBalance} adet"
                else
                    "Fazla kapatma parantezi '}' - ${-bracketBalance.curlyBalance} adet",
                lineNumber = null,
                suggestedFix = if (bracketBalance.curlyBalance > 0) "}".repeat(bracketBalance.curlyBalance) else null
            ))
        }

        if (bracketBalance.parenBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_PARENTHESIS,
                description = if (bracketBalance.parenBalance > 0)
                    "Eksik kapatma parantezi ')' - ${bracketBalance.parenBalance} adet"
                else
                    "Fazla kapatma parantezi ')' - ${-bracketBalance.parenBalance} adet",
                lineNumber = null,
                suggestedFix = if (bracketBalance.parenBalance > 0) ")".repeat(bracketBalance.parenBalance) else null
            ))
        }

        // Check for unclosed strings
        lines.forEachIndexed { index, line ->
            if (hasUnclosedString(line)) {
                errors.add(CodeError(
                    errorType = ErrorType.UNCLOSED_STRING,
                    description = "Kapatılmamış string ifadesi",
                    lineNumber = index + 1,
                    suggestedFix = "\""
                ))
            }
        }

        // Check for template literal issues
        var templateLiteralCount = 0
        code.forEach { char ->
            if (char == '`') templateLiteralCount++
        }
        if (templateLiteralCount % 2 != 0) {
            errors.add(CodeError(
                errorType = ErrorType.UNCLOSED_STRING,
                description = "Kapatılmamış template literal",
                lineNumber = null,
                suggestedFix = "`"
            ))
        }

        return errors
    }

    /**
     * Analyzes HTML code for common errors
     */
    private fun analyzeHtmlCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()

        // Check for unclosed tags
        val tagPattern = Regex("""<(\w+)[^>]*(?<!/)>""")
        val closingTagPattern = Regex("""</(\w+)>""")
        val selfClosingTags = setOf("br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "source", "track", "wbr")

        val openTags = mutableListOf<String>()
        tagPattern.findAll(code).forEach { match ->
            val tagName = match.groupValues[1].lowercase()
            if (!selfClosingTags.contains(tagName)) {
                openTags.add(tagName)
            }
        }

        closingTagPattern.findAll(code).forEach { match ->
            val tagName = match.groupValues[1].lowercase()
            if (openTags.isNotEmpty() && openTags.last() == tagName) {
                openTags.removeAt(openTags.size - 1)
            }
        }

        openTags.forEach { tag ->
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = "Kapatılmamış HTML etiketi: <$tag>",
                lineNumber = null,
                suggestedFix = "</$tag>"
            ))
        }

        return errors
    }

    /**
     * Analyzes JSON code for common errors
     */
    private fun analyzeJsonCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()

        // Check for trailing commas
        val trailingCommaPattern = Regex(""",\s*[}\]]""")
        if (trailingCommaPattern.containsMatchIn(code)) {
            errors.add(CodeError(
                errorType = ErrorType.SYNTAX_ERROR,
                description = "JSON'da fazladan virgül var (trailing comma)",
                lineNumber = null,
                suggestedFix = null
            ))
        }

        // Check for unmatched brackets
        val bracketBalance = checkBracketBalance(code)
        if (bracketBalance.curlyBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.curlyBalance > 0)
                    "Eksik kapatma süslü parantezi '}'"
                else
                    "Fazla kapatma süslü parantezi '}'",
                lineNumber = null,
                suggestedFix = if (bracketBalance.curlyBalance > 0) "}" else null
            ))
        }

        if (bracketBalance.squareBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.squareBalance > 0)
                    "Eksik kapatma köşeli parantezi ']'"
                else
                    "Fazla kapatma köşeli parantezi ']'",
                lineNumber = null,
                suggestedFix = if (bracketBalance.squareBalance > 0) "]" else null
            ))
        }

        return errors
    }

    /**
     * Analyzes XML code for common errors
     */
    private fun analyzeXmlCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()

        // Similar to HTML but more strict
        val tagPattern = Regex("""<(\w+)[^>]*(?<!/)>""")
        val closingTagPattern = Regex("""</(\w+)>""")

        val openTags = mutableListOf<String>()
        tagPattern.findAll(code).forEach { match ->
            openTags.add(match.groupValues[1])
        }

        closingTagPattern.findAll(code).forEach { match ->
            val tagName = match.groupValues[1]
            if (openTags.isNotEmpty() && openTags.last() == tagName) {
                openTags.removeAt(openTags.size - 1)
            }
        }

        openTags.forEach { tag ->
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = "Kapatılmamış XML etiketi: <$tag>",
                lineNumber = null,
                suggestedFix = "</$tag>"
            ))
        }

        return errors
    }

    /**
     * Analyzes generic code for common errors
     */
    private fun analyzeGenericCode(code: String): List<CodeError> {
        val errors = mutableListOf<CodeError>()

        // Check bracket balance for generic code
        val bracketBalance = checkBracketBalance(code)
        
        if (bracketBalance.curlyBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.curlyBalance > 0)
                    "Eksik kapatma parantezi '}'"
                else
                    "Fazla kapatma parantezi '}'",
                lineNumber = null,
                suggestedFix = if (bracketBalance.curlyBalance > 0) "}".repeat(bracketBalance.curlyBalance) else null
            ))
        }

        if (bracketBalance.parenBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_PARENTHESIS,
                description = if (bracketBalance.parenBalance > 0)
                    "Eksik kapatma parantezi ')'"
                else
                    "Fazla kapatma parantezi ')'",
                lineNumber = null,
                suggestedFix = if (bracketBalance.parenBalance > 0) ")".repeat(bracketBalance.parenBalance) else null
            ))
        }

        if (bracketBalance.squareBalance != 0) {
            errors.add(CodeError(
                errorType = ErrorType.MISSING_BRACKET,
                description = if (bracketBalance.squareBalance > 0)
                    "Eksik kapatma köşeli parantezi ']'"
                else
                    "Fazla kapatma köşeli parantezi ']'",
                lineNumber = null,
                suggestedFix = if (bracketBalance.squareBalance > 0) "]".repeat(bracketBalance.squareBalance) else null
            ))
        }

        return errors
    }

    /**
     * Data class to hold bracket balance counts
     */
    private data class BracketBalance(
        val curlyBalance: Int,
        val squareBalance: Int,
        val parenBalance: Int
    )

    /**
     * Checks the balance of different bracket types in code
     */
    private fun checkBracketBalance(code: String): BracketBalance {
        var curlyBalance = 0
        var squareBalance = 0
        var parenBalance = 0
        var inString = false
        var stringChar = ' '
        var escaped = false

        code.forEach { char ->
            if (escaped) {
                escaped = false
                return@forEach
            }

            if (char == '\\') {
                escaped = true
                return@forEach
            }

            if (!inString && (char == '"' || char == '\'')) {
                inString = true
                stringChar = char
            } else if (inString && char == stringChar) {
                inString = false
            } else if (!inString) {
                when (char) {
                    '{' -> curlyBalance++
                    '}' -> curlyBalance--
                    '[' -> squareBalance++
                    ']' -> squareBalance--
                    '(' -> parenBalance++
                    ')' -> parenBalance--
                }
            }
        }

        return BracketBalance(curlyBalance, squareBalance, parenBalance)
    }

    /**
     * Checks if a line has an unclosed string
     */
    private fun hasUnclosedString(line: String): Boolean {
        var doubleQuoteCount = 0
        var singleQuoteCount = 0
        var escaped = false

        line.forEach { char ->
            if (escaped) {
                escaped = false
                return@forEach
            }

            if (char == '\\') {
                escaped = true
                return@forEach
            }

            when (char) {
                '"' -> doubleQuoteCount++
                '\'' -> singleQuoteCount++
            }
        }

        return doubleQuoteCount % 2 != 0 || singleQuoteCount % 2 != 0
    }

    /**
     * Checks if a line needs a semicolon based on the language
     */
    private fun needsSemicolon(line: String, language: String): Boolean {
        if (line.isEmpty() || 
            line.endsWith("{") || 
            line.endsWith("}") || 
            line.startsWith("//") ||
            line.startsWith("/*") ||
            line.startsWith("*") ||
            line.endsWith("*/") ||
            line.startsWith("import ") ||
            line.startsWith("package ")) {
            return false
        }

        return when (language.lowercase()) {
            "java" -> {
                // Java statements that typically need semicolons
                line.contains("=") ||
                line.contains("return") ||
                line.contains("throw") ||
                line.contains("break") ||
                line.contains("continue") ||
                line.matches(Regex(""".*\w+\s*\([^)]*\)\s*$"""))
            }
            "javascript" -> {
                // JavaScript is more lenient but still needs semicolons in certain cases
                line.contains("var ") ||
                line.contains("let ") ||
                line.contains("const ") ||
                line.contains("return") ||
                line.contains("throw")
            }
            else -> false
        }
    }

    /**
     * Fixes missing semicolons in code
     */
    private fun fixMissingSemicolons(code: String, language: String?): String {
        if (language?.lowercase() != "java" && language?.lowercase() != "javascript") {
            return code
        }

        val lines = code.lines().toMutableList()
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (needsSemicolon(trimmedLine, language) && !trimmedLine.endsWith(";")) {
                lines[index] = line + ";"
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * Fixes missing brackets in code
     */
    private fun fixMissingBrackets(code: String): String {
        val balance = checkBracketBalance(code)
        var fixedCode = code

        if (balance.curlyBalance > 0) {
            fixedCode += "\n" + "}".repeat(balance.curlyBalance)
        }

        if (balance.squareBalance > 0) {
            fixedCode += "]".repeat(balance.squareBalance)
        }

        return fixedCode
    }

    /**
     * Fixes missing parentheses in code
     */
    private fun fixMissingParentheses(code: String): String {
        val balance = checkBracketBalance(code)
        var fixedCode = code

        if (balance.parenBalance > 0) {
            fixedCode += ")".repeat(balance.parenBalance)
        }

        return fixedCode
    }

    /**
     * Fixes unclosed strings in code
     */
    private fun fixUnclosedStrings(code: String): String {
        val lines = code.lines().toMutableList()
        
        lines.forEachIndexed { index, line ->
            if (hasUnclosedString(line)) {
                // Count quotes to determine which one is unclosed
                var doubleQuoteCount = 0
                var singleQuoteCount = 0
                var escaped = false

                line.forEach { char ->
                    if (escaped) {
                        escaped = false
                        return@forEach
                    }
                    if (char == '\\') {
                        escaped = true
                        return@forEach
                    }
                    when (char) {
                        '"' -> doubleQuoteCount++
                        '\'' -> singleQuoteCount++
                    }
                }

                if (doubleQuoteCount % 2 != 0) {
                    lines[index] = line + "\""
                } else if (singleQuoteCount % 2 != 0) {
                    lines[index] = line + "'"
                }
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * Fixes missing colons in Python code
     */
    private fun fixMissingColons(code: String, language: String?): String {
        if (language?.lowercase() != "python") {
            return code
        }

        val colonPatterns = listOf(
            Regex("""^(\s*def\s+\w+\s*\([^)]*\))\s*$"""),
            Regex("""^(\s*if\s+.+)\s*$"""),
            Regex("""^(\s*elif\s+.+)\s*$"""),
            Regex("""^(\s*else)\s*$"""),
            Regex("""^(\s*for\s+.+\s*in\s+.+)\s*$"""),
            Regex("""^(\s*while\s+.+)\s*$"""),
            Regex("""^(\s*class\s+\w+.*)\s*$"""),
            Regex("""^(\s*try)\s*$"""),
            Regex("""^(\s*except.*)\s*$"""),
            Regex("""^(\s*finally)\s*$"""),
            Regex("""^(\s*with\s+.+)\s*$""")
        )

        val lines = code.lines().toMutableList()
        
        lines.forEachIndexed { index, line ->
            colonPatterns.forEach { pattern ->
                val match = pattern.find(line)
                if (match != null && !line.trim().endsWith(":")) {
                    lines[index] = line + ":"
                }
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * Generates a user-friendly error summary in Turkish
     */
    fun generateErrorSummary(result: CodeAnalysisResult): String {
        if (!result.hasErrors) {
            return "✅ Kodda hata bulunamadı."
        }

        val summary = StringBuilder()
        summary.append("⚠️ Kodda ${result.errors.size} adet olası hata tespit edildi:\n\n")

        result.errors.forEachIndexed { index, error ->
            summary.append("${index + 1}. ")
            if (error.lineNumber != null) {
                summary.append("[Satır ${error.lineNumber}] ")
            }
            summary.append("${error.description}\n")
            if (error.suggestedFix != null) {
                summary.append("   💡 Öneri: '${error.suggestedFix}' ekleyin\n")
            }
        }

        if (result.fixedCode != null) {
            summary.append("\n✨ Otomatik düzeltme mevcut. Düzeltilmiş kodu görmek ister misiniz?")
        }

        return summary.toString()
    }

    /**
     * Checks if code contains errors that should trigger auto-fix suggestion
     */
    fun shouldSuggestAutoFix(code: String): Boolean {
        val result = analyzeCode(code)
        return result.hasErrors && result.fixedCode != null
    }
}
