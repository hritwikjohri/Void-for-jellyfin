package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.theme.DeepBlack
import com.hritwik.avoid.presentation.ui.theme.VoidDarkBlue
import com.hritwik.avoid.presentation.ui.theme.VoidDarkMagenta
import kotlin.math.max

@Composable
fun NetworkImage(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderResId: Int = R.drawable.void_icon
) {
    val context = LocalContext.current
    val rememberedData = remember(data) { data }
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(rememberedData)
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .size(Size.ORIGINAL)
            .build()
    )

    Box(
        modifier = modifier
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
        val painterState = painter.state
        val isShowingPlaceholder = painterState is AsyncImagePainter.State.Loading ||
                painterState is AsyncImagePainter.State.Error ||
                rememberedData == null

        val imageModifier = if (isShowingPlaceholder) {
            Modifier.fillMaxSize(0.4f)
        } else {
            Modifier.fillMaxSize()
        }

        val imageContentScale = if (isShowingPlaceholder) {
            ContentScale.Fit
        } else {
            contentScale
        }

        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = imageContentScale
        )
    }
}