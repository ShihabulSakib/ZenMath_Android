package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // 1. Device boot / Package replaced actions -> Re-arm alarms without showing notification
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = PreferencesManager(context)
            val settings = prefs.loadSettings()
            if (settings.notificationsEnabled) {
                NotificationScheduler.scheduleReminders(context, settings.notificationTimes)
            }
            return
        }

        // 2. Alarm trigger -> Deliver reminder notification and self-sustain reschedule
        if (action.startsWith(NotificationScheduler.ACTION_REMINDER_PREFIX)) {
            val prefs = PreferencesManager(context)
            val settings = prefs.loadSettings()
            if (!settings.notificationsEnabled) return

            val fallbackSlot = action.removePrefix(NotificationScheduler.ACTION_REMINDER_PREFIX).toIntOrNull() ?: 0
            val slotIndex = intent.getIntExtra(NotificationScheduler.EXTRA_SLOT_INDEX, fallbackSlot)
            val timeStr = intent.getStringExtra(NotificationScheduler.EXTRA_TIME_STR)
                ?: settings.notificationTimes.sorted().getOrNull(slotIndex)
                ?: ""

            val dailyProgress = prefs.getTodayProgress()
            val dailyGoal = settings.dailyGoal
            val remaining = dailyGoal - dailyProgress

            val title = "ZenMath — Daily Goal"
            val body = if (remaining > 0) {
                "Only $remaining session${if (remaining > 1) "s" else ""} left to reach your daily goal! Keep it up."
            } else {
                "Time to sharpen your mind! Daily goal completed, keep the streak going!"
            }

            val notificationId = 1000 + slotIndex
            showNotification(context, title, body, notificationId)

            // Self-Sustaining Re-schedule: Re-arm alarm for this slot for tomorrow (+24h)
            if (timeStr.isNotEmpty() && settings.notificationsEnabled) {
                NotificationScheduler.scheduleSingleReminder(context, slotIndex, timeStr)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "zenmath_daily_reminders"
        const val CHANNEL_NAME = "Daily Reminders"

        fun showNotification(
            context: Context,
            title: String,
            body: String,
            notificationId: Int = 1001
        ) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "ZenMath daily practice and streak reminders"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(notificationId, notification)
        }
    }
}
