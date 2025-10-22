package com.hritwik.avoid.presentation.ui.components.jellyseer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.presentation.ui.components.jellyseer.JellyseerAvailabilityBadge
import com.hritwik.avoid.presentation.ui.components.jellyseer.JellyseerRequestStatusBadge
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JellyseerSearchResultCard(
    modifier: Modifier = Modifier,
    result: JellyseerSearchResult,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(calculateRoundedValue(12).sdp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(calculateRoundedValue(12).sdp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
        ) {
            NetworkImage(
                data = result.posterPath?.let { TMDB_IMAGE_BASE + it },
                contentDescription = result.title,
                modifier = Modifier.size(calculateRoundedValue(60).sdp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(calculateRoundedValue(4).sdp))

                val subtitle = buildString {
                    append(if (result.mediaType == JellyseerMediaType.MOVIE) "Movie" else "TV Series")
                    result.releaseDate?.takeIf { it.isNotBlank() }?.let {
                        append(" • ")
                        append(it.take(4))
                    }
                    result.voteAverage?.let {
                        append(" • ⭐ ")
                        append(String.format("%.1f", it))
                    }
                }

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                result.mediaInfo?.let { info ->
                    Spacer(modifier = Modifier.height(calculateRoundedValue(6).sdp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp),
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
                    ) {
                        JellyseerAvailabilityBadge(status = info.status)

                        info.requests
                            .filter { it.status.shouldDisplayBadge }
                            .forEach { request ->
                                JellyseerRequestStatusBadge(
                                    status = request.status,
                                    is4k = request.is4k
                                )
                            }
                    }
                }
            }
        }
    }
}

