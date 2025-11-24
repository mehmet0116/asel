package com.aikodasistani.aikodasistani.util

object CodeDetectionUtil {

    fun detectLanguageAndCode(content: String): Pair<String?, String?> {
        val codePatterns = mapOf(
            "kotlin" to listOf(
                Regex("""fun\s+\w+\s*\([^)]*\)\s*(:\s*\w+)?\s*\{"""),
                Regex("""class\s+\w+"""),
                Regex("""val\s+\w+\s*:\s*\w+"""),
                Regex("""println\s*\([^)]*\)""")
            ),
            "java" to listOf(
                Regex("""public\s+class\s+\w+"""),
                Regex("""System\.out\.println\s*\([^)]*\)"""),
                Regex("""public\s+static\s+void\s+main\s*\([^)]*\)"""),
                Regex("""import\s+java\.\w+""")
            ),
            "python" to listOf(
                Regex("""def\s+\w+\s*\([^)]*\):"""),
                Regex("""print\s*\([^)]*\)"""),
                Regex("""import\s+\w+"""),
                Regex("""class\s+\w+:""")
            ),
            "javascript" to listOf(
                Regex("""function\s+\w+\s*\([^)]*\)\s*\{"""),
                Regex("""console\.log\s*\([^)]*\)"""),
                Regex("""const\s+\w+\s*=\s*[^;]+"""),
                Regex("""document\.getElementById""")
            ),
            "html" to listOf(
                Regex("""<!DOCTYPE\s+html>"""),
                Regex("""<html[^>]*>"""),
                Regex("""<head>"""),
                Regex("""<body>"""),
                Regex("""<div[^>]*>""")
            ),
            "css" to listOf(
                Regex("""\.\w+\s*\{[^}]*\}"""),
                Regex("""#\w+\s*\{[^}]*\}"""),
                Regex("""@media[^{]*\{""")
            ),
            "json" to listOf(
                Regex("""\{\s*"[^"]*"\s*:\s*[^}]+\}"""),
                Regex("""\[\s*\{[^}]+\}\s*\]"""),
                Regex(""""[^"]*"\s*:\s*"[^"]*""")
            ),
            "xml" to listOf(
                Regex("""<\?xml[^?>]*\?>"""),
                Regex("""<[^>]+>[^<]*</[^>]+>"""),
                Regex("""<[A-Za-z][A-Za-z0-9]*[^>]*>""")
            )
        )

        // Kod bloğu kontrolü (``` ile çevrili içerik)
        val codeBlockPattern = Regex("```(\\w+)?\\s*([\\s\\S]*?)```")
        val codeBlockMatch = codeBlockPattern.find(content)

        if (codeBlockMatch != null) {
            val languageHint = codeBlockMatch.groupValues[1].ifBlank { null }
            val codeContent = codeBlockMatch.groupValues[2].trim()

            if (languageHint != null) {
                return Pair(languageHint, codeContent)
            }

            // Dil ipucu yoksa, içeriğe göre tespit et
            for ((language, patterns) in codePatterns) {
                if (patterns.any { it.containsMatchIn(codeContent) }) {
                    return Pair(language, codeContent)
                }
            }

            // Eğer hala tespit edilemediyse ama kod bloğu varsa, genel "code" olarak işaretle
            if (codeContent.isNotBlank()) {
                return Pair("text", codeContent)
            }
        }

        // Kod bloğu yoksa satır içi kod ve pattern kontrolü
        for ((language, patterns) in codePatterns) {
            if (patterns.any { it.containsMatchIn(content) }) {
                // İçerikten kod kısmını çıkar
                val extractedCode = extractCodeFromContent(content, language)
                return Pair(language, extractedCode)
            }
        }

        return Pair(null, null)
    }

    private fun extractCodeFromContent(content: String, language: String): String {
        return when (language) {
            "kotlin", "java", "python", "javascript" -> {
                // Bu diller için kod benzeri yapıları çıkar
                val lines = content.lines()
                lines.filter { line ->
                    line.contains(Regex("""\b(fun|class|def|function|import|val|var|public|private)\b""")) ||
                            line.contains(Regex("""[{}();=]""")) &&
                            !line.startsWith("//") &&
                            !line.startsWith("#") &&
                            line.trim().isNotEmpty()
                }.joinToString("\n")
            }
            "html", "xml" -> {
                content.lines().filter { line ->
                    line.contains(Regex("""<[^>]+>"""))
                }.joinToString("\n")
            }
            "json" -> {
                val jsonPattern = Regex("""(\{[^{}]*\}|\[[^\[\]]*\])""")
                jsonPattern.find(content)?.value ?: content
            }
            else -> content
        }
    }

    fun getFileExtension(language: String?): String {
        return when (language?.lowercase()) {
            "kotlin" -> ".kt"
            "java" -> ".java"
            "python" -> ".py"
            "javascript" -> ".js"
            "html" -> ".html"
            "css" -> ".css"
            "json" -> ".json"
            "xml" -> ".xml"
            "markdown", "md" -> ".md"
            "text" -> ".txt"
            else -> ".txt"
        }
    }

    fun shouldShowDownloadButton(content: String): Boolean {
        val (language, codeContent) = detectLanguageAndCode(content)
        return language != null && codeContent != null && codeContent.length > 10
    }
}

