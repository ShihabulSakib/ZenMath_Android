package com.example.data

import com.example.domain.DateBucketUtils
import com.example.domain.DayBucket
import com.example.domain.ModeStats
import com.example.domain.RecentPerformanceDay
import com.example.domain.StatsData
import com.example.domain.WeakSpotEntry
import com.example.domain.WeakSpotsSummary
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
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

    suspend fun getAllQuestions(): List<QuestionRecordEntity> = dao.getAllQuestions()

    fun getAllQuestionsFlow(): Flow<List<QuestionRecordEntity>> = dao.getAllQuestionsFlow()

    suspend fun insertSession(session: SessionEntity, questions: List<QuestionRecordEntity>) {
        dao.insertSessionWithQuestions(session, questions)
    }

    suspend fun clearAllData() {
        dao.clearAll()
        preferencesManager.clearAllPreferences()
    }

    fun calculateStreak(
        sessions: List<SessionEntity>,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone? = null
    ): Int {
        return DateBucketUtils.calculateStreak(sessions, nowMs, timeZone)
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

    fun getRecentPerformance(
        sessions: List<SessionEntity>,
        days: Int = 7,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<RecentPerformanceDay> {
        val dateList = DateBucketUtils.lastNDates(days, nowMs, timeZone)
        val grouped = DateBucketUtils.groupSessionsByLocalDate(sessions)

        return dateList.map { dateStr ->
            val daySessions = grouped[dateStr] ?: emptyList()
            val correct = daySessions.sumOf { it.correct }
            val totalQ = daySessions.sumOf { it.totalQuestions }
            val sessionsCount = daySessions.size
            RecentPerformanceDay(
                date = dateStr,
                accuracy = if (totalQ > 0) (correct.toDouble() / totalQ.toDouble()) * 100.0 else 0.0,
                count = totalQ,
                sessionsCount = sessionsCount
            )
        }
    }

    fun getHorizonWaveBuckets(
        sessions: List<SessionEntity>,
        daysCount: Int = 10,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<DayBucket> {
        val dateList = DateBucketUtils.lastNDates(daysCount, nowMs, timeZone)
        val grouped = DateBucketUtils.groupSessionsByLocalDate(sessions)

        return dateList.map { dateStr ->
            val label = DateBucketUtils.formatDayLabel(dateStr, timeZone)
            val daySessions = grouped[dateStr] ?: emptyList()
            val qCount = daySessions.sumOf { it.totalQuestions }
            val cCount = daySessions.sumOf { it.correct }
            val sCount = daySessions.size
            val totalTimeSec = daySessions.sumOf { (it.avgTimeMs / 1000.0) * it.totalQuestions }

            val accuracy = if (qCount > 0) ((cCount.toDouble() / qCount.toDouble()) * 100.0).toInt() else 0
            val avgSpeed = if (qCount > 0) totalTimeSec / qCount.toDouble() else 0.0

            DayBucket(
                date = dateStr,
                label = label,
                questions = qCount,
                correct = cCount,
                accuracy = accuracy,
                avgSpeed = avgSpeed,
                sessions = sCount
            )
        }
    }

    /**
     * Aggregates saved question records by `topicTag` and by `questionText` (Section 4.2).
     */
    fun computeWeakSpots(questions: List<QuestionRecordEntity>): WeakSpotsSummary {
        if (questions.isEmpty()) return WeakSpotsSummary()

        // Group by topicTag
        val byTopic = questions.groupBy { q ->
            q.topicTag.ifBlank { q.mode.ifBlank { "general" } }
        }
        val worstTopics = byTopic.map { (tag, records) ->
            val attempts = records.size
            val errors = records.count { !it.isCorrect || it.hintUsed }
            val errorRate = if (attempts > 0) (errors.toDouble() / attempts.toDouble()) * 100.0 else 0.0
            val avgTimeMs = if (attempts > 0) records.sumOf { it.timeMs }.toDouble() / attempts.toDouble() else 0.0
            val latest = records.maxByOrNull { it.id } ?: records.first()
            WeakSpotEntry(
                key = tag,
                displayLabel = tag.replace("_", " ").replace("-", " ").replaceFirstChar { it.uppercase() },
                topicTag = tag,
                modeId = latest.mode,
                attempts = attempts,
                errors = errors,
                errorRate = errorRate,
                avgTimeMs = avgTimeMs,
                lastSeenSessionId = latest.sessionId
            )
        }
            .sortedWith(compareByDescending<WeakSpotEntry> { it.errorRate }.thenByDescending { it.avgTimeMs })
            .take(8)

        // Group by individual questionText
        val byQuestion = questions.groupBy { q ->
            q.questionText.ifBlank {
                if (q.operation in listOf("+", "−", "-", "×", "*", "÷", "/")) {
                    "${q.num1.toLong()} ${q.operation} ${q.num2.toLong()}"
                } else {
                    "${q.operation} (${q.correctAnswer})"
                }
            }
        }
        val worstQuestions = byQuestion.map { (qText, records) ->
            val attempts = records.size
            val errors = records.count { !it.isCorrect || it.hintUsed }
            val errorRate = if (attempts > 0) (errors.toDouble() / attempts.toDouble()) * 100.0 else 0.0
            val avgTimeMs = if (attempts > 0) records.sumOf { it.timeMs }.toDouble() / attempts.toDouble() else 0.0
            val latest = records.maxByOrNull { it.id } ?: records.first()
            val tag = latest.topicTag.ifBlank { latest.mode.ifBlank { "general" } }
            WeakSpotEntry(
                key = qText,
                displayLabel = qText,
                topicTag = tag,
                modeId = latest.mode,
                attempts = attempts,
                errors = errors,
                errorRate = errorRate,
                avgTimeMs = avgTimeMs,
                lastSeenSessionId = latest.sessionId
            )
        }
            .filter { it.errors > 0 || it.avgTimeMs > 5000.0 }
            .sortedWith(compareByDescending<WeakSpotEntry> { it.errorRate }.thenByDescending { it.errors }.thenByDescending { it.avgTimeMs })
            .take(10)

        return WeakSpotsSummary(
            worstTopics = worstTopics,
            worstQuestions = worstQuestions
        )
    }

    suspend fun exportBackupJson(): String {
        val sessions = dao.getAllSessions()
        val questions = dao.getAllQuestions()

        val root = JSONObject()
        root.put("type", "zenmath-backup")
        root.put("version", 2)
        root.put("exportedAt", DateBucketUtils.nowUtcIso())

        val sessionsArray = JSONArray()
        for (s in sessions) {
            val resolvedLocalDate = DateBucketUtils.resolveSessionLocalDate(s)
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("date", s.date)
                put("localDate", resolvedLocalDate)
                put("sessionType", s.sessionType.ifBlank { SessionEntity.SESSION_TYPE_NORMAL })
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
                put("questionText", q.questionText)
                put("topicTag", q.topicTag)
                put("hintUsed", q.hintUsed)
            }
            questionsArray.put(qObj)
        }
        root.put("questions", questionsArray)

        return root.toString(2)
    }

    /**
     * Imports a JSON backup file. Supports both legacy v1 backup files (where `localDate`,
     * `sessionType`, `questionText`, `topicTag`, `hintUsed` are omitted) and v2 backup files.
     */
    suspend fun importBackupJson(jsonStr: String) {
        val root = JSONObject(jsonStr)
        val version = root.optInt("version", 0)
        if (root.optString("type") != "zenmath-backup" || version !in 1..2) {
            throw IllegalArgumentException("Invalid backup file format.")
        }

        val sessionsArray = root.optJSONArray("sessions") ?: JSONArray()
        val questionsArray = root.optJSONArray("questions") ?: JSONArray()

        val sessionsList = mutableListOf<SessionEntity>()
        for (i in 0 until sessionsArray.length()) {
            val s = sessionsArray.getJSONObject(i)
            val utcDate = s.getString("date")
            val localDate = s.optString("localDate", "").ifBlank {
                DateBucketUtils.utcIsoToLocalDate(utcDate)
            }
            val sessionType = s.optString("sessionType", SessionEntity.SESSION_TYPE_NORMAL)
                .ifBlank { SessionEntity.SESSION_TYPE_NORMAL }

            sessionsList.add(
                SessionEntity(
                    id = s.getString("id"),
                    date = utcDate,
                    mode = s.getString("mode"),
                    totalQuestions = s.getInt("totalQuestions"),
                    correct = s.getInt("correct"),
                    avgTimeMs = s.getDouble("avgTimeMs"),
                    difficulty = s.getString("difficulty"),
                    duration = s.getDouble("duration"),
                    localDate = localDate,
                    sessionType = sessionType
                )
            )
        }

        val questionsList = mutableListOf<QuestionRecordEntity>()
        for (i in 0 until questionsArray.length()) {
            val q = questionsArray.getJSONObject(i)
            val num1 = q.optDouble("num1", 0.0)
            val num2 = q.optDouble("num2", 0.0)
            val operation = q.optString("operation", "")
            val mode = q.optString("mode", "addition")
            val fallbackQuestionText = if (operation in listOf("+", "−", "-", "×", "*", "÷", "/")) {
                "${num1.toLong()} $operation ${num2.toLong()}"
            } else {
                operation
            }
            val questionText = q.optString("questionText", "").ifBlank { fallbackQuestionText }
            val topicTag = q.optString("topicTag", "").ifBlank { mode }
            val hintUsed = q.optBoolean("hintUsed", false)

            questionsList.add(
                QuestionRecordEntity(
                    id = q.optLong("id", 0L),
                    sessionId = q.getString("sessionId"),
                    num1 = num1,
                    num2 = num2,
                    operation = operation,
                    correctAnswer = q.getString("correctAnswer"),
                    userAnswer = if (q.isNull("userAnswer")) null else q.optString("userAnswer"),
                    timeMs = q.getLong("timeMs"),
                    isCorrect = q.getBoolean("isCorrect"),
                    mode = mode,
                    difficulty = q.optString("difficulty", "medium"),
                    questionText = questionText,
                    topicTag = topicTag,
                    hintUsed = hintUsed
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
