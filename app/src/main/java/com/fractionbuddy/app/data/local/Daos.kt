package com.fractionbuddy.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Insert
    suspend fun insert(entity: CalculationEntity): Long

    @Query("SELECT * FROM calculations ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<CalculationEntity>>

    @Query(
        "DELETE FROM calculations WHERE id NOT IN " +
            "(SELECT id FROM calculations ORDER BY createdAt DESC, id DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM calculations")
    suspend fun clear()
}

@Dao
interface PracticeDao {
    @Insert
    suspend fun insertSession(session: PracticeSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestions(questions: List<FractionQuestionEntity>)

    @Query("SELECT * FROM practice_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<PracticeSessionEntity?>

    @Query("SELECT * FROM practice_sessions WHERE id = :id")
    suspend fun getSession(id: Long): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<PracticeSessionEntity?>

    @Query("SELECT * FROM fraction_questions WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getQuestions(sessionId: Long): List<FractionQuestionEntity>

    @Query("SELECT * FROM fraction_questions WHERE sessionId = :sessionId ORDER BY position")
    fun observeQuestions(sessionId: Long): Flow<List<FractionQuestionEntity>>

    @Query("SELECT * FROM fraction_questions WHERE id = :id")
    suspend fun getQuestion(id: Long): FractionQuestionEntity?

    /** Records the first submitted answer only; returns 0 if an answer already exists. */
    @Query(
        "UPDATE fraction_questions SET selectedAnswer = :answer, isCorrect = :correct, answeredAt = :at " +
            "WHERE id = :id AND selectedAnswer IS NULL",
    )
    suspend fun recordAnswer(id: Long, answer: String, correct: Boolean, at: Long): Int

    @Query("UPDATE fraction_questions SET hintUsed = 1 WHERE id = :id")
    suspend fun markHintUsed(id: Long)

    @Query(
        "UPDATE practice_sessions SET answeredCount = answeredCount + 1, " +
            "correctCount = correctCount + :correctIncrement WHERE id = :id",
    )
    suspend fun incrementCounts(id: Long, correctIncrement: Int)

    @Query("UPDATE practice_sessions SET status = :status, finishedAt = :finishedAt WHERE id = :id")
    suspend fun finishSession(id: Long, status: String, finishedAt: Long)

    @Query("SELECT id, startedAt, answeredCount, status FROM practice_sessions")
    suspend fun sessionRefs(): List<SessionRefRow>

    @Query("DELETE FROM fraction_questions WHERE sessionId IN (:ids)")
    suspend fun deleteQuestionsFor(ids: List<Long>)

    @Query("DELETE FROM practice_sessions WHERE id IN (:ids)")
    suspend fun deleteSessions(ids: List<Long>)

    @Query(
        "SELECT * FROM practice_sessions WHERE status != 'ACTIVE' AND answeredCount > 0 " +
            "ORDER BY startedAt DESC, id DESC LIMIT :limit",
    )
    fun observeHistory(limit: Int): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM progress_totals WHERE id = 1")
    suspend fun getTotals(): ProgressTotalsEntity?

    @Query("SELECT * FROM progress_totals WHERE id = 1")
    fun observeTotals(): Flow<ProgressTotalsEntity?>

    @Upsert
    suspend fun upsertTotals(totals: ProgressTotalsEntity)

    @Query("DELETE FROM fraction_questions")
    suspend fun deleteAllQuestions()

    @Query("DELETE FROM practice_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM progress_totals")
    suspend fun deleteTotals()
}
