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

enum class GameMode(val id: String, val displayName: String, val defaultSymbol: String) {
    ADDITION("addition", "Addition", "+"),
    SUBTRACTION("subtraction", "Subtraction", "−"),
    MULTIPLICATION("multiplication", "Multiplication", "×"),
    DIVISION("division", "Division", "÷"),
    MIXED("mixed", "Mixed Operations", "shuffle"),
    MULTIPLICATION_TABLE("multiplication-table", "Tables", "grid_on"),
    FACTOR_FINDING("factor-finding", "Factors", "exposure"),
    SQUARE("square", "Squares", "x²"),
    FRACTION("fraction", "Fractions", "½"),
    PERCENTAGE("percentage", "Percent", "%"),
    SQUARE_ROOT("square-root", "Roots", "√"),
    APPROXIMATION("approximation", "Estimation", "≈"),
    NUMBER_SERIES("number-series", "Series", "linear_scale"),
    RATIO("ratio", "Ratio", "::"),
    CHAIN_CALCULATION("chain-calculation", "Chain", "multiple_stop");

    companion object {
        fun fromId(id: String): GameMode {
            return entries.find { it.id == id } ?: ADDITION
        }
    }
}

data class QuestionResult(
    val num1: Double,
    val num2: Double,
    val operation: String,
    val correctAnswer: String,
    val userAnswer: String?,
    val isCorrect: Boolean,
    val timeTaken: Int, // seconds
    val timedOut: Boolean
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
)

data class DayBucket(
    val date: String,
    val label: String,
    val questions: Int,
    val correct: Int,
    val accuracy: Int,
    val avgSpeed: Double,
    val sessions: Int
)

data class GameSettings(
    val totalQuestions: Int = 5,
    val timeLimit: Int = 20,
    val dailyGoal: Int = 10,
    val adaptiveDifficulty: Boolean = false,
    val showStreak: Boolean = true,
    val hapticFeedback: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val notificationTimes: Set<String> = emptySet(),
    val isDarkTheme: Boolean = true
)
