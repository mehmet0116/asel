package com.aikodasistani.aikodasistani

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip

/**
 * Code Playground Activity - Run JavaScript and HTML code in a sandbox
 */
class CodePlaygroundActivity : AppCompatActivity() {

    private lateinit var etCodeInput: EditText
    private lateinit var tvOutput: TextView
    private lateinit var webViewPreview: WebView
    private lateinit var btnRun: Button
    private lateinit var btnClearOutput: Button
    private lateinit var chipJavaScript: Chip
    private lateinit var chipHtml: Chip

    private var currentLanguage = Language.JAVASCRIPT

    enum class Language {
        JAVASCRIPT, HTML
    }

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_LANGUAGE = "language"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_playground)

        setupToolbar()
        initializeViews()
        setupLanguageChips()
        setupButtons()
        setupWebView()

        // Handle incoming code
        handleIncomingCode()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initializeViews() {
        etCodeInput = findViewById(R.id.etCodeInput)
        tvOutput = findViewById(R.id.tvOutput)
        webViewPreview = findViewById(R.id.webViewPreview)
        btnRun = findViewById(R.id.btnRun)
        btnClearOutput = findViewById(R.id.btnClearOutput)
        chipJavaScript = findViewById(R.id.chipJavaScript)
        chipHtml = findViewById(R.id.chipHtml)
    }

    private fun setupLanguageChips() {
        chipJavaScript.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentLanguage = Language.JAVASCRIPT
                updateUIForLanguage()
            }
        }

        chipHtml.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentLanguage = Language.HTML
                updateUIForLanguage()
            }
        }
    }

    private fun updateUIForLanguage() {
        when (currentLanguage) {
            Language.JAVASCRIPT -> {
                tvOutput.visibility = View.VISIBLE
                webViewPreview.visibility = View.GONE
                etCodeInput.hint = getString(R.string.js_code_hint)
            }
            Language.HTML -> {
                tvOutput.visibility = View.GONE
                webViewPreview.visibility = View.VISIBLE
                etCodeInput.hint = getString(R.string.html_code_hint)
            }
        }
    }

    private fun setupButtons() {
        btnRun.setOnClickListener {
            val code = etCodeInput.text.toString()
            if (code.isBlank()) {
                Toast.makeText(this, R.string.enter_code, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            runCode(code)
        }

        btnClearOutput.setOnClickListener {
            tvOutput.text = ""
            webViewPreview.loadUrl("about:blank")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webViewPreview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
        }
        
        webViewPreview.webViewClient = WebViewClient()
        webViewPreview.webChromeClient = WebChromeClient()
        
        // Add JavaScript interface for console.log
        webViewPreview.addJavascriptInterface(ConsoleInterface(), "AndroidConsole")
    }

    private fun runCode(code: String) {
        when (currentLanguage) {
            Language.JAVASCRIPT -> runJavaScript(code)
            Language.HTML -> runHtml(code)
        }
    }

    private fun runJavaScript(code: String) {
        // Clear previous output
        tvOutput.text = ""
        
        // Create HTML wrapper with console capture
        val htmlWrapper = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <script>
                    // Capture console.log
                    var output = [];
                    var originalConsoleLog = console.log;
                    console.log = function() {
                        var args = Array.prototype.slice.call(arguments);
                        var message = args.map(function(arg) {
                            if (typeof arg === 'object') {
                                try {
                                    return JSON.stringify(arg, null, 2);
                                } catch (e) {
                                    return String(arg);
                                }
                            }
                            return String(arg);
                        }).join(' ');
                        output.push(message);
                        AndroidConsole.log(message);
                    };
                    
                    // Capture errors
                    window.onerror = function(message, source, lineno, colno, error) {
                        AndroidConsole.error("Hata: " + message + " (satır " + lineno + ")");
                        return true;
                    };
                </script>
            </head>
            <body>
                <script>
                    try {
                        $code
                    } catch (e) {
                        AndroidConsole.error("❌ " + e.name + ": " + e.message);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        
        // Load in hidden WebView to execute
        val tempWebView = WebView(this)
        tempWebView.settings.javaScriptEnabled = true
        tempWebView.addJavascriptInterface(ConsoleInterface(), "AndroidConsole")
        tempWebView.loadDataWithBaseURL(null, htmlWrapper, "text/html", "UTF-8", null)
    }

    private fun runHtml(code: String) {
        // Load HTML directly in WebView
        webViewPreview.loadDataWithBaseURL(null, code, "text/html", "UTF-8", null)
    }

    private fun handleIncomingCode() {
        intent.getStringExtra(EXTRA_CODE)?.let { code ->
            etCodeInput.setText(code)
        }
        
        intent.getStringExtra(EXTRA_LANGUAGE)?.let { lang ->
            when (lang.lowercase()) {
                "javascript", "js" -> {
                    chipJavaScript.isChecked = true
                    currentLanguage = Language.JAVASCRIPT
                }
                "html" -> {
                    chipHtml.isChecked = true
                    currentLanguage = Language.HTML
                }
            }
            updateUIForLanguage()
        }
    }

    inner class ConsoleInterface {
        @JavascriptInterface
        fun log(message: String) {
            runOnUiThread {
                val currentText = tvOutput.text.toString()
                tvOutput.text = if (currentText.isEmpty() || currentText == getString(R.string.output_placeholder)) {
                    "📤 $message"
                } else {
                    "$currentText\n📤 $message"
                }
            }
        }

        @JavascriptInterface
        fun error(message: String) {
            runOnUiThread {
                val currentText = tvOutput.text.toString()
                tvOutput.text = if (currentText.isEmpty() || currentText == getString(R.string.output_placeholder)) {
                    message
                } else {
                    "$currentText\n$message"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewPreview.destroy()
    }
}
