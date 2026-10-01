package com.fractionbuddy.app.domain.generation

import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.FractionMath
import com.fractionbuddy.app.domain.fractions.MAX_RENDERED_PARTS
import com.fractionbuddy.app.domain.fractions.ModelType
import kotlin.random.Random

/** Difficulty rules, enforced by exact candidate filtering. */
object DifficultyRules {

    fun identifyCandidates(d: Difficulty): List<Fraction> =
        d.denominators.flatMap { den -> (0..den).map { Fraction(it, den) } }

    private fun proper(d: Difficulty): List<Fraction> =
        d.denominators.flatMap { den -> (1 until den).map { Fraction(it, den) } }

    fun comparePairs(d: Difficulty, equal: Boolean): List<Pair<Fraction, Fraction>> {
        val all = proper(d)
        return all.flatMap { a -> all.map { b -> a to b } }.filter { (a, b) ->
            if (equal) a != b && a.sameValueAs(b) else !a.sameValueAs(b)
        }
    }

    fun isValidAddition(d: Difficulty, a: Fraction, b: Fraction): Boolean {
        if (a.denominator !in d.denominators || b.denominator !in d.denominators) return false
        if (a.numerator !in 1 until a.denominator || b.numerator !in 1 until b.denominator) return false
        val sum = a + b
        val common = FractionMath.lcm(a.denominator, b.denominator)
        return when (d) {
            Difficulty.EASY -> a.denominator == b.denominator && sum <= Fraction(1, 1)
            Difficulty.MEDIUM -> common <= MAX_RENDERED_PARTS && sum <= Fraction(2, 1)
            Difficulty.HARD -> a.denominator != b.denominator && common <= MAX_RENDERED_PARTS && sum <= Fraction(2, 1)
        }
    }

    fun additionPairs(d: Difficulty): List<Pair<Fraction, Fraction>> {
        val all = proper(d)
        return all.flatMap { a -> all.map { b -> a to b } }.filter { (a, b) -> isValidAddition(d, a, b) }
    }

    /** (given, correct answer) pairs for equivalence questions. */
    fun equivalencePairs(d: Difficulty): List<Pair<Fraction, Fraction>> {
        val maxDen = if (d == Difficulty.EASY) 12 else MAX_RENDERED_PARTS
        val base = proper(d).filter { it.isSimplified }
        val expand = base.flatMap { f ->
            (2..4).map { k -> f to Fraction(f.numerator * k, f.denominator * k) }
                .filter { it.second.denominator <= maxDen }
        }
        if (d != Difficulty.HARD) return expand
        // Hard also asks to simplify: the given fraction is an expanded form.
        val simplify = proper(d).filter { !it.isSimplified }.map { it to it.simplified() }
        return expand + simplify
    }
}

/**
 * Generates practice sessions locally. Inject [random] (seeded in tests) for repeatability.
 */
class QuestionGenerator(private val random: Random) {

    fun generateSession(
        selection: TopicSelection,
        difficulty: Difficulty,
        modelType: ModelType,
        count: Int = SESSION_LENGTH,
    ): List<PracticeQuestion> {
        val topics = topicPlan(selection, count)
        val compareSlots = topics.indices.filter { topics[it] == Topic.COMPARE }
        val equalSlots: Set<Int> = when {
            compareSlots.size >= 2 -> {
                val n = maxOf(1, (compareSlots.size + 2) / 4)
                compareSlots.shuffled(random).take(n).toSet()
            }
            else -> compareSlots.filter { random.nextInt(4) == 0 }.toSet()
        }
        return topics.mapIndexed { i, topic ->
            when (topic) {
                Topic.IDENTIFY -> identify(i, difficulty, modelType)
                Topic.COMPARE -> compare(i, difficulty, modelType, equal = i in equalSlots)
                Topic.ADD -> add(i, difficulty, modelType)
                Topic.EQUIVALENT -> equivalent(i, difficulty, modelType)
            }
        }
    }

    private fun topicPlan(selection: TopicSelection, count: Int): List<Topic> {
        selection.topic?.let { t -> return List(count) { t } }
        val all = Topic.entries
        val plan = MutableList(count) { all[it % all.size] }
        return plan.shuffled(random)
    }

    fun identify(position: Int, d: Difficulty, model: ModelType): PracticeQuestion {
        val f = DifficultyRules.identifyCandidates(d).random(random)
        val n = f.numerator
        val den = f.denominator
        val plausible = buildList {
            add(Fraction(den - n, den)) // counted unshaded parts
            if (n > 0) add(Fraction(den, n)) // swapped numerator and denominator
            if (n + 1 <= den) add(Fraction(n + 1, den))
            if (n - 1 >= 0) add(Fraction(n - 1, den))
            add(Fraction(n, den + 1)) // miscounted parts
            if (den - 1 >= 1) add(Fraction(n.coerceAtMost(den - 1), den - 1))
        }
        val options = buildOptions(f, plausible, d)
        return PracticeQuestion(position, Topic.IDENTIFY, f, null, f.encode(), options, model)
    }

    fun compare(position: Int, d: Difficulty, model: ModelType, equal: Boolean): PracticeQuestion {
        val (a, b) = DifficultyRules.comparePairs(d, equal).random(random)
        return PracticeQuestion(
            position, Topic.COMPARE, a, b,
            PracticeQuestion.compareCorrect(a, b), CompareAnswer.ALL, model,
        )
    }

    fun add(position: Int, d: Difficulty, model: ModelType): PracticeQuestion {
        val (a, b) = DifficultyRules.additionPairs(d).random(random)
        val sum = (a + b).simplified()
        val common = FractionMath.lcm(a.denominator, b.denominator)
        val atCommon = a + b
        val plausible = buildList {
            add(Fraction(a.numerator + b.numerator, a.denominator + b.denominator)) // added denominators
            if (a.denominator != b.denominator) {
                add(Fraction(a.numerator + b.numerator, common)) // forgot to convert
                add(Fraction(a.numerator + b.numerator, maxOf(a.denominator, b.denominator)))
            } else {
                add(Fraction(a.numerator + b.numerator, a.denominator * 2))
            }
            add(Fraction(atCommon.numerator + 1, common)) // nearby
            if (atCommon.numerator - 1 >= 0) add(Fraction(atCommon.numerator - 1, common))
            if (sum.numerator > 0) add(Fraction(sum.denominator, sum.numerator))
        }
        val options = buildOptions(sum, plausible, d)
        return PracticeQuestion(position, Topic.ADD, a, b, sum.encode(), options, model)
    }

    fun equivalent(position: Int, d: Difficulty, model: ModelType): PracticeQuestion {
        val (given, answer) = DifficultyRules.equivalencePairs(d).random(random)
        val n = given.numerator
        val den = given.denominator
        val k = if (answer.denominator > den) answer.denominator / den else 2
        val plausible = buildList {
            add(Fraction(n + k, den + k)) // added the same number
            add(Fraction(n, den * k)) // multiplied only the bottom
            add(Fraction(n * k, den)) // multiplied only the top
            add(Fraction(answer.numerator + 1, answer.denominator))
            if (answer.numerator - 1 > 0) add(Fraction(answer.numerator - 1, answer.denominator))
            if (n > 0) add(Fraction(den, n))
        }
        val options = buildOptions(answer, plausible, d)
        return PracticeQuestion(position, Topic.EQUIVALENT, given, null, answer.encode(), options, model)
    }

    /**
     * Four options with exactly one correct value. Uniqueness is by exact value: two options
     * that simplify to the same fraction are never both offered.
     */
    private fun buildOptions(correct: Fraction, plausible: List<Fraction>, d: Difficulty): List<String> {
        val chosen = mutableListOf(correct)
        fun tryAdd(f: Fraction) {
            if (chosen.size >= OPTION_COUNT) return
            if (chosen.none { it.sameValueAs(f) }) chosen += f
        }
        plausible.shuffled(random).forEach(::tryAdd)
        // Fallback: nearby fractions from the level's denominators.
        var guard = 0
        while (chosen.size < OPTION_COUNT && guard < 500) {
            val den = d.denominators.random(random)
            tryAdd(Fraction(random.nextInt(0, den + 1), den))
            guard++
        }
        check(chosen.size == OPTION_COUNT) { "Could not build distinct options for $correct" }
        return chosen.shuffled(random).map { it.encode() }
    }

    companion object {
        const val SESSION_LENGTH = 10
        const val OPTION_COUNT = 4
    }
}
