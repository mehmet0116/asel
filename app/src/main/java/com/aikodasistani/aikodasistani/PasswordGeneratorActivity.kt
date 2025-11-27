package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.security.SecureRandom

class PasswordGeneratorActivity : AppCompatActivity() {
    
    private lateinit var tvGeneratedPassword: TextView
    private lateinit var sliderLength: SeekBar
    private lateinit var tvLength: TextView
    private lateinit var cbUppercase: CheckBox
    private lateinit var cbLowercase: CheckBox
    private lateinit var cbNumbers: CheckBox
    private lateinit var cbSymbols: CheckBox
    private lateinit var cbAvoidAmbiguous: CheckBox
    private lateinit var btnGenerate: Button
    private lateinit var btnCopy: Button
    private lateinit var tvStrength: TextView
    private lateinit var progressStrength: ProgressBar
    private lateinit var historyContainer: LinearLayout
    
    private val passwordHistory = mutableListOf<String>()
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        private const val NUMBERS = "0123456789"
        private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        private const val AMBIGUOUS = "0O1lI"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_generator)
        
        supportActionBar?.apply {
            title = "🔐 Şifre Üretici"
            setDisplayHomeAsUpEnabled(true)
        }
        
        initViews()
        setupListeners()
        generatePassword()
    }
    
    private fun initViews() {
        tvGeneratedPassword = findViewById(R.id.tvGeneratedPassword)
        sliderLength = findViewById(R.id.sliderLength)
        tvLength = findViewById(R.id.tvLength)
        cbUppercase = findViewById(R.id.cbUppercase)
        cbLowercase = findViewById(R.id.cbLowercase)
        cbNumbers = findViewById(R.id.cbNumbers)
        cbSymbols = findViewById(R.id.cbSymbols)
        cbAvoidAmbiguous = findViewById(R.id.cbAvoidAmbiguous)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnCopy = findViewById(R.id.btnCopy)
        tvStrength = findViewById(R.id.tvStrength)
        progressStrength = findViewById(R.id.progressStrength)
        historyContainer = findViewById(R.id.historyContainer)
        
        // Varsayılan değerler
        sliderLength.progress = 16
        tvLength.text = "Uzunluk: 16"
        cbUppercase.isChecked = true
        cbLowercase.isChecked = true
        cbNumbers.isChecked = true
        cbSymbols.isChecked = true
    }
    
    private fun setupListeners() {
        sliderLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val length = maxOf(8, progress)
                tvLength.text = "Uzunluk: $length"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                generatePassword()
            }
        })
        
        val checkboxListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            // En az bir seçenek seçili olmalı
            if (!cbUppercase.isChecked && !cbLowercase.isChecked && 
                !cbNumbers.isChecked && !cbSymbols.isChecked) {
                cbLowercase.isChecked = true
            }
            generatePassword()
        }
        
        cbUppercase.setOnCheckedChangeListener(checkboxListener)
        cbLowercase.setOnCheckedChangeListener(checkboxListener)
        cbNumbers.setOnCheckedChangeListener(checkboxListener)
        cbSymbols.setOnCheckedChangeListener(checkboxListener)
        cbAvoidAmbiguous.setOnCheckedChangeListener(checkboxListener)
        
        btnGenerate.setOnClickListener {
            generatePassword()
        }
        
        btnCopy.setOnClickListener {
            copyToClipboard(tvGeneratedPassword.text.toString())
        }
    }
    
    private fun generatePassword() {
        val length = maxOf(8, sliderLength.progress)
        var charset = ""
        
        if (cbUppercase.isChecked) charset += UPPERCASE
        if (cbLowercase.isChecked) charset += LOWERCASE
        if (cbNumbers.isChecked) charset += NUMBERS
        if (cbSymbols.isChecked) charset += SYMBOLS
        
        if (cbAvoidAmbiguous.isChecked) {
            charset = charset.filter { it !in AMBIGUOUS }
        }
        
        if (charset.isEmpty()) {
            charset = LOWERCASE
        }
        
        val password = StringBuilder()
        repeat(length) {
            password.append(charset[secureRandom.nextInt(charset.length)])
        }
        
        val generatedPassword = password.toString()
        tvGeneratedPassword.text = generatedPassword
        
        // Şifre gücünü hesapla
        calculateStrength(generatedPassword)
        
        // Geçmişe ekle
        addToHistory(generatedPassword)
    }
    
    private fun calculateStrength(password: String): Int {
        var score = 0
        
        // Uzunluk puanı
        score += when {
            password.length >= 16 -> 25
            password.length >= 12 -> 20
            password.length >= 10 -> 15
            password.length >= 8 -> 10
            else -> 5
        }
        
        // Karakter çeşitliliği
        if (password.any { it.isUpperCase() }) score += 20
        if (password.any { it.isLowerCase() }) score += 15
        if (password.any { it.isDigit() }) score += 20
        if (password.any { !it.isLetterOrDigit() }) score += 20
        
        progressStrength.progress = score
        
        val (strengthText: String, color: Int) = when {
            score >= 80 -> Pair("Çok Güçlü 💪", R.color.difficulty_hard)
            score >= 60 -> Pair("Güçlü ✅", R.color.difficulty_medium)
            score >= 40 -> Pair("Orta ⚠️", R.color.warning)
            else -> Pair("Zayıf ❌", R.color.difficulty_easy)
        }
        
        tvStrength.text = strengthText
        tvStrength.setTextColor(ContextCompat.getColor(this, color))
        
        return score
    }
    
    private fun addToHistory(password: String) {
        if (passwordHistory.contains(password)) return
        
        passwordHistory.add(0, password)
        if (passwordHistory.size > 5) {
            passwordHistory.removeAt(passwordHistory.size - 1)
        }
        
        updateHistoryUI()
    }
    
    private fun updateHistoryUI() {
        historyContainer.removeAllViews()
        
        passwordHistory.forEach { password ->
            val itemView = layoutInflater.inflate(R.layout.item_password_history, historyContainer, false)
            val tvPassword = itemView.findViewById<TextView>(R.id.tvPassword)
            val btnCopyHistory = itemView.findViewById<ImageButton>(R.id.btnCopyHistory)
            
            tvPassword.text = password
            btnCopyHistory.setOnClickListener {
                copyToClipboard(password)
            }
            
            historyContainer.addView(itemView)
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("password", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Şifre kopyalandı! 🔐", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
