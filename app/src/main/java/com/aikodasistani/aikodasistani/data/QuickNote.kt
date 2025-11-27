package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * QuickNote entity for quick note-taking with code highlighting
 */
@Entity(tableName = "quick_notes")
data class QuickNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val language: String? = null, // For code highlighting
    val color: String = "#FFFFFF", // Note background color
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
