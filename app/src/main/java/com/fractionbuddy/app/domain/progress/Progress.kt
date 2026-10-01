package com.fractionbuddy.app.domain.progress

import com.fractionbuddy.app.domain.generation.Topic

enum class SessionStatus { ACTIVE, COMPLETED, ENDED_EARLY }

/** Accuracy computed from first submitted answers only. */
data class Score(val answered: Int, val correct: Int) {
    init {
        require(answered >= 0 && correct in 0..answered)
    }

    /** Whole-number percentage, or null when nothing was answered ("No answers yet"). */
    val accuracyPercent: Int? get() = if (answered == 0) null else Math.round(correct * 100.0 / answered).toInt()

    fun accuracyText(): String = accuracyPercent?.let { "$it%" } ?: "No answers yet"
}

data class TopicTotals(val topic: Topic, val answered: Int, val correct: Int) {
    val score: Score get() = Score(answered, correct)
}

/** History retention rules. */
object RetentionPolicy {
    const val MAX_SESSIONS = 100
    const val MAX_CALCULATIONS = 50

    data class SessionRef(val id: Long, val startedAt: Long, val answeredCount: Int, val status: SessionStatus)

    /**
     * Sessions to delete: unanswered finished sessions, plus answered sessions beyond the newest
     * [MAX_SESSIONS]. The active session is never deleted here.
     */
    fun sessionsToDelete(sessions: List<SessionRef>): List<Long> {
        val finished = sessions.filter { it.status != SessionStatus.ACTIVE }
        val empty = finished.filter { it.answeredCount == 0 }.map { it.id }
        val overflow = finished.filter { it.answeredCount > 0 }
            .sortedWith(compareByDescending<SessionRef> { it.startedAt }.thenByDescending { it.id })
            .drop(MAX_SESSIONS)
            .map { it.id }
        return empty + overflow
    }
}

/** Where to resume an active session: the first unanswered question, or null when all are done. */
fun resumePosition(answeredFlags: List<Boolean>): Int? = answeredFlags.indexOfFirst { !it }.takeIf { it >= 0 }
