package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val language: String = "kotlin",
    val color: String = "#4CAF50",
    val filesCount: Int = 0,
    val snippetsCount: Int = 0,
    val templatesCount: Int = 0,
    val notesCount: Int = 0,
    val lastAccessed: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
