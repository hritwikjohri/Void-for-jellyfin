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
import com.hritwik.avoid.domain.usecase.library.GetMoviesUseCase
import com.hritwik.avoid.domain.usecase.library.GetLibraryItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetLibraryGenresUseCase
import com.hritwik.avoid.domain.usecase.library.GetHomeScreenDataUseCase
import com.hritwik.avoid.domain.usecase.library.GetNextUpUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecentlyReleasedMoviesUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecentlyReleasedShowsUseCase
import com.hritwik.avoid.domain.usecase.library.GetRecommendedItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetResumeItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetShowsUseCase
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.library.GetUserLibrariesUseCase
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.presentation.ui.state.HomeSection
import com.hritwik.avoid.presentation.ui.state.HomeSectionLoadState
import com.hritwik.avoid.presentation.ui.state.LibraryItemsState
import com.hritwik.avoid.presentation.ui.state.LibraryState
import com.hritwik.avoid.presentation.ui.state.LibraryGenresState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val getMoviesUseCase: GetMoviesUseCase,
    private val getShowsUseCase: GetShowsUseCase,
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

    companion object {
        const val HOME_SECTION_LIMIT = 8  // Smaller first paint to keep batch fast; fetch more later as needed
    }

    private var homeContentJob: Job? = null

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    private val _itemsState = MutableStateFlow(LibraryItemsState())
    val itemsState: StateFlow<LibraryItemsState> = _itemsState.asStateFlow()

    private val _genresState = MutableStateFlow<Map<String, LibraryGenresState>>(emptyMap())
    val genresState: StateFlow<Map<String, LibraryGenresState>> = _genresState.asStateFlow()

    data class LibrarySortPreference(
        val sortId: String,
        val sortDirection: LibrarySortDirection
    )

    private val _librarySortPreferences = MutableStateFlow<Map<String, LibrarySortPreference>>(emptyMap())
    val librarySortPreferences: StateFlow<Map<String, LibrarySortPreference>> =
        _librarySortPreferences.asStateFlow()

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

    fun reset() {
        homeContentJob?.cancel()
        homeContentJob = null
        _libraryState.value = LibraryState()
        _itemsState.value = LibraryItemsState()
        _genresState.value = emptyMap()
        clearAllLibrarySortPreferences()
        pagerCache.clear()
        _pagerGeneration.update { it + 1 }
    }

    fun invalidateLibraryPagerCache() {
        pagerCache.clear()
        _pagerGeneration.update { it + 1 }
    }

    fun updateLibrarySortPreference(
        libraryId: String,
        sortId: String,
        sortDirection: LibrarySortDirection
    ) {
        _librarySortPreferences.update { current ->
            val existing = current[libraryId]
            if (existing?.sortId == sortId && existing.sortDirection == sortDirection) {
                current
            } else {
                current + (libraryId to LibrarySortPreference(sortId, sortDirection))
            }
        }
    }

    fun getLibrarySortPreference(libraryId: String): LibrarySortPreference? {
        return _librarySortPreferences.value[libraryId]
    }

    fun clearLibrarySortPreference(libraryId: String) {
        _librarySortPreferences.update { current -> current - libraryId }
    }

    private fun clearAllLibrarySortPreferences() {
        _librarySortPreferences.value = emptyMap()
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

    private fun updateSectionState(section: HomeSection, state: HomeSectionLoadState) {
        _libraryState.update { current ->
            current.copy(sectionStates = current.sectionStates + (section to state))
        }
    }

    /**
     * Loads home screen content using a FIFO queue system.
     * Sections load sequentially in priority order so the most important
     * content (Continue Watching) appears first, then Next Up, and so on.
     */
    fun loadHomeContentQueued(
        userId: String,
        accessToken: String,
        sectionLimit: Int = HOME_SECTION_LIMIT
    ) {
        homeContentJob?.cancel()
        homeContentJob = viewModelScope.launch {
            _libraryState.update { it.copy(isLoading = true, error = null, sectionStates = emptyMap()) }

            // Load library list first (fast, needed for per-library section later)
            val librariesResult = getUserLibrariesUseCase(
                GetUserLibrariesUseCase.Params(userId, accessToken)
            )
            val libs = when (librariesResult) {
                is NetworkResult.Success -> librariesResult.data
                is NetworkResult.Error -> {
                    println("Failed to load libraries: ${librariesResult.message}")
                    _libraryState.value.libraries
                }
                is NetworkResult.Loading -> _libraryState.value.libraries
            }
            _libraryState.update { it.copy(libraries = libs) }

            val queue = HomeContentLoadQueue(::updateSectionState)

            // 1. Continue Watching — highest priority, loads first
            queue.enqueue(HomeSection.CONTINUE_WATCHING) {
                when (val result = getResumeItemsUseCase(
                    GetResumeItemsUseCase.Params(userId, accessToken, limit = sectionLimit)
                )) {
                    is NetworkResult.Success ->
                        _libraryState.update { it.copy(resumeItems = result.data) }
                    is NetworkResult.Error ->
                        println("Failed to load resume items: ${result.message}")
                    is NetworkResult.Loading -> {}
                }
            }

            // 2. Next Up — second priority
            queue.enqueue(HomeSection.NEXT_UP) {
                when (val result = getNextUpUseCase(
                    GetNextUpUseCase.Params(userId, accessToken, limit = sectionLimit)
                )) {
                    is NetworkResult.Success ->
                        _libraryState.update { it.copy(nextUpItems = result.data) }
                    is NetworkResult.Error ->
                        println("Failed to load next up items: ${result.message}")
                    is NetworkResult.Loading -> {}
                }
            }

            // 3. Recently Added Movies
            queue.enqueue(HomeSection.LATEST_MOVIES) {
                when (val result = getLatestMoviesUseCase(
                    GetLatestMoviesUseCase.Params(userId, accessToken, limit = sectionLimit)
                )) {
                    is NetworkResult.Success ->
                        _libraryState.update { it.copy(latestMovies = result.data) }
                    is NetworkResult.Error ->
                        println("Failed to load latest movies: ${result.message}")
                    is NetworkResult.Loading -> {}
                }
            }

            // 4. Latest Items (used for Recently Added Shows filter in UI)
            queue.enqueue(HomeSection.LATEST_ITEMS) {
                when (val result = getLatestItemsUseCase(
                    GetLatestItemsUseCase.Params(userId, accessToken, sectionLimit)
                )) {
                    is NetworkResult.Success ->
                        _libraryState.update { it.copy(latestItems = result.data) }
                    is NetworkResult.Error ->
                        println("Failed to load latest items: ${result.message}")
                    is NetworkResult.Loading -> {}
                }
            }

            // 5. Recently Added Episodes
            queue.enqueue(HomeSection.LATEST_EPISODES) {
                when (val result = getLatestEpisodesUseCase(
                    GetLatestEpisodesUseCase.Params(userId, accessToken, limit = sectionLimit)
                )) {
                    is NetworkResult.Success ->
                        _libraryState.update { it.copy(latestEpisodes = result.data) }
                    is NetworkResult.Error ->
                        println("Failed to load latest episodes: ${result.message}")
                    is NetworkResult.Loading -> {}
                }
            }

            // 6. Recently Added per Library — lowest priority, loads last
            queue.enqueue(HomeSection.PER_LIBRARY) {
                if (_libraryState.value.libraries.isNotEmpty()) {
                    loadRecentlyAddedByLibraryInternal(userId, accessToken, sectionLimit)
                }
            }

            // Process the queue — sections load one at a time in FIFO order
            queue.processAll()

            _libraryState.update { it.copy(isLoading = false) }

            // After all visible sections loaded, fetch remaining data in background
            // (recommended, recently released, etc. for other screens)
            loadBackgroundHomeData(userId, accessToken, sectionLimit)
        }
    }

    /**
     * Loads data that isn't directly visible on the home screen but may be
     * used by other screens. Fires all requests in parallel after the
     * queue-based visible content has finished loading.
     */
    private suspend fun loadBackgroundHomeData(
        userId: String,
        accessToken: String,
        sectionLimit: Int
    ) {
        coroutineScope {
            val recommendedDeferred = async {
                getRecommendedItemsUseCase(
                    GetRecommendedItemsUseCase.Params(userId, accessToken, limit = sectionLimit)
                )
            }
            val recentMoviesDeferred = async {
                getRecentlyReleasedMoviesUseCase(
                    GetRecentlyReleasedMoviesUseCase.Params(userId, accessToken, limit = sectionLimit)
                )
            }
            val recentShowsDeferred = async {
                getRecentlyReleasedShowsUseCase(
                    GetRecentlyReleasedShowsUseCase.Params(userId, accessToken, limit = sectionLimit)
                )
            }

            val recommendedResult = recommendedDeferred.await()
            val recentMoviesResult = recentMoviesDeferred.await()
            val recentShowsResult = recentShowsDeferred.await()

            _libraryState.update { state ->
                state.copy(
                    recommendedItems = when (recommendedResult) {
                        is NetworkResult.Success -> recommendedResult.data
                        else -> state.recommendedItems
                    },
                    recentlyReleasedMovies = when (recentMoviesResult) {
                        is NetworkResult.Success -> recentMoviesResult.data
                        else -> state.recentlyReleasedMovies
                    },
                    recentlyReleasedShows = when (recentShowsResult) {
                        is NetworkResult.Success -> recentShowsResult.data
                        else -> state.recentlyReleasedShows
                    }
                )
            }
        }
    }

    /**
     * Suspend version of loadRecentlyAddedByLibrary for use within the queue.
     * Loads all library items in parallel within this single queue step.
     */
    private suspend fun loadRecentlyAddedByLibraryInternal(
        userId: String,
        accessToken: String,
        limit: Int
    ) {
        val libraries = _libraryState.value.libraries
        if (libraries.isEmpty()) return

        val recentlyAddedMap = coroutineScope {
            libraries.map { library ->
                async {
                    when (val result = libraryRepository.getLatestItemsByLibrary(
                        userId = userId,
                        libraryId = library.id,
                        accessToken = accessToken,
                        limit = limit
                    )) {
                        is NetworkResult.Success -> library.id to result.data
                        is NetworkResult.Error -> {
                            println("Failed to load recently added for library ${library.name}: ${result.message}")
                            library.id to emptyList()
                        }
                        is NetworkResult.Loading -> library.id to emptyList()
                    }
                }
            }.awaitAll().toMap()
        }

        _libraryState.update { state ->
            state.copy(latestItemsByLibrary = recentlyAddedMap.filterValues { it.isNotEmpty() })
        }
    }

    fun loadLibraries(
        userId: String,
        accessToken: String,
        mergeHomeRequests: Boolean = false,
        sectionLimit: Int = HOME_SECTION_LIMIT,  // Keep above-the-fold home fetch within ~5s
        prefetchSecondaryContent: Boolean = true
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
                                movies = data.movies,
                                shows = data.shows,
                                recentlyReleasedMovies = data.recentlyReleasedMovies,
                                recentlyReleasedShows = data.recentlyReleasedShows,
                                recommendedItems = data.recommendedItems,
                                collections = data.collections
                            )
                        }

                        if (prefetchSecondaryContent && data.collections.isNotEmpty()) {
                            loadCollectionPreviews(
                                userId = userId,
                                accessToken = accessToken,
                                collections = data.collections
                            )
                        }

                        // Load recently added items per library
                        if (prefetchSecondaryContent && data.libraries.isNotEmpty()) {
                            loadRecentlyAddedByLibrary(userId, accessToken, sectionLimit)
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
                // Kick off all home calls together so total wall time is bound by the slowest call
                val librariesDeferred = async {
                    getUserLibrariesUseCase(GetUserLibrariesUseCase.Params(userId, accessToken))
                }
                val latestItemsDeferred = async {
                    getLatestItemsUseCase(GetLatestItemsUseCase.Params(userId, accessToken, sectionLimit))
                }
                val resumeItemsDeferred = async {
                    getResumeItemsUseCase(GetResumeItemsUseCase.Params(userId, accessToken, limit = sectionLimit))
                }
                val latestMoviesDeferred = async {
                    getLatestMoviesUseCase(GetLatestMoviesUseCase.Params(userId, accessToken, limit = sectionLimit))
                }
                val recommendedItemsDeferred = async {
                    getRecommendedItemsUseCase(
                        GetRecommendedItemsUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            limit = sectionLimit
                        )
                    )
                }
                // Secondary sections: start immediately, but await after UI is unblocked
                val latestEpisodesDeferred = async {
                    getLatestEpisodesUseCase(GetLatestEpisodesUseCase.Params(userId, accessToken, limit = sectionLimit))
                }
                val moviesDeferred = if (prefetchSecondaryContent) {
                    async { getMoviesUseCase(GetMoviesUseCase.Params(userId, accessToken, limit = sectionLimit)) }
                } else null
                val showsDeferred = if (prefetchSecondaryContent) {
                    async { getShowsUseCase(GetShowsUseCase.Params(userId, accessToken, limit = sectionLimit)) }
                } else null
                val recentlyReleasedMoviesDeferred = async {
                    getRecentlyReleasedMoviesUseCase(
                        GetRecentlyReleasedMoviesUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            limit = sectionLimit
                        )
                    )
                }
                val recentlyReleasedShowsDeferred = async {
                    getRecentlyReleasedShowsUseCase(
                        GetRecentlyReleasedShowsUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            limit = sectionLimit
                        )
                    )
                }
                val collectionsDeferred = if (prefetchSecondaryContent) async {
                    getCollectionsUseCase(
                        GetCollectionsUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            limit = sectionLimit
                        )
                    )
                } else null
                val nextUpDeferred = async {
                    getNextUpUseCase(GetNextUpUseCase.Params(userId, accessToken, limit = sectionLimit))
                }

                // Await critical results
                val librariesResult = librariesDeferred.await()
                val latestItemsResult = latestItemsDeferred.await()
                val resumeItemsResult = resumeItemsDeferred.await()
                val latestMoviesResult = latestMoviesDeferred.await()
                val errors = mutableListOf<String>()

                // Process critical results and update state
                val libraries = when (librariesResult) {
                    is NetworkResult.Success -> librariesResult.data
                    is NetworkResult.Error -> {
                        println("Failed to load libraries: ${librariesResult.message}")
                        errors += "Failed to load libraries: ${librariesResult.message}"
                        _libraryState.value.libraries
                    }
                    is NetworkResult.Loading -> _libraryState.value.libraries
                }

                val latestItems = when (latestItemsResult) {
                    is NetworkResult.Success -> latestItemsResult.data
                    is NetworkResult.Error -> {
                        println("Failed to load latest items: ${latestItemsResult.message}")
                        _libraryState.value.latestItems
                    }
                    is NetworkResult.Loading -> _libraryState.value.latestItems
                }

                val resumeItems = when (resumeItemsResult) {
                    is NetworkResult.Success -> resumeItemsResult.data
                    is NetworkResult.Error -> {
                        println("Failed to load resume items: ${resumeItemsResult.message}")
                        _libraryState.value.resumeItems
                    }
                    is NetworkResult.Loading -> _libraryState.value.resumeItems
                }

                val latestMovies = when (latestMoviesResult) {
                    is NetworkResult.Success -> latestMoviesResult.data
                    is NetworkResult.Error -> {
                        println("Failed to load latest movies: ${latestMoviesResult.message}")
                        _libraryState.value.latestMovies
                    }
                    is NetworkResult.Loading -> _libraryState.value.latestMovies
                }

                // CRITICAL: Update state with critical data and unblock UI rendering early
                _libraryState.update { state ->
                    state.copy(
                        isLoading = false, // UI unblocks here!
                        libraries = libraries,
                        latestItems = latestItems,
                        resumeItems = resumeItems,
                        latestMovies = latestMovies,
                        error = errors.joinToString(separator = "\n").ifBlank { null }
                    )
                }

                // Await background results (they were started above, so they overlap with the critical phase)
                val latestEpisodesResult = latestEpisodesDeferred.await()
                val moviesResult = moviesDeferred?.await()
                val showsResult = showsDeferred?.await()
                val recentlyReleasedMoviesResult = recentlyReleasedMoviesDeferred.await()
                val recentlyReleasedShowsResult = recentlyReleasedShowsDeferred.await()
                val collectionsResult = collectionsDeferred?.await()
                val nextUpResult = nextUpDeferred.await()
                val recommendedItemsResult = recommendedItemsDeferred.await()

                val collectionsData = (collectionsResult as? NetworkResult.Success)?.data

                // Update state with background data
                _libraryState.update { state ->
                    val latestEpisodes = when (latestEpisodesResult) {
                        is NetworkResult.Success -> latestEpisodesResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load latest episodes: ${latestEpisodesResult.message}")
                            state.latestEpisodes
                        }
                        is NetworkResult.Loading -> state.latestEpisodes
                    }

                    val movies = when (moviesResult) {
                        is NetworkResult.Success -> moviesResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load movies: ${moviesResult.message}")
                            state.movies
                        }
                        is NetworkResult.Loading, null -> state.movies
                    }

                    val shows = when (showsResult) {
                        is NetworkResult.Success -> showsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load shows: ${showsResult.message}")
                            state.shows
                        }
                        is NetworkResult.Loading, null -> state.shows
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

                    val nextUpItems = when (nextUpResult) {
                        is NetworkResult.Success -> nextUpResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load next up items: ${nextUpResult.message}")
                            state.nextUpItems
                        }
                        is NetworkResult.Loading -> state.nextUpItems
                    }

                    val collections = when (collectionsResult) {
                        is NetworkResult.Success -> collectionsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load collections: ${collectionsResult.message}")
                            state.collections
                        }
                        is NetworkResult.Loading, null -> state.collections
                    }

                    val recommendedItems = when (recommendedItemsResult) {
                        is NetworkResult.Success -> recommendedItemsResult.data
                        is NetworkResult.Error -> {
                            println("Failed to load recommended items: ${recommendedItemsResult.message}")
                            state.recommendedItems
                        }
                        is NetworkResult.Loading -> state.recommendedItems
                    }

                    state.copy(
                        latestEpisodes = latestEpisodes,
                        movies = movies,
                        shows = shows,
                        recentlyReleasedMovies = recentlyReleasedMovies,
                        recentlyReleasedShows = recentlyReleasedShows,
                        nextUpItems = nextUpItems,
                        collections = collections,
                        recommendedItems = recommendedItems
                    )
                }

                if (prefetchSecondaryContent) {
                    collectionsData?.let {
                        loadCollectionPreviews(
                            userId = userId,
                            accessToken = accessToken,
                            collections = it
                        )
                    }
                }

                // Load recently added items per library
                if (prefetchSecondaryContent) {
                    val currentLibraries = _libraryState.value.libraries
                    if (currentLibraries.isNotEmpty()) {
                        loadRecentlyAddedByLibrary(userId, accessToken, sectionLimit)
                    }
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

        // Lazy loading: Load collection previews in batches to reduce initial load time
        viewModelScope.launch {
            val batchSize = 3 // Load 3 collections at a time
            collectionsToFetch.chunked(batchSize).forEach { batch ->
                val previewResults = batch.map { collection ->
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

                // Small delay between batches to avoid overwhelming the server
                if (batch != collectionsToFetch.chunked(batchSize).last()) {
                    kotlinx.coroutines.delay(100)
                }
            }
        }
    }

    fun loadLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String,
        supportsAlphaScroller: Boolean = false
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

            // Load all items for alpha scroller (if supported)
            if (supportsAlphaScroller) {
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
                            // Sanitize items - filter out blank names
                            val sanitizedItems = allResult.data.filter { item ->
                                item.name.isNotBlank() && item.name.trim().isNotEmpty()
                            }

                            // Build sectioned grid items with headers
                            val (sectionedItems, headerIndices) =
                                com.hritwik.avoid.utils.helpers.AlphaScrollHelper.buildSectionedGridItems(sanitizedItems)

                            _itemsState.value = _itemsState.value.copy(
                                items = allResult.data.take(pageSize),
                                hasMorePages = allResult.data.size > pageSize,
                                allTitles = allResult.data.map { it.name },
                                sectionedItems = sectionedItems,
                                sectionHeaderIndices = headerIndices
                            )
                        }

                        else -> {}
                    }
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
            when (val result = getLatestItemsUseCase(GetLatestItemsUseCase.Params(userId, accessToken, 10))) {
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

    fun refreshResumeItems(userId: String, accessToken: String, limit: Int = HOME_SECTION_LIMIT) {
        viewModelScope.launch {
            when (val result = getResumeItemsUseCase(
                GetResumeItemsUseCase.Params(
                    userId,
                    accessToken,
                    limit = limit
                )
            )) {
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

    fun refreshNextUpItems(userId: String, accessToken: String, limit: Int = HOME_SECTION_LIMIT) {
        viewModelScope.launch {
            when (val result = getNextUpUseCase(
                GetNextUpUseCase.Params(
                    userId,
                    accessToken,
                    limit = limit
                )
            )) {
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

    fun loadRecentlyAddedByLibrary(userId: String, accessToken: String, limit: Int = HOME_SECTION_LIMIT) {
        viewModelScope.launch {
        val libraries = _libraryState.value.libraries
        if (libraries.isEmpty()) return@launch

        // Load recently added items for all libraries in parallel
        val recentlyAddedMap = libraries.map { library ->
            async {
                when (val result = libraryRepository.getLatestItemsByLibrary(
                    userId = userId,
                    libraryId = library.id,
                    accessToken = accessToken,
                    limit = limit
                )) {
                    is NetworkResult.Success -> library.id to result.data
                    is NetworkResult.Error -> {
                        println("Failed to load recently added for library ${library.name}: ${result.message}")
                        library.id to emptyList()
                    }
                    is NetworkResult.Loading -> library.id to emptyList()
                }
            }
        }.awaitAll().toMap()

        _libraryState.update { state ->
            state.copy(latestItemsByLibrary = recentlyAddedMap.filterValues { it.isNotEmpty() })
        }
        }
    }

    fun refreshHomeScreen(userId: String, accessToken: String) {
        viewModelScope.launch {
            // Refresh all home screen sections in parallel
            val resumeDeferred = async { refreshResumeItems(userId, accessToken) }
            val nextUpDeferred = async { refreshNextUpItems(userId, accessToken) }
            // Wait for critical sections
            listOf(resumeDeferred, nextUpDeferred).awaitAll()
        }
    }

    fun prefetchSecondaryHomeContent(
        userId: String,
        accessToken: String,
        sectionLimit: Int = HOME_SECTION_LIMIT
    ) {
        viewModelScope.launch {
            val existingCollections = _libraryState.value.collections
            if (existingCollections.isNotEmpty()) {
                // We already have collections; just make sure previews are hydrated
                loadCollectionPreviews(userId, accessToken, existingCollections)
            } else {
                val collectionsResult = getCollectionsUseCase(
                    GetCollectionsUseCase.Params(
                        userId,
                        accessToken,
                        limit = sectionLimit
                    )
                )

                if (collectionsResult is NetworkResult.Success) {
                    _libraryState.update { state ->
                        state.copy(collections = collectionsResult.data)
                    }
                    loadCollectionPreviews(userId, accessToken, collectionsResult.data)
                }
            }

            val currentLibraries = _libraryState.value.libraries
            if (currentLibraries.isNotEmpty()) {
                loadRecentlyAddedByLibrary(userId, accessToken, sectionLimit)
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
