package com.aikodasistani.aikodasistani.projectgenerator.data

import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectFile
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectMetadata
import com.aikodasistani.aikodasistani.projectgenerator.domain.ProjectStructure

/**
 * A strict regex-based parser for AI-generated project structure output.
 * 
 * This parser ONLY handles the `>>> FILE:` format defined in the spec:
 * ```
 * >>> FILE: path/to/file.ext
 * file content here
 * multiple lines
 * >>> FILE: path/to/another.ext
 * another content
 * ```
 * 
 * Design principles:
 * 1. STRICT FORMAT: Only accepts `>>> FILE: path` format, nothing else
 * 2. FAIL-FAST: Returns error immediately on malformed input
 * 3. CONTENT PRESERVATION: File content is preserved exactly as-is (no trimming)
 * 4. PATH VALIDATION: Rejects invalid or dangerous paths
 */
object RegexResponseParser {

    private const val TAG = "RegexResponseParser"

    /**
     * The primary regex pattern for detecting file markers.
     * Matches: `>>> FILE: path/to/file.ext` at the start of a line
     * 
     * Group 1: The file path (everything after ">>> FILE: ")
     */
    private val FILE_MARKER_PATTERN = Regex("^>>> FILE:\\s*(.+)$", RegexOption.MULTILINE)

    /**
     * Pattern for splitting the input by file markers while capturing the paths.
     */
    private val FILE_SPLIT_PATTERN = Regex("(?=^>>> FILE:\\s*.+$)", RegexOption.MULTILINE)

    /**
     * Result of parsing operation.
     */
    sealed class ParseResult {
        /**
         * Successful parse with the extracted project structure.
         */
        data class Success(val structure: ProjectStructure) : ParseResult()

        /**
         * Parse failed with error details.
         */
        data class Error(
            val message: String,
            val lineNumber: Int? = null,
            val details: String? = null
        ) : ParseResult()
    }

    /**
     * Parses raw AI output that uses the `>>> FILE:` format.
     * 
     * @param rawOutput The raw text output from the AI
     * @param projectRoot The name of the project root folder
     * @return ParseResult indicating success with structure or failure with error details
     */
    fun parse(rawOutput: String, projectRoot: String = "project"): ParseResult {
        // Validate input is not empty
        if (rawOutput.isBlank()) {
            return ParseResult.Error(
                message = "Empty AI response",
                details = "The AI returned an empty or whitespace-only response"
            )
        }

        // Check if the response contains the expected format
        if (!rawOutput.contains(">>> FILE:")) {
            return ParseResult.Error(
                message = "Invalid format: No file markers found",
                details = "Expected format: >>> FILE: path/to/file followed by content. " +
                        "The AI response does not contain any '>>> FILE:' markers."
            )
        }

        return try {
            val files = extractFiles(rawOutput)

            if (files.isEmpty()) {
                return ParseResult.Error(
                    message = "No valid files extracted",
                    details = "Found >>> FILE: markers but could not extract any valid files. " +
                            "Check that each marker is followed by a valid path."
                )
            }

            // Validate for duplicate paths
            val duplicates = files.groupBy { it.path }.filter { it.value.size > 1 }
            if (duplicates.isNotEmpty()) {
                return ParseResult.Error(
                    message = "Duplicate file paths detected",
                    details = "Duplicate paths: ${duplicates.keys.joinToString(", ")}"
                )
            }

            val totalSize = files.sumOf { it.content.length.toLong() }

            val structure = ProjectStructure(
                root = projectRoot,
                files = files,
                metadata = ProjectMetadata(
                    totalFiles = files.size,
                    totalSize = totalSize
                )
            )

            log("Successfully parsed ${files.size} files, total size: $totalSize bytes")
            ParseResult.Success(structure)

        } catch (e: Exception) {
            log("Parsing failed: ${e.message}")
            ParseResult.Error(
                message = "Parsing failed: ${e.message}",
                details = e.stackTraceToString()
            )
        }
    }

    /**
     * Extracts files from the raw AI output using the >>> FILE: pattern.
     */
    private fun extractFiles(rawOutput: String): List<ProjectFile> {
        val files = mutableListOf<ProjectFile>()

        // Split by file markers
        val segments = FILE_SPLIT_PATTERN.split(rawOutput)

        for (segment in segments) {
            if (segment.isBlank()) continue

            // Extract the file path from the marker
            val matchResult = FILE_MARKER_PATTERN.find(segment) ?: continue

            val filePath = matchResult.groupValues[1].trim()

            // Validate the path
            if (!isValidPath(filePath)) {
                log("Skipping invalid path: $filePath")
                continue
            }

            // Extract content (everything after the marker line)
            val markerEnd = matchResult.range.last + 1
            val content = if (markerEnd < segment.length) {
                segment.substring(markerEnd).trimStart('\n', '\r')
            } else {
                ""
            }

            // Don't add empty files
            if (content.isBlank()) {
                log("Skipping empty file: $filePath")
                continue
            }

            val sanitizedPath = sanitizePath(filePath)
            files.add(ProjectFile(sanitizedPath, content.trimEnd()))

            log("Extracted file: $sanitizedPath (${content.length} chars)")
        }

        return files
    }

    /**
     * Validates that a path is safe and properly formatted.
     */
    private fun isValidPath(path: String): Boolean {
        if (path.isBlank()) return false

        // Reject path traversal attempts
        if (path.contains("..")) return false

        // Reject absolute paths
        if (path.startsWith("/") || path.startsWith("\\")) return false

        // Reject Windows-style absolute paths
        if (path.length >= 2 && path[1] == ':') return false

        // Must have a file extension (contains at least one dot that's not at the start)
        val fileName = path.substringAfterLast('/')
        if (!fileName.contains('.') || fileName.startsWith('.')) return false

        // Reject null bytes and other control characters
        if (path.any { it.code < 32 }) return false

        return true
    }

    /**
     * Sanitizes a file path to ensure it's clean and consistent.
     */
    private fun sanitizePath(path: String): String {
        var sanitized = path.trim()

        // Normalize path separators
        sanitized = sanitized.replace('\\', '/')

        // Remove leading slashes
        sanitized = sanitized.trimStart('/')

        // Remove double slashes
        while (sanitized.contains("//")) {
            sanitized = sanitized.replace("//", "/")
        }

        // Remove any invalid filesystem characters
        val invalidChars = listOf(':', '*', '?', '"', '<', '>', '|')
        for (char in invalidChars) {
            sanitized = sanitized.replace(char.toString(), "_")
        }

        return sanitized
    }

    /**
     * Validates a path for safety without modifying it.
     * Public API for external validation needs.
     *
     * @param path The path to validate
     * @return true if the path is safe to use, false otherwise
     */
    fun validatePath(path: String): Boolean = isValidPath(path)

    /**
     * Logger for debugging.
     * Uses a pluggable logger for testability.
     */
    private var logger: Logger = DefaultLogger()

    interface Logger {
        fun d(tag: String, message: String)
    }

    private class DefaultLogger : Logger {
        override fun d(tag: String, message: String) {
            try {
                android.util.Log.d(tag, message)
            } catch (_: RuntimeException) {
                // Expected in unit test environments
            }
        }
    }

    /**
     * Sets a custom logger (for testing).
     */
    fun setLogger(customLogger: Logger) {
        logger = customLogger
    }

    private fun log(message: String) {
        logger.d(TAG, message)
    }
}
