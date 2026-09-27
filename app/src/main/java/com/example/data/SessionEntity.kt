package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val mode: String,
    val totalQuestions: Int,
    val correct: Int,
    val avgTimeMs: Double,
    val difficulty: String,
    val duration: Double
)
