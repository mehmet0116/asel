package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Entity for storing code snippets.
 * Users can save frequently used code blocks for quick access.
 */
@Entity(tableName = "snippets")
data class Snippet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "language")
    val language: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "tags")
    val tags: String? = null, // Comma-separated tags

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "usageCount")
    val usageCount: Int = 0,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get tags as a list
     */
    fun getTagsList(): List<String> {
        return tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    companion object {
        /**
         * Create a snippet from code content
         */
        fun fromCode(title: String, code: String, language: String? = null): Snippet {
            return Snippet(
                title = title,
                code = code,
                language = language
            )
        }
    }
}
