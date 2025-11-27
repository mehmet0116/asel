package com.aikodasistani.aikodasistani.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [Session::class, ArchivedMessage::class, Snippet::class, CodingChallenge::class, Lesson::class, CodeTemplate::class, UsageStats::class, Bookmark::class, QuickNote::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun snippetDao(): SnippetDao
    abstract fun codingChallengeDao(): CodingChallengeDao
    abstract fun lessonDao(): LessonDao
    abstract fun codeTemplateDao(): CodeTemplateDao
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun quickNoteDao(): QuickNoteDao

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

        // Migration from version 4 to 5 - adds code_templates table
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS code_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        code TEXT NOT NULL,
                        language TEXT NOT NULL,
                        category TEXT NOT NULL,
                        tags TEXT,
                        variables TEXT,
                        usageCount INTEGER NOT NULL DEFAULT 0,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        isBuiltIn INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 5 to 6 - adds usage_stats table
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS usage_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        totalMessages INTEGER NOT NULL DEFAULT 0,
                        codeGenerations INTEGER NOT NULL DEFAULT 0,
                        voiceInputs INTEGER NOT NULL DEFAULT 0,
                        snippetsSaved INTEGER NOT NULL DEFAULT 0,
                        challengesCompleted INTEGER NOT NULL DEFAULT 0,
                        lessonsCompleted INTEGER NOT NULL DEFAULT 0,
                        templatesUsed INTEGER NOT NULL DEFAULT 0,
                        toolsUsed INTEGER NOT NULL DEFAULT 0,
                        playgroundRuns INTEGER NOT NULL DEFAULT 0,
                        totalTokensUsed INTEGER NOT NULL DEFAULT 0,
                        sessionDurationMinutes INTEGER NOT NULL DEFAULT 0,
                        providersUsed TEXT,
                        mostUsedFeature TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 6 to 7 - adds bookmarks table
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'Genel',
                        tags TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        sessionId INTEGER NOT NULL DEFAULT -1,
                        messageId INTEGER NOT NULL DEFAULT -1
                    )
                """.trimIndent())
            }
        }

        // Migration from version 7 to 8 - adds quick_notes table
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS quick_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        language TEXT,
                        color TEXT NOT NULL DEFAULT '#FFFFFF',
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}