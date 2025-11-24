package com.aikodasistani.aikodasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "archived_messages")
data class ArchivedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sessionId")
    val sessionId: Long,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "isSentByUser")
    val isSentByUser: Boolean,

    @ColumnInfo(name = "language")
    val language: String? = null,

    @ColumnInfo(name = "isCode")
    val isCode: Boolean = false,

    @ColumnInfo(name = "codeContent")
    val codeContent: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)