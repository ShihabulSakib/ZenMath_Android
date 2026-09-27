package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.GameSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zenmath_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<GameSettings> = _settingsFlow.asStateFlow()

    private val _dailyProgressFlow = MutableStateFlow(getTodayProgress())
    val dailyProgressFlow: StateFlow<Int> = _dailyProgressFlow.asStateFlow()

    private val _bestStreakFlow = MutableStateFlow(prefs.getInt(KEY_BEST_STREAK, 0))
    val bestStreakFlow: StateFlow<Int> = _bestStreakFlow.asStateFlow()

    fun loadSettings(): GameSettings {
        val totalQuestions = prefs.getInt(KEY_TOTAL_QUESTIONS, 5).coerceIn(1, 50)
        val timeLimit = prefs.getInt(KEY_TIME_LIMIT, 20).coerceIn(5, 60)
        val dailyGoal = prefs.getInt(KEY_DAILY_GOAL, 10).coerceIn(1, 100)
        val adaptiveDifficulty = prefs.getBoolean(KEY_ADAPTIVE_DIFF, false)
        val showStreak = prefs.getBoolean(KEY_SHOW_STREAK, true)
        val hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true)
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIF_ENABLED, false)
        val notificationTimes = prefs.getStringSet(KEY_NOTIF_TIMES, setOf("08:00", "12:00", "18:00")) ?: setOf("08:00", "12:00", "18:00")
        val isDarkTheme = prefs.getBoolean(KEY_THEME_DARK, true)

        return GameSettings(
            totalQuestions = totalQuestions,
            timeLimit = timeLimit,
            dailyGoal = dailyGoal,
            adaptiveDifficulty = adaptiveDifficulty,
            showStreak = showStreak,
            hapticFeedback = hapticFeedback,
            notificationsEnabled = notificationsEnabled,
            notificationTimes = notificationTimes,
            isDarkTheme = isDarkTheme
        )
    }

    fun saveSettings(settings: GameSettings) {
        prefs.edit()
            .putInt(KEY_TOTAL_QUESTIONS, settings.totalQuestions.coerceIn(1, 50))
            .putInt(KEY_TIME_LIMIT, settings.timeLimit.coerceIn(5, 60))
            .putInt(KEY_DAILY_GOAL, settings.dailyGoal.coerceIn(1, 100))
            .putBoolean(KEY_ADAPTIVE_DIFF, settings.adaptiveDifficulty)
            .putBoolean(KEY_SHOW_STREAK, settings.showStreak)
            .putBoolean(KEY_HAPTIC, settings.hapticFeedback)
            .putBoolean(KEY_NOTIF_ENABLED, settings.notificationsEnabled)
            .putStringSet(KEY_NOTIF_TIMES, settings.notificationTimes)
            .putBoolean(KEY_THEME_DARK, settings.isDarkTheme)
            .apply()
        _settingsFlow.value = settings
    }

    fun toggleTheme(): Boolean {
        val current = _settingsFlow.value.isDarkTheme
        val newTheme = !current
        val updated = _settingsFlow.value.copy(isDarkTheme = newTheme)
        saveSettings(updated)
        return newTheme
    }

    fun updateDailyGoal(goal: Int) {
        val updated = _settingsFlow.value.copy(dailyGoal = goal.coerceIn(1, 100))
        saveSettings(updated)
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getTodayProgress(): Int {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_DAILY_DATE, "")
        return if (savedDate == today) {
            prefs.getInt(KEY_DAILY_COUNT, 0)
        } else {
            0
        }
    }

    fun incrementDailyProgress(): Int {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_DAILY_DATE, "")
        val currentCount = if (savedDate == today) prefs.getInt(KEY_DAILY_COUNT, 0) else 0
        val newCount = currentCount + 1
        prefs.edit()
            .putString(KEY_DAILY_DATE, today)
            .putInt(KEY_DAILY_COUNT, newCount)
            .apply()
        _dailyProgressFlow.value = newCount
        return newCount
    }

    fun updateBestStreak(streak: Int) {
        val current = prefs.getInt(KEY_BEST_STREAK, 0)
        if (streak > current) {
            prefs.edit().putInt(KEY_BEST_STREAK, streak).apply()
            _bestStreakFlow.value = streak
        }
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        _settingsFlow.value = loadSettings()
        _dailyProgressFlow.value = 0
        _bestStreakFlow.value = 0
    }

    companion object {
        private const val KEY_TOTAL_QUESTIONS = "zenmath_total_questions"
        private const val KEY_TIME_LIMIT = "zenmath_time_limit"
        private const val KEY_DAILY_GOAL = "zenmath_daily_goal"
        private const val KEY_ADAPTIVE_DIFF = "zenmath_adaptive_diff"
        private const val KEY_SHOW_STREAK = "zenmath_show_streak"
        private const val KEY_HAPTIC = "zenmath_haptic_feedback"
        private const val KEY_NOTIF_ENABLED = "zenmath_notif_enabled"
        private const val KEY_NOTIF_TIMES = "zenmath_notif_times"
        private const val KEY_THEME_DARK = "zenmath_theme_dark"
        private const val KEY_DAILY_DATE = "zenmath_daily_date"
        private const val KEY_DAILY_COUNT = "zenmath_daily_count"
        private const val KEY_BEST_STREAK = "zenmath_best_streak"
    }
}
