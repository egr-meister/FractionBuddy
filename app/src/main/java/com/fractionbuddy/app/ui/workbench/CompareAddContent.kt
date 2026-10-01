package com.fractionbuddy.app.ui.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fractionbuddy.app.domain.fractions.AdditionBreakdown
import com.fractionbuddy.app.domain.fractions.ComparisonBreakdown
import com.fractionbuddy.app.domain.fractions.ComparisonResult
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.MAX_RENDERED_PARTS
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.ui.models.FractionEditor
import com.fractionbuddy.app.ui.models.FractionLabel
import com.fractionbuddy.app.ui.models.FractionModel
import com.fractionbuddy.app.ui.models.MixedNumberLabel
import com.fractionbuddy.app.ui.models.ModelPalette
import com.fractionbuddy.app.ui.models.ModelSelector
import com.fractionbuddy.app.ui.models.MultiWholeModel
import com.fractionbuddy.app.ui.models.SectionCard
import com.fractionbuddy.app.ui.models.firstPieces
import com.fractionbuddy.app.ui.theme.Workbook

/** Two models with identical overall dimensions, side by side when space allows. */
@Composable
internal fun PairOfModels(
    a: Fraction,
    b: Fraction,
    model: ModelType,
    sideBySide: Boolean,
    labelA: String = "A",
    labelB: String = "B",
    separator: String? = null,
) {
    val cell: @Composable (String, Fraction, ModelPalette, Modifier) -> Unit = { label, f, palette, m ->
        Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.titleLarge, color = Workbook.Navy)
                FractionLabel(f, fontSize = 22)
            }
            FractionModel(model, f.denominator, f.firstPieces(), Modifier.fillMaxWidth(), palette)
        }
    }
    if (sideBySide) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            cell(labelA, a, ModelPalette.Primary, Modifier.weight(1f))
            if (separator != null) Text(separator, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            cell(labelB, b, ModelPalette.Secondary, Modifier.weight(1f))
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cell(labelA, a, ModelPalette.Primary, Modifier.fillMaxWidth(0.8f))
            if (separator != null) Text(separator, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            cell(labelB, b, ModelPalette.Secondary, Modifier.fillMaxWidth(0.8f))
        }
    }
}

@Composable
private fun Editors(
    a: Fraction,
    b: Fraction,
    titleA: String,
    titleB: String,
    onA: (Fraction, Boolean) -> Unit,
    onB: (Fraction, Boolean) -> Unit,
    wide: Boolean,
) {
    val ea: @Composable (Modifier) -> Unit = { m ->
        SectionCard(m) {
            FractionEditor(a, { onA(it, false) }, title = titleA, onClamped = { onA(it, true) })
        }
    }
    val eb: @Composable (Modifier) -> Unit = { m ->
        SectionCard(m) {
            FractionEditor(b, { onB(it, false) }, title = titleB, onClamped = { onB(it, true) })
        }
    }
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ea(Modifier.weight(1f))
            eb(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ea(Modifier.fillMaxWidth())
            eb(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun CompareContent(state: WorkbenchUiState, model: ModelType, wide: Boolean, vm: WorkbenchViewModel) {
    val a = state.compareA
    val b = state.compareB
    val breakdown = ComparisonBreakdown(a, b)
    val sideBySide = wide || model != ModelType.BAR

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModelSelector(model, vm::setModel, vertical = false)
        PairOfModels(a, b, model, sideBySide)
        Editors(a, b, "Fraction A", "Fraction B", vm::setCompareA, vm::setCompareB, wide)

        SectionCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Which fraction is larger?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        ComparisonResult.A_LARGER to "A is larger",
                        ComparisonResult.B_LARGER to "B is larger",
                        ComparisonResult.EQUAL to "They are equal",
                    ).forEach { (r, label) ->
                        OutlinedButton(
                            onClick = { vm.guess(r) },
                            modifier = Modifier
                                .weight(1f)
                                .sizeIn(minHeight = 48.dp),
                        ) { Text(label, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                    }
                }
                Button(onClick = vm::revealComparison, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                    Text("Show comparison")
                }
                if (state.compareRevealed) {
                    Column(
                        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.compareGuess?.let { g ->
                            Text(
                                if (g == breakdown.result) "That's right!" else "Let's look at the parts.",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (g == breakdown.result) Workbook.Success else Workbook.Gentle,
                            )
                        }
                        val verdict = when (breakdown.result) {
                            ComparisonResult.A_LARGER -> "$a is larger than $b."
                            ComparisonResult.B_LARGER -> "$b is larger than $a."
                            ComparisonResult.EQUAL -> "$a and $b are equal."
                        }
                        Text(verdict, style = MaterialTheme.typography.titleLarge)
                        Text(breakdown.explanation(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        if (state.compareRevealed) {
            CommonDenominatorView(a, b, breakdown.commonDenominator, breakdown.aConverted, breakdown.bConverted)
        }
    }
}

/** Repartitioned bars at the common denominator, or an exact symbolic fallback above 24 parts. */
@Composable
private fun CommonDenominatorView(a: Fraction, b: Fraction, common: Int, aConv: Fraction, bConv: Fraction) {
    SectionCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Same-size parts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (common <= MAX_RENDERED_PARTS) {
                listOf(Triple(a, aConv, ModelPalette.Primary), Triple(b, bConv, ModelPalette.Secondary)).forEach { (orig, conv, pal) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("$orig = $conv", style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 96.dp))
                        FractionModel(ModelType.BAR, common, conv.firstPieces(), Modifier.weight(1f), pal)
                    }
                }
            } else {
                Text("The common denominator is $common.", style = MaterialTheme.typography.titleLarge)
                Text(
                    "$a = $aConv and $b = $bConv. That is too many parts to draw clearly, so the models above " +
                        "keep their original parts. The calculation is still exact.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
internal fun AddContent(state: WorkbenchUiState, model: ModelType, wide: Boolean, vm: WorkbenchViewModel) {
    val a = state.addA
    val b = state.addB
    val sum = AdditionBreakdown(a, b)
    val sideBySide = wide || model != ModelType.BAR

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModelSelector(model, vm::setModel, vertical = false)
        PairOfModels(a, b, model, sideBySide, labelA = "First", labelB = "Second", separator = "+")
        Editors(a, b, "First fraction", "Second fraction", vm::setAddA, vm::setAddB, wide)
        Button(onClick = vm::revealSum, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Show sum") }

        if (state.sumRevealed) {
            SectionCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(sum.equation(), style = MaterialTheme.typography.headlineSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Sum:", style = MaterialTheme.typography.titleLarge)
                        if (!sum.isWholeNumber) FractionLabel(sum.simplified, fontSize = 26)
                        if (sum.isAboveOne || sum.isWholeNumber) {
                            if (!sum.isWholeNumber) Text("=", style = MaterialTheme.typography.titleLarge)
                            MixedNumberLabel(sum.simplified, fontSize = 26)
                        }
                    }
                    sum.steps().forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    if (!sum.canRenderCommon) {
                        Text("The common denominator is ${sum.commonDenominator}.", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            SumModels(sum, model)
        }
    }
}

@Composable
private fun SumModels(sum: AdditionBreakdown, model: ModelType) {
    val drawAt: Fraction? = when {
        sum.canRenderCommon -> sum.sumAtCommon
        sum.simplified.denominator <= MAX_RENDERED_PARTS -> sum.simplified
        else -> null
    }
    SectionCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The sum as equal parts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (drawAt == null) {
                Text(
                    "${sum.simplified} has ${sum.simplified.denominator} parts in each whole, which is too many to draw " +
                        "clearly. The calculation above is exact.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                if (sum.canRenderCommon) {
                    Text(
                        "Converted: ${sum.aConverted} + ${sum.bConverted}, drawn in ${sum.commonDenominator} equal parts per whole.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text("Shown in simplest form, $drawAt.", style = MaterialTheme.typography.bodyMedium)
                }
                MultiWholeModel(
                    value = drawAt,
                    type = if (drawAt.denominator > 12 && model != ModelType.BAR) ModelType.BAR else model,
                    modelWidth = if (model == ModelType.BAR || drawAt.denominator > 12) 280.dp else 150.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
