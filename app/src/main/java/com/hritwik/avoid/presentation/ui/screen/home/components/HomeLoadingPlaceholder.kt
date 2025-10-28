package com.hritwik.avoid.presentation.ui.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun HomeFeaturedPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(calculateRoundedValue(600).sdp)
    ) {
        PlaceholderBox(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = calculateRoundedValue(0).sdp
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = calculateRoundedValue(24).sdp),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlaceholderBox(
                modifier = Modifier
                    .width(calculateRoundedValue(200).sdp)
                    .height(calculateRoundedValue(280).sdp),
                cornerRadius = calculateRoundedValue(16).sdp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    PlaceholderBox(
                        modifier = Modifier
                            .width(calculateRoundedValue(40).sdp)
                            .height(calculateRoundedValue(20).sdp),
                        cornerRadius = calculateRoundedValue(12).sdp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaceholderBox(
                    modifier = Modifier
                        .width(calculateRoundedValue(140).sdp)
                        .height(calculateRoundedValue(56).sdp),
                    cornerRadius = calculateRoundedValue(32).sdp
                )

                PlaceholderBox(
                    modifier = Modifier
                        .width(calculateRoundedValue(56).sdp)
                        .height(calculateRoundedValue(56).sdp),
                    cornerRadius = calculateRoundedValue(28).sdp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(calculateRoundedValue(16).sdp)
        ) {
            PlaceholderBox(
                modifier = Modifier.size(calculateRoundedValue(56).sdp),
                cornerRadius = calculateRoundedValue(28).sdp
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = calculateRoundedValue(32).sdp),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
        ) {
            repeat(5) { index ->
                val width = if (index == 0) 8 else 4
                PlaceholderBox(
                    modifier = Modifier
                        .width(calculateRoundedValue(width).sdp)
                        .height(calculateRoundedValue(4).sdp),
                    cornerRadius = calculateRoundedValue(4).sdp
                )
            }
        }
    }
}

@Composable
fun HomeSectionPlaceholder(
    titleWidth: Dp,
    cardType: MediaCardType,
    showTitle: Boolean,
    itemCount: Int,
    showAction: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = calculateRoundedValue(4).sdp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = calculateRoundedValue(10).sdp,
                    bottom = calculateRoundedValue(10).sdp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaceholderBox(
                modifier = Modifier
                    .width(titleWidth)
                    .height(calculateRoundedValue(20).sdp),
                cornerRadius = calculateRoundedValue(8).sdp
            )

            if (showAction) {
                PlaceholderBox(
                    modifier = Modifier
                        .padding(end = calculateRoundedValue(10).sdp)
                        .width(calculateRoundedValue(70).sdp)
                        .height(calculateRoundedValue(20).sdp),
                cornerRadius = calculateRoundedValue(8).sdp
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
            contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
        ) {
            items(itemCount) {
                MediaCardPlaceholder(
                    cardType = cardType,
                    showTitle = showTitle
                )
            }
        }
    }
}

@Composable
private fun MediaCardPlaceholder(
    cardType: MediaCardType,
    showTitle: Boolean
) {
    val width = when (cardType) {
        MediaCardType.POSTER, MediaCardType.SQUARE -> calculateRoundedValue(120).sdp
        MediaCardType.THUMBNAIL -> calculateRoundedValue(240).sdp
    }

    val aspectRatio = when (cardType) {
        MediaCardType.POSTER -> 2f / 3.15f
        MediaCardType.THUMBNAIL -> 16f / 9f
        MediaCardType.SQUARE -> 1f
    }

    Column(
        modifier = Modifier.width(width)
    ) {
        PlaceholderBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            cornerRadius = calculateRoundedValue(12).sdp
        )

        if (showTitle) {
            Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

            PlaceholderBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calculateRoundedValue(16).sdp),
                cornerRadius = calculateRoundedValue(6).sdp
            )

            Spacer(modifier = Modifier.height(calculateRoundedValue(6).sdp))

            PlaceholderBox(
                modifier = Modifier
                    .width(width * 0.6f)
                    .height(calculateRoundedValue(14).sdp),
                cornerRadius = calculateRoundedValue(6).sdp
            )
        }
    }
}

@Composable
private fun PlaceholderBox(
    modifier: Modifier,
    cornerRadius: Dp
) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(placeholderColor)
    )
}
