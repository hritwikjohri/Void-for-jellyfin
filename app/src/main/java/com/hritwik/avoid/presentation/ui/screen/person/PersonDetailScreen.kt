package com.hritwik.avoid.presentation.ui.screen.person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.ErrorMessage
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.screen.person.components.PersonHeroSection
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.media.PersonViewModel
import com.hritwik.avoid.utils.helpers.GestureHelper.swipeBack
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun PersonDetailScreen(
    personId: String,
    onBackClick: () -> Unit = {},
    onMediaItemClick: (String) -> Unit = {},
    authViewModel: AuthServerViewModel,
    personViewModel: PersonViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val personState by personViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(personId, authState.authSession) {
        authState.authSession?.let { session ->
            personViewModel.loadPersonDetails(
                personId = personId,
                userId = session.userId.id,
                accessToken = session.accessToken
            )
        }
    }

    when {
        personState.isLoading -> {
            AnimatedAmbientBackground {
                LoadingState()
            }
        }

        personState.error != null -> {
            ErrorMessage(
                error = personState.error!!,
                onRetry = {
                    authState.authSession?.let { session ->
                        personViewModel.loadPersonDetails(
                            personId = personId,
                            userId = session.userId.id,
                            accessToken = session.accessToken
                        )
                    }
                },
                onDismiss = { personViewModel.clearError() }
            )
        }

        personState.personDetail != null -> {
            PersonContent(
                personDetail = personState.personDetail!!,
                appearances = personState.appearances,
                serverUrl = authState.authSession?.server?.url ?: "",
                onBackClick = onBackClick,
                onMediaItemClick = onMediaItemClick
            )
        }
    }
}

@Composable
private fun PersonContent(
    personDetail: MediaItem,
    appearances: List<MediaItem>,
    serverUrl: String,
    onBackClick: () -> Unit,
    onMediaItemClick: (String) -> Unit
) {
    val imageUrl = personDetail.primaryImageTag?.let { tag ->
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        "${baseUrl}Items/${personDetail.id}/Images/Primary?tag=$tag&quality=90&maxWidth=800"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeBack(onBackClick)
    ) {
        AnimatedAmbientBackground(
            imageUrl = imageUrl
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = calculateRoundedValue(80).sdp)
            ) {
                // Hero Section
                item {
                    PersonHeroSection(
                        personDetail = personDetail,
                        serverUrl = serverUrl,
                        onBackClick = onBackClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
                }

                // Overview/Biography
                personDetail.overview?.let { overview ->
                    if (overview.isNotBlank()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                            ) {
                                Text(
                                    text = "About",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))
                                Text(
                                    text = overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(calculateRoundedValue(24).sdp))
                        }
                    }
                }

                // Appearances Section
                if (appearances.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "In Library",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp),
                                color = PrimaryText
                            )

                            Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                contentPadding = PaddingValues(horizontal = calculateRoundedValue(16).sdp)
                            ) {
                                items(appearances) { mediaItem ->
                                    MediaItemCard(
                                        mediaItem = mediaItem,
                                        serverUrl = serverUrl,
                                        cardType = MediaCardType.POSTER,
                                        onClick = { onMediaItemClick(mediaItem.id) }
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
