package com.hritwik.avoid.presentation.ui.screen.profile.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.presentation.ui.components.common.SettingItem
import com.hritwik.avoid.presentation.ui.components.common.SettingItemWithSwitch
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlin.math.abs

@Composable
fun PersonalizationTabContent(
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val settings by userDataViewModel.personalizationSettings.collectAsStateWithLifecycle()
    val themeMode = settings.themeMode
    val fontScale = settings.fontScale
    val gestures = settings.gesturesEnabled
    val highContrast = settings.highContrast

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        item {
            Text(
                text = "Personalization",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        }

        item {
            val themes = listOf("dark", "colorblind")
            val index = themes.indexOf(themeMode).let { if (it == -1) 0 else it }
            SettingItem(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                subtitle = "Choose app theme",
                trailingText = themes[index].replaceFirstChar { it.uppercase() },
                onClick = {
                    val next = themes[(index + 1) % themes.size]
                    userDataViewModel.setThemeMode(next)
                }
            )
        }

        item {
            val scales = listOf(0.85f, 1.0f, 1.15f, 1.3f)
            val current = scales.indexOfFirst { abs(it - fontScale) < 0.01f }
            SettingItem(
                icon = Icons.Default.TextFields,
                title = "Font Scale",
                subtitle = "Adjust overall text size",
                trailingText = String.format("%.2fx", fontScale),
                onClick = {
                    val next = scales[(current + 1) % scales.size]
                    userDataViewModel.setFontScale(next)
                }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.TouchApp,
                title = "Gesture Controls",
                subtitle = "Enable player gestures",
                checked = gestures,
                onCheckedChange = { userDataViewModel.setGesturesEnabled(it) }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.InvertColors,
                title = "High Contrast",
                subtitle = "Improve text visibility",
                checked = highContrast,
                onCheckedChange = { userDataViewModel.setHighContrastEnabled(it) }
            )
        }
    }
}
