package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun SplashScreen() {
    AnimatedAmbientBackground(
        drawableRes = R.drawable.jellyfin_logo
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.drawable.void_icon,
                contentDescription = "Jellyfin Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(calculateRoundedValue(120).sdp)
            )
        }
        SubtleShinySignature(
            modifier = Modifier.padding(bottom = calculateRoundedValue(90).sdp)
        )
    }
}