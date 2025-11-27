package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Code Formatter Activity - Format and beautify code in multiple languages
 */
class CodeFormatterActivity : AppCompatActivity() {
    
    private lateinit var spinnerLanguage: Spinner
    private lateinit var etInputCode: EditText
    private lateinit var etOutputCode: EditText
    private lateinit var btnFormat: Button
    private lateinit var btnMinify: Button
    private lateinit var btnCopy: Button
    private lateinit var btnClear: Button
    private lateinit var btnSwap: ImageButton
    
    private val languages = listOf(
        "JSON", "HTML", "CSS", "JavaScript", "SQL", "XML", "Kotlin", "Java", "Python"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_formatter)
        
        supportActionBar?.apply {
            title = getString(R.string.code_formatter_title)
            setDisplayHomeAsUpEnabled(true)
        }
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        spinnerLanguage = findViewById(R.id.spinnerFormatterLanguage)
        etInputCode = findViewById(R.id.etFormatterInput)
        etOutputCode = findViewById(R.id.etFormatterOutput)
        btnFormat = findViewById(R.id.btnFormat)
        btnMinify = findViewById(R.id.btnMinify)
        btnCopy = findViewById(R.id.btnCopyFormatted)
        btnClear = findViewById(R.id.btnClearFormatter)
        btnSwap = findViewById(R.id.btnSwapFormatter)
        
        spinnerLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )
    }
    
    private fun setupListeners() {
        btnFormat.setOnClickListener {
            val code = etInputCode.text.toString()
            if (code.isBlank()) {
                Toast.makeText(this, R.string.enter_code, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val language = spinnerLanguage.selectedItem.toString()
            val formatted = formatCode(code, language)
            etOutputCode.setText(formatted)
        }
        
        btnMinify.setOnClickListener {
            val code = etInputCode.text.toString()
            if (code.isBlank()) {
                Toast.makeText(this, R.string.enter_code, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val language = spinnerLanguage.selectedItem.toString()
            val minified = minifyCode(code, language)
            etOutputCode.setText(minified)
        }
        
        btnCopy.setOnClickListener {
            val output = etOutputCode.text.toString()
            if (output.isBlank()) {
                Toast.makeText(this, R.string.output_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Formatted Code", output)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        
        btnClear.setOnClickListener {
            etInputCode.text.clear()
            etOutputCode.text.clear()
        }
        
        btnSwap.setOnClickListener {
            val input = etInputCode.text.toString()
            val output = etOutputCode.text.toString()
            etInputCode.setText(output)
            etOutputCode.setText(input)
        }
    }
    
    private fun formatCode(code: String, language: String): String {
        return when (language.uppercase()) {
            "JSON" -> formatJson(code)
            "HTML", "XML" -> formatXml(code)
            "CSS" -> formatCss(code)
            "JAVASCRIPT", "JS" -> formatJavaScript(code)
            "SQL" -> formatSql(code)
            "KOTLIN", "JAVA" -> formatBraceLanguage(code)
            "PYTHON" -> code.trim() // Python formatting requires AST parsing
            else -> code
        }
    }
    
    private fun minifyCode(code: String, language: String): String {
        return when (language.uppercase()) {
            "JSON" -> minifyJson(code)
            "HTML", "XML" -> minifyHtml(code)
            "CSS" -> minifyCss(code)
            "JAVASCRIPT", "JS" -> minifyJavaScript(code)
            "SQL" -> minifySql(code)
            else -> code.lines().joinToString(" ") { it.trim() }
        }
    }
    
    // JSON formatting
    private fun formatJson(json: String): String {
        try {
            val sb = StringBuilder()
            var indent = 0
            var inString = false
            var escapeNext = false
            
            for (char in json) {
                when {
                    escapeNext -> {
                        sb.append(char)
                        escapeNext = false
                    }
                    char == '\\' -> {
                        sb.append(char)
                        escapeNext = true
                    }
                    char == '"' -> {
                        sb.append(char)
                        inString = !inString
                    }
                    inString -> sb.append(char)
                    char == '{' || char == '[' -> {
                        sb.append(char)
                        indent++
                        sb.append("\n").append("  ".repeat(indent))
                    }
                    char == '}' || char == ']' -> {
                        indent--
                        sb.append("\n").append("  ".repeat(indent)).append(char)
                    }
                    char == ',' -> {
                        sb.append(char).append("\n").append("  ".repeat(indent))
                    }
                    char == ':' -> {
                        sb.append(": ")
                    }
                    !char.isWhitespace() -> sb.append(char)
                }
            }
            return sb.toString()
        } catch (e: Exception) {
            return "❌ Format hatası: ${e.message}"
        }
    }
    
    private fun minifyJson(json: String): String {
        return json.lines().joinToString("") { it.trim() }
            .replace(Regex("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"), "")
    }
    
    // HTML/XML formatting
    private fun formatXml(html: String): String {
        try {
            val sb = StringBuilder()
            var indent = 0
            val lines = html.replace(">", ">\n").replace("<", "\n<").lines()
                .filter { it.isNotBlank() }
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("</")) {
                    indent = maxOf(0, indent - 1)
                    sb.append("  ".repeat(indent)).append(trimmed).append("\n")
                } else if (trimmed.startsWith("<") && !trimmed.startsWith("<!") && !trimmed.endsWith("/>")) {
                    sb.append("  ".repeat(indent)).append(trimmed).append("\n")
                    if (!trimmed.contains("</")) {
                        indent++
                    }
                } else {
                    sb.append("  ".repeat(indent)).append(trimmed).append("\n")
                }
            }
            return sb.toString().trim()
        } catch (e: Exception) {
            return "❌ Format hatası: ${e.message}"
        }
    }
    
    private fun minifyHtml(html: String): String {
        return html.lines().joinToString("") { it.trim() }
            .replace(Regex(">\\s+<"), "><")
    }
    
    // CSS formatting
    private fun formatCss(css: String): String {
        try {
            return css
                .replace("{", " {\n  ")
                .replace("}", "\n}\n")
                .replace(";", ";\n  ")
                .replace("  \n}", "\n}")
                .trim()
        } catch (e: Exception) {
            return "❌ Format hatası: ${e.message}"
        }
    }
    
    private fun minifyCss(css: String): String {
        return css
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("\\s+"), " ")
            .replace(" {", "{")
            .replace("{ ", "{")
            .replace(" }", "}")
            .replace("} ", "}")
            .replace("; ", ";")
            .replace(": ", ":")
            .trim()
    }
    
    // JavaScript formatting
    private fun formatJavaScript(js: String): String {
        return formatBraceLanguage(js)
    }
    
    private fun minifyJavaScript(js: String): String {
        return js
            .replace(Regex("//.*"), "")
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines().joinToString(" ") { it.trim() }
            .replace(Regex("\\s+"), " ")
    }
    
    // SQL formatting
    private fun formatSql(sql: String): String {
        val keywords = listOf(
            "SELECT", "FROM", "WHERE", "AND", "OR", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER",
            "ON", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET", "INSERT", "INTO", "VALUES",
            "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "INDEX", "DROP", "ALTER", "ADD", "COLUMN"
        )
        
        var result = sql.uppercase()
        keywords.forEach { keyword ->
            result = result.replace(Regex("\\b$keyword\\b"), "\n$keyword")
        }
        return result.trim()
    }
    
    private fun minifySql(sql: String): String {
        return sql.lines().joinToString(" ") { it.trim() }
            .replace(Regex("\\s+"), " ")
    }
    
    // Brace-based language formatting (Kotlin, Java, JavaScript, etc.)
    private fun formatBraceLanguage(code: String): String {
        try {
            val sb = StringBuilder()
            var indent = 0
            var inString = false
            var stringChar: Char? = null
            var i = 0
            var lineStart = true
            
            while (i < code.length) {
                val char = code[i]
                val nextChar = if (i + 1 < code.length) code[i + 1] else null
                
                when {
                    // Handle strings
                    (char == '"' || char == '\'') && (i == 0 || code[i - 1] != '\\') -> {
                        if (!inString) {
                            inString = true
                            stringChar = char
                        } else if (char == stringChar) {
                            inString = false
                            stringChar = null
                        }
                        if (lineStart) {
                            sb.append("  ".repeat(indent))
                            lineStart = false
                        }
                        sb.append(char)
                    }
                    inString -> {
                        if (lineStart) {
                            sb.append("  ".repeat(indent))
                            lineStart = false
                        }
                        sb.append(char)
                    }
                    char == '{' -> {
                        if (lineStart) {
                            sb.append("  ".repeat(indent))
                            lineStart = false
                        }
                        sb.append(" {\n")
                        indent++
                        lineStart = true
                    }
                    char == '}' -> {
                        indent = maxOf(0, indent - 1)
                        if (!lineStart) sb.append("\n")
                        sb.append("  ".repeat(indent)).append("}\n")
                        lineStart = true
                    }
                    char == ';' -> {
                        if (lineStart) {
                            sb.append("  ".repeat(indent))
                            lineStart = false
                        }
                        sb.append(";\n")
                        lineStart = true
                    }
                    char == '\n' -> {
                        if (!lineStart) {
                            sb.append("\n")
                            lineStart = true
                        }
                    }
                    char.isWhitespace() -> {
                        if (!lineStart && nextChar?.isWhitespace() != true) {
                            sb.append(' ')
                        }
                    }
                    else -> {
                        if (lineStart) {
                            sb.append("  ".repeat(indent))
                            lineStart = false
                        }
                        sb.append(char)
                    }
                }
                i++
            }
            return sb.toString().trim()
        } catch (e: Exception) {
            return "❌ Format hatası: ${e.message}"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
