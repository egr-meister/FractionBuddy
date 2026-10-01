package com.fractionbuddy.app.domain.fractions

/**
 * An exact, non-negative fraction with a positive denominator.
 *
 * The entered form is retained: `Fraction(2, 4)` stays 2/4; use [simplified]
 * for the reduced form. Value comparisons never use floating point.
 */
data class Fraction(val numerator: Int, val denominator: Int) : Comparable<Fraction> {

    init {
        require(denominator > 0) { "Denominator must be positive (was $denominator)" }
        require(numerator >= 0) { "Negative fractions are not supported (was $numerator)" }
    }

    /** Reduced by the greatest common divisor. 0/n simplifies to 0/1. */
    fun simplified(): Fraction {
        if (numerator == 0) return Fraction(0, 1)
        val g = FractionMath.gcd(numerator, denominator)
        return Fraction(numerator / g, denominator / g)
    }

    val isSimplified: Boolean get() = simplified() == this
    val isZero: Boolean get() = numerator == 0
    val isWhole: Boolean get() = numerator % denominator == 0
    val isOneWhole: Boolean get() = numerator == denominator
    val isProperOrWhole: Boolean get() = numerator <= denominator
    val wholePart: Int get() = numerator / denominator
    val remainder: Int get() = numerator % denominator

    /** Exact value equality (1/2 == 2/4), independent of the entered form. */
    fun sameValueAs(other: Fraction): Boolean =
        numerator.toLong() * other.denominator == other.numerator.toLong() * denominator

    /** Cross-multiplication comparison. */
    override fun compareTo(other: Fraction): Int =
        (numerator.toLong() * other.denominator).compareTo(other.numerator.toLong() * denominator)

    /** Exact sum, expressed over the least common denominator (not yet simplified). */
    operator fun plus(other: Fraction): Fraction {
        val common = FractionMath.lcm(denominator, other.denominator)
        val a = numerator * (common / denominator)
        val b = other.numerator * (common / other.denominator)
        return Fraction(a + b, common)
    }

    /** Same value expressed over [newDenominator]; requires an exact multiple. */
    fun expandTo(newDenominator: Int): Fraction {
        require(newDenominator % denominator == 0) { "$newDenominator is not a multiple of $denominator" }
        return Fraction(numerator * (newDenominator / denominator), newDenominator)
    }

    /** Canonical, locale-independent encoding used for storage ("3/4"). */
    fun encode(): String = "$numerator/$denominator"

    /** Display text, e.g. "3/4". */
    override fun toString(): String = "$numerator/$denominator"

    /** Mixed-number text: "1 1/2", "2", "3/4". */
    fun mixedText(): String {
        val s = simplified()
        return when {
            s.remainder == 0 -> s.wholePart.toString()
            s.wholePart == 0 -> s.toString()
            else -> "${s.wholePart} ${s.remainder}/${s.denominator}"
        }
    }

    companion object {
        const val MIN_DENOMINATOR = 1
        const val MAX_DENOMINATOR = 12

        /** Decodes the canonical [encode] format; returns null for anything invalid. */
        fun decode(text: String): Fraction? {
            val parts = text.split('/')
            if (parts.size != 2) return null
            val n = parts[0].toIntOrNull() ?: return null
            val d = parts[1].toIntOrNull() ?: return null
            if (d <= 0 || n < 0) return null
            return Fraction(n, d)
        }

        /** Validates a learning input (denominator 1..12, numerator 0..denominator). */
        fun isValidLearningInput(numerator: Int, denominator: Int): Boolean =
            denominator in MIN_DENOMINATOR..MAX_DENOMINATOR && numerator in 0..denominator
    }
}

object FractionMath {
    fun gcd(a: Int, b: Int): Int {
        var x = kotlin.math.abs(a)
        var y = kotlin.math.abs(b)
        while (y != 0) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    fun lcm(a: Int, b: Int): Int {
        require(a > 0 && b > 0) { "lcm requires positive values" }
        return a / gcd(a, b) * b
    }
}
