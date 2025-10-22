package com.hritwik.avoid.presentation.ui.components.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection


suspend fun extractDominantColor(
    imageUrl: String?,
    timeoutMs: Long = 10_000L
): Color? {
    if (imageUrl.isNullOrEmpty()) return null

    return withContext(Dispatchers.IO) {
        
        withTimeoutOrNull(timeoutMs) {
            extractDominantColorInternal(imageUrl)
        }
    }
}


private suspend fun extractDominantColorInternal(imageUrl: String): Color? {
    var connection: URLConnection? = null
    var inputStream: InputStream? = null
    var bufferedInputStream: BufferedInputStream? = null
    var bitmap: Bitmap? = null

    try {
        
        val url = URL(imageUrl)
        connection = url.openConnection().apply {
            
            connectTimeout = 10_000 
            readTimeout = 15_000 

            
            setRequestProperty("User-Agent", "Jellyfin Android Client")
            setRequestProperty("Accept", "image/webp,image/png,image/jpeg,image/*,*/*;q=0.8")

            
            useCaches = true

            
            setRequestProperty("Accept-Encoding", "gzip, deflate")
        }

        
        if (connection is HttpURLConnection) {
            connection.instanceFollowRedirects = true

            
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                android.util.Log.w("ExtractDominantColor", "HTTP error: $responseCode for URL: $imageUrl")
                return null
            }
        }

        
        inputStream = connection.getInputStream()
        bufferedInputStream = BufferedInputStream(inputStream, 8192)

        
        val options = BitmapFactory.Options().apply {
            
            inSampleSize = calculateInSampleSize(connection.contentLength)
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565 
            inTempStorage = ByteArray(16 * 1024) 
        }

        bitmap = BitmapFactory.decodeStream(bufferedInputStream, null, options)

        if (bitmap == null) {
            android.util.Log.w("ExtractDominantColor", "Failed to decode bitmap from URL: $imageUrl")
            return null
        }

        
        return withContext(Dispatchers.Default) {
            extractDominantColorFromBitmap(bitmap)
        }

    } catch (e: Exception) {
        android.util.Log.e("ExtractDominantColor", "Error extracting color from URL: $imageUrl", e)
        return null
    } finally {
        
        

        
        try {
            bitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExtractDominantColor", "Error recycling bitmap", e)
        }

        
        try {
            bufferedInputStream?.close()
        } catch (e: Exception) {
            android.util.Log.e("ExtractDominantColor", "Error closing buffered input stream", e)
        }

        try {
            inputStream?.close()
        } catch (e: Exception) {
            android.util.Log.e("ExtractDominantColor", "Error closing input stream", e)
        }

        
        try {
            (connection as? HttpURLConnection)?.disconnect()
        } catch (e: Exception) {
            android.util.Log.e("ExtractDominantColor", "Error disconnecting HTTP connection", e)
        }
    }
}


private fun extractDominantColorFromBitmap(bitmap: Bitmap): Color? {
    try {
        val palette = Palette.from(bitmap)
            .maximumColorCount(16) 
            .generate()

        
        val dominantColor = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.lightMutedSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb

        return dominantColor?.let { Color(it) }
    } catch (e: Exception) {
        android.util.Log.e("ExtractDominantColor", "Error extracting palette colors", e)
        return null
    }
}


private fun calculateInSampleSize(contentLength: Int): Int {
    return when {
        contentLength <= 0 -> 1 
        contentLength < 100_000 -> 1 
        contentLength < 500_000 -> 2 
        contentLength < 1_000_000 -> 4 
        else -> 8 
    }
}