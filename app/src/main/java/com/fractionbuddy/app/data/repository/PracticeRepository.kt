package com.fractionbuddy.app.data.repository

import androidx.room.withTransaction
import com.fractionbuddy.app.data.local.AppDatabase
import com.fractionbuddy.app.data.local.FractionQuestionEntity
import com.fractionbuddy.app.data.local.PracticeSessionEntity
import com.fractionbuddy.app.data.local.ProgressTotalsEntity
import com.fractionbuddy.app.domain.Clock
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.domain.generation.Difficulty
import com.fractionbuddy.app.domain.generation.PracticeQuestion
import com.fractionbuddy.app.domain.generation.Topic
import com.fractionbuddy.app.domain.generation.TopicSelection
import com.fractionbuddy.app.domain.progress.RetentionPolicy
import com.fractionbuddy.app.domain.progress.Score
import com.fractionbuddy.app.domain.progress.SessionStatus
import com.fractionbuddy.app.domain.progress.TopicTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A stored question together with the child's first answer, if any. */
data class StoredQuestion(
    val id: Long,
    val question: PracticeQuestion,
    val selectedAnswer: String?,
    val isCorrect: Boolean?,
    val hintUsed: Boolean,
) {
    val answered: Boolean get() = selectedAnswer != null
}

data class SessionInfo(
    val id: Long,
    val topicSelection: TopicSelection,
    val difficulty: Difficulty,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: SessionStatus,
    val score: Score,
)

data class LifetimeTotals(val overall: Score, val perTopic: List<TopicTotals>)

class PracticeRepository(private val db: AppDatabase, private val clock: Clock) {

    private val dao = db.practiceDao()

    fun observeActiveSession(): Flow<SessionInfo?> = dao.observeActiveSession().map { it?.toInfo() }

    suspend fun getActiveSession(): SessionInfo? = dao.getActiveSession()?.toInfo()

    fun observeSession(id: Long): Flow<SessionInfo?> = dao.observeSession(id).map { it?.toInfo() }

    suspend fun getSession(id: Long): SessionInfo? = dao.getSession(id)?.toInfo()

    fun observeQuestions(sessionId: Long): Flow<List<StoredQuestion>> =
        dao.observeQuestions(sessionId).map { list -> list.map { it.toStored() } }

    suspend fun getQuestions(sessionId: Long): List<StoredQuestion> = dao.getQuestions(sessionId).map { it.toStored() }

    fun observeHistory(): Flow<List<SessionInfo>> =
        dao.observeHistory(RetentionPolicy.MAX_SESSIONS).map { list -> list.map { it.toInfo() } }

    fun observeTotals(): Flow<LifetimeTotals> = dao.observeTotals().map { (it ?: ProgressTotalsEntity()).toLifetime() }

    /** Starts a new session. Any other unfinished session is ended first (one at a time). */
    suspend fun startSession(
        selection: TopicSelection,
        difficulty: Difficulty,
        questions: List<PracticeQuestion>,
    ): Long = db.withTransaction {
        dao.getActiveSession()?.let { endSessionInternal(it) }
        val id = dao.insertSession(
            PracticeSessionEntity(
                topicSelection = selection.name,
                difficulty = difficulty.name,
                startedAt = clock.now(),
                status = SessionStatus.ACTIVE.name,
            ),
        )
        dao.insertQuestions(questions.map { it.toEntity(id) })
        id
    }

    /**
     * Records the first submitted answer and updates session and lifetime totals in one
     * transaction. A second submission for the same question is ignored. Returns correctness.
     */
    suspend fun recordAnswer(questionId: Long, answer: String): Boolean = db.withTransaction {
        val entity = dao.getQuestion(questionId) ?: error("Unknown question $questionId")
        if (entity.selectedAnswer != null) return@withTransaction entity.isCorrect == true
        val question = entity.toStored().question
        val correct = question.isCorrect(answer)
        val updated = dao.recordAnswer(questionId, answer, correct, clock.now())
        if (updated == 1) {
            dao.incrementCounts(entity.sessionId, if (correct) 1 else 0)
            val t = dao.getTotals() ?: ProgressTotalsEntity()
            dao.upsertTotals(t.add(question.topic, correct))
        }
        correct
    }

    suspend fun markHintUsed(questionId: Long) = dao.markHintUsed(questionId)

    suspend fun completeSession(sessionId: Long) {
        db.withTransaction {
            val s = dao.getSession(sessionId) ?: return@withTransaction
            if (s.status == SessionStatus.ACTIVE.name) {
                dao.finishSession(sessionId, SessionStatus.COMPLETED.name, clock.now())
            }
            applyRetention()
        }
    }

    /**
     * Ends a session early. Answered questions are kept; unanswered ones are not counted as
     * errors. A session with no answers is discarded. Returns true if the session was kept.
     */
    suspend fun endSession(sessionId: Long): Boolean = db.withTransaction {
        val s = dao.getSession(sessionId) ?: return@withTransaction false
        val kept = endSessionInternal(s)
        applyRetention()
        kept
    }

    private suspend fun endSessionInternal(s: PracticeSessionEntity): Boolean {
        if (s.status != SessionStatus.ACTIVE.name) return s.answeredCount > 0
        return if (s.answeredCount == 0) {
            dao.deleteQuestionsFor(listOf(s.id))
            dao.deleteSessions(listOf(s.id))
            false
        } else {
            dao.finishSession(s.id, SessionStatus.ENDED_EARLY.name, clock.now())
            true
        }
    }

    private suspend fun applyRetention() {
        val refs = dao.sessionRefs().map {
            RetentionPolicy.SessionRef(it.id, it.startedAt, it.answeredCount, SessionStatus.valueOf(it.status))
        }
        val ids = RetentionPolicy.sessionsToDelete(refs)
        if (ids.isNotEmpty()) {
            ids.chunked(500).forEach {
                dao.deleteQuestionsFor(it)
                dao.deleteSessions(it)
            }
        }
    }

    suspend fun clearProgress() {
        db.withTransaction {
            dao.deleteAllQuestions()
            dao.deleteAllSessions()
            dao.deleteTotals()
        }
    }

    // --- Mapping ---------------------------------------------------------------------------

    private fun PracticeSessionEntity.toInfo() = SessionInfo(
        id = id,
        topicSelection = TopicSelection.valueOf(topicSelection),
        difficulty = Difficulty.valueOf(difficulty),
        startedAt = startedAt,
        finishedAt = finishedAt,
        status = SessionStatus.valueOf(status),
        score = Score(answeredCount, correctCount),
    )

    private fun PracticeQuestion.toEntity(sessionId: Long) = FractionQuestionEntity(
        sessionId = sessionId,
        position = position,
        topic = topic.name,
        firstNumerator = first.numerator,
        firstDenominator = first.denominator,
        secondNumerator = second?.numerator,
        secondDenominator = second?.denominator,
        correctAnswer = correctAnswer,
        answerOptions = options.joinToString(","),
        modelType = modelType.name,
    )

    private fun FractionQuestionEntity.toStored(): StoredQuestion {
        val second = if (secondNumerator != null && secondDenominator != null) {
            Fraction(secondNumerator, secondDenominator)
        } else null
        val q = PracticeQuestion(
            position = position,
            topic = Topic.valueOf(topic),
            first = Fraction(firstNumerator, firstDenominator),
            second = second,
            correctAnswer = correctAnswer,
            options = answerOptions.split(','),
            modelType = ModelType.valueOf(modelType),
        )
        return StoredQuestion(id, q, selectedAnswer, isCorrect, hintUsed)
    }

    private fun ProgressTotalsEntity.add(topic: Topic, correct: Boolean): ProgressTotalsEntity {
        val c = if (correct) 1 else 0
        val base = copy(answeredCount = answeredCount + 1, correctCount = correctCount + c)
        return when (topic) {
            Topic.IDENTIFY -> base.copy(identifyAnswered = identifyAnswered + 1, identifyCorrect = identifyCorrect + c)
            Topic.COMPARE -> base.copy(compareAnswered = compareAnswered + 1, compareCorrect = compareCorrect + c)
            Topic.ADD -> base.copy(addAnswered = addAnswered + 1, addCorrect = addCorrect + c)
            Topic.EQUIVALENT -> base.copy(equivalentAnswered = equivalentAnswered + 1, equivalentCorrect = equivalentCorrect + c)
        }
    }

    private fun ProgressTotalsEntity.toLifetime() = LifetimeTotals(
        overall = Score(answeredCount, correctCount),
        perTopic = listOf(
            TopicTotals(Topic.IDENTIFY, identifyAnswered, identifyCorrect),
            TopicTotals(Topic.COMPARE, compareAnswered, compareCorrect),
            TopicTotals(Topic.ADD, addAnswered, addCorrect),
            TopicTotals(Topic.EQUIVALENT, equivalentAnswered, equivalentCorrect),
        ),
    )
}
