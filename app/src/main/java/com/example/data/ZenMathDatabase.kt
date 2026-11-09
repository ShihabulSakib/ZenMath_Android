package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.DateBucketUtils
import com.example.domain.drills.TopicTagResolver
import java.util.TimeZone

@Database(
    entities = [SessionEntity::class, QuestionRecordEntity::class],
    version = 3,
    exportSchema = true
)
abstract class ZenMathDatabase : RoomDatabase() {
    abstract fun dao(): ZenMathDao

    companion object {
        @Volatile
        private var INSTANCE: ZenMathDatabase? = null

        /**
         * Real Room migration from schema v1 to v2 (Section 1.5 & 1.6):
         * - Adds `localDate` and `sessionType` to `sessions`, converting legacy UTC timestamps
         *   into the device's local calendar date (`yyyy-MM-dd`).
         * - Adds `questionText`, `topicTag`, and `hintUsed` to `questions`, populating sensible
         *   placeholders for existing v1 rows.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Rebuild `sessions` with `localDate` and `sessionType`
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sessions_new` (
                        `id` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `correct` INTEGER NOT NULL,
                        `avgTimeMs` REAL NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `duration` REAL NOT NULL,
                        `localDate` TEXT NOT NULL,
                        `sessionType` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `sessions_new` (
                        `id`, `date`, `mode`, `totalQuestions`, `correct`,
                        `avgTimeMs`, `difficulty`, `duration`, `localDate`, `sessionType`
                    )
                    SELECT
                        `id`, `date`, `mode`, `totalQuestions`, `correct`,
                        `avgTimeMs`, `difficulty`, `duration`,
                        substr(`date`, 1, 10),
                        'normal'
                    FROM `sessions`
                    """.trimIndent()
                )

                // Convert each migrated session's UTC ISO timestamp into the device timezone's local date
                val cursor = db.query("SELECT `id`, `date` FROM `sessions_new`")
                val updates = mutableListOf<Pair<String, String>>()
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0)
                        val utcDate = c.getString(1)
                        val localDate = DateBucketUtils.utcIsoToLocalDate(utcDate, TimeZone.getDefault())
                        if (localDate.isNotBlank()) {
                            updates.add(id to localDate)
                        }
                    }
                }
                for ((id, localDate) in updates) {
                    db.execSQL(
                        "UPDATE `sessions_new` SET `localDate` = ? WHERE `id` = ?",
                        arrayOf<Any>(localDate, id)
                    )
                }

                db.execSQL("DROP TABLE `sessions`")
                db.execSQL("ALTER TABLE `sessions_new` RENAME TO `sessions`")

                // 2. Rebuild `questions` with `questionText`, `topicTag`, and `hintUsed`
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `questions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `num1` REAL NOT NULL,
                        `num2` REAL NOT NULL,
                        `operation` TEXT NOT NULL,
                        `correctAnswer` TEXT NOT NULL,
                        `userAnswer` TEXT,
                        `timeMs` INTEGER NOT NULL,
                        `isCorrect` INTEGER NOT NULL,
                        `mode` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `questionText` TEXT NOT NULL,
                        `topicTag` TEXT NOT NULL,
                        `hintUsed` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `questions_new` (
                        `id`, `sessionId`, `num1`, `num2`, `operation`,
                        `correctAnswer`, `userAnswer`, `timeMs`, `isCorrect`,
                        `mode`, `difficulty`, `questionText`, `topicTag`, `hintUsed`
                    )
                    SELECT
                        `id`, `sessionId`, `num1`, `num2`, `operation`,
                        `correctAnswer`, `userAnswer`, `timeMs`, `isCorrect`,
                        `mode`, `difficulty`,
                        CASE
                            WHEN `operation` IN ('+', '−', '-', '×', '*', '÷', '/')
                            THEN CAST(CAST(`num1` AS INTEGER) AS TEXT) || ' ' || `operation` || ' ' || CAST(CAST(`num2` AS INTEGER) AS TEXT)
                            ELSE `operation`
                        END,
                        CASE
                            WHEN `mode` != '' THEN `mode`
                            ELSE 'general'
                        END,
                        0
                    FROM `questions`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `questions`")
                db.execSQL("ALTER TABLE `questions_new` RENAME TO `questions`")
            }
        }

        /**
         * Real Room migration from schema v2 to v3:
         * - Adds `sourceSessionId` and `scopeLabel` to `sessions`.
         * - Adds `drillMode`, `answerFormat`, and `problemJson` to `questions`, backfilling
         *   `drillMode` from `mode` for concrete drills or via [TopicTagResolver] for compound modes.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `sourceSessionId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `scopeLabel` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `questions` ADD COLUMN `drillMode` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `answerFormat` TEXT NOT NULL DEFAULT 'typed'")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `problemJson` TEXT NOT NULL DEFAULT ''")

                // Direct SQL backfill for non-compound modes
                db.execSQL(
                    """
                    UPDATE `questions`
                    SET `drillMode` = `mode`
                    WHERE `mode` NOT IN ('speed-round', 'weak-spots', 'mistake-review', '')
                    """.trimIndent()
                )

                // Resolve compound rows via TopicTagResolver
                val cursor = db.query(
                    "SELECT `id`, `topicTag`, `mode` FROM `questions` WHERE `drillMode` = ''"
                )
                val updates = mutableListOf<Pair<Long, String>>()
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val tag = c.getString(1) ?: ""
                        val mode = c.getString(2) ?: ""
                        val resolved = TopicTagResolver.resolveDrillMode(topicTag = tag, modeId = mode).id
                        updates.add(id to resolved)
                    }
                }
                for ((id, drillMode) in updates) {
                    db.execSQL(
                        "UPDATE `questions` SET `drillMode` = ? WHERE `id` = ?",
                        arrayOf<Any>(drillMode, id)
                    )
                }
            }
        }

        fun getDatabase(context: Context): ZenMathDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenMathDatabase::class.java,
                    "zenmath-db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
