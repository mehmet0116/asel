package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class UnitConverterActivity : AppCompatActivity() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private lateinit var inputValue: TextInputEditText
    private lateinit var outputValue: TextView
    private lateinit var btnSwap: ImageButton
    private lateinit var formulaText: TextView

    private var currentCategory = "length"
    
    private val unitData = mapOf(
        "length" to listOf(
            "Metre (m)" to 1.0,
            "Kilometre (km)" to 1000.0,
            "Santimetre (cm)" to 0.01,
            "Milimetre (mm)" to 0.001,
            "Mil (mi)" to 1609.344,
            "Yard (yd)" to 0.9144,
            "Feet (ft)" to 0.3048,
            "İnç (in)" to 0.0254
        ),
        "weight" to listOf(
            "Kilogram (kg)" to 1.0,
            "Gram (g)" to 0.001,
            "Miligram (mg)" to 0.000001,
            "Ton (t)" to 1000.0,
            "Pound (lb)" to 0.453592,
            "Ons (oz)" to 0.0283495
        ),
        "temperature" to listOf(
            "Celsius (°C)" to 1.0,
            "Fahrenheit (°F)" to 1.0,
            "Kelvin (K)" to 1.0
        ),
        "speed" to listOf(
            "m/s" to 1.0,
            "km/h" to 0.277778,
            "mph" to 0.44704,
            "knot" to 0.514444
        ),
        "data" to listOf(
            "Byte (B)" to 1.0,
            "Kilobyte (KB)" to 1024.0,
            "Megabyte (MB)" to 1048576.0,
            "Gigabyte (GB)" to 1073741824.0,
            "Terabyte (TB)" to 1099511627776.0,
            "Bit (b)" to 0.125
        ),
        "area" to listOf(
            "Metrekare (m²)" to 1.0,
            "Kilometrekare (km²)" to 1000000.0,
            "Hektar (ha)" to 10000.0,
            "Dönüm" to 1000.0,
            "Feet kare (ft²)" to 0.092903,
            "Mil kare (mi²)" to 2589988.0
        ),
        "volume" to listOf(
            "Litre (L)" to 1.0,
            "Mililitre (mL)" to 0.001,
            "Metreküp (m³)" to 1000.0,
            "Galon (gal)" to 3.78541,
            "Pint (pt)" to 0.473176
        ),
        "time" to listOf(
            "Saniye (s)" to 1.0,
            "Dakika (min)" to 60.0,
            "Saat (h)" to 3600.0,
            "Gün (d)" to 86400.0,
            "Hafta (w)" to 604800.0,
            "Yıl (y)" to 31536000.0
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unit_converter)

        setupToolbar()
        initViews()
        setupCategoryChips()
        setupSpinners()
        setupListeners()
        updateCategory("length")
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.unit_converter)
    }

    private fun initViews() {
        chipGroup = findViewById(R.id.chipGroupCategories)
        fromSpinner = findViewById(R.id.spinnerFrom)
        toSpinner = findViewById(R.id.spinnerTo)
        inputValue = findViewById(R.id.inputValue)
        outputValue = findViewById(R.id.outputValue)
        btnSwap = findViewById(R.id.btnSwap)
        formulaText = findViewById(R.id.formulaText)
    }

    private fun setupCategoryChips() {
        val categories = listOf(
            "length" to getString(R.string.unit_length),
            "weight" to getString(R.string.unit_weight),
            "temperature" to getString(R.string.unit_temperature),
            "speed" to getString(R.string.unit_speed),
            "data" to getString(R.string.unit_data),
            "area" to getString(R.string.unit_area),
            "volume" to getString(R.string.unit_volume),
            "time" to getString(R.string.unit_time)
        )

        categories.forEach { (key, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = key == "length"
                tag = key
                setOnClickListener {
                    updateCategory(key)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateCategory(category: String) {
        currentCategory = category
        
        // Update chip selection
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.isChecked = chip?.tag == category
        }

        setupSpinners()
        convert()
    }

    private fun setupSpinners() {
        val units = unitData[currentCategory]?.map { it.first } ?: emptyList()
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        fromSpinner.adapter = adapter
        toSpinner.adapter = adapter
        
        if (units.size > 1) {
            toSpinner.setSelection(1)
        }
    }

    private fun setupListeners() {
        inputValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                convert()
            }
        })

        fromSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                convert()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        toSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                convert()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSwap.setOnClickListener {
            val fromPos = fromSpinner.selectedItemPosition
            val toPos = toSpinner.selectedItemPosition
            fromSpinner.setSelection(toPos)
            toSpinner.setSelection(fromPos)
        }
    }

    private fun convert() {
        val input = inputValue.text.toString().toDoubleOrNull() ?: 0.0
        val fromUnit = fromSpinner.selectedItem?.toString() ?: return
        val toUnit = toSpinner.selectedItem?.toString() ?: return

        val result = if (currentCategory == "temperature") {
            convertTemperature(input, fromUnit, toUnit)
        } else {
            val fromFactor = unitData[currentCategory]?.find { it.first == fromUnit }?.second ?: 1.0
            val toFactor = unitData[currentCategory]?.find { it.first == toUnit }?.second ?: 1.0
            (input * fromFactor) / toFactor
        }

        outputValue.text = formatResult(result)
        formulaText.text = "$input $fromUnit = ${formatResult(result)} $toUnit"
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        // Convert to Celsius first
        val celsius = when {
            from.contains("Celsius") -> value
            from.contains("Fahrenheit") -> (value - 32) * 5 / 9
            from.contains("Kelvin") -> value - 273.15
            else -> value
        }

        // Convert from Celsius to target
        return when {
            to.contains("Celsius") -> celsius
            to.contains("Fahrenheit") -> celsius * 9 / 5 + 32
            to.contains("Kelvin") -> celsius + 273.15
            else -> celsius
        }
    }

    private fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.6f", value).trimEnd('0').trimEnd('.')
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
