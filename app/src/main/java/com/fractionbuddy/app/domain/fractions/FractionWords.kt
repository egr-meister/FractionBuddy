package com.fractionbuddy.app.domain.fractions

/** Plain-English names for fractions, used for screen readers and explanations. */
object FractionWords {

    private val ones = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen",
    )
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    fun number(n: Int): String {
        require(n >= 0)
        return when {
            n < 20 -> ones[n]
            n < 100 -> tens[n / 10] + if (n % 10 != 0) "-" + ones[n % 10] else ""
            n < 1000 -> ones[n / 100] + " hundred" + if (n % 100 != 0) " " + number(n % 100) else ""
            else -> number(n / 1000) + " thousand" + if (n % 1000 != 0) " " + number(n % 1000) else ""
        }
    }

    private fun ordinal(n: Int): String {
        val words = number(n)
        val head = words.substringBeforeLast(' ', "").let { if (it.isEmpty()) "" else "$it " }
        val lastWord = words.substringAfterLast(' ')
        val hyphenHead = lastWord.substringBeforeLast('-', "").let { if (it.isEmpty()) "" else "$it-" }
        val last = lastWord.substringAfterLast('-')
        val ord = when (last) {
            "one" -> "first"
            "two" -> "second"
            "three" -> "third"
            "five" -> "fifth"
            "eight" -> "eighth"
            "nine" -> "ninth"
            "twelve" -> "twelfth"
            else -> if (last.endsWith("y")) last.dropLast(1) + "ieth" else last + "th"
        }
        return head + hyphenHead + ord
    }

    /**
     * Name of one part: "half", "third", "quarter" (spoken style) or "fourth" (explanation style).
     */
    fun unitName(denominator: Int, plural: Boolean, quarters: Boolean = true): String = when (denominator) {
        1 -> if (plural) "wholes" else "whole"
        2 -> if (plural) "halves" else "half"
        4 -> if (quarters) {
            if (plural) "quarters" else "quarter"
        } else {
            if (plural) "fourths" else "fourth"
        }
        else -> ordinal(denominator) + if (plural) "s" else ""
    }

    /** "three quarters", "one half", "zero sixths", "two wholes". */
    fun spoken(f: Fraction, quarters: Boolean = true): String {
        val plural = f.numerator != 1
        return "${number(f.numerator)} ${unitName(f.denominator, plural, quarters)}"
    }

    /** Spoken form for explanations, using "fourths" as in classroom wording. */
    fun spokenClassroom(f: Fraction): String = spoken(f, quarters = false)

    /** Accessible description, e.g. "3 over 4, three quarters". */
    fun accessible(f: Fraction): String = "${f.numerator} over ${f.denominator}, ${spoken(f)}"

    /** Mixed number for screen readers: "one and one half". */
    fun spokenMixed(f: Fraction): String {
        val s = f.simplified()
        return when {
            s.remainder == 0 -> number(s.wholePart) + if (s.wholePart == 1) " whole" else " wholes"
            s.wholePart == 0 -> spoken(s)
            else -> number(s.wholePart) + " and " + spoken(Fraction(s.remainder, s.denominator))
        }
    }
}
