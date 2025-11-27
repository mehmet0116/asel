package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageStatsDao {
    @Query("SELECT * FROM usage_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<UsageStats>>
    
    @Query("SELECT * FROM usage_stats WHERE date = :date")
    suspend fun getByDate(date: String): UsageStats?
    
    @Query("SELECT * FROM usage_stats ORDER BY date DESC LIMIT :limit")
    fun getRecentStats(limit: Int): Flow<List<UsageStats>>
    
    @Query("SELECT * FROM usage_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getStatsInRange(startDate: String, endDate: String): Flow<List<UsageStats>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: UsageStats): Long
    
    @Update
    suspend fun update(stats: UsageStats)
    
    @Query("UPDATE usage_stats SET totalMessages = totalMessages + 1 WHERE date = :date")
    suspend fun incrementMessages(date: String)
    
    @Query("UPDATE usage_stats SET codeGenerations = codeGenerations + 1 WHERE date = :date")
    suspend fun incrementCodeGenerations(date: String)
    
    @Query("UPDATE usage_stats SET voiceInputs = voiceInputs + 1 WHERE date = :date")
    suspend fun incrementVoiceInputs(date: String)
    
    @Query("UPDATE usage_stats SET snippetsSaved = snippetsSaved + 1 WHERE date = :date")
    suspend fun incrementSnippetsSaved(date: String)
    
    @Query("UPDATE usage_stats SET challengesCompleted = challengesCompleted + 1 WHERE date = :date")
    suspend fun incrementChallengesCompleted(date: String)
    
    @Query("UPDATE usage_stats SET lessonsCompleted = lessonsCompleted + 1 WHERE date = :date")
    suspend fun incrementLessonsCompleted(date: String)
    
    @Query("UPDATE usage_stats SET templatesUsed = templatesUsed + 1 WHERE date = :date")
    suspend fun incrementTemplatesUsed(date: String)
    
    @Query("UPDATE usage_stats SET toolsUsed = toolsUsed + 1 WHERE date = :date")
    suspend fun incrementToolsUsed(date: String)
    
    @Query("UPDATE usage_stats SET playgroundRuns = playgroundRuns + 1 WHERE date = :date")
    suspend fun incrementPlaygroundRuns(date: String)
    
    // Aggregate queries
    @Query("SELECT SUM(totalMessages) FROM usage_stats")
    suspend fun getTotalMessages(): Int?
    
    @Query("SELECT SUM(codeGenerations) FROM usage_stats")
    suspend fun getTotalCodeGenerations(): Int?
    
    @Query("SELECT SUM(voiceInputs) FROM usage_stats")
    suspend fun getTotalVoiceInputs(): Int?
    
    @Query("SELECT SUM(challengesCompleted) FROM usage_stats")
    suspend fun getTotalChallengesCompleted(): Int?
    
    @Query("SELECT SUM(lessonsCompleted) FROM usage_stats")
    suspend fun getTotalLessonsCompleted(): Int?
    
    @Query("SELECT COUNT(DISTINCT date) FROM usage_stats")
    suspend fun getTotalActiveDays(): Int
    
    @Query("SELECT AVG(totalMessages) FROM usage_stats")
    suspend fun getAverageMessagesPerDay(): Float?
    
    @Delete
    suspend fun delete(stats: UsageStats)
    
    @Query("DELETE FROM usage_stats WHERE date < :beforeDate")
    suspend fun deleteOldStats(beforeDate: String)
}
