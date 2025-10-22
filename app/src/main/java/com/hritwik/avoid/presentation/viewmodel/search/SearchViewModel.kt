package com.hritwik.avoid.presentation.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.jellyseer.JellyseerConfig
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.provider.AuthSessionProvider
import com.hritwik.avoid.domain.usecase.search.GetSearchSuggestionsUseCase
import com.hritwik.avoid.domain.usecase.search.SearchItemsUseCase
import com.hritwik.avoid.domain.usecase.jellyseer.SearchJellyseerMediaUseCase
import com.hritwik.avoid.presentation.ui.state.SearchFilters
import com.hritwik.avoid.presentation.ui.state.SearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchItemsUseCase: SearchItemsUseCase,
    private val authSessionProvider: AuthSessionProvider,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val preferencesManager: PreferencesManager,
    private val searchJellyseerMediaUseCase: SearchJellyseerMediaUseCase
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _useJellyseer = MutableStateFlow(false)
    private val _jellyseerConfig = MutableStateFlow(JellyseerConfig())
    private var lastSearchedQuery: String = ""
    private var authSession: AuthSession? = null

    private val searchDebounceMs = 300L

    val searchResults = combine(_searchQuery, _searchFilters, _useJellyseer) { query, filters, useJellyseer ->
        Triple(query, filters, useJellyseer)
    }
        .debounce(searchDebounceMs)
        .filter { (query, _, useJellyseer) -> query.isNotBlank() && query.length >= 2 && !useJellyseer }
        .distinctUntilChanged()
        .flatMapLatest { (query, filters, _) ->
            val session = authSession ?: return@flatMapLatest emptyFlow()
            Pager(PagingConfig(pageSize = 50)) {
                SearchPagingSource(
                    searchItemsUseCase = searchItemsUseCase,
                    userId = session.userId,
                    accessToken = session.accessToken,
                    query = query,
                    itemTypes = filters.toItemTypes()
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    val jellyseerResults = combine(_searchQuery, _useJellyseer) { query, useJellyseer ->
        query to useJellyseer
    }
        .debounce(searchDebounceMs)
        .filter { (query, useJellyseer) -> useJellyseer && query.isNotBlank() && query.length >= 2 }
        .distinctUntilChanged()
        .flatMapLatest { (query, _) ->
            Pager(PagingConfig(pageSize = 20)) {
                JellyseerSearchPagingSource(
                    searchUseCase = searchJellyseerMediaUseCase,
                    query = query,
                    onError = { setJellyseerError(it) },
                    onSuccess = { clearJellyseerError() }
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    init {
        authSessionProvider.authSession
            .onEach { session ->
                authSession = session
            }
            .launchIn(viewModelScope)

        preferencesManager.getRecentSearches()
            .onEach { searches ->
                _searchState.value = _searchState.value.copy(recentSearches = searches)
            }
            .launchIn(viewModelScope)

        preferencesManager.getJellyseerConfig()
            .onEach { config ->
                _jellyseerConfig.value = config
                if (!config.isConfigured && _useJellyseer.value) {
                    _useJellyseer.value = false
                }
                _searchState.update { state ->
                    state.copy(
                        isJellyseerConfigured = config.isConfigured,
                        isJellyseerSearchEnabled = _useJellyseer.value && config.isConfigured,
                        jellyseerError = state.jellyseerError?.takeIf { config.isConfigured }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchState.update { state ->
            val baseState = state.copy(
                searchQuery = query,
                isSearchActive = query.isNotBlank(),
                suggestionsError = null,
                isJellyseerSearchEnabled = _useJellyseer.value && _jellyseerConfig.value.isConfigured
            )
            if (_useJellyseer.value) {
                baseState.copy(suggestions = emptyList())
            } else {
                baseState.copy(suggestions = if (query.isBlank()) emptyList() else state.suggestions)
            }
        }
        if (query.isBlank()) {
            lastSearchedQuery = ""
            clearJellyseerError()
        }
    }

    fun fetchSearchSuggestions(query: String, userId: String, accessToken: String, limit: Int = 10) {
        if (_useJellyseer.value) {
            return
        }
        if (query.isBlank()) {
            _searchState.value = _searchState.value.copy(suggestions = emptyList(), suggestionsError = null)
            return
        }
        viewModelScope.launch {
            when (val result = getSearchSuggestionsUseCase(
                GetSearchSuggestionsUseCase.Params(
                    userId = userId,
                    accessToken = accessToken,
                    query = query,
                    limit = limit
                )
            )) {
                is NetworkResult.Success -> {
                    _searchState.value = _searchState.value.copy(
                        suggestions = result.data,
                        suggestionsError = null
                    )
                }
                is NetworkResult.Error -> {
                    _searchState.value = _searchState.value.copy(
                        suggestions = emptyList(),
                        suggestionsError = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun performImmediateSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        _searchQuery.value = trimmed
        clearJellyseerError()
        _searchState.update { state ->
            state.copy(
                searchQuery = trimmed,
                isSearchActive = true,
                suggestions = emptyList(),
                suggestionsError = null,
                isJellyseerSearchEnabled = _useJellyseer.value && _jellyseerConfig.value.isConfigured
            )
        }
        if (trimmed.length >= 3 && trimmed != lastSearchedQuery) {
            addToRecentSearches(trimmed)
            lastSearchedQuery = trimmed
        }
    }

    fun toggleJellyseerSearch() {
        val config = _jellyseerConfig.value
        if (!config.isConfigured) {
            setJellyseerError("Configure Jellyseer integration in the Jellyseer settings tab to enable external search.")
            return
        }
        val newValue = !_useJellyseer.value
        _useJellyseer.value = newValue
        _searchState.update { state ->
            state.copy(
                isJellyseerSearchEnabled = newValue,
                suggestions = if (newValue) emptyList() else state.suggestions,
                suggestionsError = null,
                jellyseerError = null
            )
        }
        _searchQuery.value = _searchQuery.value
    }

    fun updateFilters(filters: SearchFilters) {
        _searchFilters.value = filters
        if (_searchQuery.value.isNotBlank()) {
            _searchQuery.value = _searchQuery.value
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        lastSearchedQuery = ""
        _searchState.update { state ->
            state.copy(
                searchQuery = "",
                suggestions = emptyList(),
                suggestionsError = null,
                isSearchActive = false,
                error = null,
                jellyseerError = null,
                isJellyseerSearchEnabled = _useJellyseer.value && _jellyseerConfig.value.isConfigured
            )
        }
    }

    fun dismissSearch() {
        _searchQuery.value = ""
        lastSearchedQuery = ""
        _useJellyseer.value = false
        _searchState.value = SearchState(
            recentSearches = _searchState.value.recentSearches,
            isJellyseerConfigured = _jellyseerConfig.value.isConfigured,
            isJellyseerSearchEnabled = false
        )
    }

    fun clearError() {
        _searchState.value = _searchState.value.copy(error = null)
    }

    fun selectRecentSearch(query: String) {
        updateSearchQuery(query)
        performImmediateSearch(query)
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferencesManager.clearRecentSearches()
        }
        _searchState.value = _searchState.value.copy(recentSearches = emptyList())
    }

    fun deactivateSearch() {
        if (_searchState.value.searchQuery.isBlank()) {
            _searchState.value = _searchState.value.copy(
                isSearchActive = false,
                error = null
            )
        }
    }

    fun activateSearch() {
        if (_searchState.value.searchQuery.isNotBlank()) {
            _searchState.value = _searchState.value.copy(isSearchActive = true)
        }
    }

    private fun setJellyseerError(message: String) {
        _searchState.update { it.copy(jellyseerError = message) }
    }

    private fun clearJellyseerError() {
        _searchState.update { it.copy(jellyseerError = null) }
    }

    private fun addToRecentSearches(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 3) return

        val currentSearches = _searchState.value.recentSearches.toMutableList()
        currentSearches.removeAll { it.equals(trimmedQuery, ignoreCase = true) }
        currentSearches.add(0, trimmedQuery)
        if (currentSearches.size > 24) {
            currentSearches.subList(24, currentSearches.size).clear()
        }
        _searchState.value = _searchState.value.copy(recentSearches = currentSearches)
        viewModelScope.launch {
            preferencesManager.saveRecentSearches(currentSearches)
        }
    }
}

private class SearchPagingSource(
    private val searchItemsUseCase: SearchItemsUseCase,
    private val userId: User,
    private val accessToken: String,
    private val query: String,
    private val itemTypes: List<String>
) : androidx.paging.PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val startIndex = params.key ?: 0
        return when (val result = searchItemsUseCase(
            SearchItemsUseCase.Params(
                userId = userId,
                accessToken = accessToken,
                searchTerm = query,
                includeItemTypes = itemTypes,
                startIndex = startIndex,
                limit = params.loadSize
            )
        )) {
            is NetworkResult.Success -> {
                val nextKey = if (result.data.isEmpty()) null else startIndex + result.data.size
                LoadResult.Page(
                    data = result.data,
                    prevKey = if (startIndex == 0) null else maxOf(0, startIndex - params.loadSize),
                    nextKey = nextKey
                )
            }
            is NetworkResult.Error -> LoadResult.Error(Throwable(result.message))
            is NetworkResult.Loading -> LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        }
    }

    override fun getRefreshKey(state: androidx.paging.PagingState<Int, MediaItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null

        anchorPage.nextKey?.let { nextKey ->
            val calculatedStart = nextKey - anchorPage.data.size
            if (calculatedStart >= 0) {
                return calculatedStart
            }
        }

        anchorPage.prevKey?.let { prevKey ->
            return maxOf(0, prevKey + state.config.pageSize)
        }

        return null
    }
}

private class JellyseerSearchPagingSource(
    private val searchUseCase: SearchJellyseerMediaUseCase,
    private val query: String,
    private val onError: (String) -> Unit,
    private val onSuccess: () -> Unit
) : androidx.paging.PagingSource<Int, JellyseerSearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, JellyseerSearchResult> {
        val page = params.key ?: 1
        return when (val result = searchUseCase(
            SearchJellyseerMediaUseCase.Params(
                query = query,
                page = page
            )
        )) {
            is NetworkResult.Success -> {
                onSuccess()
                val data = result.data.results
                val nextKey = if (page >= result.data.totalPages || data.isEmpty()) null else page + 1
                LoadResult.Page(
                    data = data,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            }
            is NetworkResult.Error -> {
                onError(result.message)
                LoadResult.Error(Throwable(result.message))
            }
            is NetworkResult.Loading -> LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        }
    }

    override fun getRefreshKey(state: androidx.paging.PagingState<Int, JellyseerSearchResult>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null

        return anchorPage.prevKey?.plus(1) ?: anchorPage.nextKey?.minus(1)
    }
}
