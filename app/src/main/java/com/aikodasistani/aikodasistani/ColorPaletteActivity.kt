package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColorPaletteActivity : AppCompatActivity() {
    
    private lateinit var colorPreview: View
    private lateinit var etHexColor: EditText
    private lateinit var sliderRed: SeekBar
    private lateinit var sliderGreen: SeekBar
    private lateinit var sliderBlue: SeekBar
    private lateinit var tvRGB: TextView
    private lateinit var tvHSL: TextView
    private lateinit var btnCopyHex: Button
    private lateinit var btnCopyRGB: Button
    private lateinit var btnGeneratePalette: Button
    private lateinit var rvPalette: RecyclerView
    private lateinit var rvPresets: RecyclerView
    private lateinit var spinnerPaletteType: Spinner
    
    private var currentColor = Color.parseColor("#6200EE")
    private var isUpdating = false
    
    private val presetColors = listOf(
        // Material Design Colors
        "#F44336" to "Red",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple",
        "#673AB7" to "Deep Purple",
        "#3F51B5" to "Indigo",
        "#2196F3" to "Blue",
        "#03A9F4" to "Light Blue",
        "#00BCD4" to "Cyan",
        "#009688" to "Teal",
        "#4CAF50" to "Green",
        "#8BC34A" to "Light Green",
        "#CDDC39" to "Lime",
        "#FFEB3B" to "Yellow",
        "#FFC107" to "Amber",
        "#FF9800" to "Orange",
        "#FF5722" to "Deep Orange",
        "#795548" to "Brown",
        "#9E9E9E" to "Grey",
        "#607D8B" to "Blue Grey",
        "#000000" to "Black",
        "#FFFFFF" to "White"
    )
    
    private val paletteTypes = listOf(
        "Complementary" to "Tamamlayıcı",
        "Analogous" to "Benzer",
        "Triadic" to "Üçlü",
        "Split-Complementary" to "Bölünmüş Tamamlayıcı",
        "Tetradic" to "Dörtlü",
        "Monochromatic" to "Tek Renk"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_palette)
        
        supportActionBar?.apply {
            title = "🎨 Renk Paleti"
            setDisplayHomeAsUpEnabled(true)
        }
        
        initViews()
        setupListeners()
        setupPresets()
        updateUI()
    }
    
    private fun initViews() {
        colorPreview = findViewById(R.id.colorPreview)
        etHexColor = findViewById(R.id.etHexColor)
        sliderRed = findViewById(R.id.sliderRed)
        sliderGreen = findViewById(R.id.sliderGreen)
        sliderBlue = findViewById(R.id.sliderBlue)
        tvRGB = findViewById(R.id.tvRGB)
        tvHSL = findViewById(R.id.tvHSL)
        btnCopyHex = findViewById(R.id.btnCopyHex)
        btnCopyRGB = findViewById(R.id.btnCopyRGB)
        btnGeneratePalette = findViewById(R.id.btnGeneratePalette)
        rvPalette = findViewById(R.id.rvPalette)
        rvPresets = findViewById(R.id.rvPresets)
        spinnerPaletteType = findViewById(R.id.spinnerPaletteType)
        
        // Setup palette type spinner
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            paletteTypes.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaletteType.adapter = adapter
        
        // Setup RecyclerViews
        rvPalette.layoutManager = GridLayoutManager(this, 5)
        rvPresets.layoutManager = GridLayoutManager(this, 7)
    }
    
    private fun setupListeners() {
        // Hex input
        etHexColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val hex = s.toString()
                if (hex.length == 6 || hex.length == 7) {
                    try {
                        val color = Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
                        currentColor = color
                        isUpdating = true
                        updateSlidersFromColor()
                        updateUI()
                        isUpdating = false
                    } catch (e: Exception) {
                        // Invalid color
                    }
                }
            }
        })
        
        // Sliders
        val sliderListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isUpdating || !fromUser) return
                updateColorFromSliders()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        
        sliderRed.setOnSeekBarChangeListener(sliderListener)
        sliderGreen.setOnSeekBarChangeListener(sliderListener)
        sliderBlue.setOnSeekBarChangeListener(sliderListener)
        
        // Buttons
        btnCopyHex.setOnClickListener {
            copyToClipboard(String.format("#%06X", 0xFFFFFF and currentColor))
        }
        
        btnCopyRGB.setOnClickListener {
            copyToClipboard("rgb(${currentColor.red}, ${currentColor.green}, ${currentColor.blue})")
        }
        
        btnGeneratePalette.setOnClickListener {
            generatePalette()
        }
        
        spinnerPaletteType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                generatePalette()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupPresets() {
        rvPresets.adapter = ColorAdapter(presetColors.map { it.first }) { hex ->
            try {
                currentColor = Color.parseColor(hex)
                isUpdating = true
                updateSlidersFromColor()
                etHexColor.setText(hex.replace("#", ""))
                updateUI()
                isUpdating = false
                generatePalette()
            } catch (e: Exception) {
                // Invalid color
            }
        }
    }
    
    private fun updateColorFromSliders() {
        currentColor = Color.rgb(sliderRed.progress, sliderGreen.progress, sliderBlue.progress)
        isUpdating = true
        etHexColor.setText(String.format("%06X", 0xFFFFFF and currentColor))
        updateUI()
        isUpdating = false
    }
    
    private fun updateSlidersFromColor() {
        sliderRed.progress = currentColor.red
        sliderGreen.progress = currentColor.green
        sliderBlue.progress = currentColor.blue
    }
    
    private fun updateUI() {
        colorPreview.setBackgroundColor(currentColor)
        
        // RGB
        tvRGB.text = "RGB: ${currentColor.red}, ${currentColor.green}, ${currentColor.blue}"
        
        // HSL
        val hsl = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor, hsl)
        tvHSL.text = "HSL: ${hsl[0].toInt()}°, ${(hsl[1] * 100).toInt()}%, ${(hsl[2] * 100).toInt()}%"
    }
    
    private fun generatePalette() {
        val paletteType = paletteTypes[spinnerPaletteType.selectedItemPosition].first
        val colors = when (paletteType) {
            "Complementary" -> generateComplementary()
            "Analogous" -> generateAnalogous()
            "Triadic" -> generateTriadic()
            "Split-Complementary" -> generateSplitComplementary()
            "Tetradic" -> generateTetradic()
            "Monochromatic" -> generateMonochromatic()
            else -> listOf(currentColor)
        }
        
        val hexColors = colors.map { String.format("#%06X", 0xFFFFFF and it) }
        rvPalette.adapter = ColorAdapter(hexColors) { hex ->
            copyToClipboard(hex)
        }
    }
    
    private fun generateComplementary(): List<Int> {
        val hsl = FloatArray(3)
        Color.colorToHSV(currentColor, hsl)
        
        val complementHsl = hsl.copyOf()
        complementHsl[0] = (hsl[0] + 180) % 360
        
        return listOf(currentColor, Color.HSVToColor(complementHsl))
    }
    
    private fun generateAnalogous(): List<Int> {
        val hsl = FloatArray(3)
        Color.colorToHSV(currentColor, hsl)
        
        return listOf(-30f, -15f, 0f, 15f, 30f).map { offset ->
            val newHsl = hsl.copyOf()
            newHsl[0] = (hsl[0] + offset + 360) % 360
            Color.HSVToColor(newHsl)
        }
    }
    
    private fun generateTriadic(): List<Int> {
        val hsl = FloatArray(3)
        Color.colorToHSV(currentColor, hsl)
        
        return listOf(0f, 120f, 240f).map { offset ->
            val newHsl = hsl.copyOf()
            newHsl[0] = (hsl[0] + offset) % 360
            Color.HSVToColor(newHsl)
        }
    }
    
    private fun generateSplitComplementary(): List<Int> {
        val hsl = FloatArray(3)
        Color.colorToHSV(currentColor, hsl)
        
        return listOf(0f, 150f, 210f).map { offset ->
            val newHsl = hsl.copyOf()
            newHsl[0] = (hsl[0] + offset) % 360
            Color.HSVToColor(newHsl)
        }
    }
    
    private fun generateTetradic(): List<Int> {
        val hsl = FloatArray(3)
        Color.colorToHSV(currentColor, hsl)
        
        return listOf(0f, 90f, 180f, 270f).map { offset ->
            val newHsl = hsl.copyOf()
            newHsl[0] = (hsl[0] + offset) % 360
            Color.HSVToColor(newHsl)
        }
    }
    
    private fun generateMonochromatic(): List<Int> {
        val hsl = FloatArray(3)
        Color.colorToHSV(currentColor, hsl)
        
        return listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).map { value ->
            val newHsl = hsl.copyOf()
            newHsl[2] = value
            Color.HSVToColor(newHsl)
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("color", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Kopyalandı: $text 🎨", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    // Inner adapter class
    inner class ColorAdapter(
        private val colors: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val colorView: View = view.findViewById(R.id.colorView)
            val tvHex: TextView = view.findViewById(R.id.tvHex)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_color, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val hex = colors[position]
            try {
                holder.colorView.setBackgroundColor(Color.parseColor(hex))
                holder.tvHex.text = hex
                holder.itemView.setOnClickListener { onClick(hex) }
            } catch (e: Exception) {
                // Invalid color
            }
        }
        
        override fun getItemCount() = colors.size
    }
}
