package com.aikodasistani.aikodasistani.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastAccessed DESC")
    fun getAllProjects(): Flow<List<Project>>
    
    @Query("SELECT * FROM projects ORDER BY lastAccessed DESC")
    suspend fun getAllProjectsList(): List<Project>
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): Project?
    
    @Query("SELECT * FROM projects WHERE language = :language ORDER BY lastAccessed DESC")
    fun getProjectsByLanguage(language: String): Flow<List<Project>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long
    
    @Update
    suspend fun updateProject(project: Project)
    
    @Delete
    suspend fun deleteProject(project: Project)
    
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
    
    @Query("UPDATE projects SET lastAccessed = :time WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, time: Long)
    
    @Query("UPDATE projects SET snippetsCount = snippetsCount + 1 WHERE id = :id")
    suspend fun incrementSnippetsCount(id: Long)
    
    @Query("UPDATE projects SET templatesCount = templatesCount + 1 WHERE id = :id")
    suspend fun incrementTemplatesCount(id: Long)
    
    @Query("UPDATE projects SET notesCount = notesCount + 1 WHERE id = :id")
    suspend fun incrementNotesCount(id: Long)
    
    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int
}
