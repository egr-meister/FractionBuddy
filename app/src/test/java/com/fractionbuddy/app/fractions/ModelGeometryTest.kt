package com.fractionbuddy.app.fractions

import com.fractionbuddy.app.domain.fractions.AdditionBreakdown
import com.fractionbuddy.app.domain.fractions.ComparisonBreakdown
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.ModelGeometry
import com.fractionbuddy.app.domain.fractions.PieceSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ModelGeometryTest {

    @Test fun sectorsAreEqualAndCoverCircle() {
        for (d in 1..12) {
            val s = ModelGeometry.sectors(d)
            assertEquals(d, s.size)
            s.forEach { assertTrue(abs(it.sweepAngle - 360f / d) < 1e-4f) }
            assertTrue(abs(s.sumOf { it.sweepAngle.toDouble() } - 360.0) < 1e-3)
            assertEquals(-90f, s.first().startAngle)
        }
    }

    @Test fun cellsAreEqualAndCoverBar() {
        for (d in 1..12) {
            val c = ModelGeometry.cells(d, 300f)
            assertEquals(0f, c.first().left)
            assertEquals(300f, c.last().right)
            c.forEach { assertTrue(abs((it.right - it.left) - 300f / d) < 1e-3f) }
        }
    }

    @Test fun hitTestingIsClockwiseFromTop() {
        // Quarters: just right of 12 o'clock is sector 0; 3 o'clock side below centre is sector 1.
        assertEquals(0, ModelGeometry.sectorAt(5f, -50f, 100f, 4))
        assertEquals(1, ModelGeometry.sectorAt(50f, 5f, 100f, 4))
        assertEquals(2, ModelGeometry.sectorAt(-5f, 50f, 100f, 4))
        assertEquals(3, ModelGeometry.sectorAt(-50f, -5f, 100f, 4))
        assertNull(ModelGeometry.sectorAt(200f, 0f, 100f, 4))
        assertEquals(0, ModelGeometry.cellAt(10f, 300f, 3))
        assertEquals(2, ModelGeometry.cellAt(299f, 300f, 3))
        assertNull(ModelGeometry.cellAt(-1f, 300f, 3))
    }

    @Test fun numeratorAlwaysMatchesSelection() {
        var s = PieceSelection.firstN(1, 2)
        assertEquals(Fraction(1, 2), s.fraction)
        s = s.toggle(1)
        assertEquals(2, s.numerator)
        s = s.toggle(0)
        assertEquals(1, s.numerator)
        s = s.increment().increment()
        assertEquals(2, s.numerator) // capped at denominator
        s = s.decrement().decrement().decrement()
        assertEquals(0, s.numerator)
    }

    @Test fun reducingDenominatorClampsAndReports() {
        val s = PieceSelection.firstN(5, 6)
        val r = s.withDenominator(3)
        assertTrue(r.clamped)
        assertEquals(Fraction(3, 3), r.selection.fraction)
        val ok = PieceSelection.firstN(2, 6).withDenominator(4)
        assertFalse(ok.clamped)
        assertEquals(Fraction(2, 4), ok.selection.fraction) // numerator kept, not proportion
    }

    @Test fun commonDenominatorRenderingFallback() {
        assertTrue(ComparisonBreakdown(Fraction(1, 6), Fraction(1, 8)).canRenderCommon) // 24
        assertFalse(ComparisonBreakdown(Fraction(1, 5), Fraction(1, 7)).canRenderCommon) // 35
        assertTrue(AdditionBreakdown(Fraction(1, 3), Fraction(1, 8)).canRenderCommon) // 24
        assertFalse(AdditionBreakdown(Fraction(1, 9), Fraction(1, 12)).canRenderCommon) // 36
    }
}
