package com.fractionbuddy.app.ui.workbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fractionbuddy.app.data.repository.PreferencesRepository
import com.fractionbuddy.app.data.repository.WorkbenchSlot
import com.fractionbuddy.app.domain.fractions.ComparisonResult
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.domain.fractions.PieceSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkbenchMode(val label: String) { EXPLORE("Explore"), COMPARE("Compare"), ADD("Add") }

data class WorkbenchUiState(
    val mode: WorkbenchMode = WorkbenchMode.EXPLORE,
    val explore: PieceSelection = PieceSelection.firstN(1, 2),
    val compareA: Fraction = Fraction(1, 2),
    val compareB: Fraction = Fraction(1, 3),
    val compareGuess: ComparisonResult? = null,
    val compareRevealed: Boolean = false,
    val addA: Fraction = Fraction(1, 2),
    val addB: Fraction = Fraction(1, 4),
    val sumRevealed: Boolean = false,
    /** Polite announcement for screen readers and sighted users (e.g. numerator clamped). */
    val announcement: String? = null,
)

class WorkbenchViewModel(private val prefs: PreferencesRepository) : ViewModel() {

    private val _state = MutableStateFlow(WorkbenchUiState())
    val state: StateFlow<WorkbenchUiState> = _state.asStateFlow()

    val modelType: StateFlow<ModelType> = prefs.preferences.map { it.preferredModel }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ModelType.CIRCLE)

    init {
        viewModelScope.launch {
            val saved = prefs.workbench.first()
            _state.update {
                it.copy(
                    explore = PieceSelection.firstN(saved.explore.numerator, saved.explore.denominator),
                    compareA = saved.compareA,
                    compareB = saved.compareB,
                    addA = saved.addA,
                    addB = saved.addB,
                )
            }
        }
    }

    fun setMode(mode: WorkbenchMode) = _state.update { it.copy(mode = mode, announcement = null) }

    fun setModel(type: ModelType) {
        viewModelScope.launch { prefs.setPreferredModel(type) }
    }

    // --- Explore ---------------------------------------------------------------------------

    fun togglePiece(index: Int) = setExplore(_state.value.explore.toggle(index))

    fun incrementNumerator() = setExplore(_state.value.explore.increment())

    fun decrementNumerator() = setExplore(_state.value.explore.decrement())

    fun setDenominator(d: Int) {
        val result = _state.value.explore.withDenominator(d)
        val msg = if (result.clamped) {
            "There are only ${result.selection.denominator} parts now, so the numerator changed to ${result.selection.numerator}."
        } else null
        setExplore(result.selection, msg)
    }

    fun resetExplore() = setExplore(PieceSelection.firstN(1, 2), "Reset to 1/2.")

    private fun setExplore(sel: PieceSelection, announcement: String? = null) {
        _state.update { it.copy(explore = sel, announcement = announcement) }
        viewModelScope.launch { prefs.setFraction(WorkbenchSlot.EXPLORE, sel.fraction) }
    }

    // --- Compare ---------------------------------------------------------------------------

    fun setCompareA(f: Fraction, clamped: Boolean = false) {
        _state.update { it.copy(compareA = f, compareGuess = null, compareRevealed = false, announcement = clampMsg("A", f, clamped)) }
        viewModelScope.launch { prefs.setFraction(WorkbenchSlot.COMPARE_A, f) }
    }

    fun setCompareB(f: Fraction, clamped: Boolean = false) {
        _state.update { it.copy(compareB = f, compareGuess = null, compareRevealed = false, announcement = clampMsg("B", f, clamped)) }
        viewModelScope.launch { prefs.setFraction(WorkbenchSlot.COMPARE_B, f) }
    }

    /** Unscored guess in Explore Compare mode. */
    fun guess(result: ComparisonResult) = _state.update { it.copy(compareGuess = result, compareRevealed = true) }

    fun revealComparison() = _state.update { it.copy(compareRevealed = true) }

    // --- Add -------------------------------------------------------------------------------

    fun setAddA(f: Fraction, clamped: Boolean = false) {
        _state.update { it.copy(addA = f, sumRevealed = false, announcement = clampMsg("the first fraction", f, clamped)) }
        viewModelScope.launch { prefs.setFraction(WorkbenchSlot.ADD_A, f) }
    }

    fun setAddB(f: Fraction, clamped: Boolean = false) {
        _state.update { it.copy(addB = f, sumRevealed = false, announcement = clampMsg("the second fraction", f, clamped)) }
        viewModelScope.launch { prefs.setFraction(WorkbenchSlot.ADD_B, f) }
    }

    fun revealSum() = _state.update { it.copy(sumRevealed = true) }

    private fun clampMsg(name: String, f: Fraction, clamped: Boolean): String? =
        if (clamped) "For $name there are only ${f.denominator} parts now, so the numerator changed to ${f.numerator}." else null
}
