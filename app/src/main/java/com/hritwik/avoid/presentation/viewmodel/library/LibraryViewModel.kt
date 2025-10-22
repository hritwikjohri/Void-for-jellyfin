package com.hritwik.avoid.presentation.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.library.GetCollectionItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetCollectionsUseCase
import com.hritwik.avoid.domain.usecase.library.GetLatestEpisodesUseCase
import com.hritwik.avoid.domain.usecase.library.GetLatestItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetLatestMoviesUseCase
import com.hritwik.avoid.domain.usecase.library.GetLibraryItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetLibraryGenresUseCase
import com.hritwik.avoid.domain.usecase.library.GetHomeScreenDataUseCase
import com.hritwik.avoid.domain.usecase.library.GetNextUpUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecentlyReleasedMoviesUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecentlyReleasedShowsUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecommendedItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetResumeItemsUseCase
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.library.GetUserLibrariesUseCase
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.presentation.ui.state.LibraryItemsState
import com.hritwik.avoid.presentation.ui.state.LibraryState
import com.hritwik.avoid.presentation.ui.state.LibraryGenresState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryItemsUseCase: GetLibraryItemsUseCase,
    private val getUserLibrariesUseCase: GetUserLibrariesUseCase,
    private val getLatestItemsUseCase: GetLatestItemsUseCase,
    private val getResumeItemsUseCase: GetResumeItemsUseCase,
    private val getLatestEpisodesUseCase: GetLatestEpisodesUseCase,
    private val getLatestMoviesUseCase: GetLatestMoviesUseCase,
    private val getRecentlyReleasedMoviesUseCase: GetRecentlyReleasedMoviesUseCase,
    private val getRecentlyReleasedShowsUseCase: GetRecentlyReleasedShowsUseCase,
    private val getRecommendedItemsUseCase: GetRecommendedItemsUseCase,
    private val getLibraryGenresUseCase: GetLibraryGenresUseCase,
    private val libraryRepository: LibraryRepository,
    private val getNextUpUseCase: GetNextUpUseCase,
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val getCollectionItemsUseCase: GetCollectionItemsUseCase,
    private val getHomeScreenDataUseCase: GetHomeScreenDataUseCase
) : ViewModel() {

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    private val _itemsState = MutableStateFlow(LibraryItemsState())

    private val _genresState = MutableStateFlow<Map<String, LibraryGenresState>>(emptyMap())
    val genresState: StateFlow<Map<String, LibraryGenresState>> = _genresState.asStateFlow()

    private val pageSize = 100
    private val collectionPreviewLimit = 3
    private data class PagerKey(
        val userId: String,
        val libraryId: String,
        val accessToken: String,
        val sortBy: List<String>,
        val sortOrder: LibrarySortDirection,
        val genre: String?,
        val generation: Int
    )
    private val pagerCache = mutableMapOf<PagerKey, Flow<PagingData<MediaItem>>>()
    private val _pagerGeneration = MutableStateFlow(0)
    val pagerGeneration: StateFlow<Int> = _pagerGeneration.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepository.resumeItemsFlow.collect { items ->
                _libraryState.value = _libraryState.value.copy(resumeItems = items)
            }
        }
        viewModelScope.launch {
            libraryRepository.nextUpItemsFlow.collect { items ->
                _libraryState.update { state ->
                    state.copy(nextUpItems = items)
                }
            }
        }
    }

    fun startResumeSync(userId: String, accessToken: String) {
        libraryRepository.startResumeItemsSync(userId, accessToken)
    }

    fun stopResumeSync() {
        libraryRepository.stopResumeItemsSync()
    }

    fun reset() {
        _libraryState.value = LibraryState()
        _itemsState.value = LibraryItemsState()
        _genresState.value = emptyMap()
        pagerCache.clear()
        _pagerGeneration.update { it + 1 }
    }

    fun invalidateLibraryPagerCache() {
        pagerCache.clear()
        _pagerGeneration.update { it + 1 }
    }

    fun loadLibraryGenres(
        userId: String,
        libraryId: String,
        accessToken: String,
        forceRefresh: Boolean = false
    ) {
        val current = _genresState.value[libraryId]
        if (!forceRefresh && current != null && current.genres.isNotEmpty()) return

        viewModelScope.launch {
            _genresState.update { state ->
                state + (libraryId to LibraryGenresState(isLoading = true, genres = current?.genres.orEmpty()))
            }

            when (val result = getLibraryGenresUseCase(
                GetLibraryGenresUseCase.Params(
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken
                )
            )) {
                is NetworkResult.Success -> {
                    _genresState.update { state ->
                        state + (libraryId to LibraryGenresState(genres = result.data))
                    }
                }

                is NetworkResult.Error -> {
                    _genresState.update { state ->
                        state + (libraryId to LibraryGenresState(
                            genres = current?.genres.orEmpty(),
                            error = result.message
                        ))
                    }
                }

                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    fun loadLibraries(
        userId: String,
        accessToken: String,
        mergeHomeRequests: Boolean = false,
        sectionLimit: Int = 20
    ) {
        viewModelScope.launch {
            _libraryState.update { it.copy(isLoading = true, error = null) }

            if (mergeHomeRequests) {
                when (val result = getHomeScreenDataUseCase(
                    GetHomeScreenDataUseCase.Params(
                        userId = userId,
                        accessToken = accessToken,
                        sectionLimit = sectionLimit
                    )
                )) {
                    is NetworkResult.Success -> {
                        val data = result.data
                        val combinedError = data.errors.joinToString(separator = "\n").ifBlank { null }
                        _libraryState.update { state ->
                            state.copy(
                                isLoading = false,
                                error = combinedError,
                                libraries = data.libraries,
                                latestItems = data.latestItems,
                                resumeItems = data.resumeItems,
                                nextUpItems = data.nextUpItems,
                                latestEpisodes = data.latestEpisodes,
                                latestMovies = data.latestMovies,
                                recentlyReleasedMovies = data.recentlyReleasedMovies,
                                recentlyReleasedShows = data.recentlyReleasedShows,
                                recommendedItems = data.recommendedItems,
                                collections = data.collections
                            )
                        }

                        if (data.collections.isNotEmpty()) {
                            loadCollectionPreviews(
                                userId = userId,
                                accessToken = accessToken,
                                collections = data.collections
                            )
                        }
                    }

                    is NetworkResult.Error -> {
                        _libraryState.update { state ->
                            state.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }

                    is NetworkResult.Loading -> {
                        
                    }
                }
                return@launch
            }

            try {
                
                val librariesDeferred = async {
                    getUserLibrariesUseCase(GetUserLibrariesUseCase.Params(userId, accessToken))
                }
                val latestItemsDeferred = async {
                    getLatestItemsUseCase(GetLatestItemsUseCase.Params(userId, accessToken))
                }
                val resumeItemsDeferred = async {
                    getResumeItemsUseCase(GetResumeItemsUseCase.Params(userId, accessToken))
                }
                val latestEpisodesDeferred = async {
                    getLatestEpisodesUseCase(GetLatestEpisodesUseCase.Params(userId, accessToken))
                }
                val latestMoviesDeferred = async {
                    getLatestMoviesUseCase(GetLatestMoviesUseCase.Params(userId, accessToken))
                }
                val nextUpDeferred = async {
                    getNextUpUseCase(GetNextUpUseCase.Params(userId, accessToken))
                }
                val recentlyReleasedMoviesDeferred = async {
                    getRecentlyReleasedMoviesUseCase(
                        GetRecentlyReleasedMoviesUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }
                val recentlyReleasedShowsDeferred = async {
                    getRecentlyReleasedShowsUseCase(
                        GetRecentlyReleasedShowsUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }
                val recommendedItemsDeferred = async {
                    getRecommendedItemsUseCase(
                        GetRecommendedItemsUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }
                val collectionsDeferred = async {
                    getCollectionsUseCase(
                        GetCollectionsUseCase.Params(
                            userId,
                            accessToken
                        )
                    )
                }

                
                val librariesResult = librariesDeferred.await()
                val latestItemsResult = latestItemsDeferred.await()
                val resumeItemsResult = resumeItemsDeferred.await()
                val latestEpisodesResult = latestEpisodesDeferred.await()
                val latestMoviesResult = latestMoviesDeferred.await()
                val nextUpResult = nextUpDeferred.await()
                val recentlyReleasedMoviesResult = recentlyReleasedMoviesDeferred.await()
                val recentlyReleasedShowsResult = recentlyReleasedShowsDeferred.await()
                val recommendedItemsResult = recommendedItemsDeferred.await()
                val collectionsResult = collectionsDeferred.await()

                val collectionsData = (collectionsResult as? NetworkResult.Success)?.data
                val errors = mutableListOf<String>()

                _libraryState.update { state ->
                    val libraries = when (librariesResult) {
                        is NetworkResult.Success -> librariesResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load libraries: ${librariesResult.message}")
                            errors += "Failed to load libraries: ${librariesResult.message}"
                            state.libraries
                        }

                        is NetworkResult.Loading -> state.libraries
                    }

                    val latestItems = when (latestItemsResult) {
                        is NetworkResult.Success -> latestItemsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load latest items: ${latestItemsResult.message}")
                            state.latestItems
                        }

                        is NetworkResult.Loading -> state.latestItems
                    }

                    val resumeItems = when (resumeItemsResult) {
                        is NetworkResult.Success -> resumeItemsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load resume items: ${resumeItemsResult.message}")
                            state.resumeItems
                        }

                        is NetworkResult.Loading -> state.resumeItems
                    }

                    val nextUpItems = when (nextUpResult) {
                        is NetworkResult.Success -> nextUpResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load next up items: ${nextUpResult.message}")
                            state.nextUpItems
                        }

                        is NetworkResult.Loading -> state.nextUpItems
                    }

                    val latestEpisodes = when (latestEpisodesResult) {
                        is NetworkResult.Success -> latestEpisodesResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load latest episodes: ${latestEpisodesResult.message}")
                            state.latestEpisodes
                        }

                        is NetworkResult.Loading -> state.latestEpisodes
                    }

                    val latestMovies = when (latestMoviesResult) {
                        is NetworkResult.Success -> latestMoviesResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load latest movies: ${latestMoviesResult.message}")
                            state.latestMovies
                        }

                        is NetworkResult.Loading -> state.latestMovies
                    }

                    val recentlyReleasedMovies = when (recentlyReleasedMoviesResult) {
                        is NetworkResult.Success -> recentlyReleasedMoviesResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load recently released movies: ${recentlyReleasedMoviesResult.message}")
                            state.recentlyReleasedMovies
                        }

                        is NetworkResult.Loading -> state.recentlyReleasedMovies
                    }

                    val recentlyReleasedShows = when (recentlyReleasedShowsResult) {
                        is NetworkResult.Success -> recentlyReleasedShowsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load recently released shows: ${recentlyReleasedShowsResult.message}")
                            state.recentlyReleasedShows
                        }

                        is NetworkResult.Loading -> state.recentlyReleasedShows
                    }

                    val recommendedItems = when (recommendedItemsResult) {
                        is NetworkResult.Success -> recommendedItemsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load recommended items: ${recommendedItemsResult.message}")
                            state.recommendedItems
                        }

                        is NetworkResult.Loading -> state.recommendedItems
                    }

                    val collections = when (collectionsResult) {
                        is NetworkResult.Success -> collectionsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load collections: ${collectionsResult.message}")
                            state.collections
                        }

                        is NetworkResult.Loading -> state.collections
                    }

                    val combinedError = errors.joinToString(separator = "\n").ifBlank { null }

                    state.copy(
                        isLoading = false,
                        error = combinedError,
                        libraries = libraries,
                        latestItems = latestItems,
                        resumeItems = resumeItems,
                        nextUpItems = nextUpItems,
                        latestEpisodes = latestEpisodes,
                        latestMovies = latestMovies,
                        recentlyReleasedMovies = recentlyReleasedMovies,
                        recentlyReleasedShows = recentlyReleasedShows,
                        recommendedItems = recommendedItems,
                        collections = collections
                    )
                }

                collectionsData?.let {
                    loadCollectionPreviews(
                        userId = userId,
                        accessToken = accessToken,
                        collections = it
                    )
                }
            } catch (e: Exception) {
                _libraryState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadCollectionPreviews(
        userId: String,
        accessToken: String,
        collections: List<MediaItem>
    ) {
        if (collections.isEmpty()) return

        val existingPreviews = _libraryState.value.collectionPreviews
        val collectionsToFetch = collections
            .filter { collection -> existingPreviews[collection.id] == null }

        if (collectionsToFetch.isEmpty()) return

        viewModelScope.launch {
            val previewResults = collectionsToFetch.map { collection ->
                async {
                    when (
                        val result = getCollectionItemsUseCase(
                            GetCollectionItemsUseCase.Params(
                                userId = userId,
                                accessToken = accessToken,
                                collectionId = collection.id,
                                limit = collectionPreviewLimit
                            )
                        )
                    ) {
                        is NetworkResult.Success -> collection.id to result.data.take(collectionPreviewLimit)
                        is NetworkResult.Error -> collection.id to emptyList()
                        is NetworkResult.Loading -> collection.id to emptyList()
                    }
                }
            }.awaitAll()

            if (previewResults.isNotEmpty()) {
                _libraryState.update { state ->
                    state.copy(collectionPreviews = state.collectionPreviews + previewResults)
                }
            }
        }
    }

    fun loadLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String
    ) {
        viewModelScope.launch {
            _itemsState.value = LibraryItemsState(isLoading = true)

            when (val firstResult = getLibraryItemsUseCase(
                GetLibraryItemsUseCase.Params(
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken,
                    startIndex = 0,
                    limit = pageSize
                )
            )) {
                is NetworkResult.Success -> {
                    _itemsState.value = LibraryItemsState(
                        items = firstResult.data,
                        hasMorePages = firstResult.data.size == pageSize,
                        currentPage = 0
                    )
                }

                is NetworkResult.Error -> {
                    _itemsState.value = LibraryItemsState(
                        isLoading = false,
                        error = firstResult.message
                    )
                }

                is NetworkResult.Loading -> {}
            }

            
            launch {
                when (val allResult = getLibraryItemsUseCase(
                    GetLibraryItemsUseCase.Params(
                        userId = userId,
                        libraryId = libraryId,
                        accessToken = accessToken,
                        startIndex = 0,
                        limit = Int.MAX_VALUE,
                        forceRefresh = true
                    )
                )) {
                    is NetworkResult.Success -> {
                        _itemsState.value = _itemsState.value.copy(
                            items = allResult.data.take(pageSize),
                            hasMorePages = allResult.data.size > pageSize,
                            allTitles = allResult.data.map { it.name }
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    fun loadMoreLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String
    ) {
        val state = _itemsState.value
        if (state.isLoading || !state.hasMorePages) return

        viewModelScope.launch {
            _itemsState.value = state.copy(isLoading = true)
            val startIndex = state.items.size
            when (val result = getLibraryItemsUseCase(
                GetLibraryItemsUseCase.Params(
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken,
                    startIndex = startIndex,
                    limit = pageSize
                )
            )) {
                is NetworkResult.Success -> {
                    val newItems = state.items + result.data
                    _itemsState.value = state.copy(
                        isLoading = false,
                        items = newItems,
                        hasMorePages = result.data.size == pageSize,
                        currentPage = state.currentPage + 1
                    )
                }
                is NetworkResult.Error -> {
                    _itemsState.value = state.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun loadLatestAdditions(userId: String, accessToken: String) {
        viewModelScope.launch {
            when (val result = getLatestItemsUseCase(GetLatestItemsUseCase.Params(userId, accessToken, 20))) {
                is NetworkResult.Success -> {
                    _libraryState.value = _libraryState.value.copy(
                        latestItems = result.data
                    )
                }
                is NetworkResult.Error -> {
                    println("Failed to refresh latest additions: ${result.message}")
                }
                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    fun refreshResumeItems(userId: String, accessToken: String) {
        viewModelScope.launch {
            when (val result = getResumeItemsUseCase(GetResumeItemsUseCase.Params(userId, accessToken))) {
                is NetworkResult.Success -> {
                    _libraryState.value = _libraryState.value.copy(
                        resumeItems = result.data
                    )
                }
                is NetworkResult.Error -> {
                    println("Failed to refresh resume items: ${result.message}")
                }
                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    fun refreshNextUpItems(userId: String, accessToken: String) {
        viewModelScope.launch {
            when (val result = getNextUpUseCase(GetNextUpUseCase.Params(userId, accessToken))) {
                is NetworkResult.Success -> {
                    _libraryState.value = _libraryState.value.copy(
                        nextUpItems = result.data
                    )
                }
                is NetworkResult.Error -> {
                    println("Failed to refresh next up items: ${result.message}")
                }
                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    fun clearError() {
        _itemsState.value = _itemsState.value.copy(error = null)
    }

    fun libraryItemsPager(
        userId: String,
        libraryId: String,
        accessToken: String,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        genre: String?
    ): Flow<PagingData<MediaItem>> {
        val key = PagerKey(
            userId,
            libraryId,
            accessToken,
            sortBy,
            sortOrder,
            genre,
            _pagerGeneration.value
        )
        return pagerCache.getOrPut(key) {
            Pager(PagingConfig(pageSize = pageSize)) {
                LibraryItemsPagingSource(
                    getLibraryItemsUseCase = getLibraryItemsUseCase,
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    genre = genre,
                    forceRefreshOnFirstLoad = true
                )
            }.flow.cachedIn(viewModelScope)
        }
    }
}