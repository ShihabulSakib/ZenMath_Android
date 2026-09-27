package com.example.data

import com.example.domain.ModeStats
import com.example.domain.RecentPerformanceDay
import com.example.domain.StatsData
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ZenMathRepository(
    private val dao: ZenMathDao,
    private val preferencesManager: PreferencesManager
) {

    fun getAllSessionsFlow(): Flow<List<SessionEntity>> = dao.getAllSessionsFlow()

    suspend fun getAllSessions(): List<SessionEntity> = dao.getAllSessions()

    suspend fun getRecentSessions(limit: Int): List<SessionEntity> = dao.getRecentSessions(limit)

    suspend fun getQuestionsForSession(sessionId: String): List<QuestionRecordEntity> =
        dao.getQuestionsForSession(sessionId)

    suspend fun insertSession(session: SessionEntity, questions: List<QuestionRecordEntity>) {
        dao.insertSessionWithQuestions(session, questions)
    }

    suspend fun clearAllData() {
        dao.clearAll()
        preferencesManager.clearAllPreferences()
    }

    fun calculateStreak(sessions: List<SessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        val uniqueDates = sessions.mapNotNull {
            try {
                it.date.split("T").firstOrNull()
            } catch (e: Exception) {
                null
            }
        }.distinct().sortedDescending()

        if (uniqueDates.isEmpty()) return 0

        var streak = 0
        val today = dateFormat.format(Date())
        val yesterday = dateFormat.format(Date(System.currentTimeMillis() - 86400000L))

        // Check if user practiced today or yesterday to maintain active streak
        if (uniqueDates.first() != today && uniqueDates.first() != yesterday) {
            return 0
        }

        var expectedDateMs = if (uniqueDates.first() == today) {
            System.currentTimeMillis()
        } else {
            System.currentTimeMillis() - 86400000L
        }

        for (dateStr in uniqueDates) {
            val expectedStr = dateFormat.format(Date(expectedDateMs))
            if (dateStr == expectedStr) {
                streak++
                expectedDateMs -= 86400000L
            } else {
                break
            }
        }
        return streak
    }

    fun computeStats(sessions: List<SessionEntity>): StatsData {
        if (sessions.isEmpty()) {
            return StatsData(
                totalSessions = 0,
                totalQuestions = 0,
                totalCorrect = 0,
                avgAccuracy = 0.0,
                avgTimeMs = 0.0,
                streakDays = 0,
                modes = emptyMap()
            )
        }

        val totalQuestions = sessions.sumOf { it.totalQuestions }
        val totalCorrect = sessions.sumOf { it.correct }
        val totalTimeMs = sessions.sumOf { it.avgTimeMs * it.totalQuestions }

        val modeMap = mutableMapOf<String, ModeAccumulator>()
        for (s in sessions) {
            val acc = modeMap.getOrPut(s.mode) { ModeAccumulator() }
            acc.sessions++
            acc.correct += s.correct
            acc.total += s.totalQuestions
            acc.timeMs += s.avgTimeMs * s.totalQuestions
        }

        val modeStats = modeMap.mapValues { (_, data) ->
            ModeStats(
                sessions = data.sessions,
                accuracy = if (data.total > 0) (data.correct.toDouble() / data.total.toDouble()) * 100.0 else 0.0,
                avgTimeMs = if (data.total > 0) data.timeMs / data.total.toDouble() else 0.0
            )
        }

        val streakDays = calculateStreak(sessions)

        return StatsData(
            totalSessions = sessions.size,
            totalQuestions = totalQuestions,
            totalCorrect = totalCorrect,
            avgAccuracy = if (totalQuestions > 0) (totalCorrect.toDouble() / totalQuestions.toDouble()) * 100.0 else 0.0,
            avgTimeMs = if (totalQuestions > 0) totalTimeMs / totalQuestions.toDouble() else 0.0,
            streakDays = streakDays,
            modes = modeStats
        )
    }

    fun getRecentPerformance(sessions: List<SessionEntity>, days: Int = 7): List<RecentPerformanceDay> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        // Generate full list of the last `days` calendar dates ending today
        val dateList = mutableListOf<String>()
        for (i in (days - 1) downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            dateList.add(dateFormat.format(c.time))
        }

        // Map sessions by date
        val sessionsByDate = mutableMapOf<String, Triple<Int, Int, Int>>() // date -> (correct, totalQuestions, sessionsCount)
        for (s in sessions) {
            val dateKey = s.date.split("T").firstOrNull() ?: continue
            val current = sessionsByDate.getOrDefault(dateKey, Triple(0, 0, 0))
            sessionsByDate[dateKey] = Triple(current.first + s.correct, current.second + s.totalQuestions, current.third + 1)
        }

        return dateList.map { dateStr ->
            val data = sessionsByDate[dateStr] ?: Triple(0, 0, 0)
            RecentPerformanceDay(
                date = dateStr,
                accuracy = if (data.second > 0) (data.first.toDouble() / data.second.toDouble()) * 100.0 else 0.0,
                count = data.second,
                sessionsCount = data.third
            )
        }
    }

    fun getHorizonWaveBuckets(sessions: List<SessionEntity>, daysCount: Int = 10): List<com.example.domain.DayBucket> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)

        val buckets = mutableListOf<com.example.domain.DayBucket>()
        for (i in (daysCount - 1) downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = dateFormat.format(c.time)
            val label = dayLabelFormat.format(c.time)

            var qCount = 0
            var cCount = 0
            var sCount = 0
            var totalTimeSec = 0.0

            for (s in sessions) {
                if (s.date.startsWith(dateStr)) {
                    qCount += s.totalQuestions
                    cCount += s.correct
                    sCount += 1
                    totalTimeSec += (s.avgTimeMs / 1000.0) * s.totalQuestions
                }
            }

            val accuracy = if (qCount > 0) ((cCount.toDouble() / qCount.toDouble()) * 100.0).toInt() else 0
            val avgSpeed = if (qCount > 0) totalTimeSec / qCount.toDouble() else 0.0

            buckets.add(
                com.example.domain.DayBucket(
                    date = dateStr,
                    label = label,
                    questions = qCount,
                    correct = cCount,
                    accuracy = accuracy,
                    avgSpeed = avgSpeed,
                    sessions = sCount
                )
            )
        }
        return buckets
    }

    suspend fun exportBackupJson(): String {
        val sessions = dao.getAllSessions()
        val questions = dao.getAllQuestions()

        val root = JSONObject()
        root.put("type", "zenmath-backup")
        root.put("version", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date()))

        val sessionsArray = JSONArray()
        for (s in sessions) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("date", s.date)
                put("mode", s.mode)
                put("totalQuestions", s.totalQuestions)
                put("correct", s.correct)
                put("avgTimeMs", s.avgTimeMs)
                put("difficulty", s.difficulty)
                put("duration", s.duration)
            }
            sessionsArray.put(sObj)
        }
        root.put("sessions", sessionsArray)

        val questionsArray = JSONArray()
        for (q in questions) {
            val qObj = JSONObject().apply {
                put("id", q.id)
                put("sessionId", q.sessionId)
                put("num1", q.num1)
                put("num2", q.num2)
                put("operation", q.operation)
                put("correctAnswer", q.correctAnswer)
                put("userAnswer", q.userAnswer ?: JSONObject.NULL)
                put("timeMs", q.timeMs)
                put("isCorrect", q.isCorrect)
                put("mode", q.mode)
                put("difficulty", q.difficulty)
            }
            questionsArray.put(qObj)
        }
        root.put("questions", questionsArray)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonStr: String) {
        val root = JSONObject(jsonStr)
        if (root.optString("type") != "zenmath-backup" || root.optInt("version", 0) != 1) {
            throw IllegalArgumentException("Invalid backup file format.")
        }

        val sessionsArray = root.optJSONArray("sessions") ?: JSONArray()
        val questionsArray = root.optJSONArray("questions") ?: JSONArray()

        val sessionsList = mutableListOf<SessionEntity>()
        for (i in 0 until sessionsArray.length()) {
            val s = sessionsArray.getJSONObject(i)
            sessionsList.add(
                SessionEntity(
                    id = s.getString("id"),
                    date = s.getString("date"),
                    mode = s.getString("mode"),
                    totalQuestions = s.getInt("totalQuestions"),
                    correct = s.getInt("correct"),
                    avgTimeMs = s.getDouble("avgTimeMs"),
                    difficulty = s.getString("difficulty"),
                    duration = s.getDouble("duration")
                )
            )
        }

        val questionsList = mutableListOf<QuestionRecordEntity>()
        for (i in 0 until questionsArray.length()) {
            val q = questionsArray.getJSONObject(i)
            questionsList.add(
                QuestionRecordEntity(
                    id = q.optLong("id", 0L),
                    sessionId = q.getString("sessionId"),
                    num1 = q.getDouble("num1"),
                    num2 = q.getDouble("num2"),
                    operation = q.getString("operation"),
                    correctAnswer = q.getString("correctAnswer"),
                    userAnswer = if (q.isNull("userAnswer")) null else q.getString("userAnswer"),
                    timeMs = q.getLong("timeMs"),
                    isCorrect = q.getBoolean("isCorrect"),
                    mode = q.getString("mode"),
                    difficulty = q.getString("difficulty")
                )
            )
        }

        dao.clearAll()
        if (sessionsList.isNotEmpty()) {
            dao.insertSessions(sessionsList)
        }
        if (questionsList.isNotEmpty()) {
            dao.insertQuestions(questionsList)
        }
    }

    private class ModeAccumulator {
        var sessions: Int = 0
        var correct: Int = 0
        var total: Int = 0
        var timeMs: Double = 0.0
    }
}
