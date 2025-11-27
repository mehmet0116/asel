package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Snippet operations
 */
@Dao
interface SnippetDao {
    
    @Query("SELECT * FROM snippets ORDER BY updatedAt DESC")
    fun getAllSnippets(): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteSnippets(): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets ORDER BY usageCount DESC LIMIT :limit")
    fun getMostUsedSnippets(limit: Int = 10): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets WHERE language = :language ORDER BY updatedAt DESC")
    fun getSnippetsByLanguage(language: String): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets WHERE title LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSnippets(query: String): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets WHERE id = :snippetId")
    suspend fun getSnippetById(snippetId: Long): Snippet?

    @Insert
    suspend fun insertSnippet(snippet: Snippet): Long

    @Update
    suspend fun updateSnippet(snippet: Snippet)

    @Delete
    suspend fun deleteSnippet(snippet: Snippet)

    @Query("DELETE FROM snippets WHERE id = :snippetId")
    suspend fun deleteSnippetById(snippetId: Long)

    @Query("UPDATE snippets SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :snippetId")
    suspend fun updateFavoriteStatus(snippetId: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE snippets SET usageCount = usageCount + 1, updatedAt = :updatedAt WHERE id = :snippetId")
    suspend fun incrementUsageCount(snippetId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT DISTINCT language FROM snippets WHERE language IS NOT NULL ORDER BY language")
    fun getAllLanguages(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM snippets")
    suspend fun getSnippetCount(): Int
}
