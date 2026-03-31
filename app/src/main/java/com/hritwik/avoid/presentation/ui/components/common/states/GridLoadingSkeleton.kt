package com.hritwik.avoid.presentation.ui.components.common.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.screen.home.components.MediaCardPlaceholder
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun GridLoadingSkeleton(
    columns: Int = 3,
    itemCount: Int = 12,
    cardType: MediaCardType = MediaCardType.POSTER
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        items(itemCount) {
            MediaCardPlaceholder(
                cardType = cardType,
                showTitle = true
            )
        }
    }
}
