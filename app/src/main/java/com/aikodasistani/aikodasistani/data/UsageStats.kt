package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UsageStats entity for tracking app usage statistics
 */
@Entity(tableName = "usage_stats")
data class UsageStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD format
    val totalMessages: Int = 0,
    val codeGenerations: Int = 0,
    val voiceInputs: Int = 0,
    val snippetsSaved: Int = 0,
    val challengesCompleted: Int = 0,
    val lessonsCompleted: Int = 0,
    val templatesUsed: Int = 0,
    val toolsUsed: Int = 0,
    val playgroundRuns: Int = 0,
    val totalTokensUsed: Int = 0,
    val sessionDurationMinutes: Int = 0,
    val providersUsed: String? = null, // JSON array of provider names used
    val mostUsedFeature: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
