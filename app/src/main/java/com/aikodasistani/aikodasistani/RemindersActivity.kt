package com.aikodasistani.aikodasistani

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aikodasistani.aikodasistani.data.AppDatabase
import com.aikodasistani.aikodasistani.data.Reminder
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RemindersActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var reminderAdapter: ReminderAdapter
    
    private var allReminders = listOf<Reminder>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)
        
        database = AppDatabase.getDatabase(this)
        
        setupViews()
        loadReminders()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        recyclerView = findViewById(R.id.recyclerReminders)
        emptyView = findViewById(R.id.emptyView)
        
        // FAB
        findViewById<FloatingActionButton>(R.id.fabAddReminder).setOnClickListener {
            showAddReminderDialog()
        }
        
        // Setup RecyclerView
        reminderAdapter = ReminderAdapter(
            onToggleClick = { reminder, isEnabled -> toggleReminder(reminder, isEnabled) },
            onEditClick = { reminder -> showEditReminderDialog(reminder) },
            onDeleteClick = { reminder -> confirmDeleteReminder(reminder) }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RemindersActivity)
            adapter = reminderAdapter
        }
    }
    
    private fun loadReminders() {
        lifecycleScope.launch {
            database.reminderDao().getAllReminders().collectLatest { reminders ->
                allReminders = reminders
                reminderAdapter.submitList(reminders)
                updateEmptyState(reminders.isEmpty())
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
    
    private fun showAddReminderDialog(reminder: Reminder? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null)
        
        val etTitle = dialogView.findViewById<EditText>(R.id.etReminderTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etReminderDescription)
        val btnSelectTime = dialogView.findViewById<MaterialButton>(R.id.btnSelectTime)
        val tvSelectedTime = dialogView.findViewById<TextView>(R.id.tvSelectedTime)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerReminderType)
        val chipGroupDays = dialogView.findViewById<ChipGroup>(R.id.chipGroupDays)
        val spinnerFeature = dialogView.findViewById<Spinner>(R.id.spinnerLinkedFeature)
        
        var selectedTime = reminder?.time ?: "09:00"
        var selectedDays = reminder?.days ?: "1,2,3,4,5,6,7"
        
        // Pre-fill if editing
        reminder?.let {
            etTitle.setText(it.title)
            etDescription.setText(it.description)
            selectedTime = it.time
            selectedDays = it.days
        }
        
        tvSelectedTime.text = selectedTime
        
        // Setup type spinner
        val types = listOf(
            getString(R.string.reminder_type_daily),
            getString(R.string.reminder_type_weekly),
            getString(R.string.reminder_type_once)
        )
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        reminder?.let {
            spinnerType.setSelection(when (it.type) {
                "daily" -> 0
                "weekly" -> 1
                "once" -> 2
                else -> 0
            })
        }
        
        // Setup feature spinner
        val features = listOf(
            getString(R.string.reminder_feature_none),
            getString(R.string.reminder_feature_challenge),
            getString(R.string.reminder_feature_learning),
            getString(R.string.reminder_feature_practice)
        )
        spinnerFeature.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, features)
        
        // Setup day chips
        val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        val selectedDaysList = selectedDays.split(",").mapNotNull { it.toIntOrNull() }.toMutableSet()
        
        dayNames.forEachIndexed { index, dayName ->
            val chip = Chip(this).apply {
                text = dayName
                isCheckable = true
                isChecked = (index + 1) in selectedDaysList
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedDaysList.add(index + 1)
                    } else {
                        selectedDaysList.remove(index + 1)
                    }
                }
            }
            chipGroupDays.addView(chip)
        }
        
        // Time picker
        btnSelectTime.setOnClickListener {
            val parts = selectedTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                tvSelectedTime.text = selectedTime
            }, hour, minute, true).show()
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (reminder == null) getString(R.string.add_reminder) else getString(R.string.edit_reminder))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save_template)) { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                if (title.isNotEmpty()) {
                    val type = when (spinnerType.selectedItemPosition) {
                        0 -> "daily"
                        1 -> "weekly"
                        2 -> "once"
                        else -> "daily"
                    }
                    
                    val linkedFeature = when (spinnerFeature.selectedItemPosition) {
                        1 -> "daily_challenge"
                        2 -> "learning_hub"
                        3 -> "practice"
                        else -> null
                    }
                    
                    lifecycleScope.launch {
                        val newReminder = Reminder(
                            id = reminder?.id ?: 0,
                            title = title,
                            description = description,
                            type = type,
                            time = selectedTime,
                            days = selectedDaysList.sorted().joinToString(","),
                            isEnabled = reminder?.isEnabled ?: true,
                            linkedFeature = linkedFeature,
                            createdAt = reminder?.createdAt ?: System.currentTimeMillis()
                        )
                        val id = database.reminderDao().insertReminder(newReminder)
                        
                        // Schedule alarm if enabled
                        if (newReminder.isEnabled) {
                            scheduleReminder(newReminder.copy(id = id))
                        }
                        
                        Toast.makeText(this@RemindersActivity, 
                            getString(R.string.reminder_saved), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.reminder_title_required), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showEditReminderDialog(reminder: Reminder) {
        showAddReminderDialog(reminder)
    }
    
    private fun toggleReminder(reminder: Reminder, isEnabled: Boolean) {
        lifecycleScope.launch {
            database.reminderDao().setReminderEnabled(reminder.id, isEnabled)
            
            if (isEnabled) {
                scheduleReminder(reminder)
            } else {
                cancelReminder(reminder)
            }
        }
    }
    
    private fun confirmDeleteReminder(reminder: Reminder) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_reminder_title))
            .setMessage(getString(R.string.delete_reminder_confirm, reminder.title))
            .setPositiveButton(getString(R.string.delete_snippet_button)) { _, _ ->
                lifecycleScope.launch {
                    cancelReminder(reminder)
                    database.reminderDao().deleteReminder(reminder)
                    Toast.makeText(this@RemindersActivity, 
                        getString(R.string.reminder_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun scheduleReminder(reminder: Reminder) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_description", reminder.description)
            putExtra("linked_feature", reminder.linkedFeature)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            val timeParts = reminder.time.split(":")
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)
            
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        when (reminder.type) {
            "daily" -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            "weekly" -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7,
                    pendingIntent
                )
            }
            "once" -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
    
    private fun cancelReminder(reminder: Reminder) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    // Inner adapter class
    inner class ReminderAdapter(
        private val onToggleClick: (Reminder, Boolean) -> Unit,
        private val onEditClick: (Reminder) -> Unit,
        private val onDeleteClick: (Reminder) -> Unit
    ) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {
        
        private var reminders = listOf<Reminder>()
        
        fun submitList(newList: List<Reminder>) {
            reminders = newList
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ReminderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false)
            return ReminderViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
            holder.bind(reminders[position])
        }
        
        override fun getItemCount() = reminders.size
        
        inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle = itemView.findViewById<TextView>(R.id.tvReminderTitle)
            private val tvTime = itemView.findViewById<TextView>(R.id.tvReminderTime)
            private val tvType = itemView.findViewById<TextView>(R.id.tvReminderType)
            private val tvDays = itemView.findViewById<TextView>(R.id.tvReminderDays)
            private val switchEnabled = itemView.findViewById<SwitchMaterial>(R.id.switchEnabled)
            private val btnEdit = itemView.findViewById<ImageButton>(R.id.btnEditReminder)
            private val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteReminder)
            
            fun bind(reminder: Reminder) {
                tvTitle.text = reminder.title
                tvTime.text = "⏰ ${reminder.time}"
                
                tvType.text = when (reminder.type) {
                    "daily" -> getString(R.string.reminder_type_daily)
                    "weekly" -> getString(R.string.reminder_type_weekly)
                    "once" -> getString(R.string.reminder_type_once)
                    else -> reminder.type
                }
                
                val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
                val days = reminder.days.split(",").mapNotNull { 
                    it.toIntOrNull()?.let { day -> dayNames.getOrNull(day - 1) }
                }
                tvDays.text = if (days.size == 7) "Her gün" else days.joinToString(", ")
                
                switchEnabled.isChecked = reminder.isEnabled
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    onToggleClick(reminder, isChecked)
                }
                
                btnEdit.setOnClickListener { onEditClick(reminder) }
                btnDelete.setOnClickListener { onDeleteClick(reminder) }
            }
        }
    }
}

// Broadcast Receiver for reminders
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("reminder_title") ?: "Hatırlatma"
        val description = intent.getStringExtra("reminder_description") ?: ""
        val linkedFeature = intent.getStringExtra("linked_feature")
        
        // Create notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Create channel for Android 8+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "reminders",
                "Hatırlatmalar",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent for notification click
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            linkedFeature?.let { putExtra("open_feature", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            clickIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, "reminders")
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 $title")
            .setContentText(description)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
