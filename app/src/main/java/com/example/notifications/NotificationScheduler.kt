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

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun scheduleReminders(context: Context, times: Set<String>) {
        val appContext = context.applicationContext ?: context
        cancelAllReminders(appContext)
        times.sorted().forEachIndexed { index, timeStr ->
            scheduleSingleReminder(appContext, index, timeStr)
        }
    }

    fun scheduleSingleReminder(context: Context, slotIndex: Int, timeStr: String) {
        val appContext = context.applicationContext ?: context
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
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
            // If the time has already passed today (or is within the next 2 seconds), schedule for tomorrow
            if (timeInMillis <= now.timeInMillis + 2000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = "$ACTION_REMINDER_PREFIX$slotIndex"
            setPackage(appContext.packageName)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
            putExtra(EXTRA_TIME_STR, timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
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
        val appContext = context.applicationContext ?: context
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (index in 0..30) {
            val intent = Intent(appContext, ReminderReceiver::class.java).apply {
                action = "$ACTION_REMINDER_PREFIX$index"
                setPackage(appContext.packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
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

    fun getNextAlarmSummary(times: Set<String>): String? {
        if (times.isEmpty()) return null
        val now = Calendar.getInstance()
        var earliestCal: Calendar? = null
        var earliestTimeStr: String? = null

        for (timeStr in times) {
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            if (earliestCal == null || cal.timeInMillis < earliestCal.timeInMillis) {
                earliestCal = cal
                earliestTimeStr = timeStr
            }
        }

        val targetCal = earliestCal ?: return null
        val targetStr = earliestTimeStr ?: return null
        val parts = targetStr.split(":")
        val h24 = parts[0].toInt()
        val m = parts[1].toInt()
        val amPm = if (h24 >= 12) "PM" else "AM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        val formattedTime = String.format(java.util.Locale.US, "%d:%02d %s", h12, m, amPm)
        val isToday = targetCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
            targetCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        return if (isToday) "Today at $formattedTime" else "Tomorrow at $formattedTime"
    }
}
