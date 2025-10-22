package com.hritwik.avoid.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9960BF),              
    secondary = Color(0xFF6F73BF),            
    tertiary = Color(0xFF0511F2),             

    
    background = Color(0xFF0A0A0A),           
    surface = Color(0xFF121212),              
    surfaceVariant = Color(0xFF1E1E1E),       

    
    primaryContainer = Color(0xFF2D1B3D),     
    secondaryContainer = Color(0xFF1A1D3A),   
    tertiaryContainer = Color(0xFF0A0F2A),    

    
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE1E1E1),         
    onSurface = Color(0xFFE1E1E1),            
    onSurfaceVariant = Color(0xFFBBBBBB),     

    
    onPrimaryContainer = Color(0xFFE1E1E1),
    onSecondaryContainer = Color(0xFFE1E1E1),
    onTertiaryContainer = Color(0xFFE1E1E1),

    
    outline = Color(0xFF2A2A2A),              
    outlineVariant = Color(0xFF1A1A1A),       

    
    error = Color(0xFFCF6679),                
    onError = Color.Black,
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFF2B8B5),

    
    inverseSurface = Color(0xFFE1E1E1),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFF6750A4)
)


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),

    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFDFBFF),
    surfaceVariant = Color(0xFFE7E0EC),

    primaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFFE8DEF8),
    tertiaryContainer = Color(0xFFFFD8E4),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),

    onPrimaryContainer = Color(0xFF21005D),
    onSecondaryContainer = Color(0xFF1D192B),
    onTertiaryContainer = Color(0xFF31111D),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFC4C7C5),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF)
)


private val ColorBlindColorScheme = DarkColorScheme.copy(
    primary = ColorBlindPrimary,
    secondary = ColorBlindSecondary,
    tertiary = ColorBlindTertiary,
    primaryContainer = ColorBlindPrimary,
    secondaryContainer = ColorBlindSecondary,
    tertiaryContainer = ColorBlindTertiary,
    inversePrimary = ColorBlindPrimary
)


@Composable
fun VoidTheme(
    themeMode: String,
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "light" -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                dynamicLightColorScheme(context)
            } else {
                LightColorScheme
            }
        }
        "colorblind" -> ColorBlindColorScheme
        else -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                dynamicDarkColorScheme(context)
            } else {
                DarkColorScheme
            }
        }
    }

    val contrastScheme = if (highContrast) {
        if (themeMode == "light") {
            colorScheme.copy(
                onBackground = Color.Black,
                onSurface = Color.Black
            )
        } else {
            colorScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color.White
            )
        }
    } else {
        colorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            val isLight = themeMode == "light"
            windowInsetsController.isAppearanceLightStatusBars = isLight
            windowInsetsController.isAppearanceLightNavigationBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = contrastScheme,
        typography = AppTypography(),
        content = content
    )
}
