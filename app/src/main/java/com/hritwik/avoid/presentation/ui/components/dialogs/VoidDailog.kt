package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun VoidAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Minsk,
    actionColor: Color = PrimaryText,
    icon: ImageVector? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismissButton: (() -> Unit)? = null,
    options: List<String>? = null,
    selectedIndex: Int? = null,
    onOptionSelected: ((Int) -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    if (!visible) return

    AlertDialog(
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = PrimaryText
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryText
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                when {
                    content != null -> content()
                    !options.isNullOrEmpty() && selectedIndex != null && onOptionSelected != null -> {
                        options.forEachIndexed { index, label ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding()
                            ) {
                                RadioButton(
                                    selected = index == selectedIndex,
                                    onClick = { onOptionSelected(index) }
                                )
                                Text(text = label, color = PrimaryText)
                            }
                        }
                    }
                    else -> Box {}
                }
            }
        },
        containerColor = Color.Black.copy(alpha = 0.8f),
        modifier = modifier
            .border(
                width = calculateRoundedValue(2).sdp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(MaterialTheme.shapes.medium),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            if (confirmButton != null) confirmButton()
            else if (confirmText != null && onConfirm != null) {
                TextButton(onClick = onConfirm) {
                    Text(text = confirmText, color = actionColor)
                }
            }
        },
        dismissButton = {
            if (dismissButton != null) dismissButton()
            else if (dismissText != null) {
                TextButton(onClick = onDismissButton ?: onDismissRequest) {
                    Text(text = dismissText, color = actionColor)
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewVoidAlertDialog() {
    VoidAlertDialog(
        visible = true,
        onDismissRequest = {},
        icon = Icons.Default.Delete,
        title = "Delete Item?",
        confirmText = "Confirm",
        onConfirm = {},
        dismissText = "Cancel",
        options = listOf("Option 1", "Option 2", "Option 3"),
        selectedIndex = 1,
        onOptionSelected = {}
    )
}
