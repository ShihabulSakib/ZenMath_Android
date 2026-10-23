package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.notifications.NotificationScheduler
import com.example.notifications.ReminderReceiver
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
        shadowOf(notificationManager).cancelAll()

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
}
