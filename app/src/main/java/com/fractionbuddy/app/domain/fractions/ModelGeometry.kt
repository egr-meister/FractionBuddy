package com.fractionbuddy.app.domain.fractions

import kotlin.math.atan2
import kotlin.math.sqrt

enum class ModelType { CIRCLE, PIZZA, BAR }

/**
 * Pure geometry for fraction models, independent of Compose so it can be unit tested.
 *
 * Circles start at 12 o'clock (-90 degrees) and proceed clockwise; every sector spans
 * exactly 360/denominator degrees. Bars split a fixed width into equal cells left to right.
 */
object ModelGeometry {

    const val START_ANGLE_DEGREES = -90f

    data class Sector(val index: Int, val startAngle: Float, val sweepAngle: Float)

    data class Cell(val index: Int, val left: Float, val right: Float)

    fun sectors(denominator: Int): List<Sector> {
        require(denominator > 0)
        val sweep = 360f / denominator
        return List(denominator) { i -> Sector(i, START_ANGLE_DEGREES + i * sweep, sweep) }
    }

    fun cells(denominator: Int, width: Float): List<Cell> {
        require(denominator > 0)
        val w = width / denominator
        return List(denominator) { i -> Cell(i, i * w, if (i == denominator - 1) width else (i + 1) * w) }
    }

    /**
     * Index of the sector containing a point relative to the circle centre, or null when the
     * point is outside the radius. Screen coordinates (y grows downwards), so increasing angle
     * from atan2 is already clockwise.
     */
    fun sectorAt(dx: Float, dy: Float, radius: Float, denominator: Int): Int? {
        require(denominator > 0)
        if (sqrt(dx * dx + dy * dy) > radius) return null
        if (denominator == 1) return 0
        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) - START_ANGLE_DEGREES
        while (deg < 0) deg += 360.0
        while (deg >= 360.0) deg -= 360.0
        return (deg / (360.0 / denominator)).toInt().coerceIn(0, denominator - 1)
    }

    fun cellAt(x: Float, width: Float, denominator: Int): Int? {
        require(denominator > 0)
        if (x < 0f || x > width || width <= 0f) return null
        return (x / (width / denominator)).toInt().coerceIn(0, denominator - 1)
    }
}

/**
 * Selection state for an interactive model. The numerator is always the number of selected
 * pieces, so the two can never disagree.
 */
data class PieceSelection(val denominator: Int, val selected: Set<Int>) {
    init {
        require(denominator in Fraction.MIN_DENOMINATOR..Fraction.MAX_DENOMINATOR)
        require(selected.all { it in 0 until denominator })
    }

    val numerator: Int get() = selected.size
    val fraction: Fraction get() = Fraction(numerator, denominator)

    fun toggle(index: Int): PieceSelection {
        require(index in 0 until denominator)
        return copy(selected = if (index in selected) selected - index else selected + index)
    }

    /** Shades the lowest unshaded piece. */
    fun increment(): PieceSelection {
        val next = (0 until denominator).firstOrNull { it !in selected } ?: return this
        return copy(selected = selected + next)
    }

    /** Unshades the highest shaded piece. */
    fun decrement(): PieceSelection {
        val last = selected.maxOrNull() ?: return this
        return copy(selected = selected - last)
    }

    /**
     * Changes the denominator while keeping the numerator. When the numerator no longer fits,
     * it is clamped to the new denominator; [ChangeResult.clamped] reports that so the UI can
     * announce it. The proportion is never silently substituted.
     */
    fun withDenominator(newDenominator: Int): ChangeResult {
        val d = newDenominator.coerceIn(Fraction.MIN_DENOMINATOR, Fraction.MAX_DENOMINATOR)
        val n = numerator
        val clamped = n > d
        val keepN = minOf(n, d)
        val kept = selected.filter { it < d }.toMutableSet()
        var i = 0
        while (kept.size < keepN) {
            if (i !in kept) kept += i
            i++
        }
        return ChangeResult(PieceSelection(d, kept), clamped)
    }

    data class ChangeResult(val selection: PieceSelection, val clamped: Boolean)

    companion object {
        fun firstN(numerator: Int, denominator: Int): PieceSelection {
            val d = denominator.coerceIn(Fraction.MIN_DENOMINATOR, Fraction.MAX_DENOMINATOR)
            val n = numerator.coerceIn(0, d)
            return PieceSelection(d, (0 until n).toSet())
        }
    }
}
