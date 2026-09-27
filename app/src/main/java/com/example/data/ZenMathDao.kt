package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ZenMathDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE mode = :mode ORDER BY date DESC")
    fun getSessionsByMode(mode: String): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionRecordEntity>)

    @Query("SELECT * FROM questions WHERE sessionId = :sessionId")
    suspend fun getQuestionsForSession(sessionId: String): List<QuestionRecordEntity>

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<QuestionRecordEntity>

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM questions")
    suspend fun clearQuestions()

    @Transaction
    suspend fun clearAll() {
        clearSessions()
        clearQuestions()
    }

    @Transaction
    suspend fun insertSessionWithQuestions(session: SessionEntity, questions: List<QuestionRecordEntity>) {
        insertSession(session)
        if (questions.isNotEmpty()) {
            insertQuestions(questions)
        }
    }
}
