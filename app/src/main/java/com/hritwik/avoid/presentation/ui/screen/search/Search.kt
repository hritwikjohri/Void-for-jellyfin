package com.hritwik.avoid.presentation.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.hritwik.avoid.domain.model.category.CategoryProvider
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.input.RecentSearches
import com.hritwik.avoid.presentation.ui.components.input.SearchBar
import com.hritwik.avoid.presentation.ui.components.jellyseer.JellyseerSearchResultCard
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.CategoryCard
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.search.SearchViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.GestureHelper.swipeBack
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Search(
    onCategoryClick: (String) -> Unit = {},
    onMediaItemClick: (MediaItem) -> Unit = {},
    onJellyseerItemClick: (JellyseerSearchResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    authViewModel: AuthServerViewModel,
    searchViewModel: SearchViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.searchState.collectAsStateWithLifecycle()
    val searchFilters by searchViewModel.searchFilters.collectAsStateWithLifecycle()
    val playedItems by userDataViewModel.playedItems.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchResults = searchViewModel.searchResults.collectAsLazyPagingItems()
    val jellyseerResults = searchViewModel.jellyseerResults.collectAsLazyPagingItems()

    LaunchedEffect(authState.authSession) {
        authState.authSession?.let { session ->
            userDataViewModel.loadPlayedItems(session.userId.id, session.accessToken)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeBack(onBackClick)
    ) {
        AnimatedAmbientBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                item {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = calculateRoundedValue(16).sdp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Back",
                                modifier = Modifier.size(calculateRoundedValue(24).sdp),
                                tint = PrimaryText
                            )
                        }

                        SearchBar(
                            query = searchState.searchQuery,
                            onSearch = { query ->
                                searchViewModel.performImmediateSearch(query)
                            },
                            onQueryChange = {
                                searchViewModel.updateSearchQuery(it)
                                authState.authSession?.let { session ->
                                    searchViewModel.fetchSearchSuggestions(
                                        query = it,
                                        userId = session.userId.id,
                                        accessToken = session.accessToken
                                    )
                                }
                            },
                            onClear = {
                                searchViewModel.clearSearch()
                            },
                            onClearFocus = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            onFocusChange = { isFocused ->
                                if (!isFocused && searchState.searchQuery.isBlank()) {
                                    searchViewModel.clearSearch()
                                }
                            },
                            showJellyseerToggle = true,
                            isJellyseerActive = searchState.isJellyseerSearchEnabled,
                            isJellyseerToggleEnabled = searchState.isJellyseerConfigured,
                            onJellyseerToggle = { searchViewModel.toggleJellyseerSearch() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!searchState.isJellyseerSearchEnabled) {
                    item {
                        SectionHeader(
                            modifier = Modifier.padding(horizontal = calculateRoundedValue(6).sdp),
                            title = "Filters",
                            subtitle = "Refine results from your Jellyfin server"
                        ) {
                            SearchFilterRow(
                                filters = searchFilters,
                                onFiltersChange = {
                                    searchViewModel.updateFilters(it)
                                    searchViewModel.performImmediateSearch(query = searchState.searchQuery)
                                },
                                modifier = Modifier.padding(horizontal = calculateRoundedValue(10).sdp)
                            )
                        }
                    }
                }

                if (searchState.isSearchActive) {
                    if (!searchState.isJellyseerSearchEnabled && searchState.suggestions.isNotEmpty()) {
                        items(searchState.suggestions) { suggestion ->
                            Text(
                                text = suggestion,
                                color = PrimaryText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        authState.authSession?.let { session ->
                                            searchViewModel.updateSearchQuery(suggestion)
                                            searchViewModel.performImmediateSearch(query = suggestion)
                                            focusManager.clearFocus()
                                        }
                                    }
                                    .padding(
                                        horizontal = calculateRoundedValue(16).sdp,
                                        vertical = calculateRoundedValue(8).sdp
                                    )
                            )
                        }
                    }

                    if (!searchState.isJellyseerSearchEnabled && searchState.suggestionsError != null) {
                        item {
                            Text(
                                text = searchState.suggestionsError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                            )
                        }
                    }

                    if (searchState.isJellyseerSearchEnabled) {
                        when {
                            jellyseerResults.loadState.refresh is LoadState.Loading && jellyseerResults.itemCount == 0 -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(calculateRoundedValue(200).sdp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            jellyseerResults.itemCount == 0 && jellyseerResults.loadState.refresh !is LoadState.Loading -> {
                                if (!searchState.jellyseerError.isNullOrBlank()) {
                                    item {
                                        Text(
                                            text = searchState.jellyseerError.orEmpty(),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                                        )
                                    }
                                }
                                item {
                                    EmptySearchResults(
                                        query = searchState.searchQuery,
                                        onDismiss = {
                                            searchViewModel.clearSearch()
                                            focusManager.clearFocus()
                                        }
                                    )
                                }
                            }

                            else -> {
                                if (!searchState.jellyseerError.isNullOrBlank()) {
                                    item {
                                        Text(
                                            text = searchState.jellyseerError.orEmpty(),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp)
                                        )
                                    }
                                }

                                val jellyseerCount = jellyseerResults.itemCount
                                if (jellyseerCount > 0) {
                                    item {
                                        SectionHeader(
                                            modifier = Modifier.padding(horizontal = calculateRoundedValue(6).sdp),
                                            title = "Jellyseer results",
                                            subtitle = buildString {
                                                append("$jellyseerCount result")
                                                if (jellyseerCount != 1) append('s')
                                                if (searchState.searchQuery.isNotBlank()) {
                                                    append(" for \"")
                                                    append(searchState.searchQuery)
                                                    append('\"')
                                                }
                                            }
                                        )
                                    }
                                }

                                items(jellyseerResults.itemCount) { index ->
                                    val result = jellyseerResults[index] ?: return@items
                                    JellyseerSearchResultCard(
                                        result = result,
                                        onClick = { onJellyseerItemClick(result) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = calculateRoundedValue(16).sdp)
                                    )
                                }

                                if (jellyseerResults.loadState.append is LoadState.Loading) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(calculateRoundedValue(16).sdp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(calculateRoundedValue(24).sdp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (searchResults.loadState.refresh is LoadState.Loading && searchResults.itemCount == 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(calculateRoundedValue(200).sdp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
                                    Text(
                                        text = "Searching...",
                                        color = PrimaryText
                                    )
                                }
                            }
                        }
                    } else if (searchResults.itemCount == 0 && searchResults.loadState.refresh !is LoadState.Loading) {
                        item {
                            EmptySearchResults(
                                query = searchState.searchQuery,
                                onDismiss = {
                                    searchViewModel.clearSearch()
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    } else if (!searchState.isJellyseerSearchEnabled) {
                        val results = searchResults.itemSnapshotList.items
                        val nonEpisodes = results.filter { it.type != "Episode" }
                        val episodes = results.filter { it.type == "Episode" }
                        val serverUrl = authState.authSession?.server?.url.orEmpty()

                        item {
                            Text(
                                text = "${searchResults.itemCount} result${if (searchResults.itemCount != 1) "s" else ""} for \"${searchState.searchQuery}\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryText,
                                modifier = Modifier.padding(horizontal = calculateRoundedValue(10).sdp)
                            )
                        }

                        if (nonEpisodes.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Movies & Series")
                            }

                            items(nonEpisodes.chunked(3)) { mediaItemRow ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = calculateRoundedValue(16).sdp),
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                                ) {
                                    mediaItemRow.forEach { mediaItem ->
                                        val cardType =
                                            if (mediaItem.type == "Season" || mediaItem.type == "Series" || mediaItem.type == "Movie") {
                                                MediaCardType.POSTER
                                            } else {
                                                MediaCardType.THUMBNAIL
                                            }

                                        MediaItemCard(
                                            mediaItem = mediaItem,
                                            serverUrl = serverUrl,
                                            cardType = cardType,
                                            showProgress = false,
                                            isWatched = playedItems.contains(mediaItem.id),
                                            onClick = onMediaItemClick,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    repeat(3 - mediaItemRow.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (episodes.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Episodes")
                            }

                            items(episodes.chunked(2)) { mediaItemRow ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = calculateRoundedValue(16).sdp),
                                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                                ) {
                                    mediaItemRow.forEach { mediaItem ->
                                        MediaItemCard(
                                            mediaItem = mediaItem,
                                            serverUrl = serverUrl,
                                            showProgress = false,
                                            cardType = MediaCardType.THUMBNAIL,
                                            isWatched = playedItems.contains(mediaItem.id),
                                            onClick = onMediaItemClick,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (mediaItemRow.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (searchResults.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(calculateRoundedValue(16).sdp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(calculateRoundedValue(24).sdp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (searchState.recentSearches.isNotEmpty()) {
                        item {
                            RecentSearches(
                                modifier = Modifier.padding(horizontal = calculateRoundedValue(16).sdp),
                                recentSearches = searchState.recentSearches,
                                onSearchClick = { query ->
                                    searchViewModel.performImmediateSearch(query)
                                },
                                onClearAll = { searchViewModel.clearRecentSearches() }
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Browse by Category",
                            subtitle = "Discover content by genre and type"
                        )
                    }

                    items(CategoryProvider.getCategories().chunked(2)) { categoryRow ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = calculateRoundedValue(16).sdp),
                            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                        ) {
                            categoryRow.forEach { category ->
                                CategoryCard(
                                    category = category,
                                    onClick = { onCategoryClick(it.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (categoryRow.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
