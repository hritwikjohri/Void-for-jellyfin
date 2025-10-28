package com.hritwik.avoid.presentation.ui.screen.media

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.components.common.ErrorMessage
import com.hritwik.avoid.presentation.ui.components.common.ThemeSongPlayer
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.EpisodeCard
import com.hritwik.avoid.presentation.ui.components.media.DownloadScope
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
    onPlayClick: (PlaybackInfo) -> Unit = {},
    onSimilarItemClick: (String) -> Unit = {},
    onSeasonClick: (String, String) -> Unit = { _, _ -> },
    onEpisodeClick: (MediaItem) -> Unit = {},
    onDownloadClick: (MediaItem, DownloadRequest, String?) -> Unit = { _, _, _ -> },
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

    LaunchedEffect(mediaId, authState.authSession) {
        authState.authSession?.let { session ->
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
                onPlayClick = onPlayClick,
                onSimilarItemClick = onSimilarItemClick,
                onSeasonClick = onSeasonClick,
                playedItems = playedItems,
                onEpisodeClick = onEpisodeClick,
                onDownloadClick = onDownloadClick,
                initialEpisodeId = initialEpisodeId
            )
        }
    }
}

private fun buildMediaAdditionalDetails(mediaItem: MediaItem): List<Pair<String, String>> {
    val details = mutableListOf<Pair<String, String>>()
    val primarySource = mediaItem.getPrimaryMediaSource()

    val runtimeTicks = mediaItem.runTimeTicks ?: primarySource?.runTimeTicks
    runtimeTicks?.takeIf { it > 0 }?.let { ticks ->
        details += "Length" to ticks.formatRuntime()
    }

    primarySource?.bitrate?.takeIf { it > 0 }?.let { bitrate ->
        details += "Bit Rate" to formatBitrate(bitrate)
    }

    primarySource?.defaultVideoStream?.frameRate?.takeIf { it > 0f }?.let { frameRate ->
        details += "Frame Rate" to formatFrameRate(frameRate)
    }

    primarySource?.defaultAudioStream?.let { audioStream ->
        formatAudioChannels(audioStream)?.let { details += "Channels" to it }
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

@Composable
private fun MediaContent(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    state: MediaDetailState,
    serverUrl: String,
    onBackClick: () -> Unit,
    onPlayClick: (PlaybackInfo) -> Unit,
    onSimilarItemClick: (String) -> Unit,
    onSeasonClick: (String, String) -> Unit,
    onEpisodeClick: (MediaItem) -> Unit,
    onDownloadClick: (MediaItem, DownloadRequest, String?) -> Unit,
    initialEpisodeId: String? = null,
    playedItems: Set<String> = emptySet()
) {
    val mediaType = mediaItem.type.lowercase()
    val showDownload = when (mediaType) {
        "series" -> false
        else -> true
    }
    val context = LocalContext.current

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

    val additionalDetails = if (mediaType == "movie" || mediaType == "episode") {
        buildMediaAdditionalDetails(detailSource)
    } else {
        emptyList()
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
                        showMediaInfo = config.showMediaInfo,
                        showDownload = showDownload,
                        playButtonSize = config.playButtonSize,
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
                    Spacer(modifier = Modifier.height(calculateRoundedValue(24).sdp))
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
                            serverUrl = serverUrl
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
}