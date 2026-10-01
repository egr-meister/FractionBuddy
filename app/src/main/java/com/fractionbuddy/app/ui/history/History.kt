package com.fractionbuddy.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fractionbuddy.app.data.repository.LifetimeTotals
import com.fractionbuddy.app.data.repository.PracticeRepository
import com.fractionbuddy.app.data.repository.SessionInfo
import com.fractionbuddy.app.domain.progress.Score
import com.fractionbuddy.app.domain.progress.SessionStatus
import com.fractionbuddy.app.ui.models.ScreenFrame
import com.fractionbuddy.app.ui.models.SectionCard
import com.fractionbuddy.app.ui.theme.Workbook
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date

class HistoryViewModel(practice: PracticeRepository) : ViewModel() {
    val sessions: StateFlow<List<SessionInfo>?> = practice.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val totals: StateFlow<LifetimeTotals?> = practice.observeTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun HistoryScreen(vm: HistoryViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    ScreenFrame(title = "History", onBack = onBack) {
        Text("All-time progress", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SectionCard(Modifier.fillMaxWidth()) {
            val t = totals
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val overall = t?.overall ?: Score(0, 0)
                Text(
                    if (overall.answered == 0) "No answers yet"
                    else "${overall.correct} of ${overall.answered} first answers correct (${overall.accuracyText()})",
                    style = MaterialTheme.typography.titleMedium,
                )
                t?.perTopic?.forEach { tt ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(tt.topic.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            if (tt.answered == 0) "No answers yet" else "${tt.correct}/${tt.answered} · ${tt.score.accuracyText()}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        Text("Recent sessions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        val list = sessions
        when {
            list == null -> Text("Loading…")
            list.isEmpty() -> Text(
                "No practice sessions yet. Finished sessions will appear here.",
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> list.forEach { s ->
                SectionCard(
                    Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .clickable(role = Role.Button, onClickLabel = "Open results") { onOpen(s.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${s.topicSelection.label} · ${s.difficulty.label}", style = MaterialTheme.typography.titleMedium)
                            Text(fmt.format(Date(s.startedAt)), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${s.score.answered} answered · " +
                                    if (s.status == SessionStatus.COMPLETED) "Completed" else "Ended early",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(s.score.accuracyText(), style = MaterialTheme.typography.headlineSmall, color = Workbook.BlueDeep)
                    }
                }
            }
        }
    }
}
