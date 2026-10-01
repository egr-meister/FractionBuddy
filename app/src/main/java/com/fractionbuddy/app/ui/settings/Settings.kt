package com.fractionbuddy.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fractionbuddy.app.AppContainer
import com.fractionbuddy.app.data.repository.AppPreferences
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.domain.generation.Difficulty
import com.fractionbuddy.app.domain.generation.TopicSelection
import com.fractionbuddy.app.ui.models.ScreenFrame
import com.fractionbuddy.app.ui.models.SectionCard
import com.fractionbuddy.app.ui.theme.Workbook
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val prefs: StateFlow<AppPreferences> = container.preferences.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    fun setTopic(t: TopicSelection) = launch { container.preferences.setDefaultTopic(t) }
    fun setDifficulty(d: Difficulty) = launch { container.preferences.setDefaultDifficulty(d) }
    fun setModel(m: ModelType) = launch { container.preferences.setPreferredModel(m) }
    fun setReducedMotion(v: Boolean) = launch { container.preferences.setReducedMotion(v) }

    fun clearCalculator(done: () -> Unit) = launch {
        container.calculator.clear()
        container.preferences.clearCalculatorDraft()
        done()
    }

    fun clearProgress(done: () -> Unit) = launch { container.practice.clearProgress(); done() }
    fun clearAll(done: () -> Unit) = launch { container.clearAllData(); done() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

private enum class Destructive(val title: String, val body: String) {
    CALCULATOR("Clear calculator history?", "All saved calculations will be removed."),
    PROGRESS("Clear practice progress?", "All practice sessions, results and all-time totals will be removed."),
    ALL("Clear all local data?", "Calculator history, practice progress and all settings will be removed. This cannot be undone."),
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit, onPrivacy: () -> Unit) {
    val p by vm.prefs.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<Destructive?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    ScreenFrame(title = "Settings", onBack = onBack) {
        Text("Practice defaults", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Default topic", style = MaterialTheme.typography.titleMedium)
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TopicSelection.entries.forEach { t ->
                FilterChip(p.defaultTopic == t, { vm.setTopic(t) }, { Text(t.label) }, Modifier.sizeIn(minHeight = 48.dp))
            }
        }
        Text("Default difficulty", style = MaterialTheme.typography.titleMedium)
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(p.defaultDifficulty == d, { vm.setDifficulty(d) }, { Text(d.label) }, Modifier.sizeIn(minHeight = 48.dp))
            }
        }
        Text("Preferred fraction model", style = MaterialTheme.typography.titleMedium)
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ModelType.CIRCLE to "Circle", ModelType.PIZZA to "Pizza", ModelType.BAR to "Bar").forEach { (m, label) ->
                FilterChip(p.preferredModel == m, { vm.setModel(m) }, { Text(label) }, Modifier.sizeIn(minHeight = 48.dp))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 56.dp)
                .toggleable(value = p.reducedMotion, role = Role.Switch, onValueChange = vm::setReducedMotion),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Reduce decorative animation", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = p.reducedMotion, onCheckedChange = null)
        }

        Text("Data", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SectionCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Everything is stored only on this device. These actions ask a grown-up to confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Destructive.entries.forEach { d ->
                    OutlinedButton(onClick = { pending = d }, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
                        Text(d.title.removeSuffix("?"))
                    }
                }
                notice?.let { Text(it, color = Workbook.Success, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        OutlinedButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
            Text("Privacy information")
        }
    }

    pending?.let { action ->
        AdultConfirmDialog(
            title = action.title,
            body = action.body,
            onDismiss = { pending = null },
            onConfirm = {
                pending = null
                when (action) {
                    Destructive.CALCULATOR -> vm.clearCalculator { notice = "Calculator history cleared." }
                    Destructive.PROGRESS -> vm.clearProgress { notice = "Practice progress cleared." }
                    Destructive.ALL -> vm.clearAll { notice = "All local data cleared." }
                }
            },
        )
    }
}

/**
 * Confirmation with a simple grown-up check (an accidental-entry safeguard, not authentication).
 * Fully usable with a screen reader and the on-screen keyboard.
 */
@Composable
private fun AdultConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val a = rememberSaveable { Random.nextInt(6, 10) }
    val b = rememberSaveable { Random.nextInt(6, 10) }
    var input by rememberSaveable { mutableStateOf("") }
    val ok = input.trim().toIntOrNull() == a * b
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body)
                Text("Grown-up check: what is $a × $b?", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = input,
                    onValueChange = { v -> input = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Answer to $a times $b") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ok, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Clear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Cancel") }
        },
    )
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    ScreenFrame(title = "Privacy", onBack = onBack) {
        val sections = listOf(
            "Works offline" to "FractionBuddy works completely without an internet connection. It does not request the " +
                "internet permission and does not contact any server.",
            "What is stored" to "Your calculator history (latest 50), practice sessions (latest 100), all-time practice " +
                "totals, the fractions you last explored and your settings. Nothing else.",
            "Where it is stored" to "Only in the app's private storage on this device. Other apps cannot read it.",
            "No accounts, ads or tracking" to "There are no accounts, no advertising, no analytics, no payments and no " +
                "external links. No personal information is needed.",
            "Backups" to "Cloud backup and device-to-device transfer are turned off for this app, so your data stays on " +
                "this device.",
            "Permissions" to "FractionBuddy needs no permissions: no camera, microphone, location or contacts.",
            "Deleting data" to "Use Settings › Data to clear calculator history, practice progress or everything. " +
                "Uninstalling the app also removes all of its data.",
        )
        sections.forEach { (h, b) ->
            Text(h, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(b, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
