package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Coding Challenge operations
 */
@Dao
interface CodingChallengeDao {
    
    @Query("SELECT * FROM coding_challenges ORDER BY dateShown DESC")
    fun getAllChallenges(): Flow<List<CodingChallenge>>

    @Query("SELECT * FROM coding_challenges WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedChallenges(): Flow<List<CodingChallenge>>

    @Query("SELECT * FROM coding_challenges WHERE isCompleted = 0 ORDER BY dateShown DESC")
    fun getPendingChallenges(): Flow<List<CodingChallenge>>

    @Query("SELECT * FROM coding_challenges WHERE difficulty = :difficulty ORDER BY dateShown DESC")
    fun getChallengesByDifficulty(difficulty: String): Flow<List<CodingChallenge>>

    @Query("SELECT * FROM coding_challenges WHERE category = :category ORDER BY dateShown DESC")
    fun getChallengesByCategory(category: String): Flow<List<CodingChallenge>>

    @Query("SELECT * FROM coding_challenges WHERE dateShown = :date LIMIT 1")
    suspend fun getChallengeForDate(date: String): CodingChallenge?

    @Query("SELECT * FROM coding_challenges WHERE id = :challengeId")
    suspend fun getChallengeById(challengeId: Long): CodingChallenge?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: CodingChallenge): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChallenges(challenges: List<CodingChallenge>)

    @Update
    suspend fun updateChallenge(challenge: CodingChallenge)

    @Query("UPDATE coding_challenges SET isCompleted = 1, userSolution = :solution, completedAt = :completedAt WHERE id = :challengeId")
    suspend fun markAsCompleted(challengeId: Long, solution: String, completedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteChallenge(challenge: CodingChallenge)

    @Query("SELECT COUNT(*) FROM coding_challenges")
    suspend fun getChallengeCount(): Int

    @Query("SELECT COUNT(*) FROM coding_challenges WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM coding_challenges WHERE isCompleted = 1 AND completedAt >= :startOfDay")
    suspend fun getCompletedTodayCount(startOfDay: Long): Int
}
