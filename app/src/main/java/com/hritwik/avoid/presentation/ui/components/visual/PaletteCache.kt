package com.hritwik.avoid.presentation.ui.components.visual

import android.util.LruCache
import androidx.compose.ui.graphics.Color

object PaletteCache {
    private val cache = LruCache<String, List<Color>>(20)

    fun get(key: String): List<Color>? = cache.get(key)

    fun put(key: String, colors: List<Color>) {
        cache.put(key, colors)
    }
}