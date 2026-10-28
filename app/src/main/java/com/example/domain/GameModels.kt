package com.example.domain

enum class Difficulty(val key: String, val label: String) {
    EASY("easy", "Easy"),
    MEDIUM("medium", "Medium"),
    HARD("hard", "Hard");

    companion object {
        fun fromKey(key: String): Difficulty {
            return entries.find { it.key == key } ?: MEDIUM
        }
    }
}

enum class GameMode(
    val id: String,
    val displayName: String,
    val defaultSymbol: String,
    val usesDifficulty: Boolean = true,
    val supportsMcqOption: Boolean = false
) {
    ADDITION("addition", "Addition", "+", usesDifficulty = true),
    SUBTRACTION("subtraction", "Subtraction", "−", usesDifficulty = true),
    MULTIPLICATION("multiplication", "Multiplication", "×", usesDifficulty = true),
    DIVISION("division", "Division", "÷", usesDifficulty = true),
    MIXED("mixed", "Mixed Operations", "shuffle", usesDifficulty = true),
    MULTIPLICATION_TABLE("multiplication-table", "Tables", "grid_on", usesDifficulty = false),
    FACTOR_FINDING("factor-finding", "Factors", "exposure", usesDifficulty = false),
    SQUARE("square", "Squares", "x²", usesDifficulty = false),
    FRACTION("fraction", "Fractions", "½", usesDifficulty = false),
    PERCENTAGE("percentage", "Percent", "%", usesDifficulty = true),
    SQUARE_ROOT("square-root", "Roots", "√", usesDifficulty = true),
    APPROXIMATION("approximation", "Estimation", "≈", usesDifficulty = true, supportsMcqOption = true),
    NUMBER_SERIES("number-series", "Series", "linear_scale", usesDifficulty = true),
    RATIO("ratio", "Ratio", "::", usesDifficulty = true),
    CHAIN_CALCULATION("chain-calculation", "Chain", "multiple_stop", usesDifficulty = true),

    // Section 3, 4, 5 new modes
    MEMORY_TABLES("memory-tables", "Memory Tables", "log/√", usesDifficulty = false, supportsMcqOption = true),
    SCIENTIFIC_NOTATION("scientific-notation", "Sci. Notation", "×10ⁿ", usesDifficulty = false, supportsMcqOption = true),
    PH_POH("ph-poh", "pH & pOH", "pH", usesDifficulty = false, supportsMcqOption = true),
    COMPOSITE_LOGS_ROOTS("composite-logs-roots", "Logs & Roots", "log k", usesDifficulty = true, supportsMcqOption = true),
    CUBES_AND_POWERS("cubes-and-powers", "Cubes & Powers", "x³", usesDifficulty = false, supportsMcqOption = true),
    CONSTANTS("constants", "Constants", "kₑ", usesDifficulty = false, supportsMcqOption = true),
    ATOMIC_MASSES("atomic-masses", "Atomic Masses", "Mᵣ", usesDifficulty = false, supportsMcqOption = true),
    UNIT_CONVERSIONS("unit-conversions", "Conversions", "⇄", usesDifficulty = false, supportsMcqOption = true),
    MATH_CONSTANTS("math-constants", "Math Constants", "π", usesDifficulty = false, supportsMcqOption = true),
    REFERENCE_VALUES("reference-values", "Ref Values", "ref", usesDifficulty = false, supportsMcqOption = true),
    SPEED_ROUND("speed-round", "Speed Round", "⚡", usesDifficulty = false, supportsMcqOption = false),
    WEAK_SPOTS("weak-spots", "Weak Spots", "🎯", usesDifficulty = false, supportsMcqOption = false);

    val supportsMcq: Boolean get() = supportsMcqOption

    companion object {
        fun fromId(id: String): GameMode {
            return entries.find { it.id == id } ?: ADDITION
        }
    }
}

enum class DrillDirection(val key: String, val label: String) {
    FORWARD("forward", "Forward"),
    REVERSE("reverse", "Reverse"),
    MIXED("mixed", "Mixed")
}

typealias MemoryDirection = DrillDirection

enum class SciNotationSubtype(val key: String, val label: String) {
    ALL("all", "All Subtypes"),
    NORMALIZE("normalize", "Normalize Decimal"),
    DECIMAL_SHIFT("shift", "Shift Direction Rule"),
    MULTIPLY_DIVIDE("mul_div", "Multiply & Divide"),
    ROOT_POWER_TEN("root_ten", "Roots of 10ⁿ")
}

enum class PhDrillSubtype(val key: String, val label: String) {
    ALL("all", "All Subtypes"),
    CONC_TO_PH("conc_to_ph", "Concentration → pH"),
    SCI_CONC_TO_PH("sci_conc_to_ph", "a×10⁻ⁿ M → pH"),
    PH_TO_CONC("ph_to_conc", "pH → [H⁺]"),
    PH_POH_CONV("ph_poh_conv", "pH ↔ pOH"),
    PERCENT_WV_TO_PH("percent_wv_to_ph", "% w/v Base → pH")
}

typealias PhSubType = PhDrillSubtype

enum class CompositeLogRootSubtype(val key: String, val label: String) {
    ALL("all", "All Subtypes"),
    LOG_SCI("log_sci", "log₁₀(k × 10ⁿ)"),
    LOG_PRODUCT("log_prod", "log₁₀(a × b) Composite"),
    ROOT_SCI("root_sci", "√(a × 10ⁿ)")
}

enum class CubesPowersSubtype(val key: String, val label: String) {
    ALL("all", "All Subtypes"),
    CUBES("cubes", "Cubes (n³)"),
    POWERS("powers", "Small Powers (aᵇ)"),
    PERFECT_CUBE_ROOTS("cube_roots", "Cube Roots (∛n)");

    companion object {
        val SMALL_POWERS = POWERS
        val CUBE_ROOTS = PERFECT_CUBE_ROOTS
    }
}

/**
 * Monotonic millisecond clock (Section 1.7).
 * Uses `System.nanoTime() / 1_000_000L` which is guaranteed monotonic across Android and JVM tests.
 */
object MonotonicClock {
    fun nowMs(): Long = System.nanoTime() / 1_000_000L
}

data class QuestionResult(
    val num1: Double,
    val num2: Double,
    val operation: String,
    val correctAnswer: String,
    val userAnswer: String?,
    val isCorrect: Boolean,
    val timeTaken: Int, // seconds (preserved for compatibility)
    val timedOut: Boolean,
    val timeTakenMs: Long = timeTaken.toLong() * 1000L,
    val questionText: String = "",
    val topicTag: String = "",
    val hintUsed: Boolean = false,
    val solutionSteps: List<String> = emptyList(),
    val unitText: String? = null
)

data class ModeStats(
    val sessions: Int,
    val accuracy: Double,
    val avgTimeMs: Double
)

data class StatsData(
    val totalSessions: Int,
    val totalQuestions: Int,
    val totalCorrect: Int,
    val avgAccuracy: Double,
    val avgTimeMs: Double,
    val streakDays: Int,
    val modes: Map<String, ModeStats>
)

data class RecentPerformanceDay(
    val date: String,
    val accuracy: Double,
    val count: Int,
    val sessionsCount: Int = 0
) {
    val totalQuestions: Int get() = count
}

data class DayBucket(
    val date: String,
    val label: String,
    val questions: Int,
    val correct: Int,
    val accuracy: Int,
    val avgSpeed: Double,
    val sessions: Int
)

/**
 * Aggregated weak-spot item by topicTag or questionText (Section 4.2).
 */
data class WeakSpotEntry(
    val key: String,
    val displayLabel: String,
    val topicTag: String,
    val modeId: String,
    val attempts: Int,
    val errors: Int,
    val errorRate: Double, // 0.0 to 100.0
    val avgTimeMs: Double,
    val lastSeenSessionId: String
) {
    val label: String get() = displayLabel
}

data class WeakSpotsSummary(
    val worstTopics: List<WeakSpotEntry> = emptyList(),
    val worstQuestions: List<WeakSpotEntry> = emptyList()
)

data class TopicBreakdown(
    val topicTag: String,
    val total: Int,
    val correct: Int,
    val avgTimeMs: Double
) {
    val accuracyPercent: Double
        get() = if (total > 0) (correct.toDouble() / total.toDouble()) * 100.0 else 0.0
}

data class GameSettings(
    val totalQuestions: Int = 5,
    val timeLimit: Int = 20,
    val dailyGoal: Int = 10,
    val adaptiveDifficulty: Boolean = false,
    val showStreak: Boolean = true,
    val hapticFeedback: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val notificationTimes: Set<String> = emptySet(),
    val isDarkTheme: Boolean = true,
    val tolerancePreset: TolerancePreset = TolerancePreset.NORMAL,
    val requireManualNextOnWrong: Boolean = true
)
