package com.hritwik.avoid.presentation.ui.components.jellyseer

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hritwik.avoid.domain.model.jellyseer.JellyseerAvailabilityStatus
import com.hritwik.avoid.domain.model.jellyseer.JellyseerRequestStatus

@Composable
fun JellyseerAvailabilityBadge(
    status: JellyseerAvailabilityStatus,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (status) {
        JellyseerAvailabilityStatus.AVAILABLE -> "Available" to MaterialTheme.colorScheme.primary
        JellyseerAvailabilityStatus.PARTIALLY_AVAILABLE ->
            "Partially available" to MaterialTheme.colorScheme.tertiary
        JellyseerAvailabilityStatus.PENDING -> "Pending approval" to MaterialTheme.colorScheme.secondary
        JellyseerAvailabilityStatus.PROCESSING -> "Processing" to MaterialTheme.colorScheme.secondary
        JellyseerAvailabilityStatus.DELETED -> "Removed" to MaterialTheme.colorScheme.error
        JellyseerAvailabilityStatus.BLACKLISTED ->
            "Blacklisted" to MaterialTheme.colorScheme.error
        JellyseerAvailabilityStatus.UNKNOWN ->
            "Availability unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        modifier = modifier,
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.16f),
            labelColor = color,
            disabledContainerColor = color.copy(alpha = 0.16f),
            disabledLabelColor = color
        )
    )
}

@Composable
fun JellyseerRequestStatusBadge(
    status: JellyseerRequestStatus,
    is4k: Boolean,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (status) {
        JellyseerRequestStatus.PENDING ->
            "Requested" to MaterialTheme.colorScheme.secondary
        JellyseerRequestStatus.APPROVED ->
            "Approved" to MaterialTheme.colorScheme.primary
        JellyseerRequestStatus.COMPLETED ->
            "Completed" to MaterialTheme.colorScheme.primary
        JellyseerRequestStatus.FAILED ->
            "Failed" to MaterialTheme.colorScheme.error
        JellyseerRequestStatus.DECLINED ->
            "Declined" to MaterialTheme.colorScheme.error
        JellyseerRequestStatus.PROCESSING ->
            "Processing" to MaterialTheme.colorScheme.secondary
    }

    val qualitySuffix = if (is4k) " (4K)" else " (HD)"

    AssistChip(
        modifier = modifier,
        onClick = {},
        enabled = false,
        label = { Text(label + qualitySuffix) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.16f),
            labelColor = color,
            disabledContainerColor = color.copy(alpha = 0.16f),
            disabledLabelColor = color
        )
    )
}
