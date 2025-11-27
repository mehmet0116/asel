package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.UsageStats
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    
    // Summary stats views
    private lateinit var tvTotalMessages: TextView
    private lateinit var tvTotalCodeGen: TextView
    private lateinit var tvTotalVoiceInputs: TextView
    private lateinit var tvActiveDays: TextView
    private lateinit var tvChallengesCompleted: TextView
    private lateinit var tvLessonsCompleted: TextView
    
    // Today's stats
    private lateinit var tvTodayMessages: TextView
    private lateinit var tvTodayCode: TextView
    private lateinit var tvTodayVoice: TextView
    private lateinit var tvTodayTools: TextView
    private lateinit var tvTodayPlayground: TextView
    private lateinit var tvTodaySnippets: TextView
    
    // Weekly chart placeholder
    private lateinit var rvWeeklyStats: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        database = AppDatabase.getDatabase(this)
        
        setupViews()
        loadStatistics()
        ensureTodayStats()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // Summary stats
        tvTotalMessages = findViewById(R.id.tvTotalMessages)
        tvTotalCodeGen = findViewById(R.id.tvTotalCodeGen)
        tvTotalVoiceInputs = findViewById(R.id.tvTotalVoiceInputs)
        tvActiveDays = findViewById(R.id.tvActiveDays)
        tvChallengesCompleted = findViewById(R.id.tvChallengesCompleted)
        tvLessonsCompleted = findViewById(R.id.tvLessonsCompleted)
        
        // Today's stats
        tvTodayMessages = findViewById(R.id.tvTodayMessages)
        tvTodayCode = findViewById(R.id.tvTodayCode)
        tvTodayVoice = findViewById(R.id.tvTodayVoice)
        tvTodayTools = findViewById(R.id.tvTodayTools)
        tvTodayPlayground = findViewById(R.id.tvTodayPlayground)
        tvTodaySnippets = findViewById(R.id.tvTodaySnippets)
        
        // Weekly RecyclerView
        rvWeeklyStats = findViewById(R.id.rvWeeklyStats)
        rvWeeklyStats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }
    
    private fun loadStatistics() {
        lifecycleScope.launch {
            // Load total statistics
            val totalMessages = database.usageStatsDao().getTotalMessages() ?: 0
            val totalCodeGen = database.usageStatsDao().getTotalCodeGenerations() ?: 0
            val totalVoice = database.usageStatsDao().getTotalVoiceInputs() ?: 0
            val activeDays = database.usageStatsDao().getTotalActiveDays()
            val challenges = database.usageStatsDao().getTotalChallengesCompleted() ?: 0
            val lessons = database.usageStatsDao().getTotalLessonsCompleted() ?: 0
            
            tvTotalMessages.text = formatNumber(totalMessages)
            tvTotalCodeGen.text = formatNumber(totalCodeGen)
            tvTotalVoiceInputs.text = formatNumber(totalVoice)
            tvActiveDays.text = formatNumber(activeDays)
            tvChallengesCompleted.text = formatNumber(challenges)
            tvLessonsCompleted.text = formatNumber(lessons)
            
            // Load today's stats
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayStats = database.usageStatsDao().getByDate(today)
            
            todayStats?.let {
                tvTodayMessages.text = it.totalMessages.toString()
                tvTodayCode.text = it.codeGenerations.toString()
                tvTodayVoice.text = it.voiceInputs.toString()
                tvTodayTools.text = it.toolsUsed.toString()
                tvTodayPlayground.text = it.playgroundRuns.toString()
                tvTodaySnippets.text = it.snippetsSaved.toString()
            }
            
            // Load weekly stats
            database.usageStatsDao().getRecentStats(7).collectLatest { weeklyStats ->
                updateWeeklyChart(weeklyStats)
            }
        }
    }
    
    private fun updateWeeklyChart(stats: List<UsageStats>) {
        val adapter = WeeklyStatsAdapter(stats)
        rvWeeklyStats.adapter = adapter
    }
    
    private fun ensureTodayStats() {
        lifecycleScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existing = database.usageStatsDao().getByDate(today)
            if (existing == null) {
                database.usageStatsDao().insert(UsageStats(date = today))
            }
        }
    }
    
    private fun formatNumber(num: Int): String {
        return when {
            num >= 1000000 -> String.format(Locale.getDefault(), "%.1fM", num / 1000000.0)
            num >= 1000 -> String.format(Locale.getDefault(), "%.1fK", num / 1000.0)
            else -> num.toString()
        }
    }
    
    // Simple adapter for weekly stats
    inner class WeeklyStatsAdapter(private val stats: List<UsageStats>) : 
        RecyclerView.Adapter<WeeklyStatsAdapter.ViewHolder>() {
        
        inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val tvDay: TextView = itemView.findViewById(R.id.tvDay)
            val tvCount: TextView = itemView.findViewById(R.id.tvCount)
            val barView: android.view.View = itemView.findViewById(R.id.barView)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_weekly_stat, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val stat = stats[position]
            
            // Parse date to get day name
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(stat.date)
                val dayFormat = SimpleDateFormat("EEE", Locale("tr"))
                holder.tvDay.text = date?.let { dayFormat.format(it) } ?: stat.date.takeLast(2)
            } catch (e: Exception) {
                holder.tvDay.text = stat.date.takeLast(2)
            }
            
            holder.tvCount.text = stat.totalMessages.toString()
            
            // Calculate bar height (max 100dp)
            val maxMessages = stats.maxOfOrNull { it.totalMessages } ?: 1
            val heightPercent = if (maxMessages > 0) stat.totalMessages.toFloat() / maxMessages else 0f
            val maxHeightDp = 100
            val heightDp = (maxHeightDp * heightPercent).toInt().coerceAtLeast(4)
            
            val params = holder.barView.layoutParams
            params.height = (heightDp * holder.itemView.context.resources.displayMetrics.density).toInt()
            holder.barView.layoutParams = params
        }
        
        override fun getItemCount() = stats.size
    }
}
