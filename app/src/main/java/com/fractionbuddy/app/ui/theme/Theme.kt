package com.fractionbuddy.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Playful geometry-workbook palette. */
object Workbook {
    val Cream = Color(0xFFFFF8EC)
    val Paper = Color(0xFFFFFDF7)
    val GridLine = Color(0xFFEDE3CF)
    val Navy = Color(0xFF1F2A44)
    val NavySoft = Color(0xFF46506A)
    val Blue = Color(0xFF6F8FB8)
    val BlueDeep = Color(0xFF3F6496)
    val BlueLight = Color(0xFFDCE6F3)
    val Teal = Color(0xFF6FB7AE)
    val TealDeep = Color(0xFF2F7D74)
    val TealLight = Color(0xFFD9EEEB)
    val Lavender = Color(0xFFA99BD3)
    val LavenderDeep = Color(0xFF6A5AA8)
    val LavenderLight = Color(0xFFECE8F7)
    val Yellow = Color(0xFFF4C84A)
    val YellowLight = Color(0xFFFFF0C2)
    val Crust = Color(0xFFD9A066)
    val CrustDark = Color(0xFFAF7440)
    val Dough = Color(0xFFFFF3DA)
    val Sauce = Color(0xFFE07A5F)
    val Unshaded = Color(0xFFFFFFFF)
    val Success = Color(0xFF2E7D4F)
    val Gentle = Color(0xFF8A5A00)
}

private val colors = lightColorScheme(
    primary = Workbook.BlueDeep,
    onPrimary = Color.White,
    primaryContainer = Workbook.BlueLight,
    onPrimaryContainer = Workbook.Navy,
    secondary = Workbook.TealDeep,
    onSecondary = Color.White,
    secondaryContainer = Workbook.TealLight,
    onSecondaryContainer = Workbook.Navy,
    tertiary = Workbook.LavenderDeep,
    onTertiary = Color.White,
    tertiaryContainer = Workbook.LavenderLight,
    onTertiaryContainer = Workbook.Navy,
    background = Workbook.Cream,
    onBackground = Workbook.Navy,
    surface = Workbook.Paper,
    onSurface = Workbook.Navy,
    surfaceVariant = Color(0xFFF3EAD8),
    onSurfaceVariant = Workbook.NavySoft,
    outline = Color(0xFF7D7663),
    outlineVariant = Color(0xFFD8CDB6),
    error = Color(0xFFB3261E),
)

private val rounded = FontFamily.SansSerif

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = rounded, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontFamily = rounded, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = rounded, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = rounded, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = rounded, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = rounded, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = rounded, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

/** Whether decorative animation is reduced (user setting). */
val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun FractionBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}

/** Subtle grid-paper background. */
fun Modifier.gridPaper(): Modifier = drawBehind {
    drawRect(Workbook.Cream)
    val step = 24.dp.toPx()
    val stroke = 1.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(Workbook.GridLine, Offset(x, 0f), Offset(x, size.height), stroke)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(Workbook.GridLine, Offset(0f, y), Offset(size.width, y), stroke)
        y += step
    }
}

@Composable
fun GridPaper(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().gridPaper(), content = content)
}
