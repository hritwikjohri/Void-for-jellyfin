package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.theme.DeepBlack
import com.hritwik.avoid.presentation.ui.theme.VoidDarkBlue
import com.hritwik.avoid.presentation.ui.theme.VoidDarkMagenta
import kotlin.math.max

@Composable
fun EmptyItem(
    image: Int? = R.drawable.void_icon
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val r = max(size.width, size.height) * 1.0f
                drawRect(DeepBlack.copy(alpha = 0.6f))
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(VoidDarkBlue.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = r
                    ),
                    size = size
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(VoidDarkMagenta.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = r
                    ),
                    size = size
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = image,
            contentDescription = "Empty media item",
            modifier = Modifier.fillMaxSize(0.4f),
            contentScale = ContentScale.Fit
        )
    }
}