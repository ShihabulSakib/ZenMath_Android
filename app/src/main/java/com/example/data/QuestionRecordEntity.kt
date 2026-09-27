package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val num1: Double,
    val num2: Double,
    val operation: String,
    val correctAnswer: String,
    val userAnswer: String?,
    val timeMs: Long,
    val isCorrect: Boolean,
    val mode: String,
    val difficulty: String
)
