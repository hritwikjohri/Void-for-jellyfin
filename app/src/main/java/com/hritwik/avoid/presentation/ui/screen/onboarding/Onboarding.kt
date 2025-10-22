package com.hritwik.avoid.presentation.ui.screen.onboarding

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.launch

private data class FeaturePoint(val icon: ImageVector, val text: String)

private data class OnboardingPage(
    val headline: String,
    val subheading: String,
    val body: String,
    val ctaText: String,
    val keyPoints: List<FeaturePoint>,
    val imageRes: Int? = null,
    val iconRes: ImageVector? = null,
    val backgroundGradient: List<Color>? = null,
)

private val onboardingPages = listOf(
    OnboardingPage(
        imageRes = R.drawable.void_icon,
        headline = "Welcome",
        subheading = "Discover your media library",
        body = "Void connects to your Jellyfin server so you can stream and organize all your media in one place.",
        ctaText = "Get Started",
        keyPoints = listOf(
            FeaturePoint(Icons.Filled.VideoLibrary, "Browse and stream your collection"),
            FeaturePoint(Icons.Filled.PlayCircle, "Fast, reliable playback"),
            FeaturePoint(Icons.Filled.Download, "Save items for offline viewing"),
        ),
    ),
    OnboardingPage(
        imageRes = R.drawable.void_cloud,
        headline = "Offline Access",
        subheading = "Media on the go",
        body = "Download your favorites so they're always available—even without internet.",
        ctaText = "Explore Features",
        keyPoints = listOf(
            FeaturePoint(Icons.Filled.OfflineBolt, "Watch without connection"),
            FeaturePoint(Icons.Filled.NotificationsActive, "Background download with notifications"),
            FeaturePoint(Icons.Filled.Settings, "Flexible quality options"),
        ),
    ),
    OnboardingPage(
        imageRes = R.drawable.void_personalize,
        headline = "Personalize",
        subheading = "Make Void yours",
        body = "Adjust Language, Font size and more to create your perfect streaming experience.",
        ctaText = "Connect to Server",
        keyPoints = listOf(
            FeaturePoint(Icons.Filled.Language, "Support for 30+ Languages"),
            FeaturePoint(Icons.Filled.TextFormat, "Choose your own font size"),
            FeaturePoint(Icons.Filled.InvertColors, "Support for colorblind people"),
        ),
    ),
    OnboardingPage(
        imageRes = R.drawable.void_permissions,
        headline = "Permissions",
        subheading = "Consent is important",
        body = "We need few permission to give you the best streaming experience.",
        ctaText = "Grant Permission",
        keyPoints = listOf(
            FeaturePoint(Icons.Filled.MusicNote, "Audio and Music permission"),
            FeaturePoint(Icons.Filled.Storage, "Storage permission"),
            FeaturePoint(Icons.Filled.NotificationsActive, "Notification permission")
        ),
    )
)

@Composable
private fun OnboardingPageView(page: OnboardingPage, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(calculateRoundedValue(16).sdp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp),
        ) {
            page.imageRes?.let { res ->
                Image(
                    painter = painterResource(id = res),
                    contentDescription = null,
                    modifier = Modifier.size(calculateRoundedValue(120).sdp)
                )
            }
            Text(
                text = page.headline,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = page.subheading,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Column(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)) {
                page.keyPoints.forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = point.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = point.text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.filterValues { !it }.keys
        if (Manifest.permission.POST_NOTIFICATIONS in denied) {
            Toast.makeText(
                context,
                "Notifications are disabled.",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (denied.any {
                it == Manifest.permission.READ_MEDIA_VIDEO ||
                        it == Manifest.permission.READ_MEDIA_AUDIO ||
                        it == Manifest.permission.READ_MEDIA_IMAGES ||
                        it == Manifest.permission.READ_EXTERNAL_STORAGE
            }
        ) {
            Toast.makeText(
                context,
                "Media access is unavailable without required permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    AnimatedAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState
            ) { page ->
                OnboardingPageView(
                    page = onboardingPages[page],
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(onboardingPages.size) { index ->
                        val color = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                                .background(color, CircleShape),
                        )
                    }
                }

                val isLast = pagerState.currentPage == onboardingPages.lastIndex
                val currentPage = onboardingPages[pagerState.currentPage]
                Button(onClick = {
                    if (isLast) {
                        val permissionsToRequest = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest += listOf(
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.READ_MEDIA_IMAGES
                            )
                        } else {
                            permissionsToRequest += Manifest.permission.READ_EXTERNAL_STORAGE
                        }

                        if (permissionsToRequest.isNotEmpty()) {
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        }

                        onFinished()
                    } else {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }) {
                    Text(text = currentPage.ctaText)
                }
            }
        }
    }
}