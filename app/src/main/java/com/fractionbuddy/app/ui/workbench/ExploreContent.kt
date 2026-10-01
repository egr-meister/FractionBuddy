package com.fractionbuddy.app.ui.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.FractionWords
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.ui.models.FractionLabel
import com.fractionbuddy.app.ui.models.FractionModel
import com.fractionbuddy.app.ui.models.SectionCard
import com.fractionbuddy.app.ui.models.Stepper

@Composable
internal fun ExploreContent(state: WorkbenchUiState, model: ModelType, wide: Boolean, vm: WorkbenchViewModel) {
    val sel = state.explore
    val modelView: @Composable (Modifier) -> Unit = { m ->
        FractionModel(
            type = model,
            denominator = sel.denominator,
            selected = sel.selected,
            modifier = m,
            onToggle = vm::togglePiece,
            onShadeNext = vm::incrementNumerator,
            onUnshade = vm::decrementNumerator,
        )
    }
    val controls: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FractionReadout(sel.fraction)
            SectionCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Stepper(
                        label = "Numerator",
                        value = sel.numerator,
                        onDecrease = vm::decrementNumerator,
                        onIncrease = vm::incrementNumerator,
                        canDecrease = sel.numerator > 0,
                        canIncrease = sel.numerator < sel.denominator,
                    )
                    Stepper(
                        label = "Denominator",
                        value = sel.denominator,
                        onDecrease = { vm.setDenominator(sel.denominator - 1) },
                        onIncrease = { vm.setDenominator(sel.denominator + 1) },
                        canDecrease = sel.denominator > Fraction.MIN_DENOMINATOR,
                        canIncrease = sel.denominator < Fraction.MAX_DENOMINATOR,
                    )
                    Text(
                        "Tip: tap the parts of the model to shade or unshade them.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = vm::resetExplore, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                        Text("Reset")
                    }
                }
            }
        }
    }

    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
            ModelWithSelector(model, vm::setModel, Modifier.weight(1.2f)) { modelView(it) }
            controls(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModelWithSelector(model, vm::setModel, Modifier.fillMaxWidth()) { modelView(it) }
            controls(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FractionReadout(f: Fraction) {
    val simplified = f.simplified()
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FractionLabel(f, fontSize = 40)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${f.numerator} selected ${if (f.numerator == 1) "part" else "parts"} / " +
                    "${f.denominator} equal ${if (f.denominator == 1) "part" else "parts"}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(FractionWords.spoken(f).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
            when {
                f.numerator == 0 -> Text("No parts selected", style = MaterialTheme.typography.titleMedium)
                f.isOneWhole -> Text("One whole", style = MaterialTheme.typography.titleMedium)
            }
            if (simplified != f && f.numerator != 0 && !f.isOneWhole) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Simplified:", style = MaterialTheme.typography.titleMedium)
                    FractionLabel(simplified, fontSize = 20)
                }
            }
        }
    }
}
