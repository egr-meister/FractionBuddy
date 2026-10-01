package com.fractionbuddy.app.ui.models

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.FractionWords
import com.fractionbuddy.app.domain.fractions.ModelGeometry
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.ui.theme.Workbook
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Colours for one model; A uses blue/teal, B uses lavender. */
data class ModelPalette(val circle: Color, val bar: Color) {
    companion object {
        val Primary = ModelPalette(circle = Workbook.Blue, bar = Workbook.Teal)
        val Secondary = ModelPalette(circle = Workbook.Lavender, bar = Workbook.Lavender)
    }
}

/** Width/height ratio used for each model so that every model represents the same-sized whole. */
fun ModelType.aspect(): Float = if (this == ModelType.BAR) 3.2f else 1f

/** Text summary for a model, e.g. "Circle model: 3 of 4 equal parts shaded, three quarters." */
fun modelSummary(type: ModelType, denominator: Int, shaded: Int): String {
    val kind = when (type) {
        ModelType.CIRCLE -> "Circle model"
        ModelType.PIZZA -> "Pizza-style circle model"
        ModelType.BAR -> "Bar model"
    }
    val words = FractionWords.spoken(Fraction(shaded, denominator))
    val parts = if (denominator == 1) "1 whole part" else "$denominator equal parts"
    val state = when (shaded) {
        0 -> "no parts shaded"
        denominator -> "all $parts shaded, one whole"
        else -> "$shaded of $parts shaded"
    }
    return "$kind: $state, $words."
}

/**
 * Exact fraction model drawn with Canvas. Every piece is mathematically equal. Selected pieces
 * differ by colour AND by a diagonal hatch pattern plus a centre marker.
 */
@Composable
fun FractionModel(
    type: ModelType,
    denominator: Int,
    selected: Set<Int>,
    modifier: Modifier = Modifier,
    palette: ModelPalette = ModelPalette.Primary,
    onToggle: ((Int) -> Unit)? = null,
    onShadeNext: (() -> Unit)? = null,
    onUnshade: (() -> Unit)? = null,
    description: String? = null,
) {
    require(denominator > 0)
    val toggle = rememberUpdatedState(onToggle)
    val summary = description ?: modelSummary(type, denominator, selected.size)
    val actions = buildList {
        if (onShadeNext != null) add(CustomAccessibilityAction("Shade one more part") { onShadeNext(); true })
        if (onUnshade != null) add(CustomAccessibilityAction("Unshade one part") { onUnshade(); true })
    }
    var m = modifier
        .aspectRatio(type.aspect())
        .semantics {
            contentDescription = summary
            if (actions.isNotEmpty()) customActions = actions
        }
    if (onToggle != null) {
        m = m.pointerInput(type, denominator) {
            detectTapGestures { pos ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val index = if (type == ModelType.BAR) {
                    val pad = barPadding(w, h)
                    if (pos.y < pad || pos.y > h - pad) null
                    else ModelGeometry.cellAt(pos.x - pad, w - 2 * pad, denominator)
                } else {
                    val r = circleRadius(type, w, h)
                    ModelGeometry.sectorAt(pos.x - w / 2f, pos.y - h / 2f, r, denominator)
                }
                if (index != null) toggle.value?.invoke(index)
            }
        }
    }
    Canvas(m) {
        when (type) {
            ModelType.BAR -> drawBar(denominator, selected, palette)
            else -> drawCircleModel(type, denominator, selected, palette)
        }
    }
}

private fun Density.circleRadius(type: ModelType, w: Float, h: Float): Float {
    val crust = if (type == ModelType.PIZZA) 11.dp.toPx() else 0f
    return min(w, h) / 2f - 3.dp.toPx() - crust
}

@Suppress("UNUSED_PARAMETER")
private fun Density.barPadding(w: Float, h: Float): Float = 3.dp.toPx()

private fun DrawScope.drawCircleModel(type: ModelType, denominator: Int, selected: Set<Int>, palette: ModelPalette) {
    val r = circleRadius(type, size.width, size.height)
    val c = Offset(size.width / 2f, size.height / 2f)
    val topLeft = Offset(c.x - r, c.y - r)
    val arcSize = Size(2 * r, 2 * r)
    val pizza = type == ModelType.PIZZA
    val base = if (pizza) Workbook.Dough else Workbook.Unshaded
    val shade = if (pizza) Workbook.Sauce else palette.circle
    val line = 2.dp.toPx()

    if (pizza) {
        // Restrained crust ring, outside the sectors so it never hides a boundary.
        drawCircle(Workbook.Crust, radius = r + 6.dp.toPx(), center = c, style = Stroke(9.dp.toPx()))
        drawCircle(Workbook.CrustDark, radius = r + 10.5.dp.toPx(), center = c, style = Stroke(1.dp.toPx()))
    }

    ModelGeometry.sectors(denominator).forEach { s ->
        val isSel = s.index in selected
        drawArc(if (isSel) shade else base, s.startAngle, s.sweepAngle, useCenter = true, topLeft = topLeft, size = arcSize)
        if (isSel) {
            val path = Path().apply {
                if (denominator == 1) {
                    addOval(Rect(topLeft, arcSize))
                } else {
                    moveTo(c.x, c.y)
                    arcTo(Rect(topLeft, arcSize), s.startAngle, s.sweepAngle, false)
                    close()
                }
            }
            clipPath(path) { hatch() }
            val mid = Math.toRadians((s.startAngle + s.sweepAngle / 2f).toDouble())
            val mr = if (denominator == 1) 0f else r * 0.62f
            marker(Offset(c.x + mr * cos(mid).toFloat(), c.y + mr * sin(mid).toFloat()), denominator)
        }
    }
    if (denominator > 1) {
        ModelGeometry.sectors(denominator).forEach { s ->
            val a = Math.toRadians(s.startAngle.toDouble())
            drawLine(Workbook.Navy, c, Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()), line)
        }
    }
    drawCircle(Workbook.Navy, radius = r, center = c, style = Stroke(line * 1.25f))
}

private fun DrawScope.drawBar(denominator: Int, selected: Set<Int>, palette: ModelPalette) {
    val pad = barPadding(size.width, size.height)
    val w = size.width - 2 * pad
    val h = size.height - 2 * pad
    val line = 2.dp.toPx()
    ModelGeometry.cells(denominator, w).forEach { cell ->
        val isSel = cell.index in selected
        val tl = Offset(pad + cell.left, pad)
        val sz = Size(cell.right - cell.left, h)
        drawRect(if (isSel) palette.bar else Workbook.Unshaded, tl, sz)
        if (isSel) {
            val path = Path().apply { addRect(Rect(tl, sz)) }
            clipPath(path) { hatch() }
            marker(Offset(tl.x + sz.width / 2f, tl.y + sz.height / 2f), denominator)
        }
    }
    ModelGeometry.cells(denominator, w).drop(1).forEach { cell ->
        drawLine(Workbook.Navy, Offset(pad + cell.left, pad), Offset(pad + cell.left, pad + h), line)
    }
    drawRect(Workbook.Navy, Offset(pad, pad), Size(w, h), style = Stroke(line * 1.25f))
}

/** Diagonal hatch: a pattern cue that does not rely on colour alone. */
private fun DrawScope.hatch() {
    val step = 9.dp.toPx()
    val stroke = 1.6.dp.toPx()
    val color = Color.White.copy(alpha = 0.55f)
    var x = -size.height
    while (x < size.width) {
        drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), stroke)
        x += step
    }
}

private fun DrawScope.marker(at: Offset, denominator: Int) {
    val r = (if (denominator > 12) 3.5.dp else 5.dp).toPx()
    drawCircle(Workbook.Yellow, radius = r, center = at)
    drawCircle(Workbook.Navy, radius = r, center = at, style = Stroke(1.2.dp.toPx()))
}
