package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText

class LoremIpsumGeneratorActivity : AppCompatActivity() {

    private lateinit var chipGroupType: ChipGroup
    private lateinit var countSlider: Slider
    private lateinit var countLabel: TextView
    private lateinit var btnGenerate: MaterialButton
    private lateinit var btnCopy: MaterialButton
    private lateinit var outputText: TextInputEditText
    private lateinit var charCount: TextView
    private lateinit var wordCount: TextView

    private var generationType = "paragraphs"
    
    private val loremWords = listOf(
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
        "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore",
        "magna", "aliqua", "enim", "ad", "minim", "veniam", "quis", "nostrud",
        "exercitation", "ullamco", "laboris", "nisi", "aliquip", "ex", "ea", "commodo",
        "consequat", "duis", "aute", "irure", "in", "reprehenderit", "voluptate",
        "velit", "esse", "cillum", "fugiat", "nulla", "pariatur", "excepteur", "sint",
        "occaecat", "cupidatat", "non", "proident", "sunt", "culpa", "qui", "officia",
        "deserunt", "mollit", "anim", "id", "est", "laborum", "proin", "sapien",
        "massa", "congue", "viverra", "mauris", "sagittis", "lacus", "vel", "augue",
        "laoreet", "rutrum", "faucibus", "arcu", "dictum", "varius", "duis", "sodales",
        "neque", "eget", "tristique", "pellentesque", "habitant", "morbi", "senectus",
        "netus", "malesuada", "fames", "turpis", "egestas", "integer", "feugiat",
        "scelerisque", "varius", "morbi", "enim", "nunc", "faucibus", "interdum",
        "posuere", "lorem", "ipsum", "pharetra", "diam", "donec", "adipiscing"
    )

    private val classicLorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lorem_ipsum_generator)

        setupToolbar()
        initViews()
        setupListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.lorem_ipsum_generator)
    }

    private fun initViews() {
        chipGroupType = findViewById(R.id.chipGroupType)
        countSlider = findViewById(R.id.countSlider)
        countLabel = findViewById(R.id.countLabel)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnCopy = findViewById(R.id.btnCopy)
        outputText = findViewById(R.id.outputText)
        charCount = findViewById(R.id.charCount)
        wordCount = findViewById(R.id.wordCount)
    }

    private fun setupListeners() {
        // Generation type chips
        val types = listOf(
            "paragraphs" to getString(R.string.lorem_paragraphs),
            "sentences" to getString(R.string.lorem_sentences),
            "words" to getString(R.string.lorem_words)
        )

        types.forEach { (key, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = key == "paragraphs"
                tag = key
                setOnClickListener {
                    generationType = key
                    updateChipSelection()
                    updateSliderRange()
                }
            }
            chipGroupType.addView(chip)
        }

        countSlider.addOnChangeListener { _, value, _ ->
            updateCountLabel(value.toInt())
        }

        btnGenerate.setOnClickListener {
            generate()
        }

        btnCopy.setOnClickListener {
            copyToClipboard()
        }

        updateSliderRange()
    }

    private fun updateChipSelection() {
        for (i in 0 until chipGroupType.childCount) {
            val chip = chipGroupType.getChildAt(i) as? Chip
            chip?.isChecked = chip?.tag == generationType
        }
    }

    private fun updateSliderRange() {
        when (generationType) {
            "paragraphs" -> {
                countSlider.valueFrom = 1f
                countSlider.valueTo = 10f
                countSlider.value = 3f
            }
            "sentences" -> {
                countSlider.valueFrom = 1f
                countSlider.valueTo = 20f
                countSlider.value = 5f
            }
            "words" -> {
                countSlider.valueFrom = 5f
                countSlider.valueTo = 200f
                countSlider.value = 50f
            }
        }
        updateCountLabel(countSlider.value.toInt())
    }

    private fun updateCountLabel(count: Int) {
        val label = when (generationType) {
            "paragraphs" -> "$count paragraf"
            "sentences" -> "$count cümle"
            "words" -> "$count kelime"
            else -> "$count"
        }
        countLabel.text = label
    }

    private fun generate() {
        val count = countSlider.value.toInt()
        
        val result = when (generationType) {
            "paragraphs" -> generateParagraphs(count)
            "sentences" -> generateSentences(count)
            "words" -> generateWords(count)
            else -> ""
        }

        outputText.setText(result)
        updateStats(result)
    }

    private fun generateParagraphs(count: Int): String {
        val paragraphs = mutableListOf<String>()
        
        // First paragraph is classic
        if (count > 0) {
            paragraphs.add(classicLorem)
        }
        
        // Generate additional random paragraphs
        for (i in 1 until count) {
            paragraphs.add(generateRandomParagraph())
        }
        
        return paragraphs.joinToString("\n\n")
    }

    private fun generateRandomParagraph(): String {
        val sentenceCount = (4..8).random()
        val sentences = (0 until sentenceCount).map { generateRandomSentence() }
        return sentences.joinToString(" ")
    }

    private fun generateSentences(count: Int): String {
        val sentences = (0 until count).map { generateRandomSentence() }
        return sentences.joinToString(" ")
    }

    private fun generateRandomSentence(): String {
        val wordCount = (8..15).random()
        val words = (0 until wordCount).map { loremWords.random() }
        val sentence = words.joinToString(" ")
        return sentence.replaceFirstChar { it.uppercase() } + "."
    }

    private fun generateWords(count: Int): String {
        val words = (0 until count).map { loremWords.random() }
        // Capitalize first word
        return words.mapIndexed { index, word ->
            if (index == 0) word.replaceFirstChar { it.uppercase() } else word
        }.joinToString(" ")
    }

    private fun updateStats(text: String) {
        val chars = text.length
        val words = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        
        charCount.text = "$chars karakter"
        wordCount.text = "$words kelime"
    }

    private fun copyToClipboard() {
        val text = outputText.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Önce metin oluşturun", Toast.LENGTH_SHORT).show()
            return
        }
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Lorem Ipsum", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
