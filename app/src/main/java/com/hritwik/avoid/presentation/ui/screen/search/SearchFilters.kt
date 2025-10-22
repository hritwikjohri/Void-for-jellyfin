package com.hritwik.avoid.presentation.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hritwik.avoid.presentation.ui.state.SearchFilters
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
internal fun SearchFilterRow(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
    ) {
        FilterChip(
            selected = filters.includeMovies,
            onClick = { onFiltersChange(filters.copy(includeMovies = !filters.includeMovies)) },
            label = { Text("Movies") }
        )
        FilterChip(
            selected = filters.includeTvShows,
            onClick = { onFiltersChange(filters.copy(includeTvShows = !filters.includeTvShows)) },
            label = { Text("TV Shows") }
        )
        FilterChip(
            selected = filters.includeEpisodes,
            onClick = { onFiltersChange(filters.copy(includeEpisodes = !filters.includeEpisodes)) },
            label = { Text("Episodes") }
        )
    }
}