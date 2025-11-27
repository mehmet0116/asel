package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bookmark entity for saving AI responses
 */
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Genel",
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val sessionId: Long = -1,
    val messageId: Long = -1
)
