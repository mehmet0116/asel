package com.aikodasistani.aikodasistani.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.aikodasistani.aikodasistani.models.GeneratedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/**
 * Utility for generating real document files from AI responses.
 * Supports CSV, TXT, Markdown, and Excel (XLSX) file creation.
 */
object DocumentGenerator {

    private const val TAG = "DocumentGenerator"
    private const val MAX_AUTO_SIZE_COLUMNS = 10

    /**
     * Result of document generation attempt.
     */
    sealed class GenerationResult {
        data class Success(val document: GeneratedDocument) : GenerationResult()
        data class Error(val message: String) : GenerationResult()
    }

    /**
     * Document generation request parsed from AI response.
     */
    data class DocumentRequest(
        val fileType: String,           // "csv", "xlsx", "txt", "md"
        val suggestedFileName: String,
        val content: String,            // CSV text, plain text, or JSON table structure
        val description: String? = null // Optional user-facing description
    )

    /**
     * Generate a document file based on the request.
     */
    suspend fun generateDocument(
        context: Context,
        request: DocumentRequest
    ): GenerationResult = withContext(Dispatchers.IO) {
        try {
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val result = when (request.fileType.lowercase()) {
                "csv" -> generateCsvFile(context, outputDir, request)
                "xlsx", "excel" -> generateExcelFile(context, outputDir, request)
                "txt", "text" -> generateTextFile(context, outputDir, request)
                "md", "markdown" -> generateMarkdownFile(context, outputDir, request)
                else -> generateTextFile(context, outputDir, request) // Default to text
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Document generation failed", e)
            GenerationResult.Error("Dosya oluşturulamadı: ${e.message}")
        }
    }

    /**
     * Generate a CSV file.
     */
    private fun generateCsvFile(
        context: Context,
        outputDir: File,
        request: DocumentRequest
    ): GenerationResult {
        val fileName = ensureExtension(request.suggestedFileName, "csv")
        val file = File(outputDir, fileName)
        
        return try {
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    writer.write(request.content)
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            GenerationResult.Success(
                GeneratedDocument(
                    file = file,
                    fileName = fileName,
                    mimeType = "text/csv",
                    sizeBytes = file.length(),
                    contentUri = uri,
                    fileType = "csv"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "CSV generation failed", e)
            GenerationResult.Error("CSV oluşturulamadı: ${e.message}")
        }
    }

    /**
     * Generate an Excel (XLSX) file.
     */
    private fun generateExcelFile(
        context: Context,
        outputDir: File,
        request: DocumentRequest
    ): GenerationResult {
        val fileName = ensureExtension(request.suggestedFileName, "xlsx")
        val file = File(outputDir, fileName)
        
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Sheet1")
            
            // Parse CSV content into rows
            val lines = request.content.lines()
            lines.forEachIndexed { rowIndex, line ->
                if (line.isNotBlank()) {
                    val row = sheet.createRow(rowIndex)
                    val cells = parseCsvLine(line)
                    cells.forEachIndexed { cellIndex, cellValue ->
                        val cell = row.createCell(cellIndex)
                        // Try to parse as number
                        val numValue = cellValue.toDoubleOrNull()
                        if (numValue != null) {
                            cell.setCellValue(numValue)
                        } else {
                            cell.setCellValue(cellValue)
                        }
                    }
                }
            }
            
            // Auto-size columns (up to MAX_AUTO_SIZE_COLUMNS columns)
            for (i in 0 until minOf(MAX_AUTO_SIZE_COLUMNS, lines.firstOrNull()?.split(",")?.size ?: 0)) {
                sheet.autoSizeColumn(i)
            }
            
            FileOutputStream(file).use { fos ->
                workbook.write(fos)
            }
            workbook.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            GenerationResult.Success(
                GeneratedDocument(
                    file = file,
                    fileName = fileName,
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    sizeBytes = file.length(),
                    contentUri = uri,
                    fileType = "xlsx"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Excel generation failed", e)
            // Fallback to CSV if Excel fails
            Log.d(TAG, "Falling back to CSV generation")
            generateCsvFile(context, outputDir, request.copy(
                suggestedFileName = request.suggestedFileName.replace(".xlsx", ".csv")
            ))
        }
    }

    /**
     * Generate a plain text file.
     */
    private fun generateTextFile(
        context: Context,
        outputDir: File,
        request: DocumentRequest
    ): GenerationResult {
        val fileName = ensureExtension(request.suggestedFileName, "txt")
        val file = File(outputDir, fileName)
        
        return try {
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    writer.write(request.content)
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            GenerationResult.Success(
                GeneratedDocument(
                    file = file,
                    fileName = fileName,
                    mimeType = "text/plain",
                    sizeBytes = file.length(),
                    contentUri = uri,
                    fileType = "txt"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Text file generation failed", e)
            GenerationResult.Error("Metin dosyası oluşturulamadı: ${e.message}")
        }
    }

    /**
     * Generate a Markdown file.
     */
    private fun generateMarkdownFile(
        context: Context,
        outputDir: File,
        request: DocumentRequest
    ): GenerationResult {
        val fileName = ensureExtension(request.suggestedFileName, "md")
        val file = File(outputDir, fileName)
        
        return try {
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    writer.write(request.content)
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            GenerationResult.Success(
                GeneratedDocument(
                    file = file,
                    fileName = fileName,
                    mimeType = "text/markdown",
                    sizeBytes = file.length(),
                    contentUri = uri,
                    fileType = "md"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Markdown file generation failed", e)
            GenerationResult.Error("Markdown dosyası oluşturulamadı: ${e.message}")
        }
    }

    /**
     * Ensure file name has the correct extension.
     */
    private fun ensureExtension(fileName: String, extension: String): String {
        val cleanName = fileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (cleanName.endsWith(".$extension", ignoreCase = true)) {
            cleanName
        } else {
            "$cleanName.$extension"
        }
    }

    /**
     * Parse a CSV line, handling quoted values and escaped quotes.
     * Handles standard CSV escaping where "" represents a literal quote.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    // Escaped quote ("") - add single quote and skip next char
                    current.append('"')
                    i += 2
                    continue
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString().trim())
        
        return result
    }

    /**
     * Parse AI response for document generation request.
     * Looks for specific markers in the response.
     * 
     * Expected format in AI response:
     * ```
     * [DOCUMENT_REQUEST]
     * fileType: csv
     * fileName: my_template.csv
     * content:
     * Header1,Header2,Header3
     * Value1,Value2,Value3
     * [/DOCUMENT_REQUEST]
     * ```
     * 
     * Or JSON format:
     * ```json
     * {
     *   "fileType": "csv",
     *   "suggestedFileName": "template.csv",
     *   "content": "Header1,Header2\nValue1,Value2"
     * }
     * ```
     */
    fun parseDocumentRequest(aiResponse: String): DocumentRequest? {
        // Try to find document request markers
        val markerPattern = Regex(
            """\[DOCUMENT_REQUEST\]\s*fileType:\s*(\w+)\s*fileName:\s*(.+?)\s*content:\s*([\s\S]*?)\s*\[/DOCUMENT_REQUEST\]""",
            RegexOption.IGNORE_CASE
        )
        
        markerPattern.find(aiResponse)?.let { match ->
            return DocumentRequest(
                fileType = match.groupValues[1].trim(),
                suggestedFileName = match.groupValues[2].trim(),
                content = match.groupValues[3].trim()
            )
        }
        
        // Try JSON pattern
        val jsonPattern = Regex(
            """\{[^{}]*"fileType"\s*:\s*"(\w+)"[^{}]*"suggestedFileName"\s*:\s*"([^"]+)"[^{}]*"content"\s*:\s*"([^"]*(?:\\.[^"]*)*)"[^{}]*\}""",
            RegexOption.IGNORE_CASE
        )
        
        jsonPattern.find(aiResponse)?.let { match ->
            val content = match.groupValues[3]
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            
            return DocumentRequest(
                fileType = match.groupValues[1].trim(),
                suggestedFileName = match.groupValues[2].trim(),
                content = content
            )
        }
        
        // Try to find CSV-like content in explicitly marked csv code blocks only
        // More strict detection to avoid false positives
        val csvBlockPattern = Regex("""```csv\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE)
        csvBlockPattern.findAll(aiResponse).forEach { match ->
            val content = match.groupValues[1].trim()
            // Validate it looks like proper CSV: multiple lines, consistent comma count
            if (isLikelyCsv(content)) {
                return DocumentRequest(
                    fileType = "csv",
                    suggestedFileName = "generated_file_${System.currentTimeMillis()}.csv",
                    content = content
                )
            }
        }
        
        return null
    }
    
    /**
     * Check if content looks like valid CSV data.
     * Validates that multiple lines have consistent comma counts.
     */
    private fun isLikelyCsv(content: String): Boolean {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return false
        
        // Count commas in first few lines and check consistency
        val commaCounts = lines.take(5).map { line -> line.count { it == ',' } }
        if (commaCounts.isEmpty() || commaCounts.first() == 0) return false
        
        // All lines should have same or similar comma count (header might differ)
        val expectedCount = commaCounts.first()
        return commaCounts.all { it == expectedCount || it == expectedCount - 1 || it == expectedCount + 1 }
    }

    /**
     * Check if AI response contains a document generation request.
     */
    fun containsDocumentRequest(aiResponse: String): Boolean {
        return parseDocumentRequest(aiResponse) != null
    }

    /**
     * Get the list of supported file types.
     */
    fun getSupportedFileTypes(): List<String> {
        return listOf("csv", "xlsx", "txt", "md")
    }
}
