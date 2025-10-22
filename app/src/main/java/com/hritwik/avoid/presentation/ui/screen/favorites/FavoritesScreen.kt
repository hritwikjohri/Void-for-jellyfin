package com.hritwik.avoid.presentation.ui.screen.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.GestureHelper.swipeBack
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onMediaItemClick: (String) -> Unit,
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val favorites by userDataViewModel.favorites.collectAsStateWithLifecycle()
    val serverUrl = authState.authSession?.server?.url ?: ""

    LaunchedEffect(authState.authSession) {
        authState.authSession?.let { session ->
            userDataViewModel.loadFavorites(session.userId.id, session.accessToken)
            userDataViewModel.loadPlayedItems(session.userId.id, session.accessToken)
        } ?: userDataViewModel.reset()
    }

    val imageHelper = LocalImageHelper.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeBack(onBack)
    ) {
        AnimatedAmbientBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = "Favorites",
                    showBackButton = true,
                    onBackClick = onBack
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (favorites.isEmpty()) {
                        EmptyState(
                            title = "No Favorites",
                            description = "Looks like you haven't liked anything yet.",
                        )
                    } else {
                        SectionHeader(title = "All Favorites") {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                contentPadding = PaddingValues(calculateRoundedValue(10).sdp)
                            ) {
                                items(favorites) { mediaItem ->
                                    val isEpisode = mediaItem.type == "Episode"
                                    val aspectRatio = if (isEpisode) 16f / 9f else 2f / 3f

                                    val imageUrl = remember(serverUrl, mediaItem.id, mediaItem.primaryImageTag, mediaItem.backdropImageTags) {
                                        if (isEpisode) {
                                            imageHelper.createBackdropUrl(
                                                serverUrl,
                                                mediaItem.id,
                                                mediaItem.backdropImageTags.firstOrNull()
                                            ) ?: imageHelper.createPosterUrl(serverUrl, mediaItem.id, mediaItem.primaryImageTag)
                                        } else {
                                            imageHelper.createPosterUrl(serverUrl, mediaItem.id, mediaItem.primaryImageTag)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = calculateRoundedValue(8).sdp, vertical = calculateRoundedValue(4).sdp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onMediaItemClick(mediaItem.id) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(calculateRoundedValue(8).sdp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).sdp),
                                            modifier = Modifier
                                                .width(calculateRoundedValue(100).sdp)
                                                .aspectRatio(aspectRatio)
                                        ) {
                                            if (imageUrl != null) {
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = mediaItem.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(modifier = Modifier.fillMaxSize())
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mediaItem.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            val meta = listOfNotNull(
                                                mediaItem.type.takeIf { it.isNotBlank() },
                                                mediaItem.year?.toString()
                                            ).joinToString(" • ")

                                            if (meta.isNotEmpty()) {
                                                Text(
                                                    text = meta,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}