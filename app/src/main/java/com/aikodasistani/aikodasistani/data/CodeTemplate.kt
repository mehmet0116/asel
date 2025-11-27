package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Code Template entity for storing pre-made code templates
 */
@Entity(tableName = "code_templates")
data class CodeTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val code: String,
    val language: String,
    val category: String, // e.g., "android", "algorithm", "web", "api", "database"
    val tags: String? = null,
    val variables: String? = null, // JSON array of replaceable variables e.g., [{"name": "className", "placeholder": "MyClass"}]
    val usageCount: Int = 0,
    val isFavorite: Boolean = false,
    val isBuiltIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
