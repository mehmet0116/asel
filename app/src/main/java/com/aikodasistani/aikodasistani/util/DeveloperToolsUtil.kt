package com.aikodasistani.aikodasistani.util

import android.util.Base64
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Developer tools utility class with JSON, Base64, and Regex utilities.
 * Part of Phase 3: Developer Tools feature set.
 */
object DeveloperToolsUtil {

    // ==================== JSON UTILITIES ====================
    
    /**
     * Result of JSON validation
     */
    data class JsonValidationResult(
        val isValid: Boolean,
        val formattedJson: String?,
        val errorMessage: String?,
        val errorPosition: Int?
    )
    
    /**
     * Validates and formats JSON string
     */
    fun validateAndFormatJson(jsonString: String): JsonValidationResult {
        return try {
            val json = Json { 
                prettyPrint = true
                isLenient = true
            }
            val element = json.parseToJsonElement(jsonString)
            val formatted = json.encodeToString(JsonElement.serializer(), element)
            
            JsonValidationResult(
                isValid = true,
                formattedJson = formatted,
                errorMessage = null,
                errorPosition = null
            )
        } catch (e: Exception) {
            // Try to find the position of the error
            val errorPos = extractErrorPosition(e.message)
            
            JsonValidationResult(
                isValid = false,
                formattedJson = null,
                errorMessage = e.message ?: "JSON parse hatası",
                errorPosition = errorPos
            )
        }
    }
    
    /**
     * Minifies JSON by removing whitespace
     */
    fun minifyJson(jsonString: String): String? {
        return try {
            val json = Json { isLenient = true }
            val element = json.parseToJsonElement(jsonString)
            json.encodeToString(JsonElement.serializer(), element)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts error position from exception message
     */
    private fun extractErrorPosition(message: String?): Int? {
        if (message == null) return null
        
        // Common patterns in error messages
        val patterns = listOf(
            Regex("""at position (\d+)"""),
            Regex("""index (\d+)"""),
            Regex("""offset (\d+)"""),
            Regex("""column (\d+)""")
        )
        
        patterns.forEach { pattern ->
            pattern.find(message)?.let { match ->
                return match.groupValues[1].toIntOrNull()
            }
        }
        
        return null
    }
    
    // ==================== BASE64 UTILITIES ====================
    
    /**
     * Result of Base64 operation
     */
    data class Base64Result(
        val success: Boolean,
        val result: String?,
        val errorMessage: String?
    )
    
    /**
     * Encodes string to Base64
     */
    fun encodeToBase64(input: String): Base64Result {
        return try {
            val encoded = Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
            Base64Result(
                success = true,
                result = encoded.trim(),
                errorMessage = null
            )
        } catch (e: Exception) {
            Base64Result(
                success = false,
                result = null,
                errorMessage = "Encode hatası: ${e.message}"
            )
        }
    }
    
    /**
     * Decodes Base64 string
     */
    fun decodeFromBase64(input: String): Base64Result {
        return try {
            val decoded = Base64.decode(input.trim(), Base64.DEFAULT)
            Base64Result(
                success = true,
                result = String(decoded, Charsets.UTF_8),
                errorMessage = null
            )
        } catch (e: Exception) {
            Base64Result(
                success = false,
                result = null,
                errorMessage = "Decode hatası: Geçersiz Base64 formatı"
            )
        }
    }
    
    /**
     * Checks if a string is valid Base64
     */
    fun isValidBase64(input: String): Boolean {
        return try {
            Base64.decode(input.trim(), Base64.DEFAULT)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ==================== REGEX UTILITIES ====================
    
    /**
     * Result of Regex validation
     */
    data class RegexValidationResult(
        val isValid: Boolean,
        val errorMessage: String?,
        val errorPosition: Int?
    )
    
    /**
     * Result of Regex test
     */
    data class RegexTestResult(
        val isValid: Boolean,
        val matches: List<RegexMatch>,
        val groupCount: Int,
        val errorMessage: String?
    )
    
    /**
     * Represents a regex match with groups
     */
    data class RegexMatch(
        val fullMatch: String,
        val startIndex: Int,
        val endIndex: Int,
        val groups: List<String?>
    )
    
    /**
     * Validates a regex pattern
     */
    fun validateRegex(pattern: String): RegexValidationResult {
        return try {
            Pattern.compile(pattern)
            RegexValidationResult(
                isValid = true,
                errorMessage = null,
                errorPosition = null
            )
        } catch (e: PatternSyntaxException) {
            RegexValidationResult(
                isValid = false,
                errorMessage = e.description,
                errorPosition = e.index
            )
        }
    }
    
    /**
     * Tests a regex pattern against input string
     */
    fun testRegex(pattern: String, input: String, flags: Int = 0): RegexTestResult {
        return try {
            val compiledPattern = Pattern.compile(pattern, flags)
            val matcher = compiledPattern.matcher(input)
            val matches = mutableListOf<RegexMatch>()
            
            while (matcher.find()) {
                val groups = mutableListOf<String?>()
                for (i in 0..matcher.groupCount()) {
                    groups.add(matcher.group(i))
                }
                
                matches.add(RegexMatch(
                    fullMatch = matcher.group(),
                    startIndex = matcher.start(),
                    endIndex = matcher.end(),
                    groups = groups
                ))
            }
            
            RegexTestResult(
                isValid = true,
                matches = matches,
                groupCount = compiledPattern.matcher("").groupCount(),
                errorMessage = null
            )
        } catch (e: PatternSyntaxException) {
            RegexTestResult(
                isValid = false,
                matches = emptyList(),
                groupCount = 0,
                errorMessage = e.description
            )
        }
    }
    
    /**
     * Replaces matches with replacement string
     */
    fun regexReplace(pattern: String, input: String, replacement: String): String? {
        return try {
            val compiledPattern = Pattern.compile(pattern)
            compiledPattern.matcher(input).replaceAll(replacement)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Common regex patterns for quick access
     */
    object CommonPatterns {
        val EMAIL = """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""
        val URL = """https?://[^\s]+"""
        val PHONE = """\+?[0-9]{1,4}[-.\s]?\(?\d{1,3}\)?[-.\s]?\d{1,4}[-.\s]?\d{1,4}[-.\s]?\d{1,9}"""
        val IP_ADDRESS = """\b(?:\d{1,3}\.){3}\d{1,3}\b"""
        val DATE_ISO = """\d{4}-\d{2}-\d{2}"""
        val TIME_24H = """([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?"""
        val HEX_COLOR = """#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})"""
        val INTEGER = """-?\d+"""
        val FLOAT = """-?\d+\.?\d*"""
        val HTML_TAG = """<[^>]+>"""
        val WHITESPACE = """\s+"""
        
        fun getAll(): List<Pair<String, String>> = listOf(
            "Email" to EMAIL,
            "URL" to URL,
            "Telefon" to PHONE,
            "IP Adresi" to IP_ADDRESS,
            "Tarih (ISO)" to DATE_ISO,
            "Saat (24h)" to TIME_24H,
            "Hex Renk" to HEX_COLOR,
            "Tam Sayı" to INTEGER,
            "Ondalıklı Sayı" to FLOAT,
            "HTML Tag" to HTML_TAG,
            "Boşluk" to WHITESPACE
        )
    }
    
    // ==================== HASH UTILITIES ====================
    
    /**
     * Generates MD5 hash of input string
     */
    fun md5(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Hash hatası"
        }
    }
    
    /**
     * Generates SHA-1 hash of input string
     */
    fun sha1(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Hash hatası"
        }
    }
    
    /**
     * Generates SHA-256 hash of input string
     */
    fun sha256(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Hash hatası"
        }
    }
    
    // ==================== URL UTILITIES ====================
    
    /**
     * URL encodes a string
     */
    fun urlEncode(input: String): String {
        return try {
            java.net.URLEncoder.encode(input, "UTF-8")
        } catch (e: Exception) {
            input
        }
    }
    
    /**
     * URL decodes a string
     */
    fun urlDecode(input: String): String {
        return try {
            java.net.URLDecoder.decode(input, "UTF-8")
        } catch (e: Exception) {
            input
        }
    }
    
    // ==================== STRING UTILITIES ====================
    
    /**
     * Counts characters, words, and lines in a string
     */
    data class TextStats(
        val characterCount: Int,
        val characterCountNoSpaces: Int,
        val wordCount: Int,
        val lineCount: Int,
        val paragraphCount: Int
    )
    
    fun getTextStats(input: String): TextStats {
        val characterCount = input.length
        val characterCountNoSpaces = input.replace(Regex("\\s"), "").length
        val wordCount = if (input.isBlank()) 0 else input.trim().split(Regex("\\s+")).size
        val lineCount = if (input.isEmpty()) 0 else input.lines().size
        val paragraphCount = if (input.isBlank()) 0 else input.split(Regex("\n\n+")).filter { it.isNotBlank() }.size
        
        return TextStats(
            characterCount = characterCount,
            characterCountNoSpaces = characterCountNoSpaces,
            wordCount = wordCount,
            lineCount = lineCount,
            paragraphCount = paragraphCount
        )
    }
}
