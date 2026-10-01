package com.fractionbuddy.app.domain.generation

import com.fractionbuddy.app.domain.fractions.ComparisonBreakdown
import com.fractionbuddy.app.domain.fractions.ComparisonResult
import com.fractionbuddy.app.domain.fractions.Explanations
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.ModelType

enum class Topic(val label: String) {
    IDENTIFY("Identify the Fraction"),
    COMPARE("Compare Fractions"),
    ADD("Add Fractions"),
    EQUIVALENT("Equivalent Fractions"),
}

enum class TopicSelection(val label: String, val topic: Topic?) {
    IDENTIFY("Identify the Fraction", Topic.IDENTIFY),
    COMPARE("Compare Fractions", Topic.COMPARE),
    ADD("Add Fractions", Topic.ADD),
    EQUIVALENT("Equivalent Fractions", Topic.EQUIVALENT),
    MIXED("Mixed", null),
}

enum class Difficulty(val label: String, val denominators: List<Int>) {
    EASY("Easy", listOf(2, 3, 4)),
    MEDIUM("Medium", listOf(2, 3, 4, 5, 6, 8)),
    HARD("Hard", (2..12).toList()),
}

/** Canonical answer codes for comparison questions. */
object CompareAnswer {
    const val A = "A"
    const val B = "B"
    const val EQUAL = "EQUAL"
    val ALL = listOf(A, B, EQUAL)

    fun of(result: ComparisonResult): String = when (result) {
        ComparisonResult.A_LARGER -> A
        ComparisonResult.B_LARGER -> B
        ComparisonResult.EQUAL -> EQUAL
    }

    fun label(code: String): String = when (code) {
        A -> "A is larger"
        B -> "B is larger"
        EQUAL -> "They are equal"
        else -> code
    }
}

/**
 * A generated practice question. Operands are structured fractions; answer options are stored
 * as canonical codes ("3/4" or A/B/EQUAL) and scored by exact value, never by display text.
 */
data class PracticeQuestion(
    val position: Int,
    val topic: Topic,
    val first: Fraction,
    val second: Fraction?,
    val correctAnswer: String,
    val options: List<String>,
    val modelType: ModelType,
) {
    fun isCorrect(answer: String): Boolean = when (topic) {
        Topic.COMPARE -> answer == correctAnswer
        else -> {
            val chosen = Fraction.decode(answer)
            val correct = Fraction.decode(correctAnswer)
            chosen != null && correct != null && chosen.sameValueAs(correct)
        }
    }

    fun prompt(): String = when (topic) {
        Topic.IDENTIFY -> "Which fraction is shaded?"
        Topic.COMPARE -> "Which fraction is larger?"
        Topic.ADD -> "What is $first + $second?"
        Topic.EQUIVALENT -> "Which fraction has the same value as $first?"
    }

    fun hint(): String = when (topic) {
        Topic.IDENTIFY -> "Count all the equal parts first. That is the bottom number. Then count the shaded parts."
        Topic.COMPARE -> "Try writing both fractions with the same denominator, then compare the tops."
        Topic.ADD -> "Make the denominators the same before you add the top numbers."
        Topic.EQUIVALENT -> "Multiplying or dividing the top and bottom by the same number keeps the value the same."
    }

    fun explanation(): String = when (topic) {
        Topic.IDENTIFY -> Explanations.identify(first)
        Topic.COMPARE -> Explanations.compare(first, second!!)
        Topic.ADD -> Explanations.add(first, second!!)
        Topic.EQUIVALENT -> Explanations.equivalent(first, Fraction.decode(correctAnswer)!!)
    }

    fun optionLabel(code: String): String {
        if (topic == Topic.COMPARE) return CompareAnswer.label(code)
        val f = Fraction.decode(code) ?: return code
        return if (f.denominator == 1) f.numerator.toString() else f.toString()
    }

    companion object {
        fun compareCorrect(a: Fraction, b: Fraction): String = CompareAnswer.of(ComparisonBreakdown(a, b).result)
    }
}
