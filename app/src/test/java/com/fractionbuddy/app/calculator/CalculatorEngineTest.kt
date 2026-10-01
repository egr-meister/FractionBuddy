package com.fractionbuddy.app.calculator

import com.fractionbuddy.app.domain.calculator.CalculatorEngine
import com.fractionbuddy.app.domain.calculator.CalculatorState
import com.fractionbuddy.app.domain.calculator.Operator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    private fun type(s: CalculatorState, text: String): CalculatorState =
        text.fold(s) { acc, c -> if (c == '.') CalculatorEngine.decimal(acc) else CalculatorEngine.digit(acc, c) }

    private fun run(a: String, op: Operator, b: String): CalculatorEngine.Outcome {
        var s = type(CalculatorState(), a)
        s = CalculatorEngine.operator(s, op).state
        s = type(s, b)
        return CalculatorEngine.equals(s)
    }

    @Test fun basicOperations() {
        assertEquals("5", run("2", Operator.ADD, "3").state.left)
        assertEquals("-1", run("2", Operator.SUBTRACT, "3").state.left)
        assertEquals("0.3", run("0.1", Operator.ADD, "0.2").state.left)
        assertEquals("2.5", run("5", Operator.DIVIDE, "2").state.left)
        assertEquals("12", run("4", Operator.MULTIPLY, "3").state.left)
    }

    @Test fun divisionRoundsHalfUpAndIsMarkedApproximate() {
        val o = run("2", Operator.DIVIDE, "3")
        assertEquals("0.666667", o.state.left)
        assertTrue(o.record!!.rounded)
        assertTrue(o.state.lastRounded)
        val exact = run("1", Operator.DIVIDE, "4")
        assertEquals("0.25", exact.state.left)
        assertFalse(exact.record!!.rounded)
    }

    @Test fun divisionByZeroIsFriendlyAndNotRecorded() {
        val o = run("5", Operator.DIVIDE, "0")
        assertNotNull(o.state.error)
        assertNull(o.record)
    }

    @Test fun outOfRangeIsRejected() {
        val o = run("1000000", Operator.ADD, "1")
        assertNotNull(o.state.error)
        assertNull(o.record)
        // Operand limit: a seventh digit beyond 1,000,000 is ignored.
        val s = type(CalculatorState(), "10000000")
        assertEquals("1000000", s.left)
    }

    @Test fun inputNormalisation() {
        assertEquals("5", type(CalculatorState(), "005").left)
        assertEquals("0.5", type(CalculatorState(), ".5").left)
        assertEquals("1.5", type(CalculatorState(), "1..5").left) // second point ignored
        assertEquals("0.123456", type(CalculatorState(), "0.1234567").left) // max 6 decimals
    }

    @Test fun negativeOperands() {
        var s = type(CalculatorState(), "3")
        s = CalculatorEngine.toggleSign(s)
        assertEquals("-3", s.left)
        s = CalculatorEngine.operator(s, Operator.MULTIPLY).state
        s = type(s, "2")
        assertEquals("-6", CalculatorEngine.equals(s).state.left)
    }

    @Test fun repeatedEqualsDoesNotRepeat() {
        val first = run("2", Operator.ADD, "3")
        val again = CalculatorEngine.equals(first.state)
        assertEquals("5", again.state.left)
        assertNull(again.record)
    }

    @Test fun trailingZerosRemoved() {
        assertEquals("1", run("0.5", Operator.ADD, "0.5").state.left)
        assertEquals("2", run("4", Operator.DIVIDE, "2").state.left)
    }

    @Test fun backspaceAndDraft() {
        var s = type(CalculatorState(), "123")
        s = CalculatorEngine.backspace(s)
        assertEquals("12", s.left)
        s = CalculatorEngine.operator(s, Operator.ADD).state
        s = type(s, "4")
        val restored = CalculatorEngine.decodeDraft(CalculatorEngine.encodeDraft(s))
        assertEquals(s.left, restored.left)
        assertEquals(s.operator, restored.operator)
        assertEquals(s.right, restored.right)
        assertEquals(CalculatorState(), CalculatorEngine.decodeDraft("garbage"))
    }
}
