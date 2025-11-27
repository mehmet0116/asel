package com.aikodasistani.aikodasistani.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders ORDER BY time ASC")
    suspend fun getAllRemindersList(): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY time ASC")
    suspend fun getEnabledReminders(): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?
    
    @Query("SELECT * FROM reminders WHERE type = :type ORDER BY time ASC")
    fun getRemindersByType(type: String): Flow<List<Reminder>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
    
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
    
    @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setReminderEnabled(id: Long, enabled: Boolean)
    
    @Query("UPDATE reminders SET lastTriggered = :time WHERE id = :id")
    suspend fun updateLastTriggered(id: Long, time: Long)
    
    @Query("SELECT COUNT(*) FROM reminders WHERE isEnabled = 1")
    suspend fun getEnabledCount(): Int
}
