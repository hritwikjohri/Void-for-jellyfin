package com.hritwik.avoid.presentation.ui.screen.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.data.download.DownloadService
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
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
    onDelete: (MediaItem) -> Unit = {},
    viewModel: UserDataViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.serviceEvents.collect {}
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeBack(onBack)
    ) {
        AnimatedAmbientBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = "Downloads",
                    showBackButton = showBackButton,
                    onBackClick = onBack
                )

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
                                    DownloadItemRow(
                                        downloadInfo = info,
                                        onClick = { },
                                        onDelete = { onDelete(info.mediaItem) },
                                        isActive = true
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
                                    DownloadItemRow(
                                        downloadInfo = info,
                                        onClick = { onPlay(info.mediaItem) },
                                        onDelete = { onDelete(info.mediaItem) }
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