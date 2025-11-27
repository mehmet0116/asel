package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>
    
    @Query("SELECT * FROM bookmarks WHERE category = :category ORDER BY createdAt DESC")
    fun getBookmarksByCategory(category: String): Flow<List<Bookmark>>
    
    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchBookmarks(query: String): Flow<List<Bookmark>>
    
    @Query("SELECT DISTINCT category FROM bookmarks ORDER BY category")
    suspend fun getAllCategories(): List<String>
    
    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: Long): Bookmark?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long
    
    @Update
    suspend fun updateBookmark(bookmark: Bookmark)
    
    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)
    
    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)
    
    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getBookmarkCount(): Int
    
    @Query("SELECT COUNT(*) FROM bookmarks WHERE category = :category")
    suspend fun getBookmarkCountByCategory(category: String): Int
}
