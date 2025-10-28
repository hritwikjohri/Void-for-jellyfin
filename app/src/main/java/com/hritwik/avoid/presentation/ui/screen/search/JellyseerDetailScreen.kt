package com.hritwik.avoid.presentation.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.jellyseer.JellyseerAvailabilityStatus
import com.hritwik.avoid.domain.model.jellyseer.JellyseerCastMember
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaDetail
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerVideoQuality
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.components.jellyseer.JellyseerAvailabilityBadge
import com.hritwik.avoid.presentation.ui.components.jellyseer.JellyseerRequestStatusBadge
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.DetailRow
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.media.OverviewSection
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.state.JellyseerDetailUiState
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.search.JellyseerDetailViewModel
import com.hritwik.avoid.utils.constants.AppConstants.TMDB_BACKDROP_BASE
import com.hritwik.avoid.utils.constants.AppConstants.TMDB_POSTER_BASE
import com.hritwik.avoid.utils.constants.AppConstants.TMDB_PROFILE_BASE
import com.hritwik.avoid.utils.helpers.GestureHelper.swipeBack
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun JellyseerDetailScreen(
    mediaId: Long,
    mediaType: JellyseerMediaType,
    onBackClick: () -> Unit,
    viewModel: JellyseerDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(mediaId, mediaType) {
        viewModel.loadDetails(mediaId, mediaType)
    }

    AnimatedAmbientBackground(imageUrl = state.detail?.backdropPath?.let { TMDB_BACKDROP_BASE + it }) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.isLoading && state.detail == null -> {
                    LoadingState()
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(calculateRoundedValue(24).sdp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "Unable to load details.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
                        Button(onClick = { viewModel.loadDetails(mediaId, mediaType) }) {
                            Text("Retry")
                        }
                    }
                }

                state.detail != null -> {
                    JellyseerDetailContent(
                        state = state,
                        onBackClick = onBackClick,
                        onRequestClick = { viewModel.requestMedia(mediaId, mediaType) },
                        onQualitySelected = viewModel::selectVideoQuality,
                        onExactTitleSelected = viewModel::selectExactTitle
                    )
                }
            }
        }
    }
}

@Composable
private fun JellyseerDetailContent(
    state: JellyseerDetailUiState,
    onBackClick: () -> Unit,
    onRequestClick: () -> Unit,
    onQualitySelected: (JellyseerVideoQuality) -> Unit,
    onExactTitleSelected: (JellyseerSearchResult) -> Unit
) {
    val detail = state.detail ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeBack(onBackClick)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = calculateRoundedValue(56).sdp),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(24).sdp)
        ) {
            item {
                JellyseerHeroSection(
                    detail = detail,
                    onBackClick = onBackClick
                )
            }

            item {
                JellyseerRequestSection(
                    state = state,
                    detail = detail,
                    onRequestClick = onRequestClick,
                    onQualitySelected = onQualitySelected,
                    onExactTitleSelected = onExactTitleSelected
                )
            }

            item {
                JellyseerMetadataSection(detail = detail)
            }

            if (!detail.overview.isNullOrBlank() || !detail.tagline.isNullOrBlank()) {
                item {
                    OverviewSection(
                        overview = detail.overview.orEmpty(),
                        tagline = detail.tagline,
                        title = "Overview",
                        modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                    )
                }
            }

            if (detail.genres.isNotEmpty()) {
                item {
                    JellyseerGenreSection(genres = detail.genres)
                }
            }

            if (detail.cast.isNotEmpty()) {
                item {
                    JellyseerCastSection(cast = detail.cast)
                }
            }
        }
    }
}

@Composable
private fun JellyseerHeroSection(
    detail: JellyseerMediaDetail,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(calculateRoundedValue(320).sdp)
    ) {
        NetworkImage(
            data = detail.backdropPath?.let { TMDB_BACKDROP_BASE + it },
            contentDescription = detail.title,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(calculateRoundedValue(8).sdp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(calculateRoundedValue(16).sdp),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
        ) {
            NetworkImage(
                data = detail.posterPath?.let { TMDB_POSTER_BASE + it },
                contentDescription = detail.title,
                modifier = Modifier
                    .height(calculateRoundedValue(180).sdp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(calculateRoundedValue(16).sdp))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
            ) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                detail.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val metadata = buildString {
                    append(if (detail.mediaType == JellyseerMediaType.MOVIE) "Movie" else "TV Series")
                    detail.releaseDate?.takeIf { it.isNotBlank() }?.let {
                        append(" • ")
                        append(it.take(4))
                    }
                    detail.runtimeMinutes?.let {
                        append(" • ")
                        append(formatRuntime(it))
                    }
                    detail.voteAverage?.let {
                        append(" • ⭐ ")
                        append(String.format("%.1f", it))
                    }
                }

                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JellyseerRequestSection(
    state: JellyseerDetailUiState,
    detail: JellyseerMediaDetail,
    onRequestClick: () -> Unit,
    onQualitySelected: (JellyseerVideoQuality) -> Unit,
    onExactTitleSelected: (JellyseerSearchResult) -> Unit
) {
    val availability = detail.availability
    val hasRequestableQuality = detail.isRequestable
    val buttonEnabled = hasRequestableQuality && !state.isRequesting
    val buttonLabel = when {
        state.isRequesting -> "Submitting..."
        detail.mediaInfo?.hasPendingRequest == true -> "Request pending"
        !hasRequestableQuality && availability.isAvailable -> "Available"
        !hasRequestableQuality -> "Unavailable"
        else -> "Request ${state.selectedVideoQuality.displayName}"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = calculateRoundedValue(16).sdp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
            ) {
                Text(
                    text = "Jellyseer request",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                ) {
                    JellyseerAvailabilityBadge(status = availability)

                    detail.mediaInfo?.requests
                        ?.filter { it.status.shouldDisplayBadge }
                        ?.forEach { request ->
                            JellyseerRequestStatusBadge(
                                status = request.status,
                                is4k = request.is4k
                            )
                        }
                }
            }

            Button(
                onClick = onRequestClick,
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isRequesting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(calculateRoundedValue(18).sdp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(buttonLabel)
                }
            }

            if (detail.isRequestable) {
                VideoQualitySelector(state = state, onQualitySelected = onQualitySelected)
            }

            if (state.requireExactTitleSelection && state.exactTitleOptions.isNotEmpty()) {
                ExactTitleSelector(
                    state = state,
                    onExactTitleSelected = onExactTitleSelected,
                    selectedId = detail.id
                )
            }

            val statusText = when (availability) {
                JellyseerAvailabilityStatus.AVAILABLE -> "This title is available on the server."
                JellyseerAvailabilityStatus.PARTIALLY_AVAILABLE ->
                    "This title is partially available. Request another quality if needed."
                JellyseerAvailabilityStatus.PENDING -> "A request is pending approval."
                JellyseerAvailabilityStatus.PROCESSING -> "The request is being processed."
                JellyseerAvailabilityStatus.BLACKLISTED -> "This title is blacklisted."
                JellyseerAvailabilityStatus.DELETED -> "This title has been removed."
                JellyseerAvailabilityStatus.UNKNOWN -> "Availability unknown."
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.requestMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            state.requestError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoQualitySelector(
    state: JellyseerDetailUiState,
    onQualitySelected: (JellyseerVideoQuality) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
    ) {
        Text(
            text = "Choose video quality",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
        ) {
            state.availableVideoQualities.forEach { quality ->
                val selected = state.selectedVideoQuality == quality
                val enabled = !state.disabledVideoQualities.contains(quality)
                AssistChip(
                    onClick = { if (enabled) onQualitySelected(quality) },
                    label = { Text(quality.displayName) },
                    enabled = enabled,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        labelColor = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }
        }

        if (state.disabledVideoQualities.isNotEmpty()) {
            Text(
                text = "Some qualities are unavailable because they are already requested.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExactTitleSelector(
    state: JellyseerDetailUiState,
    onExactTitleSelected: (JellyseerSearchResult) -> Unit,
    selectedId: Long
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
    ) {
        Text(
            text = "Select the exact title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
        ) {
            items(state.exactTitleOptions, key = { it.id }) { option ->
                val isSelected = option.id == selectedId
                JellyseerExactTitleItem(
                    option = option,
                    isSelected = isSelected,
                    onClick = { onExactTitleSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun JellyseerExactTitleItem(
    option: JellyseerSearchResult,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
    ) {
        Box {
            MediaItemCard(
                mediaItem = option.toMediaItemCardModel(),
                showProgress = false,
                customImageUrl = option.posterPath?.let { TMDB_POSTER_BASE + it },
                onClick = { onClick() }
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            width = calculateRoundedValue(2).sdp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
                        )
                )
            }
        }
        Text(
            text = option.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        option.releaseDate?.takeIf { it.isNotBlank() }?.let { release ->
            Text(
                text = release.take(4),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun JellyseerSearchResult.toMediaItemCardModel(): MediaItem {
    return MediaItem(
        id = "jellyseer-$id",
        name = title,
        title = title,
        type = if (mediaType == JellyseerMediaType.MOVIE) "Movie" else "Series",
        overview = overview,
        year = releaseDate?.take(4)?.toIntOrNull(),
        communityRating = voteAverage,
        runTimeTicks = null,
        primaryImageTag = null,
        thumbImageTag = null,
        logoImageTag = null,
        backdropImageTags = emptyList(),
        genres = emptyList(),
        isFolder = false,
        childCount = null,
        userData = null
    )
}

@Composable
private fun JellyseerMetadataSection(detail: JellyseerMediaDetail) {
    val metadata = mutableListOf<Pair<String, String>>()

    metadata += "Type" to if (detail.mediaType == JellyseerMediaType.MOVIE) "Movie" else "TV Series"
    detail.releaseDate?.takeIf { it.isNotBlank() }?.let { release ->
        metadata += "Release" to release
    }
    detail.runtimeMinutes?.takeIf { detail.mediaType == JellyseerMediaType.MOVIE }?.let { runtime ->
        metadata += "Runtime" to formatRuntime(runtime)
    }
    detail.episodeRunTime?.takeIf { detail.mediaType == JellyseerMediaType.TV }?.let { runtime ->
        metadata += "Episode length" to formatRuntime(runtime)
    }
    detail.numberOfSeasons?.takeIf { detail.mediaType == JellyseerMediaType.TV }?.let { seasons ->
        metadata += "Seasons" to seasons.toString()
    }
    detail.numberOfEpisodes?.takeIf { detail.mediaType == JellyseerMediaType.TV }?.let { episodes ->
        metadata += "Episodes" to episodes.toString()
    }
    detail.voteAverage?.let { rating ->
        metadata += "Rating" to String.format("%.1f / 10", rating)
    }

    if (metadata.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = calculateRoundedValue(16).sdp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            metadata.forEach { (label, value) ->
                DetailRow(label = label, value = value)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JellyseerGenreSection(genres: List<String>) {
    if (genres.isEmpty()) return

    SectionHeader(
        modifier = Modifier.padding(horizontal = calculateRoundedValue(6).sdp),
        title = "Genres"
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = calculateRoundedValue(16).sdp),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
        ) {
            genres.forEach { genre ->
                AssistChip(
                    onClick = {},
                    label = { Text(genre) },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        labelColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        disabledLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun JellyseerCastSection(cast: List<JellyseerCastMember>) {
    if (cast.isEmpty()) return

    SectionHeader(
        modifier = Modifier.padding(horizontal = calculateRoundedValue(6).sdp),
        title = "Cast",
        subtitle = "Top billed"
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
            contentPadding = PaddingValues(horizontal = calculateRoundedValue(16).sdp)
        ) {
            itemsIndexed(cast.take(12)) { _, castMember ->
                JellyseerCastCard(castMember)
            }
        }
    }
}

@Composable
private fun JellyseerCastCard(member: JellyseerCastMember) {
    Column(
        modifier = Modifier.width(calculateRoundedValue(96).sdp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
    ) {
        NetworkImage(
            data = member.profilePath?.let { TMDB_PROFILE_BASE + it },
            contentDescription = member.name,
            modifier = Modifier
                .size(calculateRoundedValue(84).sdp)
                .clip(RoundedCornerShape(calculateRoundedValue(16).sdp))
        )

        Text(
            text = member.name ?: "Unknown",
            style = MaterialTheme.typography.bodySmall,
            color = PrimaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        member.character?.takeIf { it.isNotBlank() }?.let { role ->
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatRuntime(minutes: Int): String {
    if (minutes <= 0) return "${minutes}m"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        if (remainingMinutes > 0) {
            "${hours}h ${remainingMinutes}m"
        } else {
            "${hours}h"
        }
    } else {
        "${remainingMinutes}m"
    }
}
