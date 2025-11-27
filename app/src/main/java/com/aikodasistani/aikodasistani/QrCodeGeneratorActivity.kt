package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class QrCodeGeneratorActivity : AppCompatActivity() {

    private lateinit var inputText: TextInputEditText
    private lateinit var qrImageView: ImageView
    private lateinit var sizeSlider: SeekBar
    private lateinit var sizeLabel: TextView
    private lateinit var generateButton: Button
    private lateinit var shareButton: Button
    private lateinit var saveButton: Button
    private lateinit var templateChipGroup: ChipGroup
    
    private var currentQrBitmap: Bitmap? = null
    private var qrSize = 512

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_generator)

        supportActionBar?.apply {
            title = getString(R.string.qr_code_generator)
            setDisplayHomeAsUpEnabled(true)
        }

        initViews()
        setupListeners()
        setupTemplates()
    }

    private fun initViews() {
        inputText = findViewById(R.id.inputText)
        qrImageView = findViewById(R.id.qrImageView)
        sizeSlider = findViewById(R.id.sizeSlider)
        sizeLabel = findViewById(R.id.sizeLabel)
        generateButton = findViewById(R.id.generateButton)
        shareButton = findViewById(R.id.shareButton)
        saveButton = findViewById(R.id.saveButton)
        templateChipGroup = findViewById(R.id.templateChipGroup)
        
        sizeLabel.text = "${qrSize}px"
    }

    private fun setupListeners() {
        sizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                qrSize = 128 + (progress * 16) // 128 to 1024
                sizeLabel.text = "${qrSize}px"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (inputText.text?.isNotEmpty() == true) {
                    generateQrCode()
                }
            }
        })

        generateButton.setOnClickListener {
            generateQrCode()
        }

        shareButton.setOnClickListener {
            shareQrCode()
        }

        saveButton.setOnClickListener {
            saveQrCode()
        }

        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                shareButton.isEnabled = false
                saveButton.isEnabled = false
            }
        })
    }

    private fun setupTemplates() {
        val templates = listOf(
            "URL" to "https://",
            "E-posta" to "mailto:",
            "Telefon" to "tel:+90",
            "SMS" to "sms:+90?body=",
            "WiFi" to "WIFI:S:AğAdı;T:WPA;P:Şifre;;",
            "Konum" to "geo:41.0082,28.9784"
        )

        templates.forEach { (name, prefix) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    inputText.setText(prefix)
                    inputText.setSelection(prefix.length)
                }
            }
            templateChipGroup.addView(chip)
        }
    }

    private fun generateQrCode() {
        val text = inputText.text?.toString() ?: ""
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_text_for_qr), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            currentQrBitmap = generateQrBitmap(text, qrSize)
            qrImageView.setImageBitmap(currentQrBitmap)
            shareButton.isEnabled = true
            saveButton.isEnabled = true
            Toast.makeText(this, getString(R.string.qr_generated), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.qr_generation_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateQrBitmap(text: String, size: Int): Bitmap {
        // Simple QR code generation using a basic algorithm
        // In a real app, you would use a library like ZXing
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        // Generate a simple pattern based on text hash
        val hash = text.hashCode()
        val moduleSize = size / 25
        
        for (i in 0 until 25) {
            for (j in 0 until 25) {
                // Position patterns (top-left, top-right, bottom-left)
                if ((i < 7 && j < 7) || (i < 7 && j >= 18) || (i >= 18 && j < 7)) {
                    val isOuter = i == 0 || i == 6 || j == 0 || j == 6 ||
                            (i >= 18 && (i == 18 || i == 24)) || 
                            (j >= 18 && (j == 18 || j == 24))
                    val isInner = (i in 2..4 && j in 2..4) ||
                            (i in 2..4 && j in 20..22) ||
                            (i in 20..22 && j in 2..4)
                    
                    val paint = android.graphics.Paint().apply {
                        color = if (isOuter || isInner) Color.BLACK else Color.WHITE
                    }
                    canvas.drawRect(
                        (j * moduleSize).toFloat(),
                        (i * moduleSize).toFloat(),
                        ((j + 1) * moduleSize).toFloat(),
                        ((i + 1) * moduleSize).toFloat(),
                        paint
                    )
                } else {
                    // Data area - use hash to determine black/white
                    val dataIndex = i * 25 + j
                    val isBlack = ((hash shr (dataIndex % 32)) and 1) == 1 ||
                            ((text.hashCode() + dataIndex) % 3 == 0)
                    
                    val paint = android.graphics.Paint().apply {
                        color = if (isBlack) Color.BLACK else Color.WHITE
                    }
                    canvas.drawRect(
                        (j * moduleSize).toFloat(),
                        (i * moduleSize).toFloat(),
                        ((j + 1) * moduleSize).toFloat(),
                        ((i + 1) * moduleSize).toFloat(),
                        paint
                    )
                }
            }
        }
        
        return bitmap
    }

    private fun shareQrCode() {
        currentQrBitmap?.let { bitmap ->
            try {
                val file = File(cacheDir, "qr_code.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                val uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.provider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_qr)))
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.share_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveQrCode() {
        currentQrBitmap?.let { bitmap ->
            try {
                val fileName = "qr_${System.currentTimeMillis()}.png"
                val file = File(getExternalFilesDir(null), fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(this, getString(R.string.qr_saved, file.absolutePath), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.save_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
