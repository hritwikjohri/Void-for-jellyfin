package com.hritwik.avoid.presentation.ui.components.common

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.presentation.ui.theme.Cinder
import com.hritwik.avoid.presentation.ui.theme.Minsk

@Composable
fun FloatingNotchDragHandle(
    notchWidth: Dp = 120.dp,
    notchDepth: Dp = 28.dp,
    lipRadius: Dp = 22.dp,
    pillWidth: Dp = 36.dp,
    pillHeight: Dp = 12.dp,
    shadowBlur: Dp = 24.dp,
    shadowOffsetY: Dp = 8.dp,
    surfaceColor: Color = Cinder,
    pillColor: Color = Minsk
) {
    val density = LocalDensity.current
    val notchW = with(density) { notchWidth.toPx() }
    val depth = with(density) { notchDepth.toPx() }
    val r = with(density) { lipRadius.toPx() }
    val blur = with(density) { shadowBlur.toPx() }
    val dy = with(density) { shadowOffsetY.toPx() }
    val headerHeight = notchDepth + shadowOffsetY

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .drawBehind {
                val w = size.width
                val h = size.height
                val cx = w / 2f

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(38, 0, 0, 0)
                        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
                        isAntiAlias = true
                    }
                    val shadowRectW = notchW * 0.9f
                    val shadowRectH = depth * 1.1f
                    canvas.nativeCanvas.drawOval(
                        cx - shadowRectW / 2f,
                        depth + dy,
                        cx + shadowRectW / 2f,
                        depth + dy + shadowRectH,
                        paint
                    )
                }

                val left = cx - notchW / 2f
                val right = cx + notchW / 2f
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(left - r, 0f)
                    quadraticTo(left, 0f, left + r, depth * 0.65f)
                    quadraticTo(cx, depth + depth * 0.55f, right - r, depth * 0.65f)
                    quadraticTo(right, 0f, right + r, 0f)
                    lineTo(w, 0f); lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(path, surfaceColor)
            },
        contentAlignment = Alignment.TopCenter
    ){
        Box(
            modifier = Modifier
                .padding(bottom = notchDepth - pillHeight / 2)
                .size(width = pillWidth, height = pillHeight)
                .background(pillColor,RoundedCornerShape(50))
        ){ }
    }
}
