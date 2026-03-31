package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hritwik.avoid.presentation.ui.components.common.shimmerEffect
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun LibraryGridCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(16f/9f),
        elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(8).sdp),
        shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect(
                    shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
                )
        )
    }
}
