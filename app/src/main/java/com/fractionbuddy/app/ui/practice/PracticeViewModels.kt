package com.fractionbuddy.app.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fractionbuddy.app.data.repository.AppPreferences
import com.fractionbuddy.app.data.repository.PracticeRepository
import com.fractionbuddy.app.data.repository.PreferencesRepository
import com.fractionbuddy.app.data.repository.SessionInfo
import com.fractionbuddy.app.data.repository.StoredQuestion
import com.fractionbuddy.app.domain.RandomProvider
import com.fractionbuddy.app.domain.generation.Difficulty
import com.fractionbuddy.app.domain.generation.PracticeQuestion
import com.fractionbuddy.app.domain.generation.QuestionGenerator
import com.fractionbuddy.app.domain.generation.TopicSelection
import com.fractionbuddy.app.domain.progress.SessionStatus
import com.fractionbuddy.app.domain.progress.resumePosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared helper: generate questions and store a new session. */
suspend fun startNewSession(
    practice: PracticeRepository,
    prefs: PreferencesRepository,
    random: RandomProvider,
    selection: TopicSelection,
    difficulty: Difficulty,
): Long {
    val model = prefs.preferences.first().preferredModel
    val questions = QuestionGenerator(random.next()).generateSession(selection, difficulty, model)
    return practice.startSession(selection, difficulty, questions)
}

// --- Setup ----------------------------------------------------------------------------------

data class SetupUiState(
    val selection: TopicSelection = TopicSelection.MIXED,
    val difficulty: Difficulty = Difficulty.EASY,
    val active: SessionInfo? = null,
    val loaded: Boolean = false,
)

class PracticeSetupViewModel(
    private val practice: PracticeRepository,
    private val prefs: PreferencesRepository,
    private val random: RandomProvider,
) : ViewModel() {

    private val choice = MutableStateFlow<Pair<TopicSelection, Difficulty>?>(null)

    val state: StateFlow<SetupUiState> = combine(
        prefs.preferences,
        practice.observeActiveSession(),
        choice,
    ) { p: AppPreferences, active: SessionInfo?, c ->
        SetupUiState(
            selection = c?.first ?: p.defaultTopic,
            difficulty = c?.second ?: p.defaultDifficulty,
            active = active,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupUiState())

    fun select(t: TopicSelection) = choice.update { (t to (it?.second ?: state.value.difficulty)) }
    fun select(d: Difficulty) = choice.update { ((it?.first ?: state.value.selection) to d) }

    fun start(onStarted: (Long) -> Unit) {
        val s = state.value
        viewModelScope.launch {
            onStarted(startNewSession(practice, prefs, random, s.selection, s.difficulty))
        }
    }

    fun endActive() {
        val id = state.value.active?.id ?: return
        viewModelScope.launch { practice.endSession(id) }
    }
}

// --- Session --------------------------------------------------------------------------------

data class SessionUiState(
    val session: SessionInfo? = null,
    val questions: List<StoredQuestion> = emptyList(),
    val index: Int = 0,
    val showHint: Boolean = false,
    val loaded: Boolean = false,
) {
    val current: StoredQuestion? get() = questions.getOrNull(index)
    val isLast: Boolean get() = index == questions.lastIndex
    val answeredCount: Int get() = questions.count { it.answered }
}

class PracticeSessionViewModel(
    private val sessionId: Long,
    private val practice: PracticeRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val index = MutableStateFlow<Int?>(savedState.get<Int>(KEY_INDEX))
    private val hint = MutableStateFlow(false)

    val state: StateFlow<SessionUiState> = combine(
        practice.observeSession(sessionId),
        practice.observeQuestions(sessionId),
        index,
        hint,
    ) { session, questions, idx, showHint ->
        val resolved = idx
            ?: resumePosition(questions.map { it.answered })
            ?: questions.lastIndex.coerceAtLeast(0)
        SessionUiState(
            session = session,
            questions = questions,
            index = resolved.coerceIn(0, (questions.size - 1).coerceAtLeast(0)),
            showHint = showHint || (questions.getOrNull(resolved)?.hintUsed == true && questions.getOrNull(resolved)?.answered == false),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    private var submitting = false

    fun answer(code: String) {
        val q = state.value.current ?: return
        if (q.answered || submitting) return
        submitting = true
        pinIndex(state.value.index)
        viewModelScope.launch {
            try {
                practice.recordAnswer(q.id, code)
            } finally {
                submitting = false
            }
        }
    }

    fun showHint() {
        val q = state.value.current ?: return
        hint.value = true
        viewModelScope.launch { practice.markHintUsed(q.id) }
    }

    fun next() {
        val s = state.value
        if (s.current?.answered != true || s.isLast) return
        hint.value = false
        pinIndex(s.index + 1)
    }

    /** Completes the session after the tenth answer; calls back with the session id for results. */
    fun finish(onDone: (Long) -> Unit) {
        viewModelScope.launch {
            practice.completeSession(sessionId)
            onDone(sessionId)
        }
    }

    fun endEarly(onDone: (kept: Boolean) -> Unit) {
        viewModelScope.launch { onDone(practice.endSession(sessionId)) }
    }

    val isActive: Boolean get() = state.value.session?.status == SessionStatus.ACTIVE

    private fun pinIndex(i: Int) {
        index.value = i
        savedState[KEY_INDEX] = i
    }

    companion object {
        private const val KEY_INDEX = "question_index"
    }
}

// --- Results --------------------------------------------------------------------------------

data class ResultsUiState(
    val session: SessionInfo? = null,
    val questions: List<StoredQuestion> = emptyList(),
    val loaded: Boolean = false,
) {
    val incorrect: List<StoredQuestion> get() = questions.filter { it.isCorrect == false }
}

class ResultsViewModel(
    sessionId: Long,
    private val practice: PracticeRepository,
    private val prefs: PreferencesRepository,
    private val random: RandomProvider,
) : ViewModel() {
    val state: StateFlow<ResultsUiState> = combine(
        practice.observeSession(sessionId),
        practice.observeQuestions(sessionId),
    ) { s, q -> ResultsUiState(s, q, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultsUiState())

    fun practiceAgain(onStarted: (Long) -> Unit) {
        val s = state.value.session ?: return
        viewModelScope.launch {
            onStarted(startNewSession(practice, prefs, random, s.topicSelection, s.difficulty))
        }
    }
}

// --- Retry (in memory; never rewrites the original result) ----------------------------------

data class RetryUiState(
    val questions: List<PracticeQuestion> = emptyList(),
    val index: Int = 0,
    val selected: String? = null,
    val showHint: Boolean = false,
    val loaded: Boolean = false,
    val finished: Boolean = false,
    val correctCount: Int = 0,
) {
    val current: PracticeQuestion? get() = questions.getOrNull(index)
}

class RetryViewModel(
    sessionId: Long,
    practice: PracticeRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(RetryUiState())
    val state: StateFlow<RetryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val incorrect = practice.getQuestions(sessionId).filter { it.isCorrect == false }.map { it.question }
            _state.value = RetryUiState(
                questions = incorrect,
                index = savedState.get<Int>("retry_index")?.coerceIn(0, (incorrect.size - 1).coerceAtLeast(0)) ?: 0,
                loaded = true,
            )
        }
    }

    fun answer(code: String) = _state.update { s ->
        val q = s.current ?: return@update s
        if (s.selected != null) s
        else s.copy(selected = code, correctCount = s.correctCount + if (q.isCorrect(code)) 1 else 0)
    }

    fun hint() = _state.update { it.copy(showHint = true) }

    fun next() = _state.update { s ->
        if (s.selected == null) return@update s
        if (s.index >= s.questions.lastIndex) {
            s.copy(finished = true)
        } else {
            savedState["retry_index"] = s.index + 1
            s.copy(index = s.index + 1, selected = null, showHint = false)
        }
    }
}
