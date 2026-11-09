package com.example.export

import com.example.data.QuestionRecordEntity
import com.example.data.SessionEntity
import com.example.domain.DateBucketUtils
import com.example.domain.GameMode
import com.example.domain.GameSettings
import com.example.domain.ScientificInputParser
import com.example.domain.drills.DrillCategory
import com.example.domain.drills.DrillRegistry
import com.example.domain.drills.TopicTagResolver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object StatsSnapshotBuilder {

    const val MAX_EXPORT_MISTAKES = 250
    const val MAX_EXPORT_SESSIONS = 100

    fun wilson95LowerBoundPct(correct: Int, total: Int): Double {
        if (total <= 0) return 0.0
        val n = total.toDouble()
        val pHat = (correct.coerceIn(0, total)).toDouble() / n
        val z = 1.96
        val z2 = z * z
        val numerator = pHat + (z2 / (2.0 * n)) - z * sqrt((pHat * (1.0 - pHat) + z2 / (4.0 * n)) / n)
        val denominator = 1.0 + (z2 / n)
        return ((numerator / denominator).coerceIn(0.0, 1.0)) * 100.0
    }

    fun percentileMs(values: List<Long>, quantile: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val pos = (quantile.coerceIn(0.0, 1.0) * (sorted.size - 1)).toDouble()
        val lower = pos.toInt()
        val upper = (lower + 1).coerceAtMost(sorted.lastIndex)
        val weight = pos - lower
        return sorted[lower] * (1.0 - weight) + sorted[upper] * weight
    }

    fun build(
        sessions: List<SessionEntity>,
        questions: List<QuestionRecordEntity>,
        settings: GameSettings,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        appVersion: String = "1.0"
    ): StatsSnapshot {
        val generatedIso = DateBucketUtils.nowUtcIso(nowMs)
        val generatedLocalDate = DateBucketUtils.formatLocalDate(nowMs, timeZone)
        val sessionById = sessions.associateBy { it.id }

        // Overview stats
        val totalSessions = sessions.size
        val normalSessions = sessions.count { it.sessionType == SessionEntity.SESSION_TYPE_NORMAL }
        val speedRoundSessions = sessions.count { it.sessionType == SessionEntity.SESSION_TYPE_SPEED_ROUND }
        val weakSpotsSessions = sessions.count { it.sessionType == SessionEntity.SESSION_TYPE_WEAK_SPOTS }
        val mistakeReviewSessions = sessions.count { it.sessionType == SessionEntity.SESSION_TYPE_MISTAKE_REVIEW }

        val totalQFromSessions = sessions.sumOf { it.totalQuestions }
        val totalCFromSessions = sessions.sumOf { it.correct }
        val totalQuestions = max(totalQFromSessions, questions.size)
        val totalCorrect = if (questions.size >= totalQFromSessions && questions.isNotEmpty()) {
            questions.count { it.isCorrect }
        } else {
            totalCFromSessions
        }
        val overallAcc = if (totalQuestions > 0) (totalCorrect.toDouble() / totalQuestions.toDouble()) * 100.0 else 0.0
        val overallWilson = wilson95LowerBoundPct(totalCorrect, totalQuestions)
        val totalPracticeSeconds = sessions.sumOf { it.duration }

        val allLatencies = questions.map { it.timeMs }.filter { it > 0L }
        val avgTimeMs = when {
            allLatencies.isNotEmpty() -> allLatencies.average()
            totalQuestions > 0 -> sessions.sumOf { it.avgTimeMs * it.totalQuestions } / totalQuestions.toDouble()
            else -> 0.0
        }
        val medianTimeMs = percentileMs(allLatencies, 0.50)
        val p90TimeMs = percentileMs(allLatencies, 0.90)

        val sessionsByDate = sessions.groupBy { s ->
            s.localDate.ifBlank { DateBucketUtils.utcIsoToLocalDate(s.date, timeZone) }
        }
        val activeDates = sessionsByDate.keys.filter { it.isNotBlank() }.sorted()
        val activeDaysCount = activeDates.size
        val dailyGoalHitCount = sessionsByDate.values.count { daySessions ->
            daySessions.size >= settings.dailyGoal.coerceAtLeast(1)
        }
        val dailyGoalHitRatePct = if (activeDaysCount > 0) {
            (dailyGoalHitCount.toDouble() / activeDaysCount.toDouble()) * 100.0
        } else 0.0

        val (currentStreak, bestStreak) = computeStreaks(activeDates.toSet(), generatedLocalDate, timeZone)

        val overview = OverviewStats(
            totalSessions = totalSessions,
            normalSessions = normalSessions,
            speedRoundSessions = speedRoundSessions,
            weakSpotsSessions = weakSpotsSessions,
            mistakeReviewSessions = mistakeReviewSessions,
            totalQuestions = totalQuestions,
            totalCorrect = totalCorrect,
            overallAccuracyPct = overallAcc,
            wilson95LowerPct = overallWilson,
            totalPracticeSeconds = totalPracticeSeconds,
            avgTimeMs = avgTimeMs,
            medianTimeMs = medianTimeMs,
            p90TimeMs = p90TimeMs,
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            activeDaysCount = activeDaysCount,
            dailyGoalHitRatePct = dailyGoalHitRatePct
        )

        // Resolve canonical drill mode for every question record
        val resolvedQuestions = questions.map { q ->
            val rawMode = TopicTagResolver.resolveDrillMode(q.topicTag, q.mode, q.drillMode)
            val canonical = TopicTagResolver.canonicalActiveMode(rawMode)
            canonical to q
        }

        // Drills breakdown
        val questionsByDrill = resolvedQuestions.groupBy({ it.first }, { it.second })
        val sessionsByMode = sessions.groupBy { s ->
            TopicTagResolver.canonicalActiveMode(
                GameMode.entries.find { it.id == s.mode } ?: GameMode.ADDITION
            )
        }

        val allRelevantModes = (DrillRegistry.all.map { it.mode } + questionsByDrill.keys + sessionsByMode.keys)
            .distinct()
            .filter { mode ->
                !mode.isLegacy || questionsByDrill[mode]?.isNotEmpty() == true || sessionsByMode[mode]?.isNotEmpty() == true
            }

        val globalAvgSpeedMs = avgTimeMs.takeIf { it > 0.0 } ?: 3000.0

        val drillRows = allRelevantModes.mapNotNull { mode ->
            val qList = questionsByDrill[mode].orEmpty()
            val sList = sessionsByMode[mode].orEmpty()
            if (qList.isEmpty() && sList.isEmpty()) return@mapNotNull null

            val descriptor = DrillRegistry.of(mode)
            val attempts = if (qList.isNotEmpty()) qList.size else sList.sumOf { it.totalQuestions }
            val correct = if (qList.isNotEmpty()) qList.count { it.isCorrect } else sList.sumOf { it.correct }
            if (attempts <= 0) return@mapNotNull null

            val acc = (correct.toDouble() / attempts.toDouble()) * 100.0
            val wilson = wilson95LowerBoundPct(correct, attempts)
            val latencies = qList.map { it.timeMs }.filter { it > 0L }
            val dAvgTime = if (latencies.isNotEmpty()) {
                latencies.average()
            } else if (sList.isNotEmpty()) {
                val tot = sList.sumOf { it.totalQuestions }.coerceAtLeast(1)
                sList.sumOf { it.avgTimeMs * it.totalQuestions } / tot.toDouble()
            } else 0.0
            val dMedian = percentileMs(latencies, 0.50)
            val dP90 = percentileMs(latencies, 0.90)

            val hints = qList.count { it.hintUsed }
            val timeouts = qList.count { !it.isCorrect && it.userAnswer.isNullOrBlank() }
            val mcqQs = qList.filter { it.answerFormat == "mcq" }
            val typedQs = qList.filter { it.answerFormat != "mcq" }

            // Compute recent vs early half trend if >= 6 question records
            val sortedQs = qList.sortedBy { it.id }
            val (trendAccDelta, trendSpeedDelta) = if (sortedQs.size >= 6) {
                val mid = sortedQs.size / 2
                val early = sortedQs.subList(0, mid)
                val recent = sortedQs.subList(mid, sortedQs.size)
                val earlyAcc = (early.count { it.isCorrect }.toDouble() / early.size) * 100.0
                val recentAcc = (recent.count { it.isCorrect }.toDouble() / recent.size) * 100.0
                val earlySpd = early.map { it.timeMs }.average()
                val recentSpd = recent.map { it.timeMs }.average()
                (recentAcc - earlyAcc) to (recentSpd - earlySpd)
            } else {
                null to null
            }

            val lastDate = qList.mapNotNull { q ->
                sessionById[q.sessionId]?.let { s ->
                    s.localDate.ifBlank { DateBucketUtils.utcIsoToLocalDate(s.date, timeZone) }
                }
            }.maxOrNull() ?: sList.maxOfOrNull { s ->
                s.localDate.ifBlank { DateBucketUtils.utcIsoToLocalDate(s.date, timeZone) }
            }

            DrillStatRow(
                mode = mode,
                displayName = descriptor.title,
                category = descriptor.category,
                isLegacy = mode.isLegacy,
                sessions = sList.size,
                attempts = attempts,
                correct = correct,
                accuracyPct = acc,
                wilson95LowerPct = wilson,
                avgTimeMs = dAvgTime,
                medianTimeMs = dMedian,
                p90TimeMs = dP90,
                hintRatePct = if (qList.isNotEmpty()) (hints.toDouble() / qList.size) * 100.0 else 0.0,
                timeoutRatePct = if (qList.isNotEmpty()) (timeouts.toDouble() / qList.size) * 100.0 else 0.0,
                mcqAttempts = mcqQs.size,
                mcqAccuracyPct = if (mcqQs.isNotEmpty()) (mcqQs.count { it.isCorrect }.toDouble() / mcqQs.size) * 100.0 else 0.0,
                typedAttempts = typedQs.size,
                typedAccuracyPct = if (typedQs.isNotEmpty()) (typedQs.count { it.isCorrect }.toDouble() / typedQs.size) * 100.0 else 0.0,
                trendDeltaAccuracyPct = trendAccDelta,
                trendDeltaSpeedMs = trendSpeedDelta,
                lastPracticedDate = lastDate
            )
        }.sortedByDescending { it.attempts }

        // Category rows
        val totalDrillAttempts = drillRows.sumOf { it.attempts }.coerceAtLeast(1)
        val displayCategories = DrillCategory.entries
        val categoryRows = displayCategories.mapNotNull { cat ->
            val catDrills = drillRows.filter { it.category == cat }
            if (catDrills.isEmpty()) return@mapNotNull null
            val catAttempts = catDrills.sumOf { it.attempts }
            val catCorrect = catDrills.sumOf { it.correct }
            val catSessions = catDrills.sumOf { it.sessions }
            val catQuestions = resolvedQuestions.filter { (m, _) ->
                DrillRegistry.categoryOf(m) == cat
            }.map { it.second }
            val catLatencies = catQuestions.map { it.timeMs }.filter { it > 0L }
            val catAvgMs = if (catLatencies.isNotEmpty()) {
                catLatencies.average()
            } else {
                catDrills.sumOf { it.avgTimeMs * it.attempts } / catAttempts.coerceAtLeast(1)
            }
            CategoryStatRow(
                category = cat,
                displayName = cat.label,
                sessions = catSessions,
                attempts = catAttempts,
                correct = catCorrect,
                accuracyPct = (catCorrect.toDouble() / catAttempts.coerceAtLeast(1)) * 100.0,
                wilson95LowerPct = wilson95LowerBoundPct(catCorrect, catAttempts),
                avgTimeMs = catAvgMs,
                medianTimeMs = percentileMs(catLatencies, 0.50),
                p90TimeMs = percentileMs(catLatencies, 0.90),
                practiceSharePct = (catAttempts.toDouble() / totalDrillAttempts.toDouble()) * 100.0
            )
        }

        // Subtopic rows
        val subtopicRows = resolvedQuestions
            .groupBy { (mode, q) -> mode to q.topicTag.ifBlank { mode.id } }
            .mapNotNull { (pair, items) ->
                val (mode, tag) = pair
                val qList = items.map { it.second }
                if (qList.isEmpty()) return@mapNotNull null
                val c = qList.count { it.isCorrect }
                val n = qList.size
                SubtopicStatRow(
                    topicTag = tag,
                    friendlyLabel = TopicTagResolver.friendlyTopicLabel(tag),
                    drillMode = mode,
                    drillDisplayName = DrillRegistry.of(mode).title,
                    attempts = n,
                    correct = c,
                    accuracyPct = (c.toDouble() / n.toDouble()) * 100.0,
                    wilson95LowerPct = wilson95LowerBoundPct(c, n),
                    avgTimeMs = qList.map { it.timeMs }.average(),
                    hintRatePct = (qList.count { it.hintUsed }.toDouble() / n.toDouble()) * 100.0
                )
            }
            .sortedWith(compareBy<SubtopicStatRow> { it.wilson95LowerPct }.thenByDescending { it.attempts })

        // Daily activity (last 182 days / 26 weeks chronological)
        val dailyActivity = buildDailyActivity(sessionsByDate, settings.dailyGoal, nowMs, timeZone, days = 182)

        // Weekly trends (last 12 weeks)
        val weeklyTrends = buildWeeklyTrends(dailyActivity)

        // Strengths & Weaknesses
        val eligibleDrills = drillRows.filter { it.attempts >= 3 && !it.isLegacy }
        val candidatePool = if (eligibleDrills.size >= 2) eligibleDrills else drillRows
        val strengths = candidatePool
            .sortedWith(compareByDescending<DrillStatRow> { it.wilson95LowerPct }.thenBy { it.avgTimeMs })
            .take(4)
            .map { d ->
                val speedRatio = if (globalAvgSpeedMs > 0) d.avgTimeMs / globalAvgSpeedMs else 1.0
                val speedNote = when {
                    speedRatio < 0.85 -> "faster than your overall average (${formatSec(d.avgTimeMs)} vs ${formatSec(globalAvgSpeedMs)})"
                    speedRatio > 1.15 -> "steady accuracy at ${formatSec(d.avgTimeMs)} per question"
                    else -> "consistent pace (${formatSec(d.avgTimeMs)} avg)"
                }
                DiagnosticFinding(
                    title = d.displayName,
                    categoryName = d.category.label,
                    accuracyPct = d.accuracyPct,
                    wilson95LowerPct = d.wilson95LowerPct,
                    avgTimeMs = d.avgTimeMs,
                    attempts = d.attempts,
                    evidenceText = "${String.format(Locale.US, "%.1f%%", d.accuracyPct)} accuracy (${d.correct}/${d.attempts}, Wilson 95% LB ${String.format(Locale.US, "%.1f%%", d.wilson95LowerPct)}), $speedNote.",
                    recommendationText = "Maintain mastery via periodic Speed Round inclusion or step up difficulty."
                )
            }

        val weaknesses = candidatePool
            .sortedWith(compareBy<DrillStatRow> { it.wilson95LowerPct }.thenByDescending { it.avgTimeMs })
            .filter { w -> strengths.none { it.title == w.displayName } || candidatePool.size <= 3 }
            .take(4)
            .map { d ->
                val worstSub = subtopicRows
                    .filter { it.drillMode == d.mode && it.attempts >= 2 }
                    .minByOrNull { it.wilson95LowerPct }
                val subNote = worstSub?.let {
                    " Weakest sub-topic: ${it.friendlyLabel} (${String.format(Locale.US, "%.0f%%", it.accuracyPct)} over ${it.attempts}Q)."
                } ?: ""
                DiagnosticFinding(
                    title = d.displayName,
                    categoryName = d.category.label,
                    accuracyPct = d.accuracyPct,
                    wilson95LowerPct = d.wilson95LowerPct,
                    avgTimeMs = d.avgTimeMs,
                    attempts = d.attempts,
                    evidenceText = "${String.format(Locale.US, "%.1f%%", d.accuracyPct)} accuracy (${d.correct}/${d.attempts}, Wilson 95% LB ${String.format(Locale.US, "%.1f%%", d.wilson95LowerPct)}), avg response ${formatSec(d.avgTimeMs)}.$subNote",
                    recommendationText = "Run targeted Mistake Review sessions on ${d.displayName} and review solution steps before timed practice."
                )
            }

        // Mistake pattern classification & Mistake export log
        val wrongRecords = questions.filter { !it.isCorrect }
        val mistakePatterns = buildMistakePatterns(wrongRecords)
        val (mistakeEntries, totalDistinctMistakes) = buildMistakeLog(
            resolvedQuestions = resolvedQuestions,
            sessionById = sessionById,
            timeZone = timeZone,
            cap = MAX_EXPORT_MISTAKES
        )

        // Punch card (dayOfWeek 1..7 x hourOfDay 0..23)
        val punchCard = buildPunchCard(sessions, questions, timeZone)

        // Recent sessions
        val recentSessions = sessions
            .sortedByDescending { it.date }
            .take(MAX_EXPORT_SESSIONS)
            .map { s ->
                val modeEnum = GameMode.entries.find { it.id == s.mode }
                val modeName = when {
                    s.sessionType == SessionEntity.SESSION_TYPE_MISTAKE_REVIEW -> "Mistake Review"
                    modeEnum != null -> DrillRegistry.of(modeEnum).title
                    else -> s.mode.replace("-", " ").replaceFirstChar { it.uppercase() }
                }
                val acc = if (s.totalQuestions > 0) (s.correct.toDouble() / s.totalQuestions.toDouble()) * 100.0 else 0.0
                SessionExportRow(
                    id = s.id,
                    dateIso = s.date,
                    localDate = s.localDate.ifBlank { DateBucketUtils.utcIsoToLocalDate(s.date, timeZone) },
                    modeId = s.mode,
                    modeDisplayName = modeName,
                    sessionType = s.sessionType,
                    difficulty = s.difficulty,
                    totalQuestions = s.totalQuestions,
                    correct = s.correct,
                    accuracyPct = acc,
                    avgTimeMs = s.avgTimeMs,
                    durationSec = s.duration,
                    scopeLabel = s.scopeLabel.takeIf { it.isNotBlank() }
                )
            }

        return StatsSnapshot(
            generatedAtIso = generatedIso,
            generatedLocalDate = generatedLocalDate,
            timezoneId = timeZone.id,
            appVersion = appVersion,
            settings = settings,
            overview = overview,
            categories = categoryRows,
            drills = drillRows,
            subtopics = subtopicRows,
            dailyActivity = dailyActivity,
            weeklyTrends = weeklyTrends,
            strengths = strengths,
            weaknesses = weaknesses,
            mistakePatterns = mistakePatterns,
            mistakes = mistakeEntries,
            totalDistinctMistakes = totalDistinctMistakes,
            punchCard = punchCard,
            recentSessions = recentSessions
        )
    }

    private fun formatSec(ms: Double): String =
        String.format(Locale.US, "%.1fs", ms / 1000.0)

    private fun computeStreaks(
        activeDates: Set<String>,
        todayStr: String,
        timeZone: TimeZone
    ): Pair<Int, Int> {
        if (activeDates.isEmpty()) return 0 to 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
            isLenient = false
        }

        // Current streak ending today or yesterday
        val cal = Calendar.getInstance(timeZone)
        cal.time = sdf.parse(todayStr) ?: Date()
        var currentStreak = 0
        var checkStr = sdf.format(cal.time)
        if (checkStr !in activeDates) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkStr = sdf.format(cal.time)
        }
        while (checkStr in activeDates) {
            currentStreak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkStr = sdf.format(cal.time)
        }

        // Best streak across sorted dates
        val sortedEpochDays = activeDates.mapNotNull { dStr ->
            try {
                val d = sdf.parse(dStr) ?: return@mapNotNull null
                val c = Calendar.getInstance(timeZone).apply {
                    time = d
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                c.timeInMillis / 86_400_000L
            } catch (_: Exception) {
                null
            }
        }.distinct().sorted()

        var best = 0
        var run = 0
        var prevDay: Long? = null
        for (day in sortedEpochDays) {
            if (prevDay == null || day == prevDay + 1L) {
                run++
            } else {
                run = 1
            }
            if (run > best) best = run
            prevDay = day
        }

        return currentStreak to max(best, currentStreak)
    }

    private fun buildDailyActivity(
        sessionsByDate: Map<String, List<SessionEntity>>,
        dailyGoal: Int,
        nowMs: Long,
        timeZone: TimeZone,
        days: Int
    ): List<DailyActivityPoint> {
        val dateKeys = DateBucketUtils.lastNDates(days, nowMs, timeZone)
        return dateKeys.map { dateStr ->
            val daySessions = sessionsByDate[dateStr].orEmpty()
            val qCount = daySessions.sumOf { it.totalQuestions }
            val cCount = daySessions.sumOf { it.correct }
            val acc = if (qCount > 0) (cCount.toDouble() / qCount.toDouble()) * 100.0 else 0.0
            val avgMs = if (qCount > 0) {
                daySessions.sumOf { it.avgTimeMs * it.totalQuestions } / qCount.toDouble()
            } else 0.0
            val dur = daySessions.sumOf { it.duration }
            DailyActivityPoint(
                localDate = dateStr,
                sessions = daySessions.size,
                questions = qCount,
                correct = cCount,
                accuracyPct = acc,
                avgTimeMs = avgMs,
                durationSec = dur,
                hitDailyGoal = daySessions.size >= dailyGoal.coerceAtLeast(1)
            )
        }
    }

    private fun buildWeeklyTrends(dailyActivity: List<DailyActivityPoint>): List<WeeklyTrendRow> {
        if (dailyActivity.isEmpty()) return emptyList()
        return dailyActivity
            .chunked(7)
            .takeLast(12)
            .mapNotNull { weekDays ->
                if (weekDays.isEmpty()) return@mapNotNull null
                val start = weekDays.first().localDate
                val end = weekDays.last().localDate
                val sessions = weekDays.sumOf { it.sessions }
                val questions = weekDays.sumOf { it.questions }
                val correct = weekDays.sumOf { it.correct }
                val durSec = weekDays.sumOf { it.durationSec }
                val acc = if (questions > 0) (correct.toDouble() / questions.toDouble()) * 100.0 else 0.0
                val avgMs = if (questions > 0) {
                    weekDays.sumOf { it.avgTimeMs * it.questions } / questions.toDouble()
                } else 0.0
                val qpm = if (durSec > 0.0) (questions.toDouble() * 60.0) / durSec else 0.0
                WeeklyTrendRow(
                    weekLabel = "$start .. $end",
                    sessions = sessions,
                    questions = questions,
                    accuracyPct = acc,
                    avgTimeMs = avgMs,
                    questionsPerMin = qpm
                )
            }
    }

    private fun buildMistakePatterns(wrongRecords: List<QuestionRecordEntity>): List<MistakePatternSummary> {
        if (wrongRecords.isEmpty()) return emptyList()
        val total = wrongRecords.size.toDouble()

        val descriptions = mapOf(
            "Timeout / Unanswered" to "Timer expired or question was skipped before submitting an answer.",
            "Sign Error" to "Numeric magnitude matched the target answer, but the +/- sign was inverted.",
            "Order-of-Magnitude / Exponent Error" to "Answer was off by a power of 10 (e.g., decimal shift or exponent slip).",
            "Near-Miss / Rounding Drift" to "Answer was within 5% or 0.15 of the exact value, indicating estimation or rounding drift.",
            "Calculation / Recall Error" to "Factual recall miss or multi-step arithmetic miscalculation."
        )

        val grouped = wrongRecords.groupBy { classifySingleMistake(it) }
        return grouped.entries
            .sortedByDescending { it.value.size }
            .map { (pattern, list) ->
                val examples = list.take(3).map { q ->
                    val qTxt = q.questionText.ifBlank { "${q.num1} ${q.operation} ${q.num2}" }
                    "$qTxt → answered '${q.userAnswer ?: "timeout"}', expected '${q.correctAnswer}'"
                }
                MistakePatternSummary(
                    patternType = pattern,
                    count = list.size,
                    sharePct = (list.size.toDouble() / total) * 100.0,
                    description = descriptions[pattern] ?: "",
                    examples = examples
                )
            }
    }

    private fun classifySingleMistake(q: QuestionRecordEntity): String {
        val rawUser = q.userAnswer?.trim().orEmpty()
        if (rawUser.isEmpty()) return "Timeout / Unanswered"

        val userVal = ScientificInputParser.parse(rawUser)?.value
        val corrVal = ScientificInputParser.parse(q.correctAnswer.trim())?.value

        if (userVal != null && corrVal != null) {
            if (corrVal != 0.0 && abs(abs(userVal) - abs(corrVal)) <= 1e-4 && userVal * corrVal < 0) {
                return "Sign Error"
            }
            if (userVal != 0.0 && corrVal != 0.0 && userVal * corrVal > 0) {
                val ratioLog = log10(abs(userVal / corrVal))
                val nearestInt = ratioLog.roundToInt()
                if (nearestInt != 0 && abs(ratioLog - nearestInt) <= 0.04) {
                    return "Order-of-Magnitude / Exponent Error"
                }
            }
            val diff = abs(userVal - corrVal)
            val threshold = max(0.15, abs(corrVal) * 0.05)
            if (diff <= threshold) {
                return "Near-Miss / Rounding Drift"
            }
        }
        return "Calculation / Recall Error"
    }

    private fun buildMistakeLog(
        resolvedQuestions: List<Pair<GameMode, QuestionRecordEntity>>,
        sessionById: Map<String, SessionEntity>,
        timeZone: TimeZone,
        cap: Int
    ): Pair<List<MistakeExportEntry>, Int> {
        val grouped = resolvedQuestions.groupBy { (mode, q) ->
            val qText = q.questionText.trim().ifBlank { "${q.num1.toInt()} ${q.operation} ${q.num2.toInt()}" }
            mode to qText
        }

        val allMistakes = mutableListOf<MistakeExportEntry>()
        for ((pair, list) in grouped) {
            val (mode, qText) = pair
            val attempts = list.map { it.second }
            val wrongAttempts = attempts.filter { !it.isCorrect }
            if (wrongAttempts.isEmpty()) continue

            val latestAttempt = attempts.maxByOrNull { it.id } ?: continue
            val latestWrong = wrongAttempts.maxByOrNull { it.id } ?: continue
            val isResolved = latestAttempt.isCorrect && !latestAttempt.hintUsed
            val descriptor = DrillRegistry.of(mode)
            val cat = descriptor.category
            val lastDate = sessionById[latestAttempt.sessionId]?.let { s ->
                s.localDate.ifBlank { DateBucketUtils.utcIsoToLocalDate(s.date, timeZone) }
            } ?: "—"

            allMistakes.add(
                MistakeExportEntry(
                    questionText = qText,
                    correctAnswer = latestWrong.correctAnswer,
                    latestUserAnswer = latestWrong.userAnswer?.takeIf { it.isNotBlank() } ?: "timeout",
                    drillDisplayName = descriptor.title,
                    categoryName = cat.label,
                    topicLabel = TopicTagResolver.friendlyTopicLabel(latestWrong.topicTag.ifBlank { mode.id }),
                    attempts = attempts.size,
                    errors = wrongAttempts.size,
                    errorRatePct = (wrongAttempts.size.toDouble() / attempts.size.toDouble()) * 100.0,
                    avgTimeMs = attempts.map { it.timeMs }.average(),
                    isResolved = isResolved,
                    lastSeenDate = lastDate
                )
            )
        }

        val sorted = allMistakes.sortedWith(
            compareBy<MistakeExportEntry> { it.isResolved }
                .thenByDescending { it.errors }
                .thenByDescending { it.errorRatePct }
                .thenByDescending { it.avgTimeMs }
        )

        return sorted.take(cap) to sorted.size
    }

    private fun buildPunchCard(
        sessions: List<SessionEntity>,
        questions: List<QuestionRecordEntity>,
        timeZone: TimeZone
    ): List<PunchCardCell> {
        val isoFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        val sessionTimeMap = sessions.mapNotNull { s ->
            var parsed: Date? = null
            for (p in isoFormats) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.US).apply {
                        this.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    parsed = sdf.parse(s.date)
                    if (parsed != null) break
                } catch (_: Exception) {}
            }
            if (parsed != null) s.id to parsed else null
        }.toMap()

        // Map (dayOfWeek 1..7, hour 0..23) -> Pair(attempts, correct)
        val cellCounts = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()

        if (questions.isNotEmpty()) {
            for (q in questions) {
                val date = sessionTimeMap[q.sessionId] ?: continue
                val cal = Calendar.getInstance(timeZone).apply { time = date }
                val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val key = dow to hour
                val prev = cellCounts[key] ?: (0 to 0)
                cellCounts[key] = (prev.first + 1) to (prev.second + if (q.isCorrect) 1 else 0)
            }
        } else {
            for (s in sessions) {
                val date = sessionTimeMap[s.id] ?: continue
                val cal = Calendar.getInstance(timeZone).apply { time = date }
                val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val key = dow to hour
                val prev = cellCounts[key] ?: (0 to 0)
                cellCounts[key] = (prev.first + s.totalQuestions) to (prev.second + s.correct)
            }
        }

        return cellCounts.entries
            .sortedWith(compareBy<Map.Entry<Pair<Int, Int>, Pair<Int, Int>>> { it.key.first }.thenBy { it.key.second })
            .map { (key, counts) ->
                val (dow, hour) = key
                val (att, corr) = counts
                PunchCardCell(
                    dayOfWeek = dow,
                    hourOfDay = hour,
                    attempts = att,
                    accuracyPct = if (att > 0) (corr.toDouble() / att.toDouble()) * 100.0 else 0.0
                )
            }
    }
}
