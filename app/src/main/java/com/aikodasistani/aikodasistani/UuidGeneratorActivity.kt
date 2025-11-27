package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*

class UuidGeneratorActivity : AppCompatActivity() {

    private lateinit var generateButton: Button
    private lateinit var countSlider: SeekBar
    private lateinit var countLabel: TextView
    private lateinit var formatChipGroup: ChipGroup
    private lateinit var uuidRecyclerView: RecyclerView
    private lateinit var copyAllButton: Button
    private lateinit var clearButton: Button
    
    private val generatedUuids = mutableListOf<String>()
    private var selectedFormat = "standard"
    private var generateCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uuid_generator)

        supportActionBar?.apply {
            title = getString(R.string.uuid_generator)
            setDisplayHomeAsUpEnabled(true)
        }

        initViews()
        setupListeners()
        setupFormats()
    }

    private fun initViews() {
        generateButton = findViewById(R.id.generateButton)
        countSlider = findViewById(R.id.countSlider)
        countLabel = findViewById(R.id.countLabel)
        formatChipGroup = findViewById(R.id.formatChipGroup)
        uuidRecyclerView = findViewById(R.id.uuidRecyclerView)
        copyAllButton = findViewById(R.id.copyAllButton)
        clearButton = findViewById(R.id.clearButton)
        
        uuidRecyclerView.layoutManager = LinearLayoutManager(this)
        countLabel.text = "1 adet"
    }

    private fun setupListeners() {
        countSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                generateCount = progress + 1 // 1 to 20
                countLabel.text = "$generateCount adet"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        generateButton.setOnClickListener {
            generateUuids()
        }

        copyAllButton.setOnClickListener {
            copyAllToClipboard()
        }

        clearButton.setOnClickListener {
            generatedUuids.clear()
            updateRecyclerView()
        }
    }

    private fun setupFormats() {
        val formats = listOf(
            "standard" to "Standart (8-4-4-4-12)",
            "uppercase" to "Büyük Harf",
            "no_hyphens" to "Tiresiz",
            "braces" to "{} Parantezli",
            "urn" to "URN Formatı"
        )

        formats.forEachIndexed { index, (id, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = index == 0
                tag = id
                setOnClickListener {
                    selectedFormat = id
                }
            }
            formatChipGroup.addView(chip)
        }
    }

    private fun generateUuids() {
        repeat(generateCount) {
            val uuid = UUID.randomUUID()
            val formatted = formatUuid(uuid)
            generatedUuids.add(0, formatted)
        }
        
        // Keep only last 50
        while (generatedUuids.size > 50) {
            generatedUuids.removeAt(generatedUuids.size - 1)
        }
        
        updateRecyclerView()
        Toast.makeText(this, getString(R.string.uuid_generated, generateCount), Toast.LENGTH_SHORT).show()
    }

    private fun formatUuid(uuid: UUID): String {
        val standard = uuid.toString()
        return when (selectedFormat) {
            "uppercase" -> standard.uppercase()
            "no_hyphens" -> standard.replace("-", "")
            "braces" -> "{$standard}"
            "urn" -> "urn:uuid:$standard"
            else -> standard
        }
    }

    private fun updateRecyclerView() {
        uuidRecyclerView.adapter = UuidAdapter(generatedUuids) { uuid ->
            copyToClipboard(uuid)
        }
        copyAllButton.isEnabled = generatedUuids.isNotEmpty()
        clearButton.isEnabled = generatedUuids.isNotEmpty()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UUID", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun copyAllToClipboard() {
        if (generatedUuids.isEmpty()) return
        val allUuids = generatedUuids.joinToString("\n")
        copyToClipboard(allUuids)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Inner Adapter class
    inner class UuidAdapter(
        private val uuids: List<String>,
        private val onCopyClick: (String) -> Unit
    ) : RecyclerView.Adapter<UuidAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val uuidText: TextView = itemView.findViewById(R.id.uuidText)
            val copyButton: ImageButton = itemView.findViewById(R.id.copyButton)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_uuid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uuid = uuids[position]
            holder.uuidText.text = uuid
            holder.copyButton.setOnClickListener { onCopyClick(uuid) }
            holder.itemView.setOnClickListener { onCopyClick(uuid) }
        }

        override fun getItemCount() = uuids.size
    }
}
