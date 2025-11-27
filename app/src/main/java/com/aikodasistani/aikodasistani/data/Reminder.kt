package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: String = "daily", // daily, weekly, once
    val time: String = "09:00", // HH:mm format
    val days: String = "1,2,3,4,5,6,7", // 1=Mon, 7=Sun
    val isEnabled: Boolean = true,
    val linkedFeature: String? = null, // daily_challenge, learning_hub, etc.
    val lastTriggered: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
