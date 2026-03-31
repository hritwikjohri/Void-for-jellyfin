package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun SelectionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    selectedColor: Color? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .focusable()
            .padding(
                horizontal = calculateRoundedValue(8).sdp,
                vertical = calculateRoundedValue(6).sdp
            ),
        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = selectedColor ?: MaterialTheme.colorScheme.primary,
                unselectedColor = (selectedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.65f)
            )
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                selectedColor ?: MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        else -> "%.1f KB".format(kb)
    }
}
