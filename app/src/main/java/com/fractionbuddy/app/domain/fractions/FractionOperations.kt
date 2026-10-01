package com.fractionbuddy.app.domain.fractions

/** Diagrams are repartitioned only up to this many equal parts; beyond it we show symbols only. */
const val MAX_RENDERED_PARTS = 24

enum class ComparisonResult { A_LARGER, B_LARGER, EQUAL }

/** Exact comparison of two fractions through a common denominator. */
data class ComparisonBreakdown(val a: Fraction, val b: Fraction) {
    val commonDenominator: Int = FractionMath.lcm(a.denominator, b.denominator)
    val aConverted: Fraction = a.expandTo(commonDenominator)
    val bConverted: Fraction = b.expandTo(commonDenominator)
    val result: ComparisonResult = when {
        a > b -> ComparisonResult.A_LARGER
        a < b -> ComparisonResult.B_LARGER
        else -> ComparisonResult.EQUAL
    }

    /** Whether a repartitioned diagram at the common denominator stays readable. */
    val canRenderCommon: Boolean = commonDenominator <= MAX_RENDERED_PARTS

    fun explanation(): String {
        val sb = StringBuilder()
        if (a.denominator == b.denominator) {
            sb.append("Both fractions use ${FractionWords.unitName(a.denominator, true, quarters = false)}. ")
        } else {
            val conversions = listOf(a to aConverted, b to bConverted)
                .map { (orig, conv) -> "$orig = $conv" }
            sb.append(conversions.joinToString(" and ")).append(". ")
            if (!canRenderCommon) sb.append("The common denominator is $commonDenominator. ")
        }
        val ac = FractionWords.spokenClassroom(aConverted)
        val bc = FractionWords.spokenClassroom(bConverted)
        when (result) {
            ComparisonResult.A_LARGER ->
                sb.append("Since $aConverted is greater than $bConverted ($ac is more than $bc), $a is larger than $b.")
            ComparisonResult.B_LARGER ->
                sb.append("Since $bConverted is greater than $aConverted ($bc is more than $ac), $b is larger than $a.")
            ComparisonResult.EQUAL ->
                if (a == b) sb.append("They are the same fraction, so they are equal.")
                else sb.append("Both are $aConverted, so $a and $b are equal. They show the same amount.")
        }
        return sb.toString()
    }
}

/** Exact addition broken into the four classroom steps. */
data class AdditionBreakdown(val a: Fraction, val b: Fraction) {
    val commonDenominator: Int = FractionMath.lcm(a.denominator, b.denominator)
    val aConverted: Fraction = a.expandTo(commonDenominator)
    val bConverted: Fraction = b.expandTo(commonDenominator)
    val sumAtCommon: Fraction = Fraction(aConverted.numerator + bConverted.numerator, commonDenominator)
    val simplified: Fraction = sumAtCommon.simplified()
    val isAboveOne: Boolean = simplified.numerator > simplified.denominator
    val isWholeNumber: Boolean = simplified.remainder == 0
    val mixedText: String = simplified.mixedText()

    val canRenderCommon: Boolean = commonDenominator <= MAX_RENDERED_PARTS

    /** Number of equal-sized wholes needed to draw the sum (at least one). */
    val wholesNeeded: Int = maxOf(1, (simplified.numerator + simplified.denominator - 1) / simplified.denominator)

    /** "3/4 + 3/4 = 6/4 = 3/2 = 1 1/2" */
    fun equation(): String {
        val parts = mutableListOf("$a + $b")
        if (aConverted != a || bConverted != b) parts += "$aConverted + $bConverted"
        parts += sumAtCommon.toString()
        if (simplified.denominator == 1) {
            parts += simplified.numerator.toString()
        } else {
            if (simplified != sumAtCommon) parts += simplified.toString()
            if (isAboveOne) parts += mixedText
        }
        return parts.zipWithNext().fold(mutableListOf(parts.first())) { acc, (prev, next) ->
            if (prev != next) acc += next
            acc
        }.joinToString(" = ")
    }

    fun steps(): List<String> {
        val steps = mutableListOf<String>()
        if (a.denominator == b.denominator) {
            steps += "1. Both fractions already use ${FractionWords.unitName(commonDenominator, true, quarters = false)}, so the common denominator is $commonDenominator."
            steps += "2. No conversion is needed: $a and $b."
        } else {
            steps += "1. Find a common denominator: the least common multiple of ${a.denominator} and ${b.denominator} is $commonDenominator."
            steps += "2. Convert both fractions: $a = $aConverted and $b = $bConverted."
        }
        steps += "3. Add the numerators: ${aConverted.numerator} + ${bConverted.numerator} = ${sumAtCommon.numerator}, so the sum is $sumAtCommon."
        steps += simplifyStep()
        return steps
    }

    private fun simplifyStep(): String {
        val g = if (sumAtCommon.numerator == 0) commonDenominator else FractionMath.gcd(sumAtCommon.numerator, commonDenominator)
        val base = when {
            sumAtCommon.numerator == 0 -> "4. Nothing was added, so the sum is 0."
            g == 1 -> "4. $sumAtCommon is already in simplest form."
            simplified.denominator == 1 ->
                "4. Simplify: divide the top and bottom by the shared factor $g. $sumAtCommon = ${simplified.numerator}."
            else -> "4. Simplify: divide the top and bottom by the shared factor $g. $sumAtCommon = $simplified."
        }
        val tail = when {
            sumAtCommon.numerator == 0 -> ""
            isWholeNumber -> " That is exactly ${FractionWords.number(simplified.wholePart)} whole" +
                (if (simplified.wholePart == 1) "." else "s.")
            isAboveOne -> " As a mixed number, $simplified = $mixedText."
            else -> ""
        }
        return base + tail
    }

    /** Short practice explanation using the actual values. */
    fun practiceExplanation(): String {
        val unit = FractionWords.unitName(commonDenominator, true, quarters = false)
        val lead = if (a.denominator == b.denominator) {
            "$a and $b both use $unit."
        } else {
            "Use $unit as the common denominator: $a = $aConverted and $b = $bConverted."
        }
        val add = "Add the parts: ${aConverted.numerator} + ${bConverted.numerator} = ${sumAtCommon.numerator}."
        val result = when {
            simplified.denominator == 1 -> "The sum is $sumAtCommon, which is ${simplified.numerator}."
            simplified != sumAtCommon -> {
                val g = FractionMath.gcd(sumAtCommon.numerator, commonDenominator)
                "The sum is $sumAtCommon. Dividing the top and bottom by $g gives $simplified" +
                    (if (isAboveOne) ", or $mixedText." else ".")
            }
            isAboveOne -> "The sum is $simplified, or $mixedText."
            else -> "The sum is $simplified."
        }
        return "$lead $add $result"
    }
}

object Explanations {

    fun identify(shaded: Fraction): String {
        val n = shaded.numerator
        val d = shaded.denominator
        val parts = if (d == 1) "the one whole part" else "the ${FractionWords.number(d)} equal parts"
        val core = when (n) {
            0 -> "None of $parts are shaded. That is $shaded, which is zero."
            d -> if (d == 1) "The whole is shaded. That is $shaded, one whole."
            else "All ${FractionWords.number(d)} of the ${FractionWords.number(d)} equal parts are shaded. That is $shaded, one whole."
            else -> "${FractionWords.number(n).replaceFirstChar { it.uppercase() }} of $parts " +
                (if (n == 1) "is" else "are") + " shaded. That is $shaded."
        }
        val simp = shaded.simplified()
        return if (n != 0 && n != d && simp != shaded) {
            val g = FractionMath.gcd(n, d)
            "$core Dividing the top and bottom by $g, it simplifies to $simp."
        } else core
    }

    fun compare(a: Fraction, b: Fraction): String = ComparisonBreakdown(a, b).explanation()

    fun add(a: Fraction, b: Fraction): String = AdditionBreakdown(a, b).practiceExplanation()

    fun equivalent(given: Fraction, answer: Fraction): String {
        return if (answer.denominator >= given.denominator && answer.denominator % given.denominator == 0 &&
            answer.numerator == given.numerator * (answer.denominator / given.denominator)
        ) {
            val k = answer.denominator / given.denominator
            "Multiply the top and bottom of $given by $k to get $answer. They show the same amount."
        } else if (given.denominator % answer.denominator == 0 &&
            given.numerator == answer.numerator * (given.denominator / answer.denominator)
        ) {
            val k = given.denominator / answer.denominator
            "Divide the top and bottom of $given by the shared factor $k to get $answer. They show the same amount."
        } else {
            val s = given.simplified()
            "$given and $answer both simplify to $s. They show the same amount."
        }
    }
}
