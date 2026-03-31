package com.hritwik.avoid.cast

import androidx.media3.common.util.UnstableApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CastEntryPoint {
    @UnstableApi
    fun castManager(): CastManager
}
