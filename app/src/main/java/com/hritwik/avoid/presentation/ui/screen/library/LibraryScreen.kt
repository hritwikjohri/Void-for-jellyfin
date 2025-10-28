package com.hritwik.avoid.presentation.ui.screen.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.ContentGrid
import com.hritwik.avoid.presentation.ui.components.common.ErrorDisplay
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyLibraryState
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.state.LibraryGenresState
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel

@Composable
fun LibraryScreen(
    libraryId: String,
    libraryName: String,
    onBackClick: () -> Unit = {},
    onMediaItemClick: (MediaItem) -> Unit = {},
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberSaveable(libraryId, saver = LazyGridState.Saver) { LazyGridState() }
    val genresState by libraryViewModel.genresState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()
    val sortPreferences by libraryViewModel.librarySortPreferences.collectAsStateWithLifecycle()
    val currentGenresState = genresState[libraryId] ?: LibraryGenresState()
    val savedSortPreference = sortPreferences[libraryId]

    var selectedSortId by rememberSaveable(libraryId) {
        mutableStateOf(savedSortPreference?.sortId ?: LibrarySortOptions.SortName.id)
    }
    val selectedSortOption = remember(selectedSortId) {
        LibrarySortOptions.All.firstOrNull { it.id == selectedSortId } ?: LibrarySortOptions.SortName
    }
    var sortDirectionName by rememberSaveable(libraryId) {
        mutableStateOf(
            savedSortPreference?.sortDirection?.name ?: selectedSortOption.defaultDirection.name
        )
    }
    LaunchedEffect(savedSortPreference) {
        savedSortPreference?.let { preference ->
            if (selectedSortId != preference.sortId) {
                selectedSortId = preference.sortId
            }
            val preferenceDirection = preference.sortDirection.name
            if (sortDirectionName != preferenceDirection) {
                sortDirectionName = preferenceDirection
            }
        }
    }
    LaunchedEffect(selectedSortId, sortDirectionName) {
        val direction = runCatching { LibrarySortDirection.valueOf(sortDirectionName) }
            .getOrDefault(selectedSortOption.defaultDirection)
        val preference = libraryViewModel.getLibrarySortPreference(libraryId)
        if (preference?.sortId != selectedSortId || preference.sortDirection != direction) {
            libraryViewModel.updateLibrarySortPreference(
                libraryId = libraryId,
                sortId = selectedSortId,
                sortDirection = direction
            )
        }
    }
    val sortDirection = runCatching { LibrarySortDirection.valueOf(sortDirectionName) }
        .getOrDefault(selectedSortOption.defaultDirection)
    var selectedGenre by rememberSaveable(libraryId) { mutableStateOf<String?>(null) }

    LaunchedEffect(libraryId, authState.authSession) {
        authState.authSession?.let { session ->
            libraryViewModel.loadLibraryGenres(
                userId = session.userId.id,
                libraryId = libraryId,
                accessToken = session.accessToken
            )
        }
    }

    var previousSortId by remember { mutableStateOf(selectedSortId) }
    var previousSortDirection by remember { mutableStateOf(sortDirectionName) }
    var previousGenre by remember { mutableStateOf(selectedGenre) }
    var hasRecordedInitialSelection by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSortId, sortDirectionName, selectedGenre) {
        if (hasRecordedInitialSelection) {
            val hasSelectionChanged =
                selectedSortId != previousSortId ||
                        sortDirectionName != previousSortDirection ||
                        selectedGenre != previousGenre

            if (hasSelectionChanged) {
                gridState.scrollToItem(0)
            }
        } else {
            hasRecordedInitialSelection = true
        }

        previousSortId = selectedSortId
        previousSortDirection = sortDirectionName
        previousGenre = selectedGenre
    }

    val pagerGeneration by libraryViewModel.pagerGeneration.collectAsStateWithLifecycle()

    val handleBack = remember(onBackClick, libraryViewModel, libraryId) {
        {
            libraryViewModel.invalidateLibraryPagerCache()
            libraryViewModel.clearLibrarySortPreference(libraryId)
            onBackClick()
        }
    }

    BackHandler(onBack = handleBack)

    val pagingItems = authState.authSession?.let { session ->
        val pager = remember(
            libraryId,
            session.userId.id,
            session.accessToken,
            selectedSortOption.id,
            sortDirectionName,
            selectedGenre,
            pagerGeneration
        ) {
            libraryViewModel.libraryItemsPager(
                userId = session.userId.id,
                libraryId = libraryId,
                accessToken = session.accessToken,
                sortBy = selectedSortOption.sortFields,
                sortOrder = sortDirection,
                genre = selectedGenre
            )
        }
        pager.collectAsLazyPagingItems()
    }

    AnimatedAmbientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = libraryName,
                showBackButton = true,
                onBackClick = handleBack,
                showSortBar = true,
                sortOptions = LibrarySortOptions.All,
                selectedSort = selectedSortOption,
                sortDirection = sortDirection,
                onSortSelected = { option ->
                    selectedSortId = option.id
                    sortDirectionName = option.defaultDirection.name
                },
                onSortDirectionChange = { direction -> sortDirectionName = direction.name },
                genres = currentGenresState.genres,
                selectedGenre = selectedGenre,
                onGenreSelected = { genre -> selectedGenre = genre },
                isGenresLoading = currentGenresState.isLoading,
                genreError = currentGenresState.error,
                onRetryGenres = {
                    authState.authSession?.let { session ->
                        libraryViewModel.loadLibraryGenres(
                            userId = session.userId.id,
                            libraryId = libraryId,
                            accessToken = session.accessToken,
                            forceRefresh = true
                        )
                    }
                }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    pagingItems == null || pagingItems.loadState.refresh is LoadState.Loading -> {
                        LoadingState()
                    }
                    pagingItems.itemCount == 0 -> {
                        EmptyLibraryState(libraryName)
                    }
                    else -> {
                        ContentGrid(
                            items = pagingItems,
                            gridState = gridState,
                            serverUrl = authState.authSession?.server?.url ?: "",
                            onMediaItemClick = onMediaItemClick,
                            showAlphaScroller = selectedSortOption.supportsAlphaScroller,
                            collectionPreviewProvider = { item ->
                                libraryState.collectionPreviews[item.id].orEmpty()
                            }
                        )
                    }
                }
            }

            if (pagingItems != null && pagingItems.loadState.refresh is LoadState.Error) {
                val message = (pagingItems.loadState.refresh as LoadState.Error).error.message ?: ""
                ErrorDisplay(
                    error = message,
                    onDismiss = { pagingItems.retry() }
                )
            }
        }
    }
}
