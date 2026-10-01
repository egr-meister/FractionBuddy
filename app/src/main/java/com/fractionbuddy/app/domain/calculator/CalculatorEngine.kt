package com.fractionbuddy.app.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode

enum class Operator(val symbol: String, val code: String, val spoken: String) {
    ADD("+", "ADD", "plus"),
    SUBTRACT("−", "SUBTRACT", "minus"),
    MULTIPLY("×", "MULTIPLY", "times"),
    DIVIDE("÷", "DIVIDE", "divided by"),
    ;

    companion object {
        fun fromCode(code: String?): Operator? = entries.firstOrNull { it.code == code }
    }
}

/** A successful calculation, ready to be stored in history. Values are canonical strings. */
data class CalculationRecord(
    val left: String,
    val operator: Operator,
    val right: String,
    val result: String,
    val rounded: Boolean,
)

data class CalculatorState(
    val left: String = "0",
    val operator: Operator? = null,
    val right: String? = null,
    val justEvaluated: Boolean = false,
    val lastRounded: Boolean = false,
    val error: String? = null,
) {
    /** Operand currently being typed. */
    val current: String get() = if (operator != null) right ?: "" else left

    val expression: String
        get() = buildString {
            append(left)
            if (operator != null) {
                append(' ').append(operator.symbol)
                if (right != null) append(' ').append(right)
            }
        }

    val mainDisplay: String get() = error ?: if (operator != null && right != null) right else left
}

/**
 * Basic one-operation-at-a-time calculator using BigDecimal.
 *
 * Limits: |operand| and |result| ≤ 1,000,000; up to six fractional digits per operand;
 * results rounded to six fractional digits with HALF_UP and flagged as approximate when rounding
 * changed the value. Errors never produce history entries.
 */
object CalculatorEngine {

    val LIMIT: BigDecimal = BigDecimal(1_000_000)
    const val MAX_FRACTION_DIGITS = 6

    data class Outcome(val state: CalculatorState, val record: CalculationRecord? = null)

    fun digit(s: CalculatorState, d: Char): CalculatorState {
        require(d in '0'..'9')
        val base = resetIfDone(s)
        val cur = if (base.operator != null) base.right ?: "" else base.left
        val next = appendDigit(cur, d) ?: return base
        return base.withCurrent(next)
    }

    fun decimal(s: CalculatorState): CalculatorState {
        val base = resetIfDone(s)
        val cur = if (base.operator != null) base.right ?: "" else base.left
        if (cur.contains('.')) return base
        val next = when (cur) {
            "", "-" -> cur + "0."
            else -> "$cur."
        }
        return base.withCurrent(next)
    }

    fun toggleSign(s: CalculatorState): CalculatorState {
        if (s.error != null) return CalculatorState()
        val base = if (s.justEvaluated) s.copy(justEvaluated = false, lastRounded = false) else s
        val cur = if (base.operator != null) base.right ?: "" else base.left
        val next = when {
            cur.isEmpty() -> "-"
            cur.startsWith("-") -> cur.drop(1).ifEmpty { if (base.operator != null) "" else "0" }
            else -> "-$cur"
        }
        return if (base.operator != null) base.copy(right = next.ifEmpty { null }) else base.copy(left = next)
    }

    fun operator(s: CalculatorState, op: Operator): Outcome {
        if (s.error != null) return Outcome(s)
        if (s.operator != null && s.right != null && parse(s.right) != null) {
            val out = evaluate(s)
            if (out.state.error != null) return out
            return Outcome(out.state.copy(operator = op, justEvaluated = false), out.record)
        }
        val left = if (parse(s.left) == null) "0" else normalize(s.left)
        return Outcome(s.copy(left = left, operator = op, right = null, justEvaluated = false, lastRounded = false))
    }

    fun equals(s: CalculatorState): Outcome {
        if (s.error != null || s.operator == null || s.right == null || parse(s.right) == null) {
            // Repeated Equals (or nothing to compute) never repeats the operation.
            return Outcome(s)
        }
        return evaluate(s)
    }

    fun clear(): CalculatorState = CalculatorState()

    fun backspace(s: CalculatorState): CalculatorState {
        if (s.error != null) return CalculatorState()
        if (s.justEvaluated) return s
        return when {
            s.operator != null && s.right != null -> {
                val r = s.right.dropLast(1)
                s.copy(right = if (r.isEmpty() || r == "-") null else r)
            }
            s.operator != null -> s.copy(operator = null)
            else -> {
                val l = s.left.dropLast(1)
                s.copy(left = if (l.isEmpty() || l == "-") "0" else l)
            }
        }
    }

    private fun evaluate(s: CalculatorState): Outcome {
        val a = parse(s.left) ?: return Outcome(s)
        val b = parse(s.right ?: return Outcome(s)) ?: return Outcome(s)
        val op = s.operator ?: return Outcome(s)
        val exact: BigDecimal = when (op) {
            Operator.ADD -> a.add(b)
            Operator.SUBTRACT -> a.subtract(b)
            Operator.MULTIPLY -> a.multiply(b)
            Operator.DIVIDE -> {
                if (b.signum() == 0) {
                    return Outcome(s.copy(error = "You can't divide by zero. Press C to start again."))
                }
                // Six fractional digits, HALF_UP; flag as approximate if not exact.
                val rounded = a.divide(b, MAX_FRACTION_DIGITS, RoundingMode.HALF_UP)
                val isExact = try {
                    a.divide(b, MAX_FRACTION_DIGITS, RoundingMode.UNNECESSARY); true
                } catch (_: ArithmeticException) { false }
                return finish(s, a, b, op, rounded, !isExact)
            }
        }
        val rounded = exact.setScale(maxOf(0, minOf(exact.scale(), MAX_FRACTION_DIGITS)), RoundingMode.HALF_UP)
        return finish(s, a, b, op, rounded, rounded.compareTo(exact) != 0)
    }

    private fun finish(
        s: CalculatorState,
        a: BigDecimal,
        b: BigDecimal,
        op: Operator,
        result: BigDecimal,
        approximate: Boolean,
    ): Outcome {
        if (result.abs() > LIMIT) {
            return Outcome(s.copy(error = "That result is bigger than 1,000,000. Press C to start again."))
        }
        val text = format(result)
        val record = CalculationRecord(format(a), op, format(b), text, approximate)
        val state = CalculatorState(left = text, justEvaluated = true, lastRounded = approximate)
        return Outcome(state, record)
    }

    private fun resetIfDone(s: CalculatorState): CalculatorState = when {
        s.error != null -> CalculatorState()
        s.justEvaluated -> CalculatorState().copy(left = "")
        else -> s
    }

    private fun CalculatorState.withCurrent(v: String): CalculatorState =
        if (operator != null) copy(right = v) else copy(left = v)

    /** Appends a digit with leading-zero normalisation and limit checks; null rejects the key. */
    internal fun appendDigit(cur: String, d: Char): String? {
        val negative = cur.startsWith("-")
        val body = if (negative) cur.drop(1) else cur
        val newBody = when {
            body.isEmpty() || body == "0" -> d.toString()
            else -> body + d
        }
        val dot = newBody.indexOf('.')
        if (dot >= 0 && newBody.length - dot - 1 > MAX_FRACTION_DIGITS) return null
        val candidate = (if (negative) "-" else "") + newBody
        val value = parse(candidate) ?: return null
        if (value.abs() > LIMIT) return null
        return candidate
    }

    fun parse(text: String): BigDecimal? {
        if (text.isEmpty() || text == "-") return null
        val t = if (text.endsWith(".")) text + "0" else text
        return try { BigDecimal(t) } catch (_: NumberFormatException) { null }
    }

    private fun normalize(text: String): String = parse(text)?.let(::format) ?: "0"

    /** Canonical decimal string: no trailing zeros, no exponent, no negative zero. */
    fun format(v: BigDecimal): String {
        if (v.signum() == 0) return "0"
        return v.stripTrailingZeros().toPlainString()
    }

    // --- Draft persistence -------------------------------------------------------------------

    fun encodeDraft(s: CalculatorState): String =
        listOf(s.left, s.operator?.code ?: "", s.right ?: "", if (s.justEvaluated) "1" else "0").joinToString("|")

    fun decodeDraft(text: String?): CalculatorState {
        if (text.isNullOrEmpty()) return CalculatorState()
        val p = text.split('|')
        if (p.size != 4) return CalculatorState()
        val left = p[0].takeIf { it.isEmpty() || it == "-" || parse(it) != null } ?: return CalculatorState()
        val op = Operator.fromCode(p[1].ifEmpty { null })
        val right = p[2].ifEmpty { null }?.takeIf { it == "-" || parse(it) != null }
        return CalculatorState(left = left.ifEmpty { "0" }, operator = op, right = if (op == null) null else right, justEvaluated = p[3] == "1")
    }
}
