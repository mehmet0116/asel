package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiTesterActivity : AppCompatActivity() {

    private lateinit var urlInput: TextInputEditText
    private lateinit var methodChipGroup: ChipGroup
    private lateinit var headersInput: TextInputEditText
    private lateinit var bodyInput: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var responseOutput: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var responseTimeText: TextView
    private lateinit var bodyContainer: LinearLayout

    private var currentMethod = "GET"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_tester)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        urlInput = findViewById(R.id.urlInput)
        methodChipGroup = findViewById(R.id.methodChipGroup)
        headersInput = findViewById(R.id.headersInput)
        bodyInput = findViewById(R.id.bodyInput)
        sendButton = findViewById(R.id.sendButton)
        responseOutput = findViewById(R.id.responseOutput)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        responseTimeText = findViewById(R.id.responseTimeText)
        bodyContainer = findViewById(R.id.bodyContainer)

        // Toolbar setup
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        // Default URL
        urlInput.setText("https://jsonplaceholder.typicode.com/posts/1")
        
        // Default headers
        headersInput.setText("Content-Type: application/json\nAccept: application/json")
    }

    private fun setupListeners() {
        methodChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = findViewById<Chip>(checkedIds[0])
                currentMethod = chip.text.toString()
                
                // Body only visible for POST, PUT, PATCH
                bodyContainer.visibility = if (currentMethod in listOf("POST", "PUT", "PATCH")) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        sendButton.setOnClickListener {
            sendRequest()
        }

        // Add common API buttons
        findViewById<Button>(R.id.btnExample1).setOnClickListener {
            urlInput.setText("https://jsonplaceholder.typicode.com/posts")
        }
        findViewById<Button>(R.id.btnExample2).setOnClickListener {
            urlInput.setText("https://jsonplaceholder.typicode.com/users")
        }
        findViewById<Button>(R.id.btnExample3).setOnClickListener {
            urlInput.setText("https://api.github.com/users/octocat")
        }
    }

    private fun sendRequest() {
        val url = urlInput.text?.toString()?.trim() ?: ""
        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.api_url_required), Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        sendButton.isEnabled = false
        responseOutput.text = ""
        statusText.text = ""
        responseTimeText.text = ""

        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val result = withContext(Dispatchers.IO) {
                    makeRequest(url)
                }
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                responseTimeText.text = getString(R.string.response_time, responseTime)
                statusText.text = result.first
                statusText.setTextColor(
                    if (result.first.startsWith("2")) getColor(R.color.status_success)
                    else if (result.first.startsWith("4") || result.first.startsWith("5")) getColor(R.color.status_error)
                    else getColor(R.color.status_warning)
                )
                responseOutput.text = formatJson(result.second)

            } catch (e: Exception) {
                statusText.text = "Error"
                statusText.setTextColor(getColor(R.color.status_error))
                responseOutput.text = "❌ ${e.message}"
            } finally {
                progressBar.visibility = View.GONE
                sendButton.isEnabled = true
            }
        }
    }

    private fun makeRequest(urlString: String): Pair<String, String> {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = currentMethod
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Parse and set headers
            val headers = headersInput.text?.toString() ?: ""
            headers.lines().forEach { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    connection.setRequestProperty(parts[0].trim(), parts[1].trim())
                }
            }

            // Send body for POST, PUT, PATCH
            if (currentMethod in listOf("POST", "PUT", "PATCH")) {
                connection.doOutput = true
                val body = bodyInput.text?.toString() ?: ""
                if (body.isNotEmpty()) {
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(body)
                        writer.flush()
                    }
                }
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            val inputStream = if (responseCode >= 400) {
                connection.errorStream ?: connection.inputStream
            } else {
                connection.inputStream
            }

            val response = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }

            return "$responseCode $responseMessage" to response

        } finally {
            connection.disconnect()
        }
    }

    private fun formatJson(text: String): String {
        return try {
            when {
                text.trim().startsWith("[") -> JSONArray(text).toString(2)
                text.trim().startsWith("{") -> JSONObject(text).toString(2)
                else -> text
            }
        } catch (e: Exception) {
            text
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
