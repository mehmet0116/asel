package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class NumberBaseConverterActivity : AppCompatActivity() {

    private lateinit var inputNumber: TextInputEditText
    private lateinit var chipGroupBase: ChipGroup
    private lateinit var binaryResult: TextView
    private lateinit var octalResult: TextView
    private lateinit var decimalResult: TextView
    private lateinit var hexResult: TextView
    private lateinit var asciiResult: TextView
    private lateinit var binaryCard: MaterialCardView
    private lateinit var octalCard: MaterialCardView
    private lateinit var decimalCard: MaterialCardView
    private lateinit var hexCard: MaterialCardView
    private lateinit var asciiCard: MaterialCardView

    private var currentBase = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_base_converter)

        setupToolbar()
        initViews()
        setupListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.number_base_converter)
    }

    private fun initViews() {
        inputNumber = findViewById(R.id.inputNumber)
        chipGroupBase = findViewById(R.id.chipGroupBase)
        binaryResult = findViewById(R.id.binaryResult)
        octalResult = findViewById(R.id.octalResult)
        decimalResult = findViewById(R.id.decimalResult)
        hexResult = findViewById(R.id.hexResult)
        asciiResult = findViewById(R.id.asciiResult)
        binaryCard = findViewById(R.id.binaryCard)
        octalCard = findViewById(R.id.octalCard)
        decimalCard = findViewById(R.id.decimalCard)
        hexCard = findViewById(R.id.hexCard)
        asciiCard = findViewById(R.id.asciiCard)
    }

    private fun setupListeners() {
        // Base selection chips
        val bases = listOf(
            "Binary (2)" to 2,
            "Octal (8)" to 8,
            "Decimal (10)" to 10,
            "Hex (16)" to 16
        )

        bases.forEach { (name, base) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = base == 10
                setOnClickListener {
                    currentBase = base
                    updateChipSelection()
                    updateHint()
                    convert()
                }
            }
            chipGroupBase.addView(chip)
        }

        inputNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                convert()
            }
        })

        // Copy on card click
        binaryCard.setOnClickListener { copyToClipboard("Binary", binaryResult.text.toString()) }
        octalCard.setOnClickListener { copyToClipboard("Octal", octalResult.text.toString()) }
        decimalCard.setOnClickListener { copyToClipboard("Decimal", decimalResult.text.toString()) }
        hexCard.setOnClickListener { copyToClipboard("Hex", hexResult.text.toString()) }
        asciiCard.setOnClickListener { copyToClipboard("ASCII", asciiResult.text.toString()) }
    }

    private fun updateChipSelection() {
        for (i in 0 until chipGroupBase.childCount) {
            val chip = chipGroupBase.getChildAt(i) as? Chip
            chip?.isChecked = when (chip?.text) {
                "Binary (2)" -> currentBase == 2
                "Octal (8)" -> currentBase == 8
                "Decimal (10)" -> currentBase == 10
                "Hex (16)" -> currentBase == 16
                else -> false
            }
        }
    }

    private fun updateHint() {
        inputNumber.hint = when (currentBase) {
            2 -> "Örn: 1010"
            8 -> "Örn: 755"
            10 -> "Örn: 255"
            16 -> "Örn: FF"
            else -> "Sayı girin"
        }
    }

    private fun convert() {
        val input = inputNumber.text.toString().trim()
        
        if (input.isEmpty()) {
            clearResults()
            return
        }

        try {
            // Parse the input based on current base
            val decimalValue = when (currentBase) {
                2 -> input.toLong(2)
                8 -> input.toLong(8)
                10 -> input.toLong(10)
                16 -> input.toLong(16)
                else -> input.toLong(10)
            }

            // Convert to all bases
            binaryResult.text = decimalValue.toString(2)
            octalResult.text = decimalValue.toString(8)
            decimalResult.text = decimalValue.toString(10)
            hexResult.text = decimalValue.toString(16).uppercase()
            
            // Convert to ASCII (if in printable range)
            asciiResult.text = if (decimalValue in 32..126) {
                "'${decimalValue.toInt().toChar()}'"
            } else if (decimalValue in 0..255) {
                buildString {
                    val binary = decimalValue.toString(2).padStart(8, '0')
                    append(binary)
                    if (decimalValue in 32..126) {
                        append(" = '${decimalValue.toInt().toChar()}'")
                    }
                }
            } else {
                "N/A"
            }

        } catch (e: NumberFormatException) {
            clearResults()
            Toast.makeText(this, getString(R.string.invalid_number_format), Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearResults() {
        binaryResult.text = "-"
        octalResult.text = "-"
        decimalResult.text = "-"
        hexResult.text = "-"
        asciiResult.text = "-"
    }

    private fun copyToClipboard(label: String, text: String) {
        if (text == "-" || text.isEmpty()) return
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label kopyalandı", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
