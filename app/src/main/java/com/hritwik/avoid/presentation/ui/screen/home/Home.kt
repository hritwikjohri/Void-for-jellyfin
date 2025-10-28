package com.hritwik.avoid.presentation.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.hritwik.avoid.presentation.ui.components.common.FeaturedPagerSection
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.CollectionItemCard
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.navigation.Routes
import com.hritwik.avoid.presentation.ui.screen.downloads.DownloadsScreen
import com.hritwik.avoid.presentation.ui.screen.home.components.HomeFeaturedPlaceholder
import com.hritwik.avoid.presentation.ui.screen.home.components.HomeSectionPlaceholder
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPlayClick: (MediaItem) -> Unit = {},
    onMediaItemClick: (MediaItem) -> Unit = {},
    onSearchClick: () -> Unit = {},
    navController: NavHostController,
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel,
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()
    val homeSettings by userDataViewModel.homeSettings.collectAsStateWithLifecycle()
    val showFeaturedHeader = homeSettings.showFeaturedHeader
    val favorites by userDataViewModel.favorites.collectAsStateWithLifecycle()
    val playedItems by userDataViewModel.playedItems.collectAsStateWithLifecycle()
    val refreshResumeItems =
        navController.currentBackStackEntry?.savedStateHandle
            ?.getStateFlow("refreshResumeItems", false)
            ?.collectAsStateWithLifecycle()
            ?.value
    val isNetworkAvailable by userDataViewModel.isConnected.collectAsStateWithLifecycle()
    val currentUserId = authState.authSession?.userId?.id
    val activeServerUrl = authState.authSession?.server?.url ?: authState.serverUrl ?: ""
    val shouldUseMergedHomeRequest = remember(activeServerUrl) {
        activeServerUrl.startsWith("https://", ignoreCase = true)
    }
    var hasRequestedInitialLibraryData by rememberSaveable(currentUserId, activeServerUrl) {
        mutableStateOf(false)
    }
    var hasRequestedLatestItems by rememberSaveable(currentUserId, activeServerUrl) {
        mutableStateOf(false)
    }
    var hasLoadedUserData by rememberSaveable(currentUserId, activeServerUrl) { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(currentUserId, activeServerUrl) {
        hasRequestedInitialLibraryData = false
        hasRequestedLatestItems = false
        hasLoadedUserData = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val updatedSessionState = rememberUpdatedState(authState.authSession)

    LaunchedEffect(authState.authSession, currentUserId, activeServerUrl) {
        authState.authSession?.let { session ->
            val userId = session.userId.id
            val accessToken = session.accessToken

            if (!hasRequestedInitialLibraryData && libraryState.libraries.isEmpty()) {
                hasRequestedInitialLibraryData = true
                if (shouldUseMergedHomeRequest) {
                    hasRequestedLatestItems = true
                }
                libraryViewModel.loadLibraries(
                    userId = userId,
                    accessToken = accessToken,
                    mergeHomeRequests = shouldUseMergedHomeRequest
                )
            }

            if (!shouldUseMergedHomeRequest && !hasRequestedLatestItems && libraryState.latestItems.isEmpty()) {
                hasRequestedLatestItems = true
                libraryViewModel.loadLatestAdditions(
                    userId = userId,
                    accessToken = accessToken
                )
            }

            if (!hasLoadedUserData) {
                userDataViewModel.loadFavorites(userId, accessToken)
                userDataViewModel.loadPlayedItems(userId, accessToken)
                hasLoadedUserData = true
            }

            libraryViewModel.startResumeSync(userId, accessToken)
        }
            ?: run {
                libraryViewModel.reset()
                libraryViewModel.stopResumeSync()
                userDataViewModel.reset()
                hasRequestedInitialLibraryData = false
                hasRequestedLatestItems = false
                hasLoadedUserData = false
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            libraryViewModel.stopResumeSync()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatedSessionState.value?.let { session ->
                    val userId = session.userId.id
                    val accessToken = session.accessToken
                    libraryViewModel.refreshResumeItems(userId, accessToken)
                    libraryViewModel.refreshNextUpItems(userId, accessToken)
                }
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(refreshResumeItems) {
        if (refreshResumeItems == true) {
            navController.currentBackStackEntry?.savedStateHandle?.set("refreshResumeItems", false)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!isNetworkAvailable) {
            DownloadsScreen(
                showBackButton = false,
                onPlay = { mediaItem ->
                    navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            )
        } else {
            AnimatedAmbientBackground {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                ) {
                    when (showFeaturedHeader) {
                        true -> {
                            item {
                                when {
                                    libraryState.latestMovies.isNotEmpty() -> {
                                        FeaturedPagerSection(
                                            mediaItems = libraryState.latestMovies.take(5),
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            onPlayClick = onPlayClick,
                                            onMediaClick = onMediaItemClick,
                                            onSearchClick = onSearchClick,
                                        )
                                    }

                                    libraryState.isLoading -> {
                                        HomeFeaturedPlaceholder()
                                    }
                                }
                            }
                        }
                        false -> item {
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Unspecified,
                                    navigationIconContentColor = Color.Unspecified,
                                    titleContentColor = MaterialTheme.colorScheme.primary,
                                    actionIconContentColor = Color.Unspecified
                                ),
                                title = {

                                },
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
                                    IconButton(onClick = { onSearchClick() }) {
                                        AsyncImage(
                                            model = R.drawable.void_search,
                                            contentDescription = "Search",
                                            modifier = Modifier.size(calculateRoundedValue(24).sdp)
                                        )
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    }

                    if (libraryState.resumeItems.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Continue Watching"
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(libraryState.resumeItems) { mediaItem ->
                                        MediaItemCard(
                                            mediaItem = mediaItem,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            cardType = MediaCardType.THUMBNAIL,
                                            showProgress = true,
                                            showTitle = true,
                                            isWatched = playedItems.contains(mediaItem.id),
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (libraryState.isLoading) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(140).sdp,
                                cardType = MediaCardType.THUMBNAIL,
                                showTitle = true,
                                itemCount = 5
                            )
                        }
                    }

                    if (libraryState.nextUpItems.isNotEmpty()) {
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
                                    items(libraryState.nextUpItems) { mediaItem ->
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
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (libraryState.isLoading) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(120).sdp,
                                cardType = MediaCardType.THUMBNAIL,
                                showTitle = true,
                                itemCount = 5
                            )
                        }
                    }

                    if (libraryState.latestMovies.isNotEmpty()) {
                        item {
                            SectionHeader (
                                title = "Movies",
                            ){
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(libraryState.latestMovies.take(10)) { movie ->
                                        MediaItemCard(
                                            mediaItem = movie,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            showProgress = false,
                                            isWatched = playedItems.contains(movie.id),
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (libraryState.isLoading) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(90).sdp,
                                cardType = MediaCardType.POSTER,
                                showTitle = true,
                                itemCount = 6
                            )
                        }
                    }

                    if (libraryState.recentlyReleasedShows.isNotEmpty()) {
                        item {
                            SectionHeader (
                                title = "Shows",
                            ){
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(libraryState.latestItems.filter { it.type == "Series" }.take(10)) { show ->
                                        MediaItemCard(
                                            mediaItem = show,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            showProgress = false,
                                            isWatched = playedItems.contains(show.id),
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (libraryState.isLoading) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(90).sdp,
                                cardType = MediaCardType.POSTER,
                                showTitle = true,
                                itemCount = 6
                            )
                        }
                    }

                    if (libraryState.latestEpisodes.isNotEmpty()) {
                        item {
                            SectionHeader (
                                title = "Recently Added Episodes",
                            ){
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(libraryState.latestEpisodes.filter { it.type == "Episode" }.take(10)) { episode ->
                                        MediaItemCard(
                                            mediaItem = episode,
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            showProgress = false,
                                            cardType = MediaCardType.THUMBNAIL,
                                            isWatched = playedItems.contains(episode.id),
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (libraryState.isLoading) {
                        item {
                            HomeSectionPlaceholder(
                                titleWidth = calculateRoundedValue(110).sdp,
                                cardType = MediaCardType.THUMBNAIL,
                                showTitle = true,
                                itemCount = 6
                            )
                        }
                    }

                    if (libraryState.collections.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Collections",
                                actionButton = {
                                    TextButton(onClick = { navController.navigate(Routes.COLLECTIONS) }) {
                                        Text(text = "View all")
                                    }
                                }
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                                    contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
                                ) {
                                    items(libraryState.collections) { collection ->
                                        val previewItems = libraryState.collectionPreviews[collection.id].orEmpty()
                                        CollectionItemCard(
                                            collection = collection,
                                            previewItems = previewItems,
                                            modifier = Modifier.padding(horizontal = 8.sdp),
                                            serverUrl = authState.authSession?.server?.url ?: "",
                                            onClick = onMediaItemClick
                                        )
                                    }
                                }
                            }
                        }
                    } else if (libraryState.isLoading) {
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

                    if (favorites.isNotEmpty())     {
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
                                            onClick = { onMediaItemClick(it) }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (libraryState.isLoading) {
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