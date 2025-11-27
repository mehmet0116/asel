package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CronHelperActivity : AppCompatActivity() {

    private lateinit var minuteInput: EditText
    private lateinit var hourInput: EditText
    private lateinit var dayOfMonthInput: EditText
    private lateinit var monthInput: EditText
    private lateinit var dayOfWeekInput: EditText
    private lateinit var cronExpressionOutput: TextView
    private lateinit var humanReadableOutput: TextView
    private lateinit var presetsRecyclerView: RecyclerView
    private lateinit var copyButton: Button

    private val cronPresets = listOf(
        CronPreset("Her dakika", "* * * * *"),
        CronPreset("Her saat", "0 * * * *"),
        CronPreset("Her gün gece yarısı", "0 0 * * *"),
        CronPreset("Her gün sabah 6:00", "0 6 * * *"),
        CronPreset("Her gün akşam 18:00", "0 18 * * *"),
        CronPreset("Her Pazartesi 09:00", "0 9 * * 1"),
        CronPreset("Hafta içi her gün 09:00", "0 9 * * 1-5"),
        CronPreset("Hafta sonu her gün 10:00", "0 10 * * 0,6"),
        CronPreset("Ayın 1\'i 00:00", "0 0 1 * *"),
        CronPreset("Her 15 dakikada", "*/15 * * * *"),
        CronPreset("Her 30 dakikada", "*/30 * * * *"),
        CronPreset("Her 2 saatte", "0 */2 * * *"),
        CronPreset("Her 6 saatte", "0 */6 * * *"),
        CronPreset("Yılın ilk günü", "0 0 1 1 *"),
        CronPreset("Her Cuma 17:00", "0 17 * * 5")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cron_helper)

        setupToolbar()
        initViews()
        setupTextWatchers()
        setupPresetsRecyclerView()
        setupCopyButton()
        updateCronExpression()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.cron_helper)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        minuteInput = findViewById(R.id.minuteInput)
        hourInput = findViewById(R.id.hourInput)
        dayOfMonthInput = findViewById(R.id.dayOfMonthInput)
        monthInput = findViewById(R.id.monthInput)
        dayOfWeekInput = findViewById(R.id.dayOfWeekInput)
        cronExpressionOutput = findViewById(R.id.cronExpressionOutput)
        humanReadableOutput = findViewById(R.id.humanReadableOutput)
        presetsRecyclerView = findViewById(R.id.presetsRecyclerView)
        copyButton = findViewById(R.id.copyButton)
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCronExpression()
            }
        }

        minuteInput.addTextChangedListener(textWatcher)
        hourInput.addTextChangedListener(textWatcher)
        dayOfMonthInput.addTextChangedListener(textWatcher)
        monthInput.addTextChangedListener(textWatcher)
        dayOfWeekInput.addTextChangedListener(textWatcher)
    }

    private fun setupPresetsRecyclerView() {
        presetsRecyclerView.layoutManager = LinearLayoutManager(this)
        presetsRecyclerView.adapter = CronPresetAdapter(cronPresets) { preset ->
            applyPreset(preset)
        }
    }

    private fun setupCopyButton() {
        copyButton.setOnClickListener {
            val expression = cronExpressionOutput.text.toString()
            if (expression.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Cron Expression", expression)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyPreset(preset: CronPreset) {
        val parts = preset.expression.split(" ")
        if (parts.size == 5) {
            minuteInput.setText(parts[0])
            hourInput.setText(parts[1])
            dayOfMonthInput.setText(parts[2])
            monthInput.setText(parts[3])
            dayOfWeekInput.setText(parts[4])
        }
    }

    private fun updateCronExpression() {
        val minute = minuteInput.text.toString().ifEmpty { "*" }
        val hour = hourInput.text.toString().ifEmpty { "*" }
        val dayOfMonth = dayOfMonthInput.text.toString().ifEmpty { "*" }
        val month = monthInput.text.toString().ifEmpty { "*" }
        val dayOfWeek = dayOfWeekInput.text.toString().ifEmpty { "*" }

        val expression = "$minute $hour $dayOfMonth $month $dayOfWeek"
        cronExpressionOutput.text = expression
        humanReadableOutput.text = parseToHumanReadable(minute, hour, dayOfMonth, month, dayOfWeek)
    }

    private fun parseToHumanReadable(minute: String, hour: String, dayOfMonth: String, month: String, dayOfWeek: String): String {
        val sb = StringBuilder()
        
        // Minute parsing
        when {
            minute == "*" -> sb.append("Her dakika")
            minute.startsWith("*/") -> sb.append("Her ${minute.substring(2)} dakikada bir")
            minute.contains(",") -> sb.append("${minute.replace(",", ", ")}. dakikalarda")
            minute.contains("-") -> sb.append("${minute}. dakikalar arasında")
            else -> sb.append("$minute. dakikada")
        }

        // Hour parsing
        when {
            hour == "*" -> sb.append(", her saat")
            hour.startsWith("*/") -> sb.append(", her ${hour.substring(2)} saatte bir")
            hour.contains(",") -> sb.append(", ${hour.replace(",", ", ")} saatlerinde")
            hour.contains("-") -> sb.append(", ${hour} saatleri arasında")
            else -> sb.append(", saat $hour")
        }

        // Day of month parsing
        when {
            dayOfMonth == "*" -> { /* her gün */ }
            dayOfMonth.startsWith("*/") -> sb.append(", her ${dayOfMonth.substring(2)} günde bir")
            else -> sb.append(", ayın $dayOfMonth. günü")
        }

        // Month parsing
        val months = listOf("", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", 
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
        when {
            month == "*" -> { /* her ay */ }
            month.toIntOrNull() != null && month.toInt() in 1..12 -> sb.append(", ${months[month.toInt()]} ayında")
            else -> sb.append(", $month aylarında")
        }

        // Day of week parsing
        val days = listOf("Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi")
        when {
            dayOfWeek == "*" -> { /* her gün */ }
            dayOfWeek == "1-5" -> sb.append(", hafta içi")
            dayOfWeek == "0,6" || dayOfWeek == "6,0" -> sb.append(", hafta sonu")
            dayOfWeek.toIntOrNull() != null && dayOfWeek.toInt() in 0..6 -> sb.append(", ${days[dayOfWeek.toInt()]} günleri")
            else -> sb.append(", $dayOfWeek günlerinde")
        }

        return sb.toString().replaceFirst(", ", "").capitalize()
    }

    data class CronPreset(val description: String, val expression: String)

    inner class CronPresetAdapter(
        private val presets: List<CronPreset>,
        private val onPresetClick: (CronPreset) -> Unit
    ) : RecyclerView.Adapter<CronPresetAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val descriptionText: TextView = view.findViewById(R.id.presetDescription)
            val expressionText: TextView = view.findViewById(R.id.presetExpression)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cron_preset, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val preset = presets[position]
            holder.descriptionText.text = preset.description
            holder.expressionText.text = preset.expression
            holder.itemView.setOnClickListener { onPresetClick(preset) }
        }

        override fun getItemCount() = presets.size
    }
}
