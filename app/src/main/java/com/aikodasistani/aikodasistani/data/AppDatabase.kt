package com.aikodasistani.aikodasistani.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [Session::class, ArchivedMessage::class, Snippet::class, CodingChallenge::class, Lesson::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun snippetDao(): SnippetDao
    abstract fun codingChallengeDao(): CodingChallengeDao
    abstract fun lessonDao(): LessonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2 - adds snippets table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS snippets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        code TEXT NOT NULL,
                        language TEXT,
                        description TEXT,
                        tags TEXT,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        usageCount INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 2 to 3 - adds coding_challenges table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS coding_challenges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        category TEXT NOT NULL,
                        starterCode TEXT NOT NULL,
                        language TEXT NOT NULL DEFAULT 'kotlin',
                        hints TEXT,
                        solution TEXT,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        userSolution TEXT,
                        completedAt INTEGER,
                        dateShown TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 3 to 4 - adds lessons table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS lessons (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        content TEXT NOT NULL,
                        codeExamples TEXT,
                        exercises TEXT,
                        duration INTEGER NOT NULL DEFAULT 10,
                        orderIndex INTEGER NOT NULL DEFAULT 0,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        progress INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aiko_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}