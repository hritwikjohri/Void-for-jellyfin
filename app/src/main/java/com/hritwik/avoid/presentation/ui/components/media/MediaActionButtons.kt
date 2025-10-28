package com.hritwik.avoid.presentation.ui.components.media

import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.core.ServiceManagerEntryPoint
import com.hritwik.avoid.data.download.DownloadService.DownloadInfo
import com.hritwik.avoid.data.download.DownloadService.DownloadStatus
import com.hritwik.avoid.domain.model.download.DownloadCodec
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.PlaybackOptions
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.components.dialogs.AudioTrackDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SelectionItem
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.VersionSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.VideoQualityDialog
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.extensions.getPosterUrl
import com.hritwik.avoid.utils.extensions.resolveSubtitleOffIndex
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import dagger.hilt.android.EntryPointAccessors
import ir.kaaveh.sdpcompose.sdp

private const val RESUME_THRESHOLD = 10_000_000L

enum class DownloadScope {
    ALL,
    UNWATCHED,
}

@OptIn(UnstableApi::class)
@Composable
fun MediaActionButtons(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    serverUrl: String,
    playbackItem: MediaItem? = null,
    shareButton: Boolean = false,
    onPlayClick: (PlaybackInfo) -> Unit,
    onDownloadClick: (MediaItem, DownloadRequest, String?, DownloadScope) -> Unit,
    playButtonSize: Int = 72,
    showDownload: Boolean = true,
    showMediaInfo: Boolean = true,
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel(),
    videoPlaybackViewModel: VideoPlaybackViewModel = hiltViewModel()
) {
    val playbackState by videoPlaybackViewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val videoOptionsState by videoPlaybackViewModel.state.collectAsStateWithLifecycle()
    val playbackTarget = playbackItem ?: mediaItem.takeIf { !mediaItem.isFolder }
    val downloadId = playbackState.playbackOptions.selectedMediaSource?.id
        ?: playbackTarget?.takeIf { !mediaItem.isFolder }?.id
        ?: mediaItem.id
    val downloadStatus by userDataViewModel.downloadStatus(downloadId).collectAsStateWithLifecycle()
    val downloadSettings by userDataViewModel.downloadSettings.collectAsStateWithLifecycle()
    val downloadsState by userDataViewModel.downloads.collectAsStateWithLifecycle()
    val activeDownloadInfo = downloadsState.find {
        it.mediaSourceId == downloadId || it.mediaItem.id == downloadId
    }
    val isStaticDownload = activeDownloadInfo?.request?.static == true
    val isTranscodeDownload = activeDownloadInfo?.request?.static == false
    val downloadProgress = if (isStaticDownload) activeDownloadInfo.progress else 0f
    val isFavorite by userDataViewModel.isFavorite(mediaItem.id).collectAsStateWithLifecycle()
    val isPlayed by userDataViewModel.isPlayed(mediaItem.id).collectAsStateWithLifecycle()
    val currentQuality = downloadSettings.downloadQuality
    val selectedCodec = downloadSettings.downloadCodec
    var showDownloadMenu by remember { mutableStateOf(false) }
    var showScopeDialog by remember { mutableStateOf(false) }
    var pendingSourceId by remember { mutableStateOf<String?>(null) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var pendingScope by remember { mutableStateOf<DownloadScope?>(null) }
    var buttonColor by remember { mutableStateOf(Color(0xFF1976D2)) }
    val canDownload = (!mediaItem.isFolder && mediaItem.type in listOf("Movie", "Episode")) || mediaItem.type in listOf("Season", "Series")
    val isSeason = mediaItem.type.equals("Season", ignoreCase = true)
    val seasonDownloads = if (isSeason) {
        downloadsState.filter { download ->
            val item = download.mediaItem
            item.type.equals("Episode", ignoreCase = true) && item.seasonId == mediaItem.id
        }
    } else {
        emptyList()
    }
    val seasonDownloadStatus = if (isSeason) {
        computeSeasonDownloadStatus(seasonDownloads, mediaItem.childCount)
    } else {
        null
    }
    val effectiveDownloadStatus = seasonDownloadStatus ?: downloadStatus.takeUnless { isSeason }
    val context = LocalContext.current
    val serviceManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ServiceManagerEntryPoint::class.java
        ).serviceManager()
    }

    val resolveSubtitleIndexForPlayback: (PlaybackOptions) -> Int? = { options ->
        val referenceItem = videoOptionsState.mediaItem ?: playbackTarget
        if (referenceItem == null) {
            null
        } else {
            val source = options.selectedMediaSource
            val offIndex = referenceItem.resolveSubtitleOffIndex(
                source?.id ?: playbackState.mediaSourceId,
                source?.audioStreams ?: playbackState.availableAudioStreams,
                source?.subtitleStreams ?: playbackState.availableSubtitleStreams
            )
            options.selectedSubtitleStream?.index
                ?: offIndex?.takeIf { options.selectedSubtitleStream == null }
        }
    }

    LaunchedEffect(playbackTarget?.id, authState.authSession) {
        val session = authState.authSession ?: return@LaunchedEffect
        val target = playbackTarget ?: return@LaunchedEffect
        videoPlaybackViewModel.initializeVideoOptions(
            mediaItem = target,
            userId = session.userId.id,
            accessToken = session.accessToken
        )
    }

    LaunchedEffect(mediaItem.id) {
        val imageUrl = mediaItem.getPosterUrl(serverUrl)
        val color = extractDominantColor(imageUrl)
        if (color != null) {
            buttonColor = color
        }
    }

    val resolvedPlaybackItem = videoOptionsState.mediaItem ?: playbackTarget
    val playbackPositionTicks = resolvedPlaybackItem?.userData?.playbackPositionTicks ?: 0L
    val runTimeTicks = resolvedPlaybackItem?.runTimeTicks
    val hasResumableProgress = playbackPositionTicks > 0 && (runTimeTicks == null || playbackPositionTicks < runTimeTicks - RESUME_THRESHOLD)
    val playbackItemForActions = resolvedPlaybackItem

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DynamicPlayButton(
                onClick = {
                    playbackItemForActions?.let { itemForPlayback ->
                        videoPlaybackViewModel.selectStartFromBeginning()
                        val playbackOptions = playbackState.playbackOptions

                        val playbackInfo = PlaybackInfo(
                            mediaItem = itemForPlayback,
                            mediaSourceId = playbackOptions.selectedMediaSource?.id,
                            audioStreamIndex = playbackOptions.selectedAudioStream?.index,
                            subtitleStreamIndex = resolveSubtitleIndexForPlayback(playbackOptions),
                            startPosition = 0L
                        )

                        onPlayClick(playbackInfo)
                    }
                },
                onResumeClick = {
                    playbackItemForActions?.let { itemForPlayback ->
                        val resumePositionTicks = itemForPlayback.userData?.playbackPositionTicks ?: 0L
                        videoPlaybackViewModel.selectResumePlayback(resumePositionTicks)
                        val playbackOptions = playbackState.playbackOptions

                        val playbackInfo = PlaybackInfo(
                            mediaItem = itemForPlayback,
                            mediaSourceId = playbackOptions.selectedMediaSource?.id,
                            audioStreamIndex = playbackOptions.selectedAudioStream?.index,
                            subtitleStreamIndex = resolveSubtitleIndexForPlayback(playbackOptions),
                            startPosition = resumePositionTicks / 10_000
                        )

                        onPlayClick(playbackInfo)
                    }
                },
                dominantColor = buttonColor,
                buttonSize = playButtonSize.dp,
                iconSize = (playButtonSize / 2).dp,
                mediaItem = playbackItemForActions,
                hasResumableProgress = hasResumableProgress
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        authState.authSession?.let { session ->
                            userDataViewModel.toggleFavorite(
                                userId = session.userId.id,
                                mediaId = mediaItem.id,
                                accessToken = session.accessToken,
                                isFavorite = isFavorite,
                                mediaItem = mediaItem
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = if (isFavorite) {
                            "Remove from favorites"
                        } else {
                            "Add to favorites"
                        },
                        tint = if (isFavorite) {
                            Color.Red
                        } else {
                            PrimaryText
                        },
                        modifier = Modifier.size(calculateRoundedValue(24).sdp)
                    )
                }

                IconButton(
                    onClick = {
                        authState.authSession?.let { session ->
                            userDataViewModel.markAsPlayed(
                                userId = session.userId.id,
                                mediaId = mediaItem.id,
                                accessToken = session.accessToken,
                                played = !isPlayed
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlayed) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.CheckCircle
                        },
                        contentDescription = if (isPlayed) {
                            "Mark as unplayed"
                        } else {
                            "Mark as played"
                        },
                        tint = if (isPlayed) {
                            Color.Green
                        } else {
                            PrimaryText
                        },
                        modifier = Modifier.size(calculateRoundedValue(24).sdp)
                    )
                }

                if (showDownload && canDownload) {
                    val originalRequest = remember(currentQuality, selectedCodec) {
                        DownloadRequest.directDownload(currentQuality, selectedCodec)
                    }
                    val hd1080Request = remember(selectedCodec) {
                        DownloadRequest.transcodedDownload(
                            quality = DownloadQuality.FHD_1080,
                            codec = selectedCodec,
                            maxWidth = 1920,
                            maxHeight = 1080,
                            videoBitrate = 10_000_000
                        )
                    }
                    val hd720Request = remember(selectedCodec) {
                        DownloadRequest.transcodedDownload(
                            quality = DownloadQuality.HD_720,
                            codec = selectedCodec,
                            maxWidth = 1280,
                            maxHeight = 720,
                            videoBitrate = 6_000_000
                        )
                    }

                    Box {
                        IconButton(
                            onClick = {
                                val sourceId = playbackState.playbackOptions.selectedMediaSource?.id
                                when (effectiveDownloadStatus) {
                                    null, DownloadStatus.FAILED -> {
                                        pendingSourceId = sourceId ?: downloadId
                                        if (isSeason || mediaItem.type.equals("Series", ignoreCase = true)) {
                                            pendingScope = null
                                            showScopeDialog = true
                                        } else {
                                            showDownloadMenu = true
                                        }
                                    }

                                    DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
                                        if (isSeason) {
                                            pauseSeasonDownloads(seasonDownloads, serviceManager)
                                        } else {
                                            serviceManager.pauseDownload(downloadId)
                                        }
                                    }

                                    DownloadStatus.PAUSED -> {
                                        if (isSeason) {
                                            resumeSeasonDownloads(seasonDownloads, serviceManager)
                                        } else {
                                            serviceManager.resumeDownload(downloadId)
                                        }
                                    }

                                    DownloadStatus.COMPLETED -> {
                                        if (isSeason) {
                                            cancelSeasonDownloads(seasonDownloads, serviceManager)
                                        } else {
                                            serviceManager.cancelDownload(downloadId)
                                        }
                                    }
                                }
                            }
                        ) {
                            val icon = when (effectiveDownloadStatus) {
                                null -> Icons.Outlined.Download
                                DownloadStatus.QUEUED -> Icons.Filled.Schedule
                                DownloadStatus.DOWNLOADING -> Icons.Filled.Pause
                                DownloadStatus.PAUSED -> Icons.Filled.PlayArrow
                                DownloadStatus.COMPLETED -> Icons.Filled.Delete
                                DownloadStatus.FAILED -> Icons.Outlined.Download
                            }
                            val description = when (effectiveDownloadStatus) {
                                null -> when {
                                    isSeason -> "Download season"
                                    mediaItem.type.equals("Series", ignoreCase = true) -> "Download series"
                                    else -> "Download"
                                }

                                DownloadStatus.QUEUED -> "Queued"
                                DownloadStatus.DOWNLOADING -> if (isSeason) "Pause season downloads" else "Pause"
                                DownloadStatus.PAUSED -> if (isSeason) "Resume season downloads" else "Resume"
                                DownloadStatus.COMPLETED -> if (isSeason) "Delete season downloads" else "Downloaded"
                                DownloadStatus.FAILED -> "Download Failed"
                            }

                            Box(contentAlignment = Alignment.Center) {
                                if (effectiveDownloadStatus in listOf(
                                        DownloadStatus.QUEUED,
                                        DownloadStatus.DOWNLOADING,
                                        DownloadStatus.PAUSED,
                                    )
                                ) {
                                    when {
                                        effectiveDownloadStatus == DownloadStatus.QUEUED -> {
                                            CircularProgressIndicator(modifier = Modifier.matchParentSize(), strokeWidth = 2.dp)
                                        }

                                        effectiveDownloadStatus == DownloadStatus.DOWNLOADING && !isSeason && isTranscodeDownload -> {
                                            CircularProgressIndicator(modifier = Modifier.matchParentSize(), strokeWidth = 2.dp)
                                        }

                                        !isSeason && isStaticDownload -> {
                                            CircularProgressIndicator(
                                                progress = { downloadProgress / 100f },
                                                modifier = Modifier.matchParentSize(),
                                                color = ProgressIndicatorDefaults.circularColor,
                                                strokeWidth = 2.dp,
                                                trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                                                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                            )
                                        }

                                        isSeason -> {
                                            CircularProgressIndicator(modifier = Modifier.matchParentSize(), strokeWidth = 2.dp)
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = description,
                                    tint = PrimaryText,
                                    modifier = Modifier.size(calculateRoundedValue(24).sdp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showDownloadMenu,
                            onDismissRequest = {
                                showDownloadMenu = false
                                pendingScope = null
                                pendingSourceId = null
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            if (isSeason) "Download season" else "Original download",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "${currentQuality.label} • ${selectedCodec.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryText.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                onClick = {
                                    showDownloadMenu = false
                                    val source = pendingSourceId
                                    pendingSourceId = null
                                    val scope = pendingScope ?: DownloadScope.ALL
                                    pendingScope = null
                                    onDownloadClick(mediaItem, originalRequest, source, scope)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            if (isSeason) "Season 1080p transcode" else "1080p transcode",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "1920x1080 • 10 Mbps",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryText.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                onClick = {
                                    showDownloadMenu = false
                                    val source = pendingSourceId
                                    pendingSourceId = null
                                    val scope = pendingScope ?: DownloadScope.ALL
                                    pendingScope = null
                                    onDownloadClick(mediaItem, hd1080Request, source, scope)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            if (isSeason) "Season 720p transcode" else "720p transcode",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "1280x720 • 6 Mbps",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryText.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                onClick = {
                                    showDownloadMenu = false
                                    val source = pendingSourceId
                                    pendingSourceId = null
                                    val scope = pendingScope ?: DownloadScope.ALL
                                    pendingScope = null
                                    onDownloadClick(mediaItem, hd720Request, source, scope)
                                }
                            )
                        }
                    }
                }

                if (showScopeDialog) {
                    DownloadScopeDialog(
                        onSelect = { scope ->
                            pendingScope = scope
                            showScopeDialog = false
                            showDownloadMenu = true
                        },
                        onDismiss = {
                            showScopeDialog = false
                            pendingScope = null
                            pendingSourceId = null
                        }
                    )
                }

                if (shareButton) {
                    IconButton(
                        onClick = {
                            val itemForPlayback = playbackItemForActions ?: return@IconButton
                            val playbackOptions = playbackState.playbackOptions
                            authState.authSession?.let { session ->
                                val url = videoPlaybackViewModel.getExternalPlaybackUrl(
                                    mediaItem = itemForPlayback,
                                    serverUrl = serverUrl,
                                    accessToken = session.accessToken,
                                    mediaSourceId = playbackOptions.selectedMediaSource?.id,
                                    audioStreamIndex = playbackOptions.selectedAudioStream?.index,
                                    subtitleStreamIndex = resolveSubtitleIndexForPlayback(playbackOptions)
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(url.toUri(), "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } ?: Toast.makeText(
                                context,
                                "No active session",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "Open in external player",
                            tint = PrimaryText,
                            modifier = Modifier.size(calculateRoundedValue(24).sdp)
                        )
                    }
                }
            }
        }

        
        if (showMediaInfo) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
            ) {
                Text(
                    text = "Media Info",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                if (playbackState.hasMultipleVersions) {
                    VideoOptionButton(
                        label = "Version",
                        selection = playbackState.currentVersionText,
                        onClick = { videoPlaybackViewModel.showVersionDialog() }
                    )
                }

                VideoOptionButton(
                    label = "Quality",
                    selection = playbackState.currentVideoQualityText,
                    onClick = {
                        if(playbackState.availableVideoQualities.size > 1) {
                            videoPlaybackViewModel.showVideoQualityDialog()
                        }
                    }
                )

                if (playbackState.availableAudioStreams.isNotEmpty()) {
                    VideoOptionButton(
                        label = "Audio",
                        selection = playbackState.currentAudioText,
                        onClick = {
                            if (playbackState.availableAudioStreams.size > 1) {
                                videoPlaybackViewModel.showAudioDialog()
                            }
                        }
                    )
                }

                if (playbackState.availableSubtitleStreams.isNotEmpty()) {
                    VideoOptionButton(
                        label = "Subtitles",
                        selection = playbackState.currentSubtitleText,
                        onClick = { videoPlaybackViewModel.showSubtitleDialog() }
                    )
                }
            }
        }

        if (showVersionDialog) {
            VersionSelectionDialog(
                versions = mediaItem.mediaSources,
                selectedVersion = playbackState.playbackOptions.selectedMediaSource,
                onVersionSelected = {
                    pendingSourceId = it.id
                    showDownloadMenu = true
                    showVersionDialog = false
                },
                onDismiss = { showVersionDialog = false }
            )
        }

        
        if (playbackState.showVersionDialog) {
            VersionSelectionDialog(
                versions = playbackState.availableVersions,
                selectedVersion = playbackState.playbackOptions.selectedMediaSource,
                onVersionSelected = { videoPlaybackViewModel.selectVersion(it) },
                onDismiss = { videoPlaybackViewModel.hideVersionDialog() }
            )
        }

        if (playbackState.showVideoQualityDialog) {
            VideoQualityDialog(
                qualities = playbackState.availableVideoQualities,
                selectedQuality = playbackState.playbackOptions.currentVideoQuality,
                onQualitySelected = { videoPlaybackViewModel.selectVideoQuality(it) },
                onDismiss = { videoPlaybackViewModel.hideVideoQualityDialog() }
            )
        }

        if (playbackState.showAudioDialog) {
            AudioTrackDialog(
                audioStreams = playbackState.availableAudioStreams,
                selectedAudioStream = playbackState.playbackOptions.selectedAudioStream,
                onAudioSelected = { videoPlaybackViewModel.selectAudioStream(it) },
                onDismiss = { videoPlaybackViewModel.hideAudioDialog() }
            )
        }

        if (playbackState.showSubtitleDialog) {
            SubtitleDialog(
                subtitleStreams = playbackState.availableSubtitleStreams,
                selectedSubtitleStream = playbackState.playbackOptions.selectedSubtitleStream,
                onSubtitleSelected = { videoPlaybackViewModel.selectSubtitleStream(it) },
                onDismiss = { videoPlaybackViewModel.hideSubtitleDialog() }
            )
        }
    }
}

private fun computeSeasonDownloadStatus(
    downloads: List<DownloadInfo>,
    totalEpisodes: Int?,
): DownloadStatus? {
    if (downloads.isEmpty()) return null

    val completed = downloads.count { it.status == DownloadStatus.COMPLETED }
    val inProgress = downloads.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    val paused = downloads.any { it.status == DownloadStatus.PAUSED }

    return when {
        totalEpisodes != null && totalEpisodes > 0 && completed >= totalEpisodes -> DownloadStatus.COMPLETED
        inProgress -> DownloadStatus.DOWNLOADING
        paused -> DownloadStatus.PAUSED
        else -> null
    }
}

private fun pauseSeasonDownloads(downloads: List<DownloadInfo>, serviceManager: com.hritwik.avoid.core.ServiceManager) {
    downloads
        .filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
        .mapNotNull { it.resolveDownloadId() }
        .distinct()
        .forEach { id -> serviceManager.pauseDownload(id) }
}

private fun resumeSeasonDownloads(downloads: List<DownloadInfo>, serviceManager: com.hritwik.avoid.core.ServiceManager) {
    downloads
        .filter { it.status == DownloadStatus.PAUSED }
        .mapNotNull { it.resolveDownloadId() }
        .distinct()
        .forEach { id -> serviceManager.resumeDownload(id) }
}

private fun cancelSeasonDownloads(downloads: List<DownloadInfo>, serviceManager: com.hritwik.avoid.core.ServiceManager) {
    downloads
        .mapNotNull { it.resolveDownloadId() }
        .distinct()
        .forEach { id -> serviceManager.cancelDownload(id) }
}

private fun DownloadInfo.resolveDownloadId(): String? {
    return mediaSourceId ?: mediaItem.mediaSources.firstOrNull()?.id ?: mediaItem.id
}

@Composable
private fun DownloadScopeDialog(
    onSelect: (DownloadScope) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectionDialog(
        title = "Download episodes",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)) {
            SelectionItem(
                title = "Download all episodes",
                subtitle = "Includes watched episodes",
                isSelected = true,
                onClick = { onSelect(DownloadScope.ALL) }
            )
            SelectionItem(
                title = "Download unwatched only",
                subtitle = "Skips already watched episodes",
                isSelected = false,
                onClick = { onSelect(DownloadScope.UNWATCHED) }
            )
        }
    }
}

@Composable
private fun VideoOptionButton(
    label: String,
    selection: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = calculateRoundedValue(6).sdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryText,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = selection,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            color = PrimaryText,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(2f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        )

    }
}