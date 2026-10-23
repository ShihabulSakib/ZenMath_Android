package com.example.domain

import com.example.data.SessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Shared day-bucketing and timezone-aware date utility used across streaks,
 * charts, calendar, and Room migrations.
 */
object DateBucketUtils {

    private val ISO_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )

    fun nowUtcIso(nowMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(nowMs))
    }

    fun formatLocalDate(epochMs: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(epochMs))
    }

    fun formatDayLabel(dateStr: String, timeZone: TimeZone = TimeZone.getDefault()): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                this.timeZone = timeZone
            }
            val parsed = parser.parse(dateStr) ?: return dateStr.takeLast(2)
            SimpleDateFormat("EEE", Locale.US).apply {
                this.timeZone = timeZone
            }.format(parsed)
        } catch (_: Exception) {
            dateStr.takeLast(2)
        }
    }

    fun parseUtcIsoToEpochMs(isoStr: String): Long? {
        val trimmed = isoStr.trim()
        if (trimmed.isEmpty()) return null
        for (pattern in ISO_PATTERNS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
                val parsed = sdf.parse(trimmed)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    fun parseUtcIsoMillis(isoStr: String): Long? = parseUtcIsoToEpochMs(isoStr)

    /**
     * Converts a UTC ISO-8601 timestamp string (e.g. "2026-10-23T20:15:00.000Z")
     * into a local calendar date string "yyyy-MM-dd" in the given [timeZone].
     * If [isoStr] is already a plain "yyyy-MM-dd" string, returns its date prefix.
     */
    fun utcIsoToLocalDate(isoStr: String, timeZone: TimeZone = TimeZone.getDefault()): String {
        val epochMs = parseUtcIsoToEpochMs(isoStr)
        if (epochMs != null) {
            return formatLocalDate(epochMs, timeZone)
        }
        return isoStr.split("T").firstOrNull()?.take(10) ?: ""
    }

    /**
     * Returns the local calendar date ("yyyy-MM-dd") for a [SessionEntity].
     * Uses the stored [SessionEntity.localDate] when present and using the default timezone,
     * or computes it from [SessionEntity.date] for a custom [timeZone] or legacy rows.
     */
    fun resolveSessionLocalDate(
        session: SessionEntity,
        timeZone: TimeZone? = null
    ): String {
        if (timeZone != null) {
            if (session.date.contains("T")) {
                return utcIsoToLocalDate(session.date, timeZone)
            }
            if (session.localDate.isNotBlank()) return session.localDate
            return utcIsoToLocalDate(session.date, timeZone)
        }
        if (session.localDate.isNotBlank()) {
            return session.localDate
        }
        return utcIsoToLocalDate(session.date, TimeZone.getDefault())
    }

    /**
     * Generates a list of the last [days] calendar dates ("yyyy-MM-dd") ending on the day of [nowMs]
     * in the given [timeZone], ordered oldest to newest.
     */
    fun lastNDates(
        days: Int,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<String> {
        if (days <= 0) return emptyList()
        val result = mutableListOf<String>()
        for (i in (days - 1) downTo 0) {
            val cal = Calendar.getInstance(timeZone, Locale.US).apply {
                timeInMillis = nowMs
                add(Calendar.DAY_OF_YEAR, -i)
            }
            result.add(formatLocalDate(cal.timeInMillis, timeZone))
        }
        return result
    }

    /**
     * Calculates active daily practice streak using local calendar dates.
     */
    fun calculateStreak(
        sessions: List<SessionEntity>,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone? = null
    ): Int {
        if (sessions.isEmpty()) return 0
        val effectiveTz = timeZone ?: TimeZone.getDefault()

        val uniqueDates = sessions
            .map { resolveSessionLocalDate(it, timeZone) }
            .filter { it.length == 10 }
            .distinct()
            .sortedDescending()

        if (uniqueDates.isEmpty()) return 0

        val todayCal = Calendar.getInstance(effectiveTz, Locale.US).apply {
            timeInMillis = nowMs
        }
        val today = formatLocalDate(todayCal.timeInMillis, effectiveTz)

        val yesterdayCal = Calendar.getInstance(effectiveTz, Locale.US).apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterday = formatLocalDate(yesterdayCal.timeInMillis, effectiveTz)

        if (uniqueDates.first() != today && uniqueDates.first() != yesterday) {
            return 0
        }

        val cursorCal = Calendar.getInstance(effectiveTz, Locale.US).apply {
            timeInMillis = if (uniqueDates.first() == today) todayCal.timeInMillis else yesterdayCal.timeInMillis
        }

        var streak = 0
        for (dateStr in uniqueDates) {
            val expectedStr = formatLocalDate(cursorCal.timeInMillis, effectiveTz)
            if (dateStr == expectedStr) {
                streak++
                cursorCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun calculateStreakDays(
        localDates: Set<String>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val dummySessions = localDates.mapIndexed { idx, d ->
            SessionEntity(
                id = "s_$idx",
                date = d,
                mode = "addition",
                totalQuestions = 1,
                correct = 1,
                avgTimeMs = 1000.0,
                difficulty = "medium",
                duration = 1.0,
                localDate = d
            )
        }
        return calculateStreak(dummySessions, nowMillis, timeZone)
    }

    fun buildRecentPerformanceDays(
        sessions: List<SessionEntity>,
        days: Int = 7,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<RecentPerformanceDay> {
        val dateList = lastNDates(days, nowMillis, timeZone)
        val grouped = groupSessionsByLocalDate(sessions, timeZone)
        return dateList.map { dateStr ->
            val daySessions = grouped[dateStr] ?: emptyList()
            val correct = daySessions.sumOf { it.correct }
            val totalQ = daySessions.sumOf { it.totalQuestions }
            RecentPerformanceDay(
                date = dateStr,
                accuracy = if (totalQ > 0) (correct.toDouble() / totalQ.toDouble()) * 100.0 else 0.0,
                count = totalQ,
                sessionsCount = daySessions.size
            )
        }
    }

    /**
     * Groups sessions by their resolved local date ("yyyy-MM-dd").
     */
    fun groupSessionsByLocalDate(
        sessions: List<SessionEntity>,
        timeZone: TimeZone? = null
    ): Map<String, List<SessionEntity>> {
        return sessions.groupBy { resolveSessionLocalDate(it, timeZone) }
            .filterKeys { it.isNotBlank() }
    }
}
