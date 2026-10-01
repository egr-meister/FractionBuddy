package com.fractionbuddy.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fractionbuddy.app.domain.generation.Difficulty
import com.fractionbuddy.app.domain.generation.TopicSelection
import com.fractionbuddy.app.ui.models.ScreenFrame
import com.fractionbuddy.app.ui.models.SectionCard
import com.fractionbuddy.app.ui.theme.Workbook
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PracticeSetupScreen(
    vm: PracticeSetupViewModel,
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ScreenFrame(title = "Practice", onBack = onBack) {
        state.active?.let { active ->
            SectionCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unfinished session", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(
                        "${active.topicSelection.label} · ${active.difficulty.label} · " +
                            "${active.score.answered} of 10 answered",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onOpenSession(active.id) }, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Resume") }
                        OutlinedButton(onClick = vm::endActive, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("End session") }
                    }
                }
            }
        }

        Text("Topic", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopicSelection.entries.forEach { t ->
                FilterChip(
                    selected = state.selection == t,
                    onClick = { vm.select(t) },
                    label = { Text(t.label) },
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                )
            }
        }
        Text("Difficulty", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Difficulty.entries.forEach { d ->
                FilterChip(
                    selected = state.difficulty == d,
                    onClick = { vm.select(d) },
                    label = {
                        val dens = if (d == Difficulty.HARD) "2–12" else d.denominators.joinToString(", ")
                        Text("${d.label} · denominators $dens")
                    },
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                )
            }
        }
        Text(
            "10 questions, no timer. Take your time and use a hint whenever you like.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = { vm.start(onOpenSession) },
            enabled = state.loaded,
            modifier = Modifier.sizeIn(minHeight = 56.dp, minWidth = 160.dp),
        ) {
            Text(if (state.active != null) "Start a new session" else "Start", style = MaterialTheme.typography.titleMedium)
        }
        if (state.active != null) {
            Text(
                "Starting a new session ends the unfinished one. Its answered questions are kept.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun PracticeSessionScreen(
    vm: PracticeSessionViewModel,
    onBack: () -> Unit,
    onResults: (Long) -> Unit,
    onEndedWithoutAnswers: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmEnd by remember { mutableStateOf(false) }
    val session = state.session
    val title = session?.let { "${it.topicSelection.label} · ${it.difficulty.label}" } ?: "Practice"

    ScreenFrame(
        title = title,
        onBack = onBack, // the session is already saved after every answer
        actions = {
            TextButton(onClick = { confirmEnd = true }, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("End") }
        },
    ) {
        val stored = state.current
        if (!state.loaded || stored == null) {
            Text("Loading…", style = MaterialTheme.typography.bodyLarge)
            return@ScreenFrame
        }
        val q = stored.question
        Text(
            "Question ${state.index + 1} of ${state.questions.size}",
            style = MaterialTheme.typography.titleMedium,
            color = Workbook.NavySoft,
        )
        LinearProgressIndicator(
            progress = { state.answeredCount / state.questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = Workbook.Teal,
            trackColor = Workbook.TealLight,
        )
        QuestionVisual(q, Modifier.fillMaxWidth())
        AnswerOptions(q, stored.selectedAnswer, vm::answer)
        HintArea(q, shown = state.showHint, enabled = !stored.answered, onHint = vm::showHint)
        if (stored.answered) {
            Feedback(
                q = q,
                correct = stored.isCorrect == true,
                nextLabel = if (state.isLast) "See results" else "Next",
                onNext = { if (state.isLast) vm.finish(onResults) else vm.next() },
            )
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End this session?") },
            text = {
                Text(
                    if (state.answeredCount == 0) "No questions have been answered yet, so this session will not be saved."
                    else "Your ${state.answeredCount} answered questions are kept. Unanswered questions do not count.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    vm.endEarly { kept -> if (kept) session?.id?.let(onResults) else onEndedWithoutAnswers() }
                }) { Text("End session") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Keep going") } },
        )
    }
}

@Composable
fun ResultsScreen(
    vm: ResultsViewModel,
    onBack: () -> Unit,
    onBackToWorkbench: () -> Unit,
    onRetry: (Long) -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ScreenFrame(title = "Results", onBack = onBack) {
        val s = state.session
        if (!state.loaded || s == null) {
            Text(if (state.loaded) "This session is no longer available." else "Loading…", style = MaterialTheme.typography.bodyLarge)
            return@ScreenFrame
        }
        SectionCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${s.topicSelection.label} · ${s.difficulty.label}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${s.score.correct} out of ${if (s.status.name == "COMPLETED") 10 else s.score.answered} correct",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text("Accuracy: ${s.score.accuracyText()}", style = MaterialTheme.typography.bodyLarge)
                if (s.status.name == "ENDED_EARLY") {
                    Text("Ended early after ${s.score.answered} answers. Unanswered questions are not counted.", style = MaterialTheme.typography.bodyMedium)
                }
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(s.startedAt)), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.incorrect.isNotEmpty()) {
                Button(onClick = { onRetry(s.id) }, modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)) {
                    Text("Try incorrect questions")
                }
            }
            OutlinedButton(onClick = { vm.practiceAgain(onOpenSession) }, modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)) {
                Text("Practice again")
            }
        }
        OutlinedButton(onClick = onBackToWorkbench, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
            Text("Back to workbench")
        }
        Text("Question review", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        state.questions.forEach { sq ->
            val q = sq.question
            SectionCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val prompt = when (q.topic.name) {
                        "IDENTIFY" -> "Which fraction is shaded? (${q.first.numerator} of ${q.first.denominator} parts)"
                        "COMPARE" -> "Which is larger: A = ${q.first} or B = ${q.second}?"
                        else -> q.prompt()
                    }
                    Text("${q.position + 1}. $prompt", style = MaterialTheme.typography.titleMedium)
                    when {
                        sq.selectedAnswer == null -> Text("Not answered", style = MaterialTheme.typography.bodyMedium)
                        sq.isCorrect == true -> Text("✓ ${q.optionLabel(sq.selectedAnswer)} — correct", color = Workbook.Success)
                        else -> Text(
                            "Your answer: ${q.optionLabel(sq.selectedAnswer)} · Answer: ${q.optionLabel(q.correctAnswer)}",
                            color = Workbook.Gentle,
                        )
                    }
                    if (sq.selectedAnswer != null) Text(q.explanation(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun RetryScreen(vm: RetryViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    ScreenFrame(title = "Try again", onBack = onDone) {
        if (!state.loaded) {
            Text("Loading…")
            return@ScreenFrame
        }
        if (state.questions.isEmpty()) {
            Text("There are no incorrect questions to try again.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDone, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Back") }
            return@ScreenFrame
        }
        if (state.finished) {
            Text("Nice work going back over these!", style = MaterialTheme.typography.headlineSmall)
            Text(
                "You answered ${state.correctCount} of ${state.questions.size} correctly this time. " +
                    "Your original result stays the same.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onDone, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Back to results") }
            return@ScreenFrame
        }
        val q = state.current ?: return@ScreenFrame
        Text("Retry ${state.index + 1} of ${state.questions.size} · not scored", style = MaterialTheme.typography.titleMedium, color = Workbook.NavySoft)
        QuestionVisual(q, Modifier.fillMaxWidth())
        AnswerOptions(q, state.selected, vm::answer)
        HintArea(q, shown = state.showHint, enabled = state.selected == null, onHint = vm::hint)
        state.selected?.let { sel ->
            Feedback(
                q = q,
                correct = q.isCorrect(sel),
                nextLabel = if (state.index == state.questions.lastIndex) "Finish" else "Next",
                onNext = vm::next,
            )
        }
    }
}
