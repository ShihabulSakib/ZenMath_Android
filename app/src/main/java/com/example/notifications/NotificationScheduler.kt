package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationScheduler {

    const val EXTRA_SLOT_INDEX = "EXTRA_SLOT_INDEX"
    const val EXTRA_TIME_STR = "EXTRA_TIME_STR"
    const val ACTION_REMINDER_PREFIX = "com.example.zenmath.DAILY_REMINDER_"

    fun scheduleReminders(context: Context, times: Set<String>) {
        cancelAllReminders(context)
        times.sorted().forEachIndexed { index, timeStr ->
            scheduleSingleReminder(context, index, timeStr)
        }
    }

    fun scheduleSingleReminder(context: Context, slotIndex: Int, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parts = timeStr.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today (or is right now), schedule for tomorrow
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "$ACTION_REMINDER_PREFIX$slotIndex"
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
            putExtra(EXTRA_TIME_STR, timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotIndex + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for Android 12+ if SCHEDULE_EXACT_ALARM is not granted
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {
                // Ignore failure if system restricts alarms
            }
        } catch (_: Exception) {
            // Safety fallback
        }
    }

    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (index in 0..10) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "$ACTION_REMINDER_PREFIX$index"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index + 100,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun sendTestNotification(context: Context) {
        ReminderReceiver.showNotification(
            context,
            "ZenMath — Daily Goal",
            "Time to sharpen your mind! Daily practice reminder.",
            notificationId = 9999
        )
    }
}
