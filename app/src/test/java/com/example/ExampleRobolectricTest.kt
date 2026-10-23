package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.data.ZenMathDatabase
import com.example.data.ZenMathRepository
import com.example.domain.DateBucketUtils
import com.example.notifications.NotificationScheduler
import com.example.notifications.ReminderReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZenMath", appName)
    }

    @Test
    fun `scheduleReminders registers alarms for all configured times`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val times = setOf("08:00", "12:00", "18:30")
        NotificationScheduler.scheduleReminders(context, times)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(3, scheduledAlarms.size)

        // Verify cancelling clears them
        NotificationScheduler.cancelAllReminders(context)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `ReminderReceiver delivers notification and reschedules next day alarm`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        val settings = prefs.loadSettings().copy(
            notificationsEnabled = true,
            notificationTimes = setOf("08:00", "18:00")
        )
        prefs.saveSettings(settings)

        // Persist updated set and reload to verify SharedPreferences StringSet immutability fix
        val reloaded = PreferencesManager(context).loadSettings()
        assertTrue(reloaded.notificationsEnabled)
        assertEquals(setOf("08:00", "18:00"), reloaded.notificationTimes)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        shadowAlarmManager.scheduledAlarms.clear()

        val receiver = ReminderReceiver()
        val alarmIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "${NotificationScheduler.ACTION_REMINDER_PREFIX}0"
            putExtra(NotificationScheduler.EXTRA_SLOT_INDEX, 0)
            putExtra(NotificationScheduler.EXTRA_TIME_STR, "08:00")
        }

        receiver.onReceive(context, alarmIntent)

        // Check notification was posted
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        assertFalse(shadowNotificationManager.allNotifications.isEmpty())

        // Check alarm was rescheduled for tomorrow
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `ReminderReceiver reschedules on boot without showing notification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        prefs.saveSettings(
            prefs.loadSettings().copy(
                notificationsEnabled = true,
                notificationTimes = setOf("08:00", "20:00")
            )
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        shadowAlarmManager.scheduledAlarms.clear()

        val receiver = ReminderReceiver()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, bootIntent)

        // Should have scheduled 2 alarms and posted 0 notifications
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun `Room MIGRATION_1_2 migrates v1 schema data and populates localDate and question fields`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_zenmath_migration_v1_v2.db"
        context.deleteDatabase(dbName)

        // 1. Create a v1 database using SupportSQLiteOpenHelper with the exact v1 schema
        val v1Callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sessions` (
                        `id` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `correct` INTEGER NOT NULL,
                        `avgTimeMs` REAL NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `duration` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `questions` (
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
                        `difficulty` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(v1Callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase
        v1Db.execSQL(
            "INSERT INTO `sessions` VALUES ('sess_old_1', '2026-10-20T14:30:00.000Z', 'addition', 10, 9, 1450.0, 'medium', 14.5)"
        )
        v1Db.execSQL(
            "INSERT INTO `questions` (`sessionId`, `num1`, `num2`, `operation`, `correctAnswer`, `userAnswer`, `timeMs`, `isCorrect`, `mode`, `difficulty`) VALUES ('sess_old_1', 12.0, 8.0, '+', '20', '20', 1400, 1, 'addition', 'medium')"
        )
        v1Db.close()
        helper.close()

        // 2. Open with Room at version 2 using ZenMathDatabase.MIGRATION_1_2 (no destructive fallback)
        val migratedRoomDb = Room.databaseBuilder(context, ZenMathDatabase::class.java, dbName)
            .addMigrations(ZenMathDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val sessions = migratedRoomDb.dao().getAllSessionsList()
            assertEquals(1, sessions.size)
            val s = sessions.first()
            assertEquals("sess_old_1", s.id)
            assertEquals("normal", s.sessionType)
            assertEquals(DateBucketUtils.utcIsoToLocalDate("2026-10-20T14:30:00.000Z"), s.localDate)

            val questions = migratedRoomDb.dao().getQuestionsForSession("sess_old_1")
            assertEquals(1, questions.size)
            val q = questions.first()
            assertEquals("20", q.correctAnswer)
            assertEquals(1400L, q.timeMs)
            assertEquals("addition", q.topicTag)
            assertTrue(q.questionText.contains("12"))
            assertFalse(q.hintUsed)
        }

        migratedRoomDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `importBackupJson imports v1 backup without v2 fields and exports v2 backup cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val roomDb = Room.inMemoryDatabaseBuilder(context, ZenMathDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = PreferencesManager(context)
        val repo = ZenMathRepository(roomDb.dao(), prefs)

        // Old v1 backup JSON (no version field, no localDate/sessionType on sessions, no questionText/topicTag/hintUsed on questions)
        val oldV1Json = """
            {
              "exportDate": "2026-10-15T10:00:00.000Z",
              "bestStreak": 12,
              "sessions": [
                {
                  "id": "v1_sess_100",
                  "date": "2026-10-15T09:15:00.000Z",
                  "mode": "multiplication",
                  "totalQuestions": 10,
                  "correct": 8,
                  "avgTimeMs": 2100.0,
                  "difficulty": "hard",
                  "duration": 21.0
                }
              ],
              "questions": [
                {
                  "sessionId": "v1_sess_100",
                  "num1": 7.0,
                  "num2": 8.0,
                  "operation": "*",
                  "correctAnswer": "56",
                  "userAnswer": "54",
                  "timeMs": 2300,
                  "isCorrect": false,
                  "mode": "multiplication",
                  "difficulty": "hard"
                }
              ]
            }
        """.trimIndent()

        runBlocking {
            repo.importBackupJson(oldV1Json)
            val sessions = roomDb.dao().getAllSessionsList()
            assertEquals(1, sessions.size)
            assertEquals("v1_sess_100", sessions[0].id)
            assertEquals("normal", sessions[0].sessionType)
            assertEquals(DateBucketUtils.utcIsoToLocalDate("2026-10-15T09:15:00.000Z"), sessions[0].localDate)

            val questions = roomDb.dao().getAllQuestionsList()
            assertEquals(1, questions.size)
            assertEquals("multiplication", questions[0].topicTag)
            assertFalse(questions[0].isCorrect)

            // Verify WeakSpots computation picks up the imported question
            val weakSpots = repo.computeWeakSpots(questions, sessions)
            assertEquals(1, weakSpots.worstTopics.size)
            assertEquals("multiplication", weakSpots.worstTopics[0].key)

            // Verify v2 export & re-import round-trip
            val exportedV2 = repo.exportBackupJson()
            assertTrue(exportedV2.contains("\"version\": 2"))
            repo.clearAllData()
            assertEquals(0, roomDb.dao().getAllSessionsList().size)
            repo.importBackupJson(exportedV2)
            assertEquals(1, roomDb.dao().getAllSessionsList().size)
            assertEquals(1, roomDb.dao().getAllQuestionsList().size)
        }

        roomDb.close()
    }
}
