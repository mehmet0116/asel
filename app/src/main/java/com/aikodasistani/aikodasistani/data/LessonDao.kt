package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Lesson operations
 */
@Dao
interface LessonDao {
    
    @Query("SELECT * FROM lessons ORDER BY orderIndex ASC")
    fun getAllLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE category = :category ORDER BY orderIndex ASC")
    fun getLessonsByCategory(category: String): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE difficulty = :difficulty ORDER BY orderIndex ASC")
    fun getLessonsByDifficulty(difficulty: String): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE isCompleted = 0 ORDER BY orderIndex ASC")
    fun getPendingLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: Long): Lesson?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLessons(lessons: List<Lesson>)

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Query("UPDATE lessons SET progress = :progress WHERE id = :lessonId")
    suspend fun updateProgress(lessonId: Long, progress: Int)

    @Query("UPDATE lessons SET isCompleted = 1, completedAt = :completedAt, progress = 100 WHERE id = :lessonId")
    suspend fun markAsCompleted(lessonId: Long, completedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteLesson(lesson: Lesson)

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun getLessonCount(): Int

    @Query("SELECT COUNT(*) FROM lessons WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Query("SELECT SUM(duration) FROM lessons WHERE isCompleted = 1")
    suspend fun getTotalLearningMinutes(): Int?

    @Query("SELECT DISTINCT category FROM lessons")
    suspend fun getAllCategories(): List<String>
}
