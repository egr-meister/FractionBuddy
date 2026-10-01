package com.fractionbuddy.app.progress

import com.fractionbuddy.app.domain.progress.RetentionPolicy
import com.fractionbuddy.app.domain.progress.Score
import com.fractionbuddy.app.domain.progress.SessionStatus
import com.fractionbuddy.app.domain.progress.resumePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    @Test fun accuracy() {
        assertEquals("No answers yet", Score(0, 0).accuracyText())
        assertEquals("70%", Score(10, 7).accuracyText())
        assertEquals(67, Score(3, 2).accuracyPercent)
    }

    @Test fun retentionKeepsNewest100AnsweredSessions() {
        val sessions = (1..130L).map {
            RetentionPolicy.SessionRef(it, startedAt = it * 1000, answeredCount = 3, status = SessionStatus.COMPLETED)
        } + RetentionPolicy.SessionRef(999, 0, 0, SessionStatus.ENDED_EARLY) +
            RetentionPolicy.SessionRef(1000, 1, 0, SessionStatus.ACTIVE)
        val del = RetentionPolicy.sessionsToDelete(sessions)
        assertEquals(31, del.size)
        assertTrue(999L in del)
        assertTrue(1000L !in del)
        assertTrue((1..30L).all { it in del })
        assertTrue(130L !in del)
    }

    @Test fun resumeFindsFirstUnanswered() {
        assertEquals(3, resumePosition(listOf(true, true, true, false, false)))
        assertNull(resumePosition(listOf(true, true)))
        assertEquals(0, resumePosition(listOf(false)))
    }
}
