package com.fractionbuddy.app.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fractionbuddy.app.data.repository.CalculatorRepository
import com.fractionbuddy.app.data.repository.HistoryCalculation
import com.fractionbuddy.app.data.repository.PreferencesRepository
import com.fractionbuddy.app.domain.calculator.CalculatorEngine
import com.fractionbuddy.app.domain.calculator.CalculatorState
import com.fractionbuddy.app.domain.calculator.Operator
import com.fractionbuddy.app.ui.models.ScreenFrame
import com.fractionbuddy.app.ui.models.SectionCard
import com.fractionbuddy.app.ui.theme.Workbook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Basic calculator. Separate from the exact fraction tools; never affects learning scores. */
class CalculatorViewModel(
    private val repo: CalculatorRepository,
    private val prefs: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryCalculation>> =
        repo.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _state.value = CalculatorEngine.decodeDraft(prefs.calculatorDraft.first()) }
    }

    fun digit(c: Char) = set(CalculatorEngine.digit(_state.value, c))
    fun decimal() = set(CalculatorEngine.decimal(_state.value))
    fun sign() = set(CalculatorEngine.toggleSign(_state.value))
    fun clear() = set(CalculatorEngine.clear())
    fun backspace() = set(CalculatorEngine.backspace(_state.value))
    fun operator(op: Operator) = applyOutcome(CalculatorEngine.operator(_state.value, op))
    fun evaluate() = applyOutcome(CalculatorEngine.equals(_state.value))

    private fun applyOutcome(outcome: CalculatorEngine.Outcome) {
        set(outcome.state)
        outcome.record?.let { r -> viewModelScope.launch { repo.add(r) } }
    }

    private fun set(s: CalculatorState) {
        _state.value = s
        viewModelScope.launch { prefs.setCalculatorDraft(CalculatorEngine.encodeDraft(s)) }
    }
}

@Composable
fun CalculatorScreen(vm: CalculatorViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    ScreenFrame(title = "Calculator", onBack = onBack) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Workbook.Paper,
            border = androidx.compose.foundation.BorderStroke(2.dp, Workbook.Navy),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    if (s.operator != null) s.expression else " ",
                    style = MaterialTheme.typography.titleMedium,
                    color = Workbook.NavySoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    (if (s.lastRounded && s.error == null) "≈ " else "") + s.mainDisplay,
                    fontSize = if (s.error != null) 20.sp else 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    color = if (s.error != null) Workbook.Gentle else Workbook.Navy,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (s.lastRounded && s.error == null) {
                    Text("Rounded to 6 decimal places", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        val rows: List<List<Key>> = listOf(
            listOf(Key("C", "Clear", Kind.Util) { vm.clear() }, Key("⌫", "Backspace", Kind.Util) { vm.backspace() },
                Key("±", "Change sign", Kind.Util) { vm.sign() }, Key("÷", "Divide", Kind.Op) { vm.operator(Operator.DIVIDE) }),
            listOf(digit('7', vm), digit('8', vm), digit('9', vm), Key("×", "Multiply", Kind.Op) { vm.operator(Operator.MULTIPLY) }),
            listOf(digit('4', vm), digit('5', vm), digit('6', vm), Key("−", "Subtract", Kind.Op) { vm.operator(Operator.SUBTRACT) }),
            listOf(digit('1', vm), digit('2', vm), digit('3', vm), Key("+", "Add", Kind.Op) { vm.operator(Operator.ADD) }),
            listOf(digit('0', vm), Key(".", "Decimal point", Kind.Digit) { vm.decimal() }, Key("=", "Equals", Kind.Eq) { vm.evaluate() }),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { k ->
                        FilledTonalButton(
                            onClick = k.action,
                            modifier = Modifier
                                .weight(if (k.kind == Kind.Eq) 2f else 1f)
                                .sizeIn(minHeight = 60.dp)
                                .semantics { contentDescription = k.spoken },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = when (k.kind) {
                                    Kind.Op -> Workbook.BlueLight
                                    Kind.Eq -> Workbook.Yellow
                                    Kind.Util -> Workbook.LavenderLight
                                    Kind.Digit -> Color.White
                                },
                                contentColor = Workbook.Navy,
                            ),
                        ) { Text(k.label, fontSize = 24.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }

        Text("History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (history.isEmpty()) {
            Text("No calculations yet.", style = MaterialTheme.typography.bodyLarge)
        } else {
            SectionCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    history.forEach { h ->
                        Text(
                            "${h.left} ${h.operator.symbol} ${h.right} = ${if (h.rounded) "≈ " else ""}${h.result}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.semantics {
                                contentDescription = "${h.left} ${h.operator.spoken} ${h.right} equals " +
                                    (if (h.rounded) "approximately " else "") + h.result
                            },
                        )
                    }
                }
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Back to workbench") }
    }
}

private enum class Kind { Digit, Op, Eq, Util }

private data class Key(val label: String, val spoken: String, val kind: Kind, val action: () -> Unit)

private fun digit(c: Char, vm: CalculatorViewModel) = Key(c.toString(), c.toString(), Kind.Digit) { vm.digit(c) }
