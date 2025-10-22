package com.hritwik.avoid.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

val systemFontFamily = FontFamily.Default

val bodyFontFamily = systemFontFamily
val displayFontFamily = systemFontFamily

val LocalFontScale = staticCompositionLocalOf { 1f }

private val baseline = Typography()

private fun TextStyle.scale(fontFamily: FontFamily, scale: Float) =
    copy(fontFamily = fontFamily, fontSize = fontSize * scale)

@Composable
fun AppTypography(): Typography {
    val scale = LocalFontScale.current
    return Typography(
        displayLarge = baseline.displayLarge.scale(displayFontFamily, scale),
        displayMedium = baseline.displayMedium.scale(displayFontFamily, scale),
        displaySmall = baseline.displaySmall.scale(displayFontFamily, scale),
        headlineLarge = baseline.headlineLarge.scale(displayFontFamily, scale),
        headlineMedium = baseline.headlineMedium.scale(displayFontFamily, scale),
        headlineSmall = baseline.headlineSmall.scale(displayFontFamily, scale),
        titleLarge = baseline.titleLarge.scale(displayFontFamily, scale),
        titleMedium = baseline.titleMedium.scale(displayFontFamily, scale),
        titleSmall = baseline.titleSmall.scale(displayFontFamily, scale),
        bodyLarge = baseline.bodyLarge.scale(bodyFontFamily, scale),
        bodyMedium = baseline.bodyMedium.scale(bodyFontFamily, scale),
        bodySmall = baseline.bodySmall.scale(bodyFontFamily, scale),
        labelLarge = baseline.labelLarge.scale(bodyFontFamily, scale),
        labelMedium = baseline.labelMedium.scale(bodyFontFamily, scale),
        labelSmall = baseline.labelSmall.scale(bodyFontFamily, scale),
    )
}