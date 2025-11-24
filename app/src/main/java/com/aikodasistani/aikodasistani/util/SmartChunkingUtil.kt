// SmartChunkingUtil.kt
package com.aikodasistani.aikodasistani.util

/**
 * Akıllı Dosya Parçalama Sistemi
 */
class SmartChunkingUtil {

    companion object {
        private const val MAX_CHUNK_SIZE = 25000
    }

    fun chunkFileIntelligently(fileName: String, fileContent: String): List<FileChunk> {
        return when {
            fileName.endsWith(".kt") || fileName.endsWith(".java") ->
                chunkKotlinJavaFile(fileName, fileContent)
            fileName.endsWith(".xml") ->
                chunkXmlFile(fileName, fileContent)
            else ->
                chunkGenericFile(fileName, fileContent)
        }
    }

    private fun chunkKotlinJavaFile(fileName: String, content: String): List<FileChunk> {
        val chunks = mutableListOf<FileChunk>()
        val lines = content.lines()

        var currentChunk = StringBuilder()
        var currentSection = "File Header"
        var braceCount = 0

        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()

            // Detect class/function boundaries
            val isClassStart = trimmedLine.startsWith("class ") ||
                    trimmedLine.startsWith("interface ")

            val isFunctionStart = trimmedLine.startsWith("fun ") ||
                    (trimmedLine.contains(" fun ") && trimmedLine.contains("("))

            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }

            if ((isClassStart || isFunctionStart) && currentChunk.isNotEmpty()) {
                addChunk(chunks, fileName, currentChunk, currentSection)
                currentChunk.clear()

                currentSection = if (isClassStart) "Class: ${extractClassName(trimmedLine)}"
                else "Function: ${extractFunctionName(trimmedLine)}"
            }

            currentChunk.appendLine(line)

            if (currentChunk.length > MAX_CHUNK_SIZE && braceCount == 0) {
                addChunk(chunks, fileName, currentChunk, currentSection)
                currentChunk.clear()
                currentSection = "Continued..."
            }
        }

        if (currentChunk.isNotEmpty()) {
            addChunk(chunks, fileName, currentChunk, currentSection)
        }

        return chunks
    }

    private fun chunkXmlFile(fileName: String, content: String): List<FileChunk> {
        // XML için basit bölme
        return chunkGenericFile(fileName, content)
    }

    private fun chunkGenericFile(fileName: String, content: String): List<FileChunk> {
        val chunks = mutableListOf<FileChunk>()
        val lines = content.lines()
        var currentChunk = StringBuilder()
        var lineCount = 0

        lines.forEachIndexed { index, line ->
            currentChunk.appendLine(line)
            lineCount++

            if (lineCount >= 200 || currentChunk.length >= 15000) {
                chunks.add(FileChunk(
                    fileName = fileName,
                    chunkContent = currentChunk.toString(),
                    sectionName = "Lines ${index - lineCount + 1}-${index + 1}",
                    startLine = index - lineCount + 2,
                    endLine = index + 1,
                    totalChunks = chunks.size + 1
                ))
                currentChunk.clear()
                lineCount = 0
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(FileChunk(
                fileName = fileName,
                chunkContent = currentChunk.toString(),
                sectionName = "Final Lines",
                startLine = lines.size - currentChunk.lines().size + 1,
                endLine = lines.size,
                totalChunks = chunks.size + 1
            ))
        }

        return chunks
    }

    private fun addChunk(chunks: MutableList<FileChunk>, fileName: String,
                         content: StringBuilder, section: String) {
        chunks.add(FileChunk(
            fileName = fileName,
            chunkContent = content.toString(),
            sectionName = section,
            startLine = 1,
            endLine = content.lines().size,
            totalChunks = chunks.size + 1
        ))
    }

    private fun extractClassName(line: String): String {
        return line.substringAfter("class ").substringBefore(" ").substringBefore(":")
    }

    private fun extractFunctionName(line: String): String {
        return line.substringAfter("fun ").substringBefore("(").substringAfterLast(".")
    }
}

data class FileChunk(
    val fileName: String,
    val chunkContent: String,
    val sectionName: String,
    val startLine: Int,
    val endLine: Int,
    val totalChunks: Int
) {
    val chunkHeader: String
        get() = "--- $fileName - $sectionName (Part ${startLine}-${endLine}/$totalChunks) ---"
}