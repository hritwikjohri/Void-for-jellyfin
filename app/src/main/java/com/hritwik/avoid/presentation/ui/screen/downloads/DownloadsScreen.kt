package com.hritwik.avoid.presentation.ui.screen.downloads

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.R
import com.hritwik.avoid.data.download.DownloadService
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.GestureHelper.swipeBack
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit = {},
    showBackButton: Boolean = true,
    onPlay: (MediaItem) -> Unit = {},
    viewModel: UserDataViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.serviceEvents.collect {}
    }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val backFocusRequester = remember { FocusRequester() }
    val activeDownloads = downloads.filter {
        it.status in setOf(
            DownloadService.DownloadStatus.DOWNLOADING,
            DownloadService.DownloadStatus.PAUSED,
            DownloadService.DownloadStatus.QUEUED
        )
    }

    val completedDownloads = downloads.filter {
        it.status == DownloadService.DownloadStatus.COMPLETED
    }

    val allActivePaused = activeDownloads.isNotEmpty() &&
        activeDownloads.all { it.status == DownloadService.DownloadStatus.PAUSED }

    LaunchedEffect(downloads) {
        val validIds = downloads.map { it.mediaSourceId ?: it.mediaItem.id }.toSet()
        val filtered = selectedIds.filter { it in validIds }.toSet()
        if (filtered != selectedIds) {
            selectedIds = filtered
        }
        if (selectionMode && downloads.isEmpty()) {
            selectionMode = false
        }
    }

    val updateSelection: (String, Boolean?) -> Unit = { id, state ->
        selectedIds = when (state) {
            true -> selectedIds + id
            false -> selectedIds - id
            null -> if (id in selectedIds) selectedIds - id else selectedIds + id
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeBack(onBack)
    ) {
        AnimatedAmbientBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(
                            horizontal = calculateRoundedValue(4).sdp,
                            vertical = calculateRoundedValue(8).sdp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Row (
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        if(showBackButton){
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .focusRequester(backFocusRequester)
                                    .semantics { role = Role.Button }
                                    .focusable()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    modifier = Modifier.size(calculateRoundedValue(28).sdp),
                                    tint = Color.White
                                )
                            }
                        }

                        Text(
                            text = "Downloads",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = calculateRoundedValue(8).sdp)
                        )
                    }

                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                    ) {
                        TextButton(
                            onClick = {
                                if (allActivePaused) {
                                    viewModel.resumeAllDownloads()
                                } else {
                                    viewModel.pauseAllDownloads()
                                }
                            },
                            enabled = activeDownloads.isNotEmpty()
                        ) {
                            Text(if (allActivePaused) "Resume All" else "Pause All")
                        }

                        if (selectionMode) {
                            TextButton(
                                onClick = {
                                    viewModel.deleteDownloads(selectedIds)
                                    selectionMode = false
                                    selectedIds = emptySet()
                                },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Text("Delete (${selectedIds.size})")
                            }

                            TextButton(onClick = {
                                selectionMode = false
                                selectedIds = emptySet()
                            }) {
                                Text("Cancel")
                            }
                        } else {
                            TextButton (
                                onClick = {
                                    selectionMode = true
                                    selectedIds = emptySet()
                                },
                                enabled = downloads.isNotEmpty()
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeDownloads.isEmpty() && completedDownloads.isEmpty()) {
                        EmptyState(
                            title = "No downloads",
                            description = "Looks like you don't have any content offline."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                            contentPadding = PaddingValues(
                                vertical = calculateRoundedValue(16).sdp
                            )
                        ) {
                            if (activeDownloads.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Downloading",
                                    )
                                }
                                items(activeDownloads) { info ->
                                    val downloadId = info.mediaSourceId ?: info.mediaItem.id
                                    DownloadItemRow(
                                        downloadInfo = info,
                                        onClick = {
                                            if (selectionMode) {
                                                updateSelection(downloadId, null)
                                            }
                                        },
                                        isActive = true,
                                        selectionEnabled = selectionMode,
                                        isSelected = downloadId in selectedIds,
                                        onSelectionChange = { checked ->
                                            updateSelection(downloadId, checked)
                                        }
                                    )
                                }
                            }

                            if (completedDownloads.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Completed Downloads",
                                    )
                                }

                                items(completedDownloads) { info ->
                                    val downloadId = info.mediaSourceId ?: info.mediaItem.id
                                    DownloadItemRow(
                                        downloadInfo = info,
                                        onClick = {
                                            if (selectionMode) {
                                                updateSelection(downloadId, null)
                                            } else {
                                                onPlay(info.mediaItem)
                                            }
                                        },
                                        selectionEnabled = selectionMode,
                                        isSelected = downloadId in selectedIds,
                                        onSelectionChange = { checked ->
                                            updateSelection(downloadId, checked)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}