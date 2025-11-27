package com.aikodasistani.aikodasistani

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TimestampConverterActivity : AppCompatActivity() {
    
    private lateinit var etTimestamp: EditText
    private lateinit var btnToDate: Button
    private lateinit var tvDateResult: TextView
    private lateinit var btnPickDate: Button
    private lateinit var tvTimestampResult: TextView
    private lateinit var btnNow: Button
    private lateinit var tvCurrentTimestamp: TextView
    private lateinit var rvCommonFormats: RecyclerView
    
    private val dateFormats = listOf(
        "yyyy-MM-dd HH:mm:ss" to "ISO 8601",
        "dd/MM/yyyy HH:mm:ss" to "TR Formatı",
        "MM/dd/yyyy HH:mm:ss" to "US Formatı",
        "EEE, dd MMM yyyy HH:mm:ss" to "RFC 2822",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to "ISO 8601 (Mili)",
        "dd MMMM yyyy, HH:mm" to "Okunabilir",
        "EEEE, dd MMMM yyyy" to "Tam Gün",
        "HH:mm:ss" to "Sadece Saat"
    )
    
    private var selectedDate: Calendar = Calendar.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timestamp_converter)
        
        supportActionBar?.apply {
            title = "⏱️ Timestamp Converter"
            setDisplayHomeAsUpEnabled(true)
        }
        
        initViews()
        setupListeners()
        updateCurrentTimestamp()
    }
    
    private fun initViews() {
        etTimestamp = findViewById(R.id.etTimestamp)
        btnToDate = findViewById(R.id.btnToDate)
        tvDateResult = findViewById(R.id.tvDateResult)
        btnPickDate = findViewById(R.id.btnPickDate)
        tvTimestampResult = findViewById(R.id.tvTimestampResult)
        btnNow = findViewById(R.id.btnNow)
        tvCurrentTimestamp = findViewById(R.id.tvCurrentTimestamp)
        rvCommonFormats = findViewById(R.id.rvCommonFormats)
        
        rvCommonFormats.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupListeners() {
        btnToDate.setOnClickListener {
            convertTimestampToDate()
        }
        
        btnPickDate.setOnClickListener {
            showDateTimePicker()
        }
        
        btnNow.setOnClickListener {
            val now = System.currentTimeMillis()
            etTimestamp.setText(now.toString())
            convertTimestampToDate()
            updateCurrentTimestamp()
        }
        
        tvCurrentTimestamp.setOnClickListener {
            copyToClipboard(tvCurrentTimestamp.text.toString().replace("Şu anki: ", ""))
        }
        
        tvDateResult.setOnClickListener {
            copyToClipboard(tvDateResult.text.toString())
        }
        
        tvTimestampResult.setOnClickListener {
            copyToClipboard(tvTimestampResult.text.toString())
        }
    }
    
    private fun convertTimestampToDate() {
        val input = etTimestamp.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(this, "Timestamp girin", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val timestamp = input.toLong()
            // Eğer saniye cinsinden ise (10 basamak) milisaniyeye çevir
            val millis = if (timestamp < 10000000000L) timestamp * 1000 else timestamp
            
            val date = Date(millis)
            selectedDate.time = date
            
            updateDateFormats(date)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Geçersiz timestamp", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateDateFormats(date: Date) {
        val results = StringBuilder()
        val locale = Locale("tr", "TR")
        
        dateFormats.forEach { (format, name) ->
            try {
                val sdf = SimpleDateFormat(format, locale)
                sdf.timeZone = TimeZone.getDefault()
                results.append("$name:\n${sdf.format(date)}\n\n")
            } catch (e: Exception) {
                // Format uyumsuzluğu, atla
            }
        }
        
        tvDateResult.text = results.toString().trim()
        
        // RecyclerView için format listesi güncelle
        updateFormatsRecyclerView(date)
    }
    
    private fun updateFormatsRecyclerView(date: Date) {
        val items = mutableListOf<FormatItem>()
        val locale = Locale("tr", "TR")
        
        dateFormats.forEach { (format, name) ->
            try {
                val sdf = SimpleDateFormat(format, locale)
                sdf.timeZone = TimeZone.getDefault()
                items.add(FormatItem(name, sdf.format(date)))
            } catch (e: Exception) {
                // Skip
            }
        }
        
        // Unix timestamps
        items.add(FormatItem("Unix (Saniye)", (date.time / 1000).toString()))
        items.add(FormatItem("Unix (Milisaniye)", date.time.toString()))
        
        rvCommonFormats.adapter = FormatAdapter(items) { text ->
            copyToClipboard(text)
        }
    }
    
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                selectedDate = calendar
                val timestamp = calendar.timeInMillis
                tvTimestampResult.text = "Unix (ms): $timestamp\nUnix (s): ${timestamp / 1000}"
                
                etTimestamp.setText(timestamp.toString())
                updateDateFormats(calendar.time)
                
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    
    private fun updateCurrentTimestamp() {
        val now = System.currentTimeMillis()
        tvCurrentTimestamp.text = "Şu anki: $now"
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("timestamp", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Kopyalandı! ⏱️", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    // Inner class for format items
    data class FormatItem(val name: String, val value: String)
    
    // Adapter for formats
    inner class FormatAdapter(
        private val items: List<FormatItem>,
        private val onCopy: (String) -> Unit
    ) : RecyclerView.Adapter<FormatAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvFormatName)
            val tvValue: TextView = view.findViewById(R.id.tvFormatValue)
            val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_format, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvValue.text = item.value
            holder.btnCopy.setOnClickListener { onCopy(item.value) }
            holder.itemView.setOnClickListener { onCopy(item.value) }
        }
        
        override fun getItemCount() = items.size
    }
}
