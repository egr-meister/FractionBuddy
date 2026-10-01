package com.fractionbuddy.app.generation

import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.FractionMath
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.domain.generation.CompareAnswer
import com.fractionbuddy.app.domain.generation.Difficulty
import com.fractionbuddy.app.domain.generation.DifficultyRules
import com.fractionbuddy.app.domain.generation.PracticeQuestion
import com.fractionbuddy.app.domain.generation.QuestionGenerator
import com.fractionbuddy.app.domain.generation.Topic
import com.fractionbuddy.app.domain.generation.TopicSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionGeneratorTest {

    private fun allSessions(): List<Pair<Difficulty, List<PracticeQuestion>>> =
        (0 until 40).flatMap { seed ->
            Difficulty.entries.flatMap { d ->
                TopicSelection.entries.map { t ->
                    d to QuestionGenerator(Random(seed * 31 + d.ordinal * 7 + t.ordinal))
                        .generateSession(t, d, ModelType.CIRCLE)
                }
            }
        }

    @Test fun sessionsHaveTenQuestionsInOrder() {
        allSessions().forEach { (_, qs) ->
            assertEquals(10, qs.size)
            qs.forEachIndexed { i, q -> assertEquals(i, q.position) }
        }
    }

    @Test fun mixedContainsAllTopics() {
        val qs = QuestionGenerator(Random(1)).generateSession(TopicSelection.MIXED, Difficulty.MEDIUM, ModelType.BAR)
        assertEquals(Topic.entries.toSet(), qs.map { it.topic }.toSet())
    }

    @Test fun exactlyOneCorrectOptionByValue() {
        allSessions().forEach { (_, qs) ->
            qs.forEach { q ->
                if (q.topic == Topic.COMPARE) {
                    assertEquals(CompareAnswer.ALL, q.options)
                    assertEquals(1, q.options.count { q.isCorrect(it) })
                } else {
                    assertEquals("4 options for $q", 4, q.options.size)
                    val values = q.options.map { Fraction.decode(it)!! }
                    // No two options share a value (so 1/2 and 2/4 never both appear).
                    for (i in values.indices) for (j in i + 1 until values.size) {
                        assertTrue("duplicate value in $q", !values[i].sameValueAs(values[j]))
                    }
                    assertEquals("one correct in $q", 1, q.options.count { q.isCorrect(it) })
                    values.forEach { assertTrue(it.denominator > 0) }
                }
            }
        }
    }

    @Test fun difficultyConstraintsHold() {
        allSessions().forEach { (d, qs) ->
            qs.forEach { q ->
                assertTrue(q.first.denominator in d.denominators)
                q.second?.let { assertTrue(it.denominator in d.denominators) }
                if (q.topic == Topic.ADD) {
                    val a = q.first
                    val b = q.second!!
                    val common = FractionMath.lcm(a.denominator, b.denominator)
                    val sum = a + b
                    when (d) {
                        Difficulty.EASY -> {
                            assertEquals(a.denominator, b.denominator)
                            assertTrue(sum <= Fraction(1, 1))
                        }
                        Difficulty.MEDIUM -> {
                            assertTrue(common <= 24)
                            assertTrue(sum <= Fraction(2, 1))
                        }
                        Difficulty.HARD -> {
                            assertTrue(a.denominator != b.denominator)
                            assertTrue(common <= 24)
                            assertTrue(sum <= Fraction(2, 1))
                        }
                    }
                }
            }
        }
    }

    @Test fun comparisonIncludesEqualValues() {
        val qs = QuestionGenerator(Random(5)).generateSession(TopicSelection.COMPARE, Difficulty.EASY, ModelType.CIRCLE)
        assertTrue(qs.any { it.correctAnswer == CompareAnswer.EQUAL })
        assertTrue(DifficultyRules.comparePairs(Difficulty.HARD, equal = true).all { (a, b) -> a.sameValueAs(b) && a != b })
    }

    @Test fun equivalenceAnswersHaveSameValue() {
        Difficulty.entries.forEach { d ->
            DifficultyRules.equivalencePairs(d).forEach { (g, a) ->
                assertTrue(g.sameValueAs(a))
                assertTrue(g != a)
            }
        }
    }

    @Test fun seededGenerationIsRepeatable() {
        val a = QuestionGenerator(Random(42)).generateSession(TopicSelection.MIXED, Difficulty.HARD, ModelType.PIZZA)
        val b = QuestionGenerator(Random(42)).generateSession(TopicSelection.MIXED, Difficulty.HARD, ModelType.PIZZA)
        assertEquals(a, b)
    }

    @Test fun explanationsMatchQuestions() {
        allSessions().forEach { (_, qs) ->
            qs.forEach { q ->
                val e = q.explanation()
                assertTrue(e.isNotBlank())
                if (q.topic != Topic.EQUIVALENT) assertTrue("$e / $q", e.contains(q.first.toString()))
                if (q.topic == Topic.ADD) assertTrue("$e / $q", e.contains(q.optionLabel(q.correctAnswer)))
            }
        }
    }
}
