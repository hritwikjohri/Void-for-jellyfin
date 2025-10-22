package com.hritwik.avoid.utils.helpers

import androidx.compose.runtime.staticCompositionLocalOf

val LocalImageHelper = staticCompositionLocalOf<ImageHelper> {
    error("ImageHelper not provided")
}

