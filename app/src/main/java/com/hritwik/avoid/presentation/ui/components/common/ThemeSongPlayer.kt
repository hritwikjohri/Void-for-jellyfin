package com.hritwik.avoid.presentation.ui.components.common

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.presentation.ui.screen.player.ThemeSongController
import com.hritwik.avoid.presentation.ui.state.ThemeSongState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@OptIn(UnstableApi::class)
@Composable
fun ThemeSongPlayer(themeSong: ThemeSongState?) {
    val context = LocalContext.current
    val controller = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ThemeSongControllerEntryPoint::class.java
        ).themeSongController()
    }

    LaunchedEffect(themeSong) {
        if (themeSong != null) {
            controller.playThemeSong(themeSong)
        }
    }
}

@EntryPoint
@OptIn(UnstableApi::class)
@InstallIn(SingletonComponent::class)
interface ThemeSongControllerEntryPoint {
    fun themeSongController(): ThemeSongController
}
