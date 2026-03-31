package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.cast.CastButton
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.navigation.Routes
import com.hritwik.avoid.presentation.ui.screen.downloads.DownloadsScreen
import com.hritwik.avoid.presentation.ui.screen.home.components.HomeSectionPlaceholder
import com.hritwik.avoid.presentation.ui.state.HomeSection
import com.hritwik.avoid.presentation.ui.state.HomeSectionLoadState
import com.hritwik.avoid.presentation.ui.state.isLoadingOrQueued
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onMediaItemClick: (MediaItem) -> Unit = {},
    onSearchClick: () -> Unit = {},
    navController: NavHostController,
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()
    val playedItems by userDataViewModel.playedItems.collectAsStateWithLifecycle()
    val homeSavedState = remember(navController) {
        runCatching { navController.getBackStackEntry(Routes.HOME).savedStateHandle }.getOrNull()
    }
    val refreshResumeItems =
        homeSavedState
            ?.getStateFlow("refreshResumeItems", false)
            ?.collectAsStateWithLifecycle()
            ?.value
    val refreshNextUp =
        homeSavedState
            ?.getStateFlow("refreshNextUp", false)
            ?.collectAsStateWithLifecycle()
            ?.value
    val refreshHome =
        homeSavedState
            ?.getStateFlow("refreshHome", false)
            ?.collectAsStateWithLifecycle()
            ?.value
    val isNetworkAvailable by userDataViewModel.isConnected.collectAsStateWithLifecycle()
    val currentUserId = authState.authSession?.userId?.id
    val activeServerUrl = authState.authSession?.server?.url ?: authState.serverUrl ?: ""
    val accessToken = authState.authSession?.accessToken
    var hasRequestedInitialLibraryData by remember(currentUserId, activeServerUrl) {
        mutableStateOf(false)
    }
    var hasClearedHomeImageCache by remember(currentUserId, activeServerUrl) {
        mutableStateOf(false)
    }
    var hasRefreshedThisSession by remember(currentUserId, activeServerUrl) {
        mutableStateOf(false)
    }
    var shouldLoadImages by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val isLoading by remember { derivedStateOf { libraryState.isLoading } }
    val sectionStates by remember { derivedStateOf { libraryState.sectionStates } }
    val libraries by remember { derivedStateOf { libraryState.libraries } }
    val resumeItems by remember { derivedStateOf { libraryState.resumeItems } }
    val nextUpItems by remember { derivedStateOf { libraryState.nextUpItems } }
    val favorites by userDataViewModel.favorites.collectAsStateWithLifecycle()
    val latestMovies by remember { derivedStateOf { libraryState.latestMovies } }
    val latestEpisodes by remember { derivedStateOf { libraryState.latestEpisodes } }
    val latestItemsByLibrary by remember { derivedStateOf { libraryState.latestItemsByLibrary } }

    // Enable image loading as soon as the first section finishes loading
    LaunchedEffect(sectionStates) {
        if (sectionStates.values.any { it == HomeSectionLoadState.LOADED }) {
            shouldLoadImages = true
        }
    }

    val recentlyAddedShows = remember(libraryState.latestItems) {
        libraryState.latestItems
            .filter { it.type.equals("Series", ignoreCase = true) }
            .distinctBy { it.id }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Initial load: use the queue system so sections load FIFO
    LaunchedEffect(authState.authSession, currentUserId, activeServerUrl) {
        authState.authSession?.let { session ->
            val userId = session.userId.id
            val accessToken = session.accessToken

            if (!hasRequestedInitialLibraryData && libraries.isEmpty()) {
                hasRequestedInitialLibraryData = true
                if (!hasClearedHomeImageCache) {
                    userDataViewModel.clearImageCache()
                    hasClearedHomeImageCache = true
                }
                libraryViewModel.loadHomeContentQueued(
                    userId = userId,
                    accessToken = accessToken
                )
            }
        } ?: run {
            libraryViewModel.reset()
            userDataViewModel.reset()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                hasRefreshedThisSession = false
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshResumeItems, refreshNextUp, refreshHome) {
        authState.authSession?.let { session ->
            val userId = session.userId.id
            val accessToken = session.accessToken

            if (refreshHome == true) {
                userDataViewModel.clearImageCache()
                libraryViewModel.loadHomeContentQueued(
                    userId = userId,
                    accessToken = accessToken
                )
                homeSavedState["refreshHome"] = false
            }

            if (refreshResumeItems == true) {
                libraryViewModel.refreshResumeItems(userId, accessToken)
                homeSavedState["refreshResumeItems"] = false
            }

            if (refreshNextUp == true) {
                libraryViewModel.refreshNextUpItems(userId, accessToken)
                homeSavedState["refreshNextUp"] = false
            }
        }
    }

    AnimatedAmbientBackground(
        bypassCache = false,
        authToken = accessToken
    ) {
        if (!isNetworkAvailable) {
            DownloadsScreen(
                showBackButton = false,
                onPlay = { mediaItem ->
                    navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            )
        } else {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
                ) {
                    item {
                        CenterAlignedTopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Unspecified,
                                navigationIconContentColor = Color.Unspecified,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                                actionIconContentColor = Color.Unspecified
                            ),
                            title = {},
                            navigationIcon = {
                                AsyncImage(
                                    model = R.drawable.void_logo,
                                    contentDescription = "Void Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(calculateRoundedValue(98).sdp)
                                        .fillMaxHeight()
                                )
                            },
                            actions = {
                                CastButton(
                                    modifier = Modifier.size(calculateRoundedValue(38).sdp)
                                )
                                IconButton(onClick = { onSearchClick() }) {
                                    AsyncImage(
                                        model = R.drawable.void_search,
                                        contentDescription = "Search",
                                        modifier = Modifier.size(calculateRoundedValue(28).sdp)
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    }

                    // Continue Watching — loaded first by the queue
                    if (resumeItems.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Continue watching") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(resumeItems) { mediaItem ->
                                        MediaItemCard(
                                            mediaItem = mediaItem,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            cardType = MediaCardType.THUMBNAIL,
                                            showProgress = true,
                                            showTitle = true,
                                            isWatched = playedItems.contains(mediaItem.id),
                                            shouldLoadImages = shouldLoadImages,
                                            bypassCache = false,
                                            authToken = accessToken,
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (sectionStates[HomeSection.CONTINUE_WATCHING].isLoadingOrQueued()) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(140).sdp,
                                cardType = MediaCardType.THUMBNAIL,
                                showTitle = true,
                                itemCount = 5
                            )
                        }
                    }

                    // Next Up — loaded second by the queue
                    if (nextUpItems.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Next Up"
                            ) {
                                val imageHelper = LocalImageHelper.current
                                val serverUrl = authState.authSession?.server?.url ?: ""
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(nextUpItems) { mediaItem ->
                                        val customImageUrl = remember(
                                            serverUrl,
                                            mediaItem.seriesId,
                                            mediaItem.backdropImageTags
                                        ) {
                                            val backdropTag = mediaItem.seriesPrimaryImageTag
                                            when {
                                                mediaItem.seriesId != null && backdropTag != null -> {
                                                    imageHelper.createBackdropUrl(
                                                        serverUrl,
                                                        mediaItem.seriesId,
                                                        backdropTag
                                                    )
                                                }

                                                else -> null
                                            }
                                        }
                                        MediaItemCard(
                                            mediaItem = mediaItem,
                                            serverUrl = serverUrl,
                                            cardType = MediaCardType.THUMBNAIL,
                                            showProgress = false,
                                            showTitle = true,
                                            customImageUrl = customImageUrl,
                                            isWatched = playedItems.contains(mediaItem.id),
                                            shouldLoadImages = shouldLoadImages,
                                            bypassCache = false,
                                            authToken = accessToken,
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (sectionStates[HomeSection.NEXT_UP].isLoadingOrQueued()) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(120).sdp,
                                cardType = MediaCardType.THUMBNAIL,
                                showTitle = true,
                                itemCount = 5
                            )
                        }
                    }

                    // Recently Added Movies — loaded third by the queue
                    if (latestMovies.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recently added Movies") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(latestMovies.take(10)) { movie ->
                                        MediaItemCard(
                                            mediaItem = movie,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            showProgress = false,
                                            isWatched = playedItems.contains(movie.id),
                                            bypassCache = false,
                                            authToken = accessToken,
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (sectionStates[HomeSection.LATEST_MOVIES].isLoadingOrQueued()) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(140).sdp,
                                cardType = MediaCardType.POSTER,
                                showTitle = true,
                                itemCount = 6
                            )
                        }
                    }

                    // Recently Added Shows — depends on LATEST_ITEMS queue step
                    if (recentlyAddedShows.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recently added Shows") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(recentlyAddedShows.take(10)) { show ->
                                        MediaItemCard(
                                            mediaItem = show,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            showProgress = false,
                                            isWatched = playedItems.contains(show.id),
                                            bypassCache = false,
                                            authToken = accessToken,
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (sectionStates[HomeSection.LATEST_ITEMS].isLoadingOrQueued()) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(140).sdp,
                                cardType = MediaCardType.POSTER,
                                showTitle = true,
                                itemCount = 6
                            )
                        }
                    }

                    // Recently Added Episodes — loaded fifth by the queue
                    if (latestEpisodes.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recently added Episodes") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(latestEpisodes.take(10)) { episode ->
                                        MediaItemCard(
                                            mediaItem = episode,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            showProgress = false,
                                            cardType = MediaCardType.THUMBNAIL,
                                            isWatched = playedItems.contains(episode.id),
                                            bypassCache = false,
                                            authToken = accessToken,
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (sectionStates[HomeSection.LATEST_EPISODES].isLoadingOrQueued()) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(110).sdp,
                                cardType = MediaCardType.THUMBNAIL,
                                showTitle = true,
                                itemCount = 6
                            )
                        }
                    }

                    // Recently Added per Library — loaded last by the queue
                    if (libraries.isNotEmpty()) {
                        libraries.forEach { library ->
                            val recentlyAddedItems = latestItemsByLibrary[library.id]
                            if (!recentlyAddedItems.isNullOrEmpty()) {
                                item(key = "recently_added_${library.id}") {
                                    SectionHeader(
                                        title = library.name,
                                    ) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                            contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                        ) {
                                            items(recentlyAddedItems.take(10), key = { it.id }) { mediaItem ->
                                                val cardType = when (mediaItem.type) {
                                                    ApiConstants.ITEM_TYPE_EPISODE -> MediaCardType.THUMBNAIL
                                                    else -> MediaCardType.POSTER
                                                }
                                                MediaItemCard(
                                                    mediaItem = mediaItem,
                                                    serverUrl = authState.authSession?.server?.url ?: "",
                                                    cardType = cardType,
                                                    showProgress = false,
                                                    showTitle = true,
                                                    isWatched = playedItems.contains(mediaItem.id),
                                                    shouldLoadImages = shouldLoadImages,
                                                    bypassCache = false,
                                                    authToken = accessToken,
                                                    onClick = onMediaItemClick
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (sectionStates[HomeSection.PER_LIBRARY].isLoadingOrQueued()) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(110).sdp,
                                cardType = MediaCardType.POSTER,
                                showTitle = true,
                                itemCount = 6,
                                showAction = true
                            )
                        }
                    }

                    // Favorites — loaded independently by UserDataViewModel
                    if (favorites.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Favorites") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(favorites) { mediaItem ->
                                        val cardType =
                                            if (mediaItem.type == ApiConstants.ITEM_TYPE_EPISODE) {
                                                MediaCardType.THUMBNAIL
                                            } else {
                                                MediaCardType.POSTER
                                            }
                                        MediaItemCard(
                                            mediaItem = mediaItem,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            showProgress = false,
                                            cardType = cardType,
                                            showTitle = true,
                                            isWatched = playedItems.contains(mediaItem.id),
                                            bypassCache = false,
                                            authToken = accessToken,
                                            onClick = { onMediaItemClick(it) }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isLoading) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(100).sdp,
                                cardType = MediaCardType.POSTER,
                                showTitle = true,
                                itemCount = 6
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(calculateRoundedValue(150).sdp))
                    }
                }
            }
        }
    }
}
