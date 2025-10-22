package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.theme.PrimaryText


@Composable
fun MediaDetailsSection(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    contentType: MediaContentType = MediaContentType.fromMediaItem(mediaItem),
    additionalDetails: List<Pair<String, String>> = emptyList(),
    episodeCount: Int? = null,
    seasonCount: Int? = null
) {
    Column(modifier = modifier) {
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(calculateRoundedValue(12).sdp))

        
        when (contentType) {
            MediaContentType.MOVIE -> {
                MovieDetails(
                    mediaItem = mediaItem
                )
            }
            MediaContentType.TV_SERIES -> {
                TVSeriesDetails(
                    mediaItem = mediaItem,
                    seasonCount = seasonCount
                )
            }
            MediaContentType.SEASON -> {
                SeasonDetails(
                    mediaItem = mediaItem,
                    episodeCount = episodeCount
                )
            }
            MediaContentType.EPISODE -> {
                EpisodeDetails(
                    mediaItem = mediaItem
                )
            }
        }

        
        additionalDetails.forEach { (label, value) ->
            DetailRow(
                label = label,
                value = value
            )
        }
    }
}


@Composable
private fun MovieDetails(
    mediaItem: MediaItem
) {
    
    if (mediaItem.genres.isNotEmpty()) {
        DetailRow(
            label = "Genres",
            value = mediaItem.genres.joinToString(", ")
        )
    }

    
    mediaItem.year?.let { year ->
        DetailRow(
            label = "Release Year",
            value = year.toString()
        )
    }
}


@Composable
private fun TVSeriesDetails(
    mediaItem: MediaItem,
    seasonCount: Int?
) {
    
    if (mediaItem.genres.isNotEmpty()) {
        DetailRow(
            label = "Genres",
            value = mediaItem.genres.joinToString(", ")
        )
    }

    
    mediaItem.year?.let { year ->
        DetailRow(
            label = "First Aired",
            value = year.toString()
        )
    }

    val seasons = seasonCount ?: mediaItem.childCount
    seasons?.let { count ->
        DetailRow(
            label = "Seasons",
            value = "$count ${if (count == 1) "season" else "seasons"}"
        )
    }
}


@Composable
private fun SeasonDetails(
    mediaItem: MediaItem,
    episodeCount: Int?
) {
    
    val episodes = episodeCount ?: mediaItem.childCount
    episodes?.let { count ->
        DetailRow(
            label = "Episodes",
            value = "$count ${if (count == 1) "episode" else "episodes"}"
        )
    }

    
    mediaItem.year?.let { year ->
        DetailRow(
            label = "Year",
            value = year.toString()
        )
    }
}


@Composable
private fun EpisodeDetails(
    mediaItem: MediaItem
) {
    
    mediaItem.runTimeTicks?.let { ticks ->
        val minutes = (ticks / 600000000).toInt()
        DetailRow(
            label = "Runtime",
            value = "${minutes}m"
        )
    }

    
    mediaItem.communityRating?.let { rating ->
        DetailRow(
            label = "Rating",
            value = "%.1f/10".format(rating)
        )
    }
}


enum class MediaContentType {
    MOVIE,
    TV_SERIES,
    SEASON,
    EPISODE;

    companion object {
        
        fun fromMediaItem(mediaItem: MediaItem): MediaContentType {
            return when (mediaItem.type.lowercase()) {
                "movie" -> MOVIE
                "series" -> TV_SERIES
                "season" -> SEASON
                "episode" -> EPISODE
                else -> MOVIE 
            }
        }
    }
}