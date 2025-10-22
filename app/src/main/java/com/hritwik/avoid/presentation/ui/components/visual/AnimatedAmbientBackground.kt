package com.hritwik.avoid.presentation.ui.components.visual

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.hritwik.avoid.VoidApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AnimatedAmbientBackground(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    drawableRes: Int? = null,
    baseColors: List<Color> = getDefaultAmbientColors(),
    intensity: Float = 0.7f,
    animationSpeed: Float = 1.0f,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = (context.applicationContext as VoidApplication).preferencesManager
    val ambientEnabled by preferencesManager.getAmbientBackgroundEnabled().collectAsState(initial = false)
    var extractedColors by remember { mutableStateOf(baseColors) }

    if (!ambientEnabled) {
        AmbientBackground (
            modifier = modifier.fillMaxSize(),
            imageUrl = imageUrl,
            drawableRes = drawableRes,
            baseColors = baseColors,
            intensity = intensity
        ){
            content()
        }
        return
    }

    LaunchedEffect(imageUrl, drawableRes) {
        extractedColors = baseColors
        launch {
            val colors = when {
                imageUrl != null -> extractColorsFromUrl(context, imageUrl)
                drawableRes != null -> extractColorsFromDrawable(context, drawableRes)
                else -> baseColors
            }
            extractedColors = colors.takeIf { it.isNotEmpty() } ?: baseColors
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val transition = rememberInfiniteTransition(label = "ambientTransition")
        val primaryX by transition.animateFloat(
            initialValue = 0f,
            targetValue = screenWidth,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = (6000 / animationSpeed).toInt()),
                repeatMode = RepeatMode.Reverse
            ), label = "primaryX"
        )

        val primaryY by transition.animateFloat(
            initialValue = 0f,
            targetValue = screenHeight,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = (8000 / animationSpeed).toInt()),
                repeatMode = RepeatMode.Reverse
            ), label = "primaryY"
        )

        
        val secondaryX by transition.animateFloat(
            initialValue = screenWidth,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = (10000 / animationSpeed).toInt()),
                repeatMode = RepeatMode.Reverse
            ), label = "secondaryX"
        )

        val secondaryY by transition.animateFloat(
            initialValue = screenHeight,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = (7000 / animationSpeed).toInt()),
                repeatMode = RepeatMode.Reverse
            ), label = "secondaryY"
        )

        val primaryBrush = remember(extractedColors, primaryX, primaryY, intensity) {
            if (extractedColors.size >= 2) {
                Brush.radialGradient(
                    colors = listOf(
                        extractedColors[0].copy(alpha = intensity * 0.8f),
                        extractedColors[1].copy(alpha = intensity * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(primaryX, primaryY),
                    radius = (screenWidth.coerceAtLeast(screenHeight) * 0.8f)
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(
                        extractedColors.getOrElse(0) { Color(0xFF6366F1) }.copy(alpha = intensity * 0.8f),
                        Color.Transparent
                    ),
                    center = Offset(primaryX, primaryY),
                    radius = (screenWidth.coerceAtLeast(screenHeight) * 0.8f)
                )
            }
        }

        val secondaryBrush = remember(extractedColors, secondaryX, secondaryY, intensity) {
            if (extractedColors.size >= 3) {
                Brush.radialGradient(
                    colors = listOf(
                        extractedColors[2].copy(alpha = intensity * 0.6f),
                        extractedColors.getOrElse(3) { extractedColors[0] }.copy(alpha = intensity * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(secondaryX, secondaryY),
                    radius = (screenWidth.coerceAtLeast(screenHeight) * 0.6f)
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(
                        extractedColors.getOrElse(1) { Color(0xFF8B5CF6) }.copy(alpha = intensity * 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(secondaryX, secondaryY),
                    radius = (screenWidth.coerceAtLeast(screenHeight) * 0.6f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        drawRect(Color.Black)
                        drawRect(primaryBrush)
                        drawRect(secondaryBrush)
                    }
                }
        ) {
            content()
        }
    }
}

private suspend fun extractColorsFromUrl(
    context: Context,
    imageUrl: String
): List<Color> = withContext(Dispatchers.IO) {
    PaletteCache.get(imageUrl)?.let { return@withContext it }

    try {
        val imageLoader = Coil.imageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .size(128)
            .scale(Scale.FIT)
            .build()

        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            val colors = bitmap?.let { extractPaletteColors(it) } ?: getDefaultAmbientColors()
            PaletteCache.put(imageUrl, colors)
            colors
        } else {
            getDefaultAmbientColors()
        }
    } catch (_: Exception) {
        getDefaultAmbientColors()
    }
}

private suspend fun extractColorsFromDrawable(
    context: Context,
    drawableRes: Int
): List<Color> = withContext(Dispatchers.IO) {
    try {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        val bitmap = drawable?.let { d ->
            if (d is BitmapDrawable) {
                d.bitmap
            } else {
                val bitmap = createBitmap(
                    d.intrinsicWidth.takeIf { it > 0 } ?: 300,
                    d.intrinsicHeight.takeIf { it > 0 } ?: 300
                )
                val canvas = Canvas(bitmap)
                d.setBounds(0, 0, canvas.width, canvas.height)
                d.draw(canvas)
                bitmap
            }
        }
        bitmap?.let { extractPaletteColors(it) } ?: getDefaultAmbientColors()
    } catch (_: Exception) {
        getDefaultAmbientColors()
    }
}

private suspend fun extractPaletteColors(bitmap: Bitmap): List<Color> = withContext(Dispatchers.Default) {
    val palette = Palette.from(bitmap)
        .maximumColorCount(48)
        .generate()

    val colors = mutableListOf<Color>()
    palette.vibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.lightVibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.darkVibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.dominantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.mutedSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.lightMutedSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.darkMutedSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.swatches.take(4).forEach { swatch ->
        val color = Color(swatch.rgb)
        if (!colors.contains(color)) {
            colors.add(color)
        }
    }

    if (colors.isEmpty()) getDefaultAmbientColors() else colors.take(6)
}

private fun getDefaultAmbientColors(): List<Color> = listOf(
    Color(0xFF6366F1), 
    Color(0xFF8B5CF6), 
    Color(0xFF06B6D4), 
    Color(0xFF10B981), 
    Color(0xFFEF4444), 
    Color(0xFFF59E0B)  
)