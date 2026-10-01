package com.fractionbuddy.app.fractions

import com.fractionbuddy.app.domain.fractions.AdditionBreakdown
import com.fractionbuddy.app.domain.fractions.ComparisonBreakdown
import com.fractionbuddy.app.domain.fractions.ComparisonResult
import com.fractionbuddy.app.domain.fractions.Explanations
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.FractionMath
import com.fractionbuddy.app.domain.fractions.FractionWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FractionTest {

    @Test fun gcdAndLcm() {
        assertEquals(6, FractionMath.gcd(12, 18))
        assertEquals(1, FractionMath.gcd(7, 12))
        assertEquals(5, FractionMath.gcd(0, 5))
        assertEquals(12, FractionMath.lcm(4, 6))
        assertEquals(132, FractionMath.lcm(11, 12))
        assertEquals(7, FractionMath.lcm(7, 7))
    }

    @Test fun normalizationKeepsEnteredForm() {
        val f = Fraction(6, 8)
        assertEquals(Fraction(6, 8), f)
        assertEquals(Fraction(3, 4), f.simplified())
        assertEquals(Fraction(0, 1), Fraction(0, 7).simplified())
        assertEquals(Fraction(1, 1), Fraction(5, 5).simplified())
        assertTrue(Fraction(0, 3).isZero)
        assertTrue(Fraction(4, 4).isOneWhole)
    }

    @Test fun denominatorValidation() {
        try { Fraction(1, 0); fail("zero denominator accepted") } catch (_: IllegalArgumentException) {}
        try { Fraction(1, -2); fail("negative denominator accepted") } catch (_: IllegalArgumentException) {}
        try { Fraction(-1, 2); fail("negative numerator accepted") } catch (_: IllegalArgumentException) {}
        assertNull(Fraction.decode("1/0"))
        assertNull(Fraction.decode("abc"))
        assertEquals(Fraction(3, 4), Fraction.decode("3/4"))
        assertTrue(Fraction.isValidLearningInput(0, 12))
        assertFalse(Fraction.isValidLearningInput(13, 12))
        assertFalse(Fraction.isValidLearningInput(1, 13))
        assertFalse(Fraction.isValidLearningInput(0, 0))
    }

    @Test fun exactComparison() {
        assertTrue(Fraction(1, 2) > Fraction(1, 3))
        assertTrue(Fraction(2, 3) < Fraction(3, 4))
        assertEquals(0, Fraction(1, 2).compareTo(Fraction(2, 4)))
        assertTrue(Fraction(1, 2).sameValueAs(Fraction(6, 12)))
        assertFalse(Fraction(1, 3).sameValueAs(Fraction(33, 100)))
        // Larger numerator is not enough: 3/12 < 1/2.
        assertTrue(Fraction(3, 12) < Fraction(1, 2))
        // Larger denominator is not enough: 7/8 > 2/3.
        assertTrue(Fraction(7, 8) > Fraction(2, 3))
    }

    @Test fun comparisonBreakdown() {
        val c = ComparisonBreakdown(Fraction(1, 2), Fraction(1, 3))
        assertEquals(6, c.commonDenominator)
        assertEquals(Fraction(3, 6), c.aConverted)
        assertEquals(Fraction(2, 6), c.bConverted)
        assertEquals(ComparisonResult.A_LARGER, c.result)
        assertTrue(c.explanation().contains("1/2 = 3/6"))
        assertTrue(c.explanation().contains("1/2 is larger than 1/3"))
        val eq = ComparisonBreakdown(Fraction(2, 4), Fraction(1, 2))
        assertEquals(ComparisonResult.EQUAL, eq.result)
        assertTrue(eq.explanation().contains("equal"))
        val big = ComparisonBreakdown(Fraction(1, 11), Fraction(1, 12))
        assertEquals(132, big.commonDenominator)
        assertFalse(big.canRenderCommon)
        assertTrue(big.explanation().contains("The common denominator is 132"))
    }

    @Test fun additionAndMixedNumbers() {
        val a = AdditionBreakdown(Fraction(1, 2), Fraction(1, 4))
        assertEquals("1/2 + 1/4 = 2/4 + 1/4 = 3/4", a.equation())
        val b = AdditionBreakdown(Fraction(3, 4), Fraction(3, 4))
        assertEquals("3/4 + 3/4 = 6/4 = 3/2 = 1 1/2", b.equation())
        assertTrue(b.isAboveOne)
        assertEquals(2, b.wholesNeeded)
        assertEquals("1 1/2", b.mixedText)
        assertEquals(4, b.steps().size)
    }

    @Test fun wholeNumberResults() {
        val w = AdditionBreakdown(Fraction(1, 2), Fraction(2, 4))
        assertEquals("1/2 + 2/4 = 2/4 + 2/4 = 4/4 = 1", w.equation())
        assertTrue(w.isWholeNumber)
        assertEquals("1", w.mixedText)
        val two = AdditionBreakdown(Fraction(4, 4), Fraction(3, 3))
        assertEquals("2", two.mixedText)
        assertFalse(two.equation().contains(" 0/"))
        assertEquals("2", Fraction(8, 4).mixedText())
        val zero = AdditionBreakdown(Fraction(0, 2), Fraction(0, 3))
        assertEquals("0", zero.mixedText)
    }

    @Test fun largeCommonDenominatorStaysExact() {
        val a = AdditionBreakdown(Fraction(1, 11), Fraction(1, 12))
        assertEquals(132, a.commonDenominator)
        assertFalse(a.canRenderCommon)
        assertEquals(Fraction(23, 132), a.simplified)
        assertEquals("1/11 + 1/12 = 12/132 + 11/132 = 23/132", a.equation())
    }

    @Test fun words() {
        assertEquals("three quarters", FractionWords.spoken(Fraction(3, 4)))
        assertEquals("one half", FractionWords.spoken(Fraction(1, 2)))
        assertEquals("two sixths", FractionWords.spoken(Fraction(2, 6)))
        assertEquals("five twelfths", FractionWords.spoken(Fraction(5, 12)))
        assertEquals("three fourths", FractionWords.spokenClassroom(Fraction(3, 4)))
        assertEquals("one and one half", FractionWords.spokenMixed(Fraction(3, 2)))
        assertEquals("one hundred thirty-second", FractionWords.unitName(132, false))
        assertEquals("twentieths", FractionWords.unitName(20, true))
    }

    @Test fun explanationsUseActualValues() {
        assertEquals("Three of the four equal parts are shaded. That is 3/4.", Explanations.identify(Fraction(3, 4)))
        assertTrue(Explanations.identify(Fraction(0, 5)).contains("None"))
        assertTrue(Explanations.identify(Fraction(2, 4)).contains("simplifies to 1/2"))
        assertEquals(
            "Multiply the top and bottom of 1/2 by 2 to get 2/4. They show the same amount.",
            Explanations.equivalent(Fraction(1, 2), Fraction(2, 4)),
        )
        assertTrue(Explanations.equivalent(Fraction(4, 8), Fraction(1, 2)).contains("shared factor 4"))
        assertEquals(
            "1/4 and 2/4 both use fourths. Add the parts: 1 + 2 = 3. The sum is 3/4.",
            Explanations.add(Fraction(1, 4), Fraction(2, 4)),
        )
    }
}
