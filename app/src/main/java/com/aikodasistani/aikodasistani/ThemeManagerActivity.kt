package com.aikodasistani.aikodasistani

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton

class ThemeManagerActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    
    private lateinit var radioGroupTheme: RadioGroup
    private lateinit var radioLight: RadioButton
    private lateinit var radioDark: RadioButton
    private lateinit var radioSystem: RadioButton
    
    private lateinit var accentColorContainer: LinearLayout
    private lateinit var primaryColorContainer: LinearLayout
    
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var fontSizeLabel: TextView
    
    private lateinit var previewCard: CardView
    private lateinit var previewText: TextView
    
    private val accentColors = listOf(
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#9C27B0", // Purple
        "#F44336", // Red
        "#FF9800", // Orange
        "#00BCD4", // Cyan
        "#E91E63", // Pink
        "#673AB7"  // Deep Purple
    )
    
    private val primaryColors = listOf(
        "#1976D2", // Blue
        "#388E3C", // Green
        "#7B1FA2", // Purple
        "#D32F2F", // Red
        "#F57C00", // Orange
        "#0097A7", // Cyan
        "#C2185B", // Pink
        "#512DA8"  // Deep Purple
    )
    
    private var selectedAccentColor = "#4CAF50"
    private var selectedPrimaryColor = "#1976D2"
    private var selectedFontSize = 1 // 0=small, 1=normal, 2=large
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_manager)
        
        prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        
        setupViews()
        loadCurrentSettings()
        setupListeners()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // Theme mode radio group
        radioGroupTheme = findViewById(R.id.radioGroupTheme)
        radioLight = findViewById(R.id.radioLight)
        radioDark = findViewById(R.id.radioDark)
        radioSystem = findViewById(R.id.radioSystem)
        
        // Color containers
        accentColorContainer = findViewById(R.id.accentColorContainer)
        primaryColorContainer = findViewById(R.id.primaryColorContainer)
        
        // Font size
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar)
        fontSizeLabel = findViewById(R.id.fontSizeLabel)
        
        // Preview
        previewCard = findViewById(R.id.previewCard)
        previewText = findViewById(R.id.previewText)
        
        // Save button
        findViewById<MaterialButton>(R.id.btnSaveTheme).setOnClickListener {
            saveSettings()
        }
        
        // Reset button
        findViewById<MaterialButton>(R.id.btnResetTheme).setOnClickListener {
            resetToDefaults()
        }
        
        // Setup color pickers
        setupColorPickers()
    }
    
    private fun setupColorPickers() {
        // Accent colors
        accentColorContainer.removeAllViews()
        accentColors.forEach { color ->
            val colorView = createColorView(color) { selectedColor ->
                selectedAccentColor = selectedColor
                updateAccentColorSelection()
                updatePreview()
            }
            accentColorContainer.addView(colorView)
        }
        
        // Primary colors
        primaryColorContainer.removeAllViews()
        primaryColors.forEach { color ->
            val colorView = createColorView(color) { selectedColor ->
                selectedPrimaryColor = selectedColor
                updatePrimaryColorSelection()
                updatePreview()
            }
            primaryColorContainer.addView(colorView)
        }
    }
    
    private fun createColorView(color: String, onClick: (String) -> Unit): View {
        val card = CardView(this).apply {
            val size = (40 * resources.displayMetrics.density).toInt()
            val margin = (4 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            radius = size / 2f
            cardElevation = 4f
            setCardBackgroundColor(android.graphics.Color.parseColor(color))
            setOnClickListener { onClick(color) }
        }
        return card
    }
    
    private fun loadCurrentSettings() {
        // Theme mode
        when (prefs.getString("theme_mode", "system")) {
            "light" -> radioLight.isChecked = true
            "dark" -> radioDark.isChecked = true
            else -> radioSystem.isChecked = true
        }
        
        // Colors
        selectedAccentColor = prefs.getString("accent_color", "#4CAF50") ?: "#4CAF50"
        selectedPrimaryColor = prefs.getString("primary_color", "#1976D2") ?: "#1976D2"
        
        // Font size
        selectedFontSize = prefs.getInt("font_size", 1)
        fontSizeSeekBar.progress = selectedFontSize
        updateFontSizeLabel()
        
        updateAccentColorSelection()
        updatePrimaryColorSelection()
        updatePreview()
    }
    
    private fun setupListeners() {
        radioGroupTheme.setOnCheckedChangeListener { _, _ ->
            updatePreview()
        }
        
        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedFontSize = progress
                updateFontSizeLabel()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updateFontSizeLabel() {
        fontSizeLabel.text = when (selectedFontSize) {
            0 -> getString(R.string.font_size_small)
            1 -> getString(R.string.font_size_normal)
            2 -> getString(R.string.font_size_large)
            else -> getString(R.string.font_size_normal)
        }
    }
    
    private fun updateAccentColorSelection() {
        for (i in 0 until accentColorContainer.childCount) {
            val view = accentColorContainer.getChildAt(i) as? CardView
            view?.let {
                val scale = if (accentColors.getOrNull(i) == selectedAccentColor) 1.2f else 1f
                it.scaleX = scale
                it.scaleY = scale
            }
        }
    }
    
    private fun updatePrimaryColorSelection() {
        for (i in 0 until primaryColorContainer.childCount) {
            val view = primaryColorContainer.getChildAt(i) as? CardView
            view?.let {
                val scale = if (primaryColors.getOrNull(i) == selectedPrimaryColor) 1.2f else 1f
                it.scaleX = scale
                it.scaleY = scale
            }
        }
    }
    
    private fun updatePreview() {
        previewCard.setCardBackgroundColor(android.graphics.Color.parseColor(selectedPrimaryColor))
        previewText.setTextColor(android.graphics.Color.parseColor(selectedAccentColor))
        
        val textSize = when (selectedFontSize) {
            0 -> 14f
            1 -> 16f
            2 -> 18f
            else -> 16f
        }
        previewText.textSize = textSize
    }
    
    private fun saveSettings() {
        val themeMode = when {
            radioLight.isChecked -> "light"
            radioDark.isChecked -> "dark"
            else -> "system"
        }
        
        prefs.edit().apply {
            putString("theme_mode", themeMode)
            putString("accent_color", selectedAccentColor)
            putString("primary_color", selectedPrimaryColor)
            putInt("font_size", selectedFontSize)
            apply()
        }
        
        // Apply theme mode
        when (themeMode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        
        Toast.makeText(this, getString(R.string.theme_saved), Toast.LENGTH_SHORT).show()
    }
    
    private fun resetToDefaults() {
        radioSystem.isChecked = true
        selectedAccentColor = "#4CAF50"
        selectedPrimaryColor = "#1976D2"
        selectedFontSize = 1
        fontSizeSeekBar.progress = 1
        
        updateAccentColorSelection()
        updatePrimaryColorSelection()
        updateFontSizeLabel()
        updatePreview()
        
        Toast.makeText(this, getString(R.string.theme_reset), Toast.LENGTH_SHORT).show()
    }
}
