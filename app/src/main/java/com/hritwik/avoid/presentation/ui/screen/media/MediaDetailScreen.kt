package com.hritwik.avoid.presentation.ui.screen.media

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.EntryPointAccessors
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.download.DownloadScope
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.components.bottomSheets.VoidModalSheet
import com.hritwik.avoid.presentation.ui.components.common.ErrorMessage
import com.hritwik.avoid.presentation.ui.components.common.ThemeSongControllerEntryPoint
import com.hritwik.avoid.presentation.ui.components.common.ThemeSongPlayer
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.components.dialogs.VoidAlertDialog
import com.hritwik.avoid.presentation.ui.components.media.extractDominantColor
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.EpisodeCard
import com.hritwik.avoid.presentation.ui.components.media.MediaActionButtons
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaDetailsSection
import com.hritwik.avoid.presentation.ui.components.media.MediaHeroSection
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.media.OverviewSection
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.config.MediaConfig
import com.hritwik.avoid.presentation.ui.state.HeroStyle
import com.hritwik.avoid.presentation.ui.state.MediaDetailState
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.media.MediaViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.extensions.formatFileSize
import com.hritwik.avoid.utils.extensions.formatRuntime
import com.hritwik.avoid.utils.extensions.getBackdropUrl
import com.hritwik.avoid.utils.extensions.getPosterUrl
import com.hritwik.avoid.utils.helpers.GestureHelper.swipeBack
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import java.util.Locale

@Composable
fun MediaDetailScreen(
    mediaId: String,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPlayClick: (PlaybackInfo) -> Unit = {},
    onSimilarItemClick: (String) -> Unit = {},
    onSeasonClick: (String, String) -> Unit = { _, _ -> },
    onSeriesClick: (String) -> Unit = {},
    onEpisodeClick: (MediaItem) -> Unit = {},
    onDownloadClick: (MediaItem, DownloadRequest, String?) -> Unit = { _, _, _ -> },
    onPersonClick: (String) -> Unit = {},
    initialEpisodeId: String? = null,
    authViewModel: AuthServerViewModel,
    mediaViewModel: MediaViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val detailState by mediaViewModel.state.collectAsStateWithLifecycle()
    val playedItems by userDataViewModel.playedItems.collectAsStateWithLifecycle()
    var mediaType by remember(mediaId) { mutableStateOf<String?>(null) }

    ThemeSongPlayer(detailState.themeSong)

    LaunchedEffect(playedItems) {
        mediaViewModel.updatePlayedState(playedItems)
    }

    LaunchedEffect(mediaId, authState.authSession, initialEpisodeId) {
        authState.authSession?.let { session ->
            mediaViewModel.setPreferredPlaybackItemId(initialEpisodeId)
            userDataViewModel.loadPlayedItems(session.userId.id, session.accessToken)
            userDataViewModel.loadFavorites(session.userId.id, session.accessToken)
            val detailType = when (mediaType?.lowercase()) {
                "movie" -> MediaViewModel.DetailType.Movie
                "series" -> MediaViewModel.DetailType.Series
                "season" -> MediaViewModel.DetailType.Season
                else -> MediaViewModel.DetailType.Generic
            }

            mediaViewModel.loadDetails(
                mediaId = mediaId,
                userId = session.userId.id,
                accessToken = session.accessToken,
                type = detailType
            )
        } ?: userDataViewModel.reset()
    }

    LaunchedEffect(detailState.mediaItem?.seasonId, detailState.mediaItem?.type, authState.authSession) {
        val mediaItem = detailState.mediaItem ?: return@LaunchedEffect
        if (!mediaItem.type.equals("Episode", ignoreCase = true)) return@LaunchedEffect
        val seasonId = mediaItem.seasonId ?: return@LaunchedEffect
        val session = authState.authSession ?: return@LaunchedEffect
        mediaViewModel.loadSeasonEpisodes(seasonId, session.userId.id, session.accessToken)
    }

    when {
        detailState.isLoading -> {
            AnimatedAmbientBackground {
                LoadingState()
            }
        }

        detailState.error != null -> {
            ErrorMessage(
                error = detailState.error!!,
                onRetry = {
                    authState.authSession?.let { session ->
                        val detailType = when (mediaType?.lowercase()) {
                            "movie" -> MediaViewModel.DetailType.Movie
                            "series" -> MediaViewModel.DetailType.Series
                            "season" -> MediaViewModel.DetailType.Season
                            else -> MediaViewModel.DetailType.Generic
                        }
                        mediaViewModel.loadDetails(
                            mediaId = mediaId,
                            userId = session.userId.id,
                            accessToken = session.accessToken,
                            type = detailType
                        )
                    }
                },
                onDismiss = { mediaViewModel.clearError() }
            )
        }

        detailState.mediaItem != null -> {
            MediaContent(
                mediaItem = detailState.mediaItem!!,
                state = detailState,
                serverUrl = authState.authSession?.server?.url ?: "",
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onPlayClick = onPlayClick,
                onSimilarItemClick = onSimilarItemClick,
                onSeasonClick = onSeasonClick,
                onSeriesClick = onSeriesClick,
                playedItems = playedItems,
                onEpisodeClick = onEpisodeClick,
                onDownloadClick = onDownloadClick,
                onPersonClick = onPersonClick,
                initialEpisodeId = initialEpisodeId
            )
        }
    }
}

private fun buildMediaAdditionalDetails(
    mediaItem: MediaItem,
    selectedMediaSource: MediaSource? = null
): List<Pair<String, String>> {
    val details = mutableListOf<Pair<String, String>>()
    val primarySource = selectedMediaSource ?: mediaItem.getPrimaryMediaSource()

    primarySource?.bitrate?.takeIf { it > 0 }?.let { bitrate ->
        details += "Bit Rate" to formatBitrate(bitrate)
    }

    primarySource?.defaultVideoStream?.let { videoStream ->
        formatVideoCodec(videoStream)?.let { details += "Codec" to it }
    }

    primarySource?.defaultVideoStream?.frameRate?.takeIf { it > 0f }?.let { frameRate ->
        details += "Frame Rate" to formatFrameRate(frameRate)
    }

    primarySource?.defaultAudioStream?.let { audioStream ->
        formatAudioChannels(audioStream)?.let { details += "Audio" to it }
    }

    primarySource?.container?.let { container ->
        if (container.isNotBlank()) {
            details += "Format" to container.uppercase(Locale.US)
        }
    }

    primarySource?.size?.takeIf { it > 0 }?.let { size ->
        details += "Size" to size.formatFileSize()
    }

    return details
}

private fun buildFullFileDetails(
    mediaItem: MediaItem,
    selectedMediaSource: MediaSource? = null
): List<Pair<String, String>> {
    val source = selectedMediaSource ?: mediaItem.getPrimaryMediaSource() ?: return emptyList()
    val details = mutableListOf<Pair<String, String>>()
    val videoStream = source.defaultVideoStream
    val audioStream = source.defaultAudioStream

    details += "Version" to source.displayName
    details += "Source ID" to source.id
    source.path?.takeIf { it.isNotBlank() }?.let { details += "Path" to it }
    source.type?.takeIf { it.isNotBlank() }?.let { details += "Type" to it }
    source.protocol?.takeIf { it.isNotBlank() }?.let { details += "Protocol" to it }
    source.container?.takeIf { it.isNotBlank() }?.let { details += "Container" to it.uppercase(Locale.US) }
    source.size?.takeIf { it > 0 }?.let { details += "Size" to it.formatFileSize() }
    source.bitrate?.takeIf { it > 0 }?.let { details += "Bit Rate" to formatBitrate(it) }
    source.runTimeTicks?.takeIf { it > 0 }?.let { details += "Length" to it.formatRuntime() }
    details += "Direct Play" to formatBooleanDetail(source.supportsDirectPlay)
    details += "Direct Stream" to formatBooleanDetail(source.supportsDirectStream)
    details += "Transcoding" to formatBooleanDetail(source.supportsTranscoding)
    details += "Remote" to formatBooleanDetail(source.isRemote)

    videoStream?.let { stream ->
        stream.codec?.takeIf { it.isNotBlank() }?.let { details += "Video Codec" to it.uppercase(Locale.US) }
        stream.resolution?.let { details += "Video Resolution" to it }
        stream.frameRate?.takeIf { it > 0f }?.let { details += "Video Frame Rate" to formatFrameRate(it) }
        stream.dynamicRangeLabel.let { details += "Video Range" to it }
        stream.bitRate?.takeIf { it > 0 }?.let { details += "Video Bit Rate" to formatBitrate(it) }
        stream.aspectRatio?.takeIf { it.isNotBlank() }?.let { details += "Aspect Ratio" to it }
    }

    audioStream?.let { stream ->
        stream.codec?.takeIf { it.isNotBlank() }?.let { details += "Audio Codec" to it.uppercase(Locale.US) }
        formatAudioChannels(stream)?.let { details += "Audio Channels" to it }
        stream.sampleRate?.takeIf { it > 0 }?.let { details += "Sample Rate" to "$it Hz" }
        stream.bitRate?.takeIf { it > 0 }?.let { details += "Audio Bit Rate" to formatBitrate(it) }
        stream.displayLanguage?.takeIf { it.isNotBlank() }?.let { details += "Audio Language" to it }
    }

    return details
}

private fun filterEpisodesForScope(
    episodes: List<MediaItem>,
    scope: DownloadScope,
    playedItems: Set<String>,
): List<MediaItem> {
    val sortedEpisodes = episodes.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
    return when (scope) {
        DownloadScope.ALL -> sortedEpisodes
        DownloadScope.UNWATCHED -> sortedEpisodes.filter { episode ->
            val isPlayed = playedItems.contains(episode.id) || episode.userData?.played == true
            !isPlayed
        }
    }
}

private fun formatBitrate(bitrate: Int): String {
    val units = arrayOf("bps", "Kbps", "Mbps", "Gbps")
    var value = bitrate.toDouble()
    var unitIndex = 0
    while (value >= 1000 && unitIndex < units.lastIndex) {
        value /= 1000
        unitIndex++
    }

    val formatted = if (unitIndex == 0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }

    return "$formatted ${units[unitIndex]}"
}

private fun formatFrameRate(frameRate: Float): String {
    val formatted = String.format(Locale.US, "%.3f", frameRate).trimEnd('0').trimEnd('.')
    return "$formatted fps"
}

private fun formatAudioChannels(audioStream: MediaStream): String? {
    val codec = audioStream.codec?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
    val layout = audioStream.channelLayout?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
    val channelCount = audioStream.channels?.takeIf { it > 0 }

    val channelDescription = when {
        layout != null -> layout
        channelCount != null -> if (channelCount == 1) "1 channel" else "$channelCount channels"
        else -> null
    }

    return when {
        channelDescription != null && codec != null -> "$channelDescription ($codec)"
        channelDescription != null -> channelDescription
        else -> codec
    }
}

private fun formatVideoCodec(videoStream: MediaStream): String? {
    val codec = videoStream.codec?.takeIf { it.isNotBlank() } ?: return null
    val codecLabel = when (codec.lowercase(Locale.US)) {
        "hevc" -> "HEVC"
        "h264" -> "h264"
        "av1" -> "AV1"
        else -> codec.uppercase(Locale.US)
    }
    val profileLabel = videoStream.profile
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        ?.replace(Regex("(?<=\\D)(?=\\d)"), " ")
        ?.trim()

    return if (profileLabel.isNullOrBlank()) codecLabel else "$codecLabel $profileLabel"
}

private fun formatBooleanDetail(value: Boolean): String = if (value) "Yes" else "No"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaContent(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    state: MediaDetailState,
    serverUrl: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onPlayClick: (PlaybackInfo) -> Unit,
    onSimilarItemClick: (String) -> Unit,
    onSeasonClick: (String, String) -> Unit,
    onSeriesClick: (String) -> Unit,
    onEpisodeClick: (MediaItem) -> Unit,
    onDownloadClick: (MediaItem, DownloadRequest, String?) -> Unit,
    onPersonClick: (String) -> Unit,
    initialEpisodeId: String? = null,
    playedItems: Set<String> = emptySet()
) {
    val videoPlaybackViewModel: VideoPlaybackViewModel = hiltViewModel()
    val playbackState by videoPlaybackViewModel.state.collectAsStateWithLifecycle()

    val mediaType = mediaItem.type.lowercase()
    val showDownload = when (mediaType) {
        "series" -> false
        else -> true
    }
    var showEpisodePicker by remember(mediaItem.id) { mutableStateOf(false) }
    var showSeasonPicker by remember(mediaItem.seriesId) { mutableStateOf(false) }
    var dynamicAccentColor by remember(mediaItem.id) { mutableStateOf(Color(0xFF1976D2)) }
    val context = LocalContext.current
    val themeSongController = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ThemeSongControllerEntryPoint::class.java
        ).themeSongController()
    }
    val isThemeSongPlaying by themeSongController.isPlaying.collectAsStateWithLifecycle()
    val seasonSwitcher = if (mediaType == "season") {
        { showSeasonPicker = true }
    } else {
        null
    }

    val config = when (mediaType) {
        "movie" -> MediaConfig(
            heroStyle = HeroStyle.MOVIE_SERIES,
            backgroundImageUrl = mediaItem.getBackdropUrl(serverUrl),
            heroHeight = 340,
            showShareButton = true,
            showMediaInfo = true,
            similarSectionTitle = "More Movies Like This",
            playButtonSize = 72,
            overviewTitle = "Overview"
        )
        "series" -> MediaConfig(
            heroStyle = HeroStyle.MOVIE_SERIES,
            backgroundImageUrl = mediaItem.getBackdropUrl(serverUrl),
            heroHeight = 340,
            showShareButton = false,
            showMediaInfo = false,
            similarSectionTitle = "More shows Like This",
            playButtonSize = 72,
            overviewTitle = "Overview"
        )
        "season" -> MediaConfig(
            heroStyle = HeroStyle.SEASON,
            backgroundImageUrl = mediaItem.getPosterUrl(serverUrl),
            heroHeight = 340,
            showShareButton = false,
            showMediaInfo = false,
            similarSectionTitle = "",
            playButtonSize = 72,
            overviewTitle = "Overview"
        )
        "episode" -> MediaConfig(
            heroStyle = HeroStyle.EPISODE,
            backgroundImageUrl = mediaItem.getPosterUrl(serverUrl),
            heroHeight = 340,
            showShareButton = true,
            showMediaInfo = true,
            similarSectionTitle = "",
            playButtonSize = 72,
            overviewTitle = "Overview"
        )
        else -> MediaConfig(
            heroStyle = HeroStyle.MOVIE_SERIES,
            backgroundImageUrl = mediaItem.getBackdropUrl(serverUrl),
            heroHeight = 400,
            showShareButton = true,
            showMediaInfo = true,
            similarSectionTitle = "",
            playButtonSize = 72,
            overviewTitle = "Overview"
        )
    }

    val detailSource = if (mediaItem.mediaSources.isNotEmpty()) {
        mediaItem
    } else {
        val playbackItem = state.playbackItem
        if (playbackItem != null && playbackItem.id == mediaItem.id && playbackItem.mediaSources.isNotEmpty()) {
            playbackItem
        } else {
            mediaItem
        }
    }

    val selectedSourceForDetails = playbackState.playbackOptions.selectedMediaSource
        ?.takeIf { source ->
            playbackState.mediaItem?.id == detailSource.id &&
                detailSource.mediaSources.any { it.id == source.id }
        }

    val additionalDetails = if (mediaType == "movie" || mediaType == "episode") {
        buildMediaAdditionalDetails(detailSource, selectedSourceForDetails)
    } else {
        emptyList()
    }
    val fullFileDetails = if (mediaType == "movie" || mediaType == "episode") {
        buildFullFileDetails(detailSource, selectedSourceForDetails)
    } else {
        emptyList()
    }
    var showFullFileDetails by remember(detailSource.id, selectedSourceForDetails?.id) {
        mutableStateOf(false)
    }
    val activeSourceForOptions = selectedSourceForDetails ?: detailSource.getPrimaryMediaSource()
    val hasSelectableMediaOptions = detailSource.mediaSources.size > 1 ||
        (activeSourceForOptions?.audioStreams?.size ?: 0) > 1 ||
        (activeSourceForOptions?.subtitleStreams?.isNotEmpty() == true)
    val moreDetailsColor = if (hasSelectableMediaOptions) dynamicAccentColor else PrimaryText

    LaunchedEffect(mediaItem.id, serverUrl) {
        val imageUrl = mediaItem.getPosterUrl(serverUrl)
        val color = extractDominantColor(imageUrl)
        if (color != null) {
            dynamicAccentColor = color
        }
    }

    var highlightedEpisodeId by remember(mediaItem.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(initialEpisodeId, mediaItem.id) {
        highlightedEpisodeId = initialEpisodeId?.takeIf { it.isNotBlank() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .swipeBack(onBackClick)
    ) {
        AnimatedAmbientBackground(
            imageUrl = config.backgroundImageUrl
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = calculateRoundedValue(80).sdp)
            ) {
                
                item {
                    MediaHeroSection(
                        mediaItem = mediaItem,
                        serverUrl = serverUrl,
                        onBackClick = onBackClick,
                        onSeasonClick = onSeasonClick,
                        onEpisodeTitleClick = {
                            if (mediaType == "episode") {
                                showEpisodePicker = true
                            }
                        },
                        onSeasonSwitcherClick = seasonSwitcher,
                        onHomeClick = onHomeClick,
                        isThemeSongPlaying = isThemeSongPlaying,
                        showThemeSongToggle = state.themeSong != null,
                        onThemeSongToggle = { themeSongController.togglePlayback() },
                        heroStyle = config.heroStyle,
                        height = config.heroHeight
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))
                }

                
                item {
                    MediaActionButtons(
                        mediaItem = mediaItem,
                        serverUrl = serverUrl,
                        playbackItem = state.playbackItem,
                        shareButton = config.showShareButton,
                        dominantColor = dynamicAccentColor,
                        showMediaInfo = config.showMediaInfo,
                        showDownload = showDownload,
                        playButtonSize = config.playButtonSize,
                        episodes = state.episodes,
                        onPlayClick = onPlayClick,
                        onDownloadClick = { item, request, mediaSourceId, scope ->
                            when {
                                item.type == "Season" && !state.episodes.isNullOrEmpty() -> {
                                    val episodesToDownload = filterEpisodesForScope(
                                        state.episodes,
                                        scope,
                                        playedItems,
                                    )
                                    if (episodesToDownload.isEmpty()) {
                                        if (scope == DownloadScope.UNWATCHED) {
                                            Toast.makeText(
                                                context,
                                                "All episodes are already watched.",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        } else {
                                            onDownloadClick(item, request, mediaSourceId)
                                        }
                                    } else {
                                        episodesToDownload.forEach { episode ->
                                            onDownloadClick(
                                                episode,
                                                request,
                                                episode.mediaSources.firstOrNull()?.id,
                                            )
                                        }
                                    }
                                }

                                item.type == "Series" && !state.seasons.isNullOrEmpty() && state.episodesBySeasonId.isNotEmpty() -> {
                                    val sortedSeasons = state.seasons.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                                    var started = false
                                    sortedSeasons.forEach { season ->
                                        val episodes = state.episodesBySeasonId[season.id] ?: emptyList()
                                        val episodesToDownload = filterEpisodesForScope(
                                            episodes,
                                            scope,
                                            playedItems,
                                        )
                                        episodesToDownload.forEach { episode ->
                                            onDownloadClick(
                                                episode,
                                                request,
                                                episode.mediaSources.firstOrNull()?.id,
                                            )
                                            started = true
                                        }
                                    }
                                    if (!started) {
                                        if (scope == DownloadScope.UNWATCHED) {
                                            Toast.makeText(
                                                context,
                                                "No unwatched episodes available to download.",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                }

                                else -> {
                                    onDownloadClick(item, request, mediaSourceId)
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                    )
                }

                item {
                    val spacerBeforeLowerSections = if (state.specialFeatures.isEmpty() && mediaItem.people.isNotEmpty()) {
                        calculateRoundedValue(8).sdp
                    } else {
                        calculateRoundedValue(24).sdp
                    }
                    Spacer(modifier = Modifier.height(spacerBeforeLowerSections))
                }

                if (!mediaItem.overview.isNullOrBlank()) {
                    item {
                        OverviewSection(
                            overview = mediaItem.overview,
                            tagline = if (mediaType == "movie") mediaItem.getPrimaryTagline() ?: "" else "",
                            title = config.overviewTitle,
                            modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(calculateRoundedValue(24).sdp))
                    }
                }

                
                item {
                    val episodeCount = if (mediaType == "season") {
                        state.episodes?.size
                    } else null

                    MediaDetailsSection(
                        mediaItem = mediaItem,
                        additionalDetails = additionalDetails,
                        onMoreDetailsClick = if (fullFileDetails.isNotEmpty()) {
                            { showFullFileDetails = true }
                        } else {
                            null
                        },
                        moreDetailsColor = moreDetailsColor,
                        episodeCount = episodeCount,
                        modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                    )
                }

                
                when (mediaType) {
                    "series" -> {
                        val seasons = state.seasons ?: emptyList()
                        if (seasons.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(calculateRoundedValue(32).sdp))
                            }

                            item {
                                Text(
                                    text = "Seasons",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText,
                                    modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
                            }

                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(16).sdp)
                                ) {
                                    val sortedSeasons = seasons.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                                    itemsIndexed(sortedSeasons) { _, season ->
                                        MediaItemCard(
                                            mediaItem = season,
                                            serverUrl = serverUrl,
                                            onClick = { onSeasonClick(it.id, it.name) }
                                        )
                                    }
                                }
                            }
                        }
                }
                    "season" -> {
                        val episodes = state.episodes ?: emptyList()
                        if (episodes.isNotEmpty()) {
                            val sortedEpisodes = episodes.sortedBy { it.indexNumber ?: Int.MAX_VALUE }

                            item {
                                Spacer(modifier = Modifier.height(calculateRoundedValue(32).sdp))
                            }

                            item {
                                Text(
                                    text = "Episodes",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = PrimaryText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
                            }

                            itemsIndexed(sortedEpisodes) { _, episode ->
                                episode.indexNumber?.let {
                                    EpisodeCard(
                                        episode = episode,
                                        serverUrl = serverUrl,
                                        episodeNumber = it,
                                        isWatched = playedItems.contains(episode.id),
                                        highlighted = highlightedEpisodeId == episode.id,
                                        onHighlightFinished = {
                                            if (highlightedEpisodeId == episode.id) {
                                                highlightedEpisodeId = null
                                            }
                                        },
                                        onClick = onEpisodeClick
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(calculateRoundedValue(24).sdp))
                }

                
                if (state.specialFeatures.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Special Features"
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                            ) {
                                items(state.specialFeatures) { mediaItem ->
                                    MediaItemCard(
                                        mediaItem = mediaItem,
                                        serverUrl = serverUrl,
                                        cardType = MediaCardType.THUMBNAIL,
                                        showProgress = true,
                                        showTitle = true,
                                        onClick = { item ->
                                            val sourceId = item.mediaSources.firstOrNull()?.id
                                            onPlayClick(
                                                PlaybackInfo(
                                                    mediaItem = item,
                                                    mediaSourceId = sourceId
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                
                if (mediaItem.people.isNotEmpty()) {
                    item {
                        PeopleSection(
                            title = "Cast & Crew",
                            people = mediaItem.people,
                            serverUrl = serverUrl,
                            onPersonClick = { person ->
                                onPersonClick(person.id)
                            }
                        )
                    }
                }

                
                if (state.similarItems.isNotEmpty() && mediaType != "season" && mediaType != "episode") {
                    item {
                        SimilarMediaSection(
                            title = config.similarSectionTitle,
                            items = state.similarItems,
                            serverUrl = serverUrl,
                            onItemClick = onSimilarItemClick,
                            showProgress = false
                        )
                    }
                }
            }
        }
    }

    if (showSeasonPicker) {
        val seasons = state.seasons.orEmpty().sortedBy { it.indexNumber ?: Int.MAX_VALUE }
        VoidModalSheet(
            onDismissRequest = { showSeasonPicker = false },
            imageUrl = config.backgroundImageUrl
        ) {
            val seriesName = mediaItem.seriesName?.takeIf { it.isNotBlank() }
            val seriesId = mediaItem.seriesId
            val seriesTitleModifier = if (seriesId != null && seriesName != null) {
                Modifier.clickable {
                    showSeasonPicker = false
                    onSeriesClick(seriesId)
                }
            } else {
                Modifier
            }
            Text(
                text = seriesName ?: "Select season",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = seriesTitleModifier
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
            ) {
                if (seasons.isEmpty()) {
                    item {
                        Text(
                            text = "No seasons available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryText.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    items(seasons) { season ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val seasonLabel = season.name
                                    onSeasonClick(season.id, seasonLabel)
                                }
                                .padding(vertical = calculateRoundedValue(6).sdp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = season.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (season.id == mediaItem.id) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Current season",
                                    tint = PrimaryText,
                                    modifier = Modifier
                                        .padding(start = calculateRoundedValue(8).sdp)
                                        .size(calculateRoundedValue(18).sdp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEpisodePicker) {
        val seasonLabel = mediaItem.seasonName?.takeIf { it.isNotBlank() }
            ?: mediaItem.parentIndexNumber?.let { "Season $it" }
        val seasonId = mediaItem.seasonId
        val episodes = state.episodes.orEmpty()
            .sortedBy { it.indexNumber ?: Int.MAX_VALUE }
        VoidModalSheet(
            onDismissRequest = { showEpisodePicker = false },
            imageUrl = config.backgroundImageUrl
        ) {
            val seriesName = mediaItem.seriesName?.takeIf { it.isNotBlank() }
            val seriesId = mediaItem.seriesId
            val seriesTitleModifier = if (seriesId != null && seriesName != null) {
                Modifier.clickable {
                    showEpisodePicker = false
                    onSeriesClick(seriesId)
                }
            } else {
                Modifier
            }
            Text(
                text = seriesName ?: mediaItem.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = seriesTitleModifier
            )
            if (seasonId != null && seasonLabel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showEpisodePicker = false
                            onSeasonClick(seasonId, seasonLabel)
                        }
                        .padding(vertical = calculateRoundedValue(6).sdp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Go to $seasonLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
            ) {
                if (episodes.isEmpty()) {
                    item {
                        Text(
                            text = "No episodes available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryText.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    items(episodes) { episode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showEpisodePicker = false
                                    onEpisodeClick(episode)
                                }
                                .padding(vertical = calculateRoundedValue(6).sdp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = buildString {
                                    episode.indexNumber?.let { append("E$it • ") }
                                    append(episode.name)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (episode.id == mediaItem.id) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected episode",
                                    tint = PrimaryText,
                                    modifier = Modifier
                                        .padding(start = calculateRoundedValue(8).sdp)
                                        .size(calculateRoundedValue(18).sdp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    VoidAlertDialog(
        visible = showFullFileDetails,
        onDismissRequest = { showFullFileDetails = false },
        title = "File Details",
        borderColor = moreDetailsColor,
        actionColor = moreDetailsColor,
        dismissText = "Close",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = calculateRoundedValue(360).sdp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(10).sdp)
            ) {
                fullFileDetails.forEach { (label, value) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryText.copy(alpha = 0.8f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText
                    )
                }
            }
        }
    )
}
