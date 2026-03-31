package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

enum class ThemeSongOption {
    OFF,
    ON_NO_FALLBACK,
    ON_WITH_FALLBACK
}

@Composable
fun ThemeSongConfigDialog(
    initialEnabled: Boolean,
    initialUrl: String,
    onSave: (enabled: Boolean, fallbackUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Determine initial option based on current state
    val initialOption = when {
        !initialEnabled -> ThemeSongOption.OFF
        initialUrl.isBlank() -> ThemeSongOption.ON_NO_FALLBACK
        else -> ThemeSongOption.ON_WITH_FALLBACK
    }

    var selectedOption by remember { mutableStateOf(initialOption) }
    var fallbackUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the text field when "On (with fallback)" is selected
    LaunchedEffect(selectedOption) {
        if (selectedOption == ThemeSongOption.ON_WITH_FALLBACK) {
            focusRequester.requestFocus()
        }
    }

    VoidAlertDialog(
        visible = true,
        title = "Theme Songs Configuration",
        icon = Icons.Default.MusicNote,
        onDismissRequest = onDismiss,
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedOption) {
                        ThemeSongOption.OFF -> onSave(false, "")
                        ThemeSongOption.ON_NO_FALLBACK -> onSave(true, "")
                        ThemeSongOption.ON_WITH_FALLBACK -> onSave(true, fallbackUrl.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
            ) {
                Text(
                    text = "Choose how theme songs should behave on details screens:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Option 1: Off
                SelectionItem(
                    title = "Off",
                    isSelected = selectedOption == ThemeSongOption.OFF,
                    onClick = { selectedOption = ThemeSongOption.OFF }
                )

                // Option 2: On (no fallback)
                SelectionItem(
                    title = "On (no fallback)",
                    isSelected = selectedOption == ThemeSongOption.ON_NO_FALLBACK,
                    onClick = { selectedOption = ThemeSongOption.ON_NO_FALLBACK }
                )

                // Option 3: On (with fallback)
                SelectionItem(
                    title = "On (with fallback URL)",
                    isSelected = selectedOption == ThemeSongOption.ON_WITH_FALLBACK,
                    onClick = { selectedOption = ThemeSongOption.ON_WITH_FALLBACK }
                )

                // Show text field when "On (with fallback)" is selected
                if (selectedOption == ThemeSongOption.ON_WITH_FALLBACK) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                    ) {
                        Text(
                            text = "If the server does not provide a theme song, we'll try <tvdbId>.mp3 at this base URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        OutlinedTextField(
                            value = fallbackUrl,
                            onValueChange = { fallbackUrl = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .focusable(),
                            label = { Text("Base URL (e.g. https://example.com/themes)") },
                            singleLine = true,
                            placeholder = { Text("https://example.com/themes") }
                        )
                    }
                }
            }
        }
    )
}
