package com.hritwik.avoid

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.presentation.ui.navigation.JellyfinNavigation
import com.hritwik.avoid.presentation.ui.screen.onboarding.OnboardingScreen
import com.hritwik.avoid.presentation.ui.theme.VoidTheme
import com.hritwik.avoid.utils.constants.PreferenceConstants
import com.hritwik.avoid.utils.extensions.ProvideFontScale
import com.hritwik.avoid.utils.helpers.ImageHelper
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import com.hritwik.avoid.utils.helpers.hideNavigationButtons
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var networkMonitor: NetworkMonitor
    @Inject
    lateinit var imageHelper: ImageHelper
    @Inject
    lateinit var preferencesManager: PreferencesManager
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.hideNavigationButtons()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        configureSystemBars()
        enableEdgeToEdge()
        setContent {
            val firstRunCompleted by preferencesManager.isFirstRunCompleted().collectAsState(initial = true)
            val themeMode by preferencesManager.getThemeMode().collectAsState(initial = PreferenceConstants.DEFAULT_THEME_MODE)
            val fontScale by preferencesManager.getFontScale().collectAsState(initial = PreferenceConstants.DEFAULT_FONT_SCALE)
            val highContrast by preferencesManager.getHighContrastEnabled().collectAsState(initial = PreferenceConstants.DEFAULT_HIGH_CONTRAST)


            CompositionLocalProvider(LocalImageHelper provides imageHelper) {
                ProvideFontScale(fontScale) {
                    VoidTheme(themeMode = themeMode, highContrast = highContrast) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if(firstRunCompleted){
                                Column {
                                    JellyfinNavigation()
                                }
                            } else {
                                OnboardingScreen(onFinished = {
                                    lifecycleScope.launch {
                                        preferencesManager.setFirstRunCompleted(
                                            true
                                        )
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}