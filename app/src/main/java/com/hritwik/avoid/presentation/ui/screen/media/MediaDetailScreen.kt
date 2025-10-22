package com.hritwik.avoid.presentation.ui.screen.media

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
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.PlaybackInfo
import com.hritwik.avoid.presentation.ui.components.common.ErrorMessage
import com.hritwik.avoid.presentation.ui.components.common.ThemeSongPlayer
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
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
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.extensions.getBackdropUrl
import com.hritwik.avoid.utils.extensions.getPosterUrl
import com.hritwik.avoid.utils.helpers.GestureHelper.swipeBack
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

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
        "season" -> false
        else -> true
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
                        onDownloadClick = { item, request, mediaSourceId ->
                            if (item.type == "Season" && state.episodes != null) {
                                val sortedEpisodes = state.episodes.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                                sortedEpisodes.forEach { episode ->
                                    onDownloadClick(episode, request, mediaSourceId)
                                }
                            } else {
                                onDownloadClick(item, request, mediaSourceId)
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