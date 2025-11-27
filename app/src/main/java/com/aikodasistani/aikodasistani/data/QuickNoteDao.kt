package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<QuickNote>>
    
    @Query("SELECT * FROM quick_notes WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedNotes(): Flow<List<QuickNote>>
    
    @Query("SELECT * FROM quick_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<QuickNote>>
    
    @Query("SELECT * FROM quick_notes WHERE language = :language ORDER BY updatedAt DESC")
    fun getNotesByLanguage(language: String): Flow<List<QuickNote>>
    
    @Query("SELECT * FROM quick_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): QuickNote?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: QuickNote): Long
    
    @Update
    suspend fun updateNote(note: QuickNote)
    
    @Delete
    suspend fun deleteNote(note: QuickNote)
    
    @Query("DELETE FROM quick_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
    
    @Query("UPDATE quick_notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)
    
    @Query("SELECT COUNT(*) FROM quick_notes")
    suspend fun getNoteCount(): Int
}
