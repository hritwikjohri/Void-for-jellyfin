package com.hritwik.avoid.presentation.ui.components.scanner

import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit,
    private val debounceDurationMillis: Long = 1500L
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    private var lastResult: String? = null
    private var lastResultTimestamp: Long = 0L
    @Volatile
    private var targetArea: RectF? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val imageWidth = image.width
        val imageHeight = image.height
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val matchingBarcode = barcodes
                    .firstOrNull { barcode ->
                        val rawValue = barcode.rawValue?.trim()
                        if (rawValue.isNullOrBlank()) {
                            return@firstOrNull false
                        }

                        isBarcodeWithinTargetArea(barcode, imageWidth, imageHeight)
                    }
                    ?: return@addOnSuccessListener

                val value = matchingBarcode.rawValue?.trim() ?: return@addOnSuccessListener

                val currentTime = SystemClock.elapsedRealtime()
                val isDuplicate = lastResult == value &&
                    (currentTime - lastResultTimestamp) < debounceDurationMillis

                if (!isDuplicate) {
                    lastResult = value
                    lastResultTimestamp = currentTime
                    onQrCodeScanned(value)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun updateTargetArea(area: RectF?) {
        targetArea = area
    }

    private fun isBarcodeWithinTargetArea(barcode: Barcode, imageWidth: Int, imageHeight: Int): Boolean {
        val area = targetArea ?: return true
        val boundingBox = barcode.boundingBox ?: return false

        return isWithinTargetArea(boundingBox, area, imageWidth, imageHeight)
    }

    private fun isWithinTargetArea(boundingBox: Rect, area: RectF, imageWidth: Int, imageHeight: Int): Boolean {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return false
        }

        val centerX = (boundingBox.left + boundingBox.right) / (2f * imageWidth)
        val centerY = (boundingBox.top + boundingBox.bottom) / (2f * imageHeight)
        if (!area.contains(centerX, centerY)) {
            return false
        }

        val normalizedRect = RectF(
            (boundingBox.left.toFloat() / imageWidth).coerceIn(0f, 1f),
            (boundingBox.top.toFloat() / imageHeight).coerceIn(0f, 1f),
            (boundingBox.right.toFloat() / imageWidth).coerceIn(0f, 1f),
            (boundingBox.bottom.toFloat() / imageHeight).coerceIn(0f, 1f)
        )

        if (normalizedRect.width() <= 0f || normalizedRect.height() <= 0f) {
            return false
        }

        val overlapLeft = maxOf(area.left, normalizedRect.left)
        val overlapTop = maxOf(area.top, normalizedRect.top)
        val overlapRight = minOf(area.right, normalizedRect.right)
        val overlapBottom = minOf(area.bottom, normalizedRect.bottom)

        val overlapWidth = overlapRight - overlapLeft
        val overlapHeight = overlapBottom - overlapTop
        if (overlapWidth <= 0f || overlapHeight <= 0f) {
            return false
        }

        val normalizedArea = normalizedRect.width() * normalizedRect.height()
        val overlapArea = overlapWidth * overlapHeight
        val overlapRatio = overlapArea / normalizedArea

        return overlapRatio >= MIN_OVERLAP_RATIO
    }

    fun clear() {
        scanner.close()
    }

    companion object {
        private const val MIN_OVERLAP_RATIO = 0.7f
    }
}
