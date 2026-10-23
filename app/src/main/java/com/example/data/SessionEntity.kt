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
    val duration: Double,
    val localDate: String = "",
    val sessionType: String = SESSION_TYPE_NORMAL
) {
    companion object {
        const val SESSION_TYPE_NORMAL = "normal"
        const val SESSION_TYPE_SPEED_ROUND = "speed_round"
        const val SESSION_TYPE_WEAK_SPOTS = "weak_spots"
    }
}
