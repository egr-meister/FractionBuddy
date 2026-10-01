package com.fractionbuddy.app.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.FractionWords
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.domain.fractions.PieceSelection
import com.fractionbuddy.app.ui.theme.Workbook

/** A fraction with a proper horizontal fraction bar. Read aloud as e.g. "three quarters". */
@Composable
fun FractionLabel(
    fraction: Fraction,
    modifier: Modifier = Modifier,
    fontSize: Int = 28,
    color: Color = Workbook.Navy,
) {
    val style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
    Column(
        modifier
            .width(IntrinsicSize.Max)
            .clearAndSetSemantics { contentDescription = FractionWords.spoken(fraction) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(fraction.numerator.toString(), style = style)
        Box(
            Modifier
                .fillMaxWidth()
                .widthIn(min = (fontSize * 0.9f).dp)
                .height((fontSize / 10f).coerceAtLeast(2f).dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(fraction.denominator.toString(), style = style)
    }
}

/** Mixed number such as 1 ½ drawn with a proper fraction bar; whole numbers show as digits. */
@Composable
fun MixedNumberLabel(fraction: Fraction, modifier: Modifier = Modifier, fontSize: Int = 28) {
    val s = fraction.simplified()
    Row(
        modifier.clearAndSetSemantics { contentDescription = FractionWords.spokenMixed(fraction) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (s.wholePart > 0 || s.remainder == 0) {
            Text(
                s.wholePart.toString(),
                fontSize = (fontSize * 1.3f).sp,
                fontWeight = FontWeight.Bold,
                color = Workbook.Navy,
            )
        }
        if (s.remainder != 0) {
            if (s.wholePart > 0) Box(Modifier.width(6.dp))
            FractionLabel(Fraction(s.remainder, s.denominator), fontSize = fontSize)
        }
    }
}

/** − value + control with 48 dp targets and spoken labels. */
@Composable
fun Stepper(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.widthIn(min = 92.dp),
        )
        FilledTonalButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "Decrease $label" },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("−", fontSize = 22.sp) }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 40.dp)
                .semantics { stateDescription = "$label $value" },
        )
        FilledTonalButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "Increase $label" },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("+", fontSize = 22.sp) }
    }
}

/** Numerator and denominator steppers enforcing the learning limits (1–12, 0–denominator). */
@Composable
fun FractionEditor(
    fraction: Fraction,
    onChange: (Fraction) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onClamped: ((Fraction) -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        Stepper(
            label = "Numerator",
            value = fraction.numerator,
            onDecrease = { onChange(Fraction(fraction.numerator - 1, fraction.denominator)) },
            onIncrease = { onChange(Fraction(fraction.numerator + 1, fraction.denominator)) },
            canDecrease = fraction.numerator > 0,
            canIncrease = fraction.numerator < fraction.denominator,
        )
        Stepper(
            label = "Denominator",
            value = fraction.denominator,
            onDecrease = {
                val d = fraction.denominator - 1
                val next = Fraction(minOf(fraction.numerator, d), d)
                onChange(next)
                if (fraction.numerator > d) onClamped?.invoke(next)
            },
            onIncrease = { onChange(Fraction(fraction.numerator, fraction.denominator + 1)) },
            canDecrease = fraction.denominator > Fraction.MIN_DENOMINATOR,
            canIncrease = fraction.denominator < Fraction.MAX_DENOMINATOR,
        )
    }
}

/** Circle / Pizza / Bar selector. */
@Composable
fun ModelSelector(
    selected: ModelType,
    onSelect: (ModelType) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
) {
    val items = listOf(ModelType.CIRCLE to "Circle", ModelType.PIZZA to "Pizza", ModelType.BAR to "Bar")
    val content: @Composable () -> Unit = {
        items.forEach { (type, label) ->
            val isSel = type == selected
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isSel) Workbook.YellowLight else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                    .border(
                        if (isSel) 2.dp else 1.dp,
                        if (isSel) Workbook.Navy else MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.small,
                    )
                    .selectable(selected = isSel, role = Role.RadioButton, onClick = { onSelect(type) }),
            ) {
                Box(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        (if (isSel) "● " else "") + label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
    if (vertical) {
        Column(
            modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    } else {
        Row(
            modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

/**
 * Draws a value as one or more equal-sized wholes (used for sums above one).
 * Only call when the denominator is small enough to draw exactly.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiWholeModel(
    value: Fraction,
    type: ModelType,
    modelWidth: Dp,
    modifier: Modifier = Modifier,
    palette: ModelPalette = ModelPalette.Primary,
) {
    val d = value.denominator
    val wholes = maxOf(1, (value.numerator + d - 1) / d)
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(wholes) { w ->
            val shaded = (value.numerator - w * d).coerceIn(0, d)
            FractionModel(
                type = type,
                denominator = d,
                selected = (0 until shaded).toSet(),
                palette = palette,
                modifier = Modifier.width(modelWidth),
                description = "Whole ${w + 1} of $wholes. " + modelSummary(type, d, shaded),
            )
        }
    }
}

/** Selection helper for read-only models. */
fun Fraction.firstPieces(): Set<Int> = (0 until numerator.coerceAtMost(denominator)).toSet()

/** Convenience: selection for a learning-range fraction. */
fun Fraction.toSelection(): PieceSelection = PieceSelection.firstN(numerator, denominator)

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun VSpace(height: Dp) = Box(Modifier.size(width = 1.dp, height = height))
