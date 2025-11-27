package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeTemplateDao {
    @Query("SELECT * FROM code_templates ORDER BY usageCount DESC, createdAt DESC")
    fun getAllTemplates(): Flow<List<CodeTemplate>>
    
    @Query("SELECT * FROM code_templates WHERE isFavorite = 1 ORDER BY usageCount DESC")
    fun getFavoriteTemplates(): Flow<List<CodeTemplate>>
    
    @Query("SELECT * FROM code_templates WHERE isFavorite = 1 ORDER BY usageCount DESC")
    suspend fun getFavorites(): List<CodeTemplate>
    
    @Query("SELECT * FROM code_templates WHERE language = :language ORDER BY usageCount DESC")
    fun getByLanguage(language: String): Flow<List<CodeTemplate>>
    
    @Query("SELECT * FROM code_templates WHERE category = :category ORDER BY usageCount DESC")
    fun getByCategory(category: String): Flow<List<CodeTemplate>>
    
    @Query("SELECT * FROM code_templates WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<CodeTemplate>>
    
    @Query("SELECT * FROM code_templates WHERE id = :id")
    suspend fun getById(id: Long): CodeTemplate?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: CodeTemplate): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<CodeTemplate>)
    
    @Update
    suspend fun update(template: CodeTemplate)
    
    @Query("UPDATE code_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)
    
    @Query("UPDATE code_templates SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    
    @Delete
    suspend fun delete(template: CodeTemplate)
    
    @Query("DELETE FROM code_templates WHERE isBuiltIn = 0")
    suspend fun deleteCustomTemplates()
    
    @Query("SELECT COUNT(*) FROM code_templates")
    suspend fun getCount(): Int
    
    @Query("SELECT DISTINCT category FROM code_templates")
    suspend fun getCategories(): List<String>
    
    @Query("SELECT DISTINCT language FROM code_templates")
    suspend fun getLanguages(): List<String>
}
