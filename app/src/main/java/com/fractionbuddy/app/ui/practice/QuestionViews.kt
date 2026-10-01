package com.fractionbuddy.app.ui.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fractionbuddy.app.domain.fractions.Fraction
import com.fractionbuddy.app.domain.fractions.FractionWords
import com.fractionbuddy.app.domain.fractions.ModelType
import com.fractionbuddy.app.domain.generation.PracticeQuestion
import com.fractionbuddy.app.domain.generation.Topic
import com.fractionbuddy.app.ui.models.FractionLabel
import com.fractionbuddy.app.ui.models.FractionModel
import com.fractionbuddy.app.ui.models.ModelPalette
import com.fractionbuddy.app.ui.models.firstPieces
import com.fractionbuddy.app.ui.theme.LocalReducedMotion
import com.fractionbuddy.app.ui.theme.Workbook

/** The model or expression for a question. No interactive controls that would reveal the answer. */
@Composable
fun QuestionVisual(q: PracticeQuestion, modifier: Modifier = Modifier) {
    val modelWidth = if (q.modelType == ModelType.BAR) 1f else 0.62f
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(q.prompt(), style = MaterialTheme.typography.headlineSmall, color = Workbook.Navy)
        when (q.topic) {
            Topic.IDENTIFY -> {
                // The shaded amount must not be spoken directly, or the answer would be revealed.
                val parts = if (q.first.denominator == 1) "1 whole part" else "${q.first.denominator} equal parts"
                FractionModel(
                    q.modelType, q.first.denominator, q.first.firstPieces(),
                    Modifier.fillMaxWidth(modelWidth).widthIn(max = 360.dp),
                    description = "Model with $parts. ${q.first.numerator} shaded.",
                )
            }
            Topic.COMPARE -> {
                val second = q.second!!
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    listOf(Triple("A", q.first, ModelPalette.Primary), Triple("B", second, ModelPalette.Secondary))
                        .forEach { (label, f, pal) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(label, style = MaterialTheme.typography.titleLarge)
                                    FractionLabel(f, fontSize = 24)
                                }
                                FractionModel(q.modelType, f.denominator, f.firstPieces(), Modifier.fillMaxWidth(), pal)
                            }
                        }
                }
            }
            Topic.ADD -> {
                val second = q.second!!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${FractionWords.spoken(q.first)} plus ${FractionWords.spoken(second)}"
                    },
                ) {
                    FractionLabel(q.first, fontSize = 30)
                    Text("+", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    FractionLabel(second, fontSize = 30)
                    Text("= ?", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FractionModel(q.modelType, q.first.denominator, q.first.firstPieces(), Modifier.weight(1f))
                    Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    FractionModel(q.modelType, second.denominator, second.firstPieces(), Modifier.weight(1f), ModelPalette.Secondary)
                }
            }
            Topic.EQUIVALENT -> {
                FractionLabel(q.first, fontSize = 34)
                FractionModel(
                    q.modelType, q.first.denominator, q.first.firstPieces(),
                    Modifier.fillMaxWidth(modelWidth).widthIn(max = 360.dp),
                )
            }
        }
    }
}

/** Answer options. After the first submission they are disabled and the correct one is marked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnswerOptions(
    q: PracticeQuestion,
    selected: String?,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        q.options.forEach { code ->
            val answered = selected != null
            val isCorrect = q.isCorrect(code)
            val isChosen = code == selected
            val (bg, border, mark) = when {
                answered && isCorrect -> Triple(Color(0xFFDDF1E4), Workbook.Success, "✓ ")
                answered && isChosen -> Triple(Workbook.YellowLight, Workbook.Gentle, "• ")
                else -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline, "")
            }
            val state = when {
                answered && isCorrect -> ", correct answer"
                answered && isChosen -> ", your answer"
                else -> ""
            }
            val label = q.optionLabel(code)
            val spoken = if (q.topic == Topic.COMPARE) label else Fraction.decode(code)?.let {
                if (it.denominator == 1) FractionWords.number(it.numerator) else FractionWords.spoken(it)
            } ?: label
            OutlinedButton(
                onClick = { onAnswer(code) },
                enabled = !answered,
                border = BorderStroke(if (answered && (isCorrect || isChosen)) 3.dp else 1.dp, border),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = bg,
                    contentColor = Workbook.Navy,
                    disabledContainerColor = bg,
                    disabledContentColor = Workbook.Navy,
                ),
                modifier = Modifier
                    .sizeIn(minWidth = if (q.topic == Topic.COMPARE) 140.dp else 88.dp, minHeight = 64.dp)
                    .semantics { contentDescription = spoken + state },
            ) {
                if (q.topic == Topic.COMPARE) {
                    Text(mark + label, style = MaterialTheme.typography.titleMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mark.isNotEmpty()) Text(mark, style = MaterialTheme.typography.titleMedium)
                        val f = Fraction.decode(code)
                        if (f != null && f.denominator != 1) FractionLabel(f, fontSize = 22)
                        else Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Friendly feedback + explanation + Next. */
@Composable
fun Feedback(
    q: PracticeQuestion,
    correct: Boolean,
    nextLabel: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    val pop = remember { Animatable(if (reduced) 1f else 0.85f) }
    LaunchedEffect(Unit) { if (!reduced) pop.animateTo(1f, spring(dampingRatio = 0.45f)) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = if (correct) Color(0xFFE6F4EA) else Workbook.YellowLight,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (correct) "That's right!" else "Let's look at the parts.",
                style = MaterialTheme.typography.titleLarge,
                color = if (correct) Workbook.Success else Workbook.Gentle,
                modifier = Modifier.scale(pop.value),
            )
            if (!correct) {
                Text("The answer is ${q.optionLabel(q.correctAnswer)}.", style = MaterialTheme.typography.titleMedium)
            }
            Text(q.explanation(), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onNext, modifier = Modifier.sizeIn(minHeight = 48.dp, minWidth = 120.dp)) { Text(nextLabel) }
        }
    }
}

@Composable
fun HintArea(q: PracticeQuestion, shown: Boolean, enabled: Boolean, onHint: () -> Unit) {
    if (shown) {
        Surface(shape = MaterialTheme.shapes.medium, color = Workbook.LavenderLight, modifier = Modifier.fillMaxWidth()) {
            Text("Hint: ${q.hint()}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
        }
    } else if (enabled) {
        TextButton(onClick = onHint, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Hint") }
    }
}
