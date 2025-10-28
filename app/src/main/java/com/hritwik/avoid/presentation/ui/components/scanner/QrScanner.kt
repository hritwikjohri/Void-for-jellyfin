package com.hritwik.avoid.presentation.ui.components.scanner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.RectF
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

@Composable
fun QrScanner(
    modifier: Modifier = Modifier,
    torchEnabled: Boolean,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    onTorchStateChanged: (Boolean) -> Unit,
    onQrCodeScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var analysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var scanCompleted by remember { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    val analyzer = remember {
        QrCodeAnalyzer(onQrCodeScanned = { value ->
            if (!scanCompleted) {
                scanCompleted = true
                onQrCodeScanned(value)
            }
        })
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            analyzer.clear()
            analysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(camera) {
        val torchObserver = androidx.lifecycle.Observer<Int> { state ->
            val isEnabled = state == androidx.camera.core.TorchState.ON
            onTorchStateChanged(isEnabled)
        }
        val hasTorch = camera?.cameraInfo?.hasFlashUnit() == true
        onTorchAvailabilityChanged(hasTorch)
        if (hasTorch) {
            camera?.cameraInfo?.torchState?.observe(lifecycleOwner, torchObserver)
        }
        onDispose {
            camera?.cameraInfo?.torchState?.removeObserver(torchObserver)
        }
    }

    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(scanCompleted) {
        if (scanCompleted) {
            analysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).await(context)
        cameraProvider = provider

        val preview = Preview.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()
            .apply { surfaceProvider = previewView.surfaceProvider }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
            }

        analysis = imageAnalysis
        provider.unbindAll()
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
        )
    }

    val overlayColor = Color.Black.copy(alpha = 0.8f)
    val scanBoxSize = 240.dp
    val scanBoxCornerRadius = 20.dp
    val density = LocalDensity.current
    val scanBoxSizePx = with(density) { scanBoxSize.roundToPx() }
    val targetArea = remember(previewSize, scanBoxSizePx) {
        if (previewSize == IntSize.Zero || scanBoxSizePx == 0) {
            null
        } else {
            val width = previewSize.width.toFloat()
            val height = previewSize.height.toFloat()
            val boxSize = min(scanBoxSizePx.toFloat(), min(width, height))
            val left = ((width - boxSize) / 2f) / width
            val top = ((height - boxSize) / 2f) / height
            val right = ((width + boxSize) / 2f) / width
            val bottom = ((height + boxSize) / 2f) / height

            RectF(
                left.coerceIn(0f, 1f),
                top.coerceIn(0f, 1f),
                right.coerceIn(0f, 1f),
                bottom.coerceIn(0f, 1f)
            )
        }
    }

    LaunchedEffect(previewSize, scanBoxSizePx) {
        analyzer.updateTargetArea(targetArea)
    }

    Box {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { previewView }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    previewSize = coordinates.size
                }
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawRect(overlayColor)

                    val boxSizePx = with(density) { scanBoxSize.toPx() }
                    val cornerRadiusPx = with(density) { scanBoxCornerRadius.toPx() }
                    val left = (size.width - boxSizePx) / 2f
                    val top = (size.height - boxSizePx) / 2f

                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(boxSizePx, boxSizePx),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )

                    drawContent()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(scanBoxSize)
                    .border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(scanBoxCornerRadius)
                    )
            )
        }
    }
}

private suspend fun ListenableFuture<ProcessCameraProvider>.await(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
        continuation.invokeOnCancellation { cancel(true) }
    }

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
