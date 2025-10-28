package com.hritwik.avoid.presentation.ui.screen.dev

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hritwik.avoid.BuildConfig
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.theme.PlayerBackground
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamVoid(
    onBackClick: () -> Unit
) {
    AnimatedAmbientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(calculateRoundedValue(12).sdp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ScreenHeader(
                    title = "Created by",
                    onBackClick = onBackClick,
                    showBackButton = true
                )
            }

            item {
                DeveloperProfileCard()
            }

            item {
                AppInfoCard()
            }

            item {
                Spacer(modifier = Modifier.height(calculateRoundedValue(72).sdp))
            }
        }

        Row (
            modifier = Modifier.fillMaxSize().padding(bottom = calculateRoundedValue(32).sdp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            FloatingContactIcons()
        }
    }
}

@Composable
private fun AppInfoCard() {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.void_icon)
                    .crossfade(true)
                    .build(),
                contentDescription = "Void Icon",
                modifier = Modifier.size(calculateRoundedValue(32).sdp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "About Void",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "A modern, minimal Android client for Jellyfin. Built entirely with Kotlin and Jetpack Compose",
            fontSize = 14.sp,
            color = PrimaryText,
            lineHeight = 20.sp
        )
    }

    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Features",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        FeatureSection(
            title = "Effortless Setup & Secure Access",
            items = listOf(
                "Server onboarding with certificate import and mutual TLS",
                "Quick Connect PIN-based login with auto polling & approval",
                "First-run onboarding for permissions, offline setup, and instant access"
            )
        )

        FeatureSection(
            title = "Offline-First Entertainment",
            items = listOf(
                "Downloads hub: pause/resume all, multi-select delete, friendly empty state",
                "Auto-switches Home to offline library when network drops",
                "Background downloads with flexible quality presets and notifications"
            )
        )

        FeatureSection(
            title = "Smart Discovery & Library Browsing",
            items = listOf(
                "Home: Continue Watching, Next Up, Movies, Shows, Recently Added",
                "Search spans Jellyfin + Jellyseer with real-time suggestions & filters",
                "Rich cards and dynamic placeholders while content loads"
            )
        )

        FeatureSection(
            title = "Immersive Playback Controls",
            items = listOf(
                "Cinematic overlay: speed, quality, tracks, decoder/display, PiP, skip/segments",
                "Auto-selects MPV or ExoPlayer per title & device capabilities",
                "Autoplay-next with synced gesture controls across players"
            )
        )

        FeatureSection(
            title = "Pro-Level Customization",
            items = listOf(
                "Swap engines, edit mpv.conf, pick preferred codecs, adjust display modes",
                "Personalization: headers, ambient backgrounds, episode nav, theme songs",
                "Auto-play and auto-skip toggles for binge sessions"
            )
        )

        FeatureSection(
            title = "Jellyseer Request Integration",
            items = listOf(
                "In-app Jellyseer console (base URL, login, API key fallback, sign-out)",
                "Success/error feedback and full request workflow without leaving Void"
            )
        )

        FeatureSection(
            title = "Connectivity & Device Perks",
            items = listOf(
                "Connection Dashboard: manage endpoints, reorder/cleanup, health notifications",
                "Quick actions: change password, favorites, downloads, Void TV login",
                "Offline guards for pairing; hybrid network support"
            )
        )

        FeatureSection(
            title = "Accessibility & Inclusivity",
            items = listOf(
                "30+ languages and colorblind-friendly palettes",
                "Adjustable typography and optional gesture controls",
                "Previewable theme songs and animated ambient backgrounds"
            )
        )

        FeatureSection(
            title = "Advanced Video & Audio",
            items = listOf(
                "Transcoding-aware playback with HDR/SDR/Dolby Vision labeling",
                "FFmpeg-powered audio (toggle on/off)",
                "Audio passthrough support (where available)",
                "Multi-user profiles with independent progress tracking",
                "In-player overlay shows end time; Continue Watching shows time remaining"
            )
        )

        FeatureSection(
            title = "Connectivity Upgrades",
            items = listOf(
                "Re-added ambient animated background",
                "Chromecast receiver integration",
                "Manual & automatic mTLS handling"
            )
        )
    }

    // ——— Existing “Void Info” block ———
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Void Info",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        InfoRow(
            icon = Icons.Default.Apps,
            label = "Version",
            value = BuildConfig.VERSION_NAME
        )
        InfoRow(
            icon = Icons.Default.Code,
            label = "Platform",
            value = "Android"
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = text,
            fontSize = 14.sp,
            color = PrimaryText.copy(alpha = 0.9f),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureSection(
    title: String,
    items: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        items.forEach { FeatureItem(it) }
    }
}

@Composable
private fun FloatingContactIcons(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingActionButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, "mailto:hritwikjohri@gmail.com".toUri())
                context.startActivity(intent)
            },
            containerColor = PlayerBackground,
            modifier = Modifier.size(56.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.gmail_icon),
                contentDescription = "Email",
                modifier = Modifier.size(24.dp),
            )
        }

        FloatingActionButton(
            onClick = {
                val intent =
                    Intent(Intent.ACTION_VIEW, "https://linkedin.com/in/hritwikjohri".toUri())
                context.startActivity(intent)
            },
            containerColor = PlayerBackground,
            modifier = Modifier.size(56.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.linkedin_icon),
                contentDescription = "LinkedIn",
                modifier = Modifier.size(32.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/hritwikjohri".toUri())
                context.startActivity(intent)
            },
            containerColor = PlayerBackground,
            modifier = Modifier.size(56.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.github_icon),
                contentDescription = "GitHub",
                modifier = Modifier.size(24.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://discord.gg/3mFRNRp4VC".toUri())
                context.startActivity(intent)
            },
            containerColor = PlayerBackground,
            modifier = Modifier.size(56.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.discord_icon),
                contentDescription = "Discord",
                modifier = Modifier.size(24.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/hritwikjohri".toUri())
                context.startActivity(intent)
            },
            containerColor = PlayerBackground,
            modifier = Modifier.size(56.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.coffee_icon),
                contentDescription = "buy me a coffee",
                modifier = Modifier.size(42.dp)
            )
        }
    }
}