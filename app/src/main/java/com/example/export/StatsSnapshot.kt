package com.example.export

import com.example.domain.GameMode
import com.example.domain.GameSettings
import com.example.domain.drills.DrillCategory

data class OverviewStats(
    val totalSessions: Int,
    val normalSessions: Int,
    val speedRoundSessions: Int,
    val weakSpotsSessions: Int,
    val mistakeReviewSessions: Int,
    val totalQuestions: Int,
    val totalCorrect: Int,
    val overallAccuracyPct: Double,
    val wilson95LowerPct: Double,
    val totalPracticeSeconds: Double,
    val avgTimeMs: Double,
    val medianTimeMs: Double,
    val p90TimeMs: Double,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val activeDaysCount: Int,
    val dailyGoalHitRatePct: Double
)

data class CategoryStatRow(
    val category: DrillCategory,
    val displayName: String,
    val sessions: Int,
    val attempts: Int,
    val correct: Int,
    val accuracyPct: Double,
    val wilson95LowerPct: Double,
    val avgTimeMs: Double,
    val medianTimeMs: Double,
    val p90TimeMs: Double,
    val practiceSharePct: Double
)

data class DrillStatRow(
    val mode: GameMode,
    val displayName: String,
    val category: DrillCategory,
    val isLegacy: Boolean,
    val sessions: Int,
    val attempts: Int,
    val correct: Int,
    val accuracyPct: Double,
    val wilson95LowerPct: Double,
    val avgTimeMs: Double,
    val medianTimeMs: Double,
    val p90TimeMs: Double,
    val hintRatePct: Double,
    val timeoutRatePct: Double,
    val mcqAttempts: Int,
    val mcqAccuracyPct: Double,
    val typedAttempts: Int,
    val typedAccuracyPct: Double,
    val trendDeltaAccuracyPct: Double?,
    val trendDeltaSpeedMs: Double?,
    val lastPracticedDate: String?
)

data class SubtopicStatRow(
    val topicTag: String,
    val friendlyLabel: String,
    val drillMode: GameMode,
    val drillDisplayName: String,
    val attempts: Int,
    val correct: Int,
    val accuracyPct: Double,
    val wilson95LowerPct: Double,
    val avgTimeMs: Double,
    val hintRatePct: Double
)

data class DailyActivityPoint(
    val localDate: String,
    val sessions: Int,
    val questions: Int,
    val correct: Int,
    val accuracyPct: Double,
    val avgTimeMs: Double,
    val durationSec: Double,
    val hitDailyGoal: Boolean
)

data class WeeklyTrendRow(
    val weekLabel: String,
    val sessions: Int,
    val questions: Int,
    val accuracyPct: Double,
    val avgTimeMs: Double,
    val questionsPerMin: Double
)

data class DiagnosticFinding(
    val title: String,
    val categoryName: String,
    val accuracyPct: Double,
    val wilson95LowerPct: Double,
    val avgTimeMs: Double,
    val attempts: Int,
    val evidenceText: String,
    val recommendationText: String
)

data class MistakePatternSummary(
    val patternType: String,
    val count: Int,
    val sharePct: Double,
    val description: String,
    val examples: List<String>
)

data class MistakeExportEntry(
    val questionText: String,
    val correctAnswer: String,
    val latestUserAnswer: String,
    val drillDisplayName: String,
    val categoryName: String,
    val topicLabel: String,
    val attempts: Int,
    val errors: Int,
    val errorRatePct: Double,
    val avgTimeMs: Double,
    val isResolved: Boolean,
    val lastSeenDate: String
)

data class PunchCardCell(
    val dayOfWeek: Int, // 1 = Mon .. 7 = Sun
    val hourOfDay: Int, // 0 .. 23
    val attempts: Int,
    val accuracyPct: Double
)

data class SessionExportRow(
    val id: String,
    val dateIso: String,
    val localDate: String,
    val modeId: String,
    val modeDisplayName: String,
    val sessionType: String,
    val difficulty: String,
    val totalQuestions: Int,
    val correct: Int,
    val accuracyPct: Double,
    val avgTimeMs: Double,
    val durationSec: Double,
    val scopeLabel: String?
)

data class StatsSnapshot(
    val generatedAtIso: String,
    val generatedLocalDate: String,
    val timezoneId: String,
    val appVersion: String,
    val settings: GameSettings,
    val overview: OverviewStats,
    val categories: List<CategoryStatRow>,
    val drills: List<DrillStatRow>,
    val subtopics: List<SubtopicStatRow>,
    val dailyActivity: List<DailyActivityPoint>,
    val weeklyTrends: List<WeeklyTrendRow>,
    val strengths: List<DiagnosticFinding>,
    val weaknesses: List<DiagnosticFinding>,
    val mistakePatterns: List<MistakePatternSummary>,
    val mistakes: List<MistakeExportEntry>,
    val totalDistinctMistakes: Int,
    val punchCard: List<PunchCardCell>,
    val recentSessions: List<SessionExportRow>
)
