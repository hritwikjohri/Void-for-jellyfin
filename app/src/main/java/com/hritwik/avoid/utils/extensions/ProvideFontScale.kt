package com.hritwik.avoid.utils.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.hritwik.avoid.presentation.ui.theme.LocalFontScale

@Composable
fun ProvideFontScale(scale: Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFontScale provides scale) {
        content()
    }
}