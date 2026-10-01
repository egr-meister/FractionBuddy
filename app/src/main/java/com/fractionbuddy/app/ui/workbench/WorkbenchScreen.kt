package com.fractionbuddy.app.ui.workbench

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.ui.theme.GridPaper
import com.fractionbuddy.app.ui.theme.Workbook

@Composable
fun WorkbenchScreen(
    viewModel: WorkbenchViewModel,
    onPractice: () -> Unit,
    onCalculator: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val model by viewModel.modelType.collectAsStateWithLifecycle()

    // Back from Compare or Add returns to Explore; from Explore it exits normally.
    BackHandler(enabled = state.mode != WorkbenchMode.EXPLORE) { viewModel.setMode(WorkbenchMode.EXPLORE) }

    GridPaper {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            UtilityBar(onHistory = onHistory, onSettings = onSettings)
            ToolStrip(
                mode = state.mode,
                onMode = viewModel::setMode,
                onPractice = onPractice,
                onCalculator = onCalculator,
            )
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val wide = maxWidth >= 600.dp
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.announcement?.let { Announcement(it) }
                    when (state.mode) {
                        WorkbenchMode.EXPLORE -> ExploreContent(state, model, wide, viewModel)
                        WorkbenchMode.COMPARE -> CompareContent(state, model, wide, viewModel)
                        WorkbenchMode.ADD -> AddContent(state, model, wide, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun UtilityBar(onHistory: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "FractionBuddy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Workbook.Navy,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        TextButton(onClick = onHistory, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("History") }
        TextButton(onClick = onSettings, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Settings") }
    }
}

@Composable
private fun ToolStrip(
    mode: WorkbenchMode,
    onMode: (WorkbenchMode) -> Unit,
    onPractice: () -> Unit,
    onCalculator: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkbenchMode.entries.forEach { m ->
            ToolTab(m.label, selected = m == mode, onClick = { onMode(m) })
        }
        ToolTab("Practice", selected = false, onClick = onPractice)
        ToolTab("Calculator", selected = false, onClick = onCalculator, subtle = true)
    }
}

@Composable
private fun ToolTab(label: String, selected: Boolean, onClick: () -> Unit, subtle: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = when {
            selected -> Workbook.Navy
            subtle -> Workbook.LavenderLight
            else -> MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) Workbook.Cream else Workbook.Navy,
        modifier = Modifier
            .sizeIn(minHeight = 48.dp, minWidth = 64.dp)
            .border(1.dp, if (selected) Workbook.Navy else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Announcement(text: String) {
    Surface(
        color = Workbook.YellowLight,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

/** Model + selector side by side; model keeps a bounded width so every whole is the same size. */
@Composable
internal fun ModelWithSelector(
    model: ModelType,
    onModel: (ModelType) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        content(
            Modifier
                .weight(1f)
                .widthIn(max = 420.dp),
        )
        com.fractionbuddy.app.ui.models.ModelSelector(selected = model, onSelect = onModel)
    }
}
