package com.hritwik.avoid.presentation.ui.components.dialogs

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.R

@Composable
fun DownloadLimitDialog(
    currentLimit: Long,
    onLimitSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val limits = listOf(5_120L, 10_240L, 20_480L, 51_200L)

    VoidAlertDialog(
        visible = true,
        title = stringResource(R.string.download_limit_title),
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                items(limits) { limit ->
                    SelectionItem(
                        title = stringResource(R.string.download_limit_item, limit / 1024),
                        isSelected = currentLimit == limit,
                        onClick = { onLimitSelected(limit) }
                    )
                }
            }
        }
    )
}