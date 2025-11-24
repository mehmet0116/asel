package com.aikodasistani.aikodasistani.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): Session?

    @Insert
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("SELECT * FROM archived_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<ArchivedMessage>

    @Insert
    suspend fun insertMessage(message: ArchivedMessage): Long

    @Update
    suspend fun updateMessage(message: ArchivedMessage)

    @Query("SELECT * FROM archived_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): ArchivedMessage?

    // Transaction ile session ve mesajlarını silme
    @Transaction
    suspend fun deleteSessionAndMessages(sessionId: Long) {
        // Önce mesajları sil
        deleteMessagesBySessionId(sessionId)
        // Sonra session'ı sil
        deleteSessionById(sessionId)
    }

    @Query("DELETE FROM archived_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySessionId(sessionId: Long)
}