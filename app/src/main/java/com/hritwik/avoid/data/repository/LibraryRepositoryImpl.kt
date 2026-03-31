package com.hritwik.avoid.data.repository

import android.content.Context
import android.provider.Settings.Secure.ANDROID_ID
import android.provider.Settings.System.getString
import com.hritwik.avoid.data.common.BaseRepository
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.common.NetworkResult.Success
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.dao.LibraryDao
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.database.dao.PendingActionDao
import com.hritwik.avoid.data.local.database.entities.LibraryEntity
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import com.hritwik.avoid.data.local.database.entities.PendingActionEntity
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.data.remote.TmdbApiService
import com.hritwik.avoid.data.remote.dto.library.BaseItemDto
import com.hritwik.avoid.data.remote.dto.library.UserDataDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackCodecProfileDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackDeviceProfileDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackInfoRequestDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackProgressRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStartRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStopRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackSubtitleProfileDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackTranscodingProfileDto
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.mapper.PlaybackMapper
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.HomeScreenData
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.PendingAction
import com.hritwik.avoid.domain.model.library.Person
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.domain.model.playback.PlaybackStreamInfo
import com.hritwik.avoid.domain.model.playback.Segment
import com.hritwik.avoid.domain.model.playback.TranscodeRequestParameters
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.repository.RelatedResources
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.constants.AppConstants.DEFAULT_NEXT_UP_LIMIT
import com.hritwik.avoid.utils.constants.AppConstants.THRESHOLD
import com.hritwik.avoid.utils.constants.PreferenceConstants
import com.hritwik.avoid.utils.extensions.extractTvdbId
import com.hritwik.avoid.utils.helpers.normalizeUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.Normalizer
import retrofit2.Retrofit
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val preferencesManager: PreferencesManager,
    private val playbackMapper: PlaybackMapper,
    private val libraryDao: LibraryDao,
    private val mediaItemDao: MediaItemDao,
    private val pendingActionDao: PendingActionDao,
    private val downloadDao: DownloadDao,
    @param:ApplicationContext private val context: Context,
    private val continueWatchingStore: ContinueWatchingStore,
    private val nextUpStore: NextUpStore,
    private val serverConnectionManager: ServerConnectionManager,
    priorityDispatcher: PriorityDispatcher
) : BaseRepository(priorityDispatcher, serverConnectionManager), LibraryRepository {

    private val deviceId: String by lazy {
        getString(context.contentResolver, ANDROID_ID) ?: "unknown"
    }

    private val movieDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val json = Json { ignoreUnknownKeys = true }

    override val resumeItemsFlow: StateFlow<List<MediaItem>>
        get() = continueWatchingStore.items
    override val nextUpItemsFlow: StateFlow<List<MediaItem>>
        get() = nextUpStore.items

    private fun createApiService(serverUrl: String): JellyfinApiService {
        val baseUrl = if (!serverUrl.endsWith("/")) "$serverUrl/" else serverUrl
        return retrofitBuilder
            .baseUrl(baseUrl)
            .build()
            .create(JellyfinApiService::class.java)
    }

    private var tmdbApiService: TmdbApiService? = null

    private fun getTmdbApiService(): TmdbApiService {
        if (tmdbApiService == null) {
            tmdbApiService = retrofitBuilder
                .baseUrl("https://api.themoviedb.org/3/")
                .build()
                .create(TmdbApiService::class.java)
        }
        return tmdbApiService!!
    }

    override suspend fun getUserLibrariesFromCache(userId: String): List<Library> {
        return libraryDao.getAllLibraries(userId).first().map { it.toDomain() }
    }

    override suspend fun getUserLibraries(
        userId: String,
        accessToken: String
    ): NetworkResult<List<Library>> {
        if (serverConnectionManager.state.value.isOffline) {
            return NetworkResult.Error(AppError.Network("Server unreachable"))
        }

        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getUserLibraries(userId, authHeader)

            val libraries = response.items.map { dto ->
                mapToLibrary(dto)
            }

            libraryDao.insertLibraries(libraries.map { it.toEntity(userId) })

            libraries
        }
    }

    override suspend fun getLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String,
        startIndex: Int,
        limit: Int,
        forceRefresh: Boolean,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        genre: String?
    ): NetworkResult<List<MediaItem>> {
        // No DB caching for library items - always fetch from network (like TV version)
        // This prevents stale data and ANR issues
        // DB is only used for user actions (favorites, resume, etc.)

        if (serverConnectionManager.state.value.isOffline) {
            return NetworkResult.Error(AppError.Network("Server unreachable"))
        }

        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = libraryId,
                startIndex = startIndex,
                limit = limit,
                sortBy = sortBy.joinToString(","),
                sortOrder = sortOrder.toApiValue(),
                genres = genre,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getLibraryGenres(
        userId: String,
        libraryId: String,
        accessToken: String,
        sortOrder: LibrarySortDirection
    ): NetworkResult<List<String>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getGenres(
                userId = userId,
                parentId = libraryId,
                limit = 200,
                sortBy = "SortName",
                sortOrder = sortOrder.toApiValue(),
                enableImages = false,
                authorization = authHeader
            )

            response.items.mapNotNull { it.name }.distinct().sortedWith(
                if (sortOrder == LibrarySortDirection.ASCENDING) compareBy { it.lowercase() }
                else compareByDescending { it.lowercase() }
            )
        }
    }

    override suspend fun getHomeScreenData(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<HomeScreenData> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val errors = mutableListOf<String>()

            val resultPair = coroutineScope {
                val librariesDeferred = async {
                    try {
                        val response = apiService.getUserLibraries(userId, authHeader)
                        val libraries = response.items.map { dto -> mapToLibrary(dto) }
                        libraryDao.insertLibraries(libraries.map { it.toEntity(userId) })
                        Result.success(libraries)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val latestItemsDeferred = async {
                    try {
                        val items = apiService.getLatestItems(userId, limit, authorization = authHeader)
                            .map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val resumeItemsDeferred = async {
                    try {
                        val items = apiService.getResumeItems(userId, limit, authorization = authHeader)
                            .items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val nextUpItemsDeferred = async {
                    try {
                        val items = apiService.getNextUpItems(
                            userId = userId,
                            limit = limit,
                            authorization = authHeader
                        ).items.filter { it.type == "Episode" }
                            .map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val latestEpisodesDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Episode",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "DateCreated",
                            sortOrder = "Descending",
                            enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val homeData = HomeScreenData(
                    libraries = emptyList(),
                    latestItems = latestItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load latest items: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    resumeItems = resumeItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load resume items: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    nextUpItems = nextUpItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load next up items: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    latestEpisodes = latestEpisodesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load latest episodes: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    latestMovies = emptyList(),
                    movies = emptyList(),
                    shows = emptyList(),
                    recentlyReleasedMovies = emptyList(),
                    recentlyReleasedShows = emptyList(),
                    recommendedItems = emptyList(),
                    collections = emptyList(),
                    errors = emptyList()
                )

                homeData to librariesDeferred.await()
            }

            val (homeData, librariesResult) = resultPair
            val libraries = if (librariesResult.isSuccess) {
                librariesResult.getOrThrow()
            } else {
                errors += "Failed to load libraries: ${librariesResult.exceptionOrNull()?.message ?: "Unknown error"}"
                emptyList()
            }

            val aggregatedErrors = errors.toList()
            homeData.copy(
                libraries = libraries,
                errors = aggregatedErrors
            )
        }
    }

    override suspend fun getLatestItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLatestItems(userId, limit, authorization = authHeader)

            response.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getLatestItemsByLibrary(
        userId: String,
        libraryId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLatestItems(
                userId = userId,
                limit = limit,
                parentId = libraryId,
                authorization = authHeader
            )

            response.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getResumeItems(
        userId: String,
        accessToken: String,
        limit: Int,
        mediaTypes: String?,
        includeItemTypes: String?
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        val result = safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getResumeItems(
                userId = userId,
                limit = limit,
                mediaTypes = mediaTypes,
                includeItemTypes = includeItemTypes,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
        if (result is Success) {
            continueWatchingStore.setInitial(result.data)
        }
        return result
    }

    override suspend fun getLatestEpisodes(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Episode",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getLatestMovies(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getMovies(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getShows(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getRecentlyReleasedMovies(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val minPremiereDate = LocalDate.now().minusMonths(6).format(movieDateFormatter)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PremiereDate",
                sortOrder = "Descending",
                minPremiereDate = minPremiereDate,
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getRecentlyReleasedShows(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val minPremiereDate = LocalDate.now().minusMonths(3).format(movieDateFormatter)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PremiereDate",
                sortOrder = "Descending",
                minPremiereDate = minPremiereDate,
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getRecommendedItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)


            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie,Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "CommunityRating",
                sortOrder = "Descending",
                isPlayed = false,
                minCommunityRating = 7.0,
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getCollections(
        userId: String,
        accessToken: String,
        startIndex: Int,
        limit: Int,
        tags: List<String>?
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "BoxSet",
                recursive = true,
                tags = tags?.joinToString(","),
                startIndex = startIndex,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getCollectionItems(
        userId: String,
        accessToken: String,
        collectionId: String,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        startIndex: Int,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                parentId = collectionId,
                recursive = true,
                startIndex = startIndex,
                limit = limit,
                sortBy = sortBy.joinToString(","),
                sortOrder = sortOrder.toApiValue(),
                enableImageTypes = ApiConstants.IMAGE_TYPE_PRIMARY,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getTrendingItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)


            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie,Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PlayCount",
                sortOrder = "Descending",
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }


    override fun getFavoriteItems(userId: String): Flow<List<MediaItem>> {
        return mediaItemDao.getFavoriteItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWatchlistItems(userId: String): Flow<List<MediaItem>> {
        return mediaItemDao.getWatchlistItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPlayedItems(userId: String): Flow<List<MediaItem>> {
        return mediaItemDao.getPlayedItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFavoriteItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = listOf(
                    ApiConstants.ITEM_TYPE_MOVIE,
                    ApiConstants.ITEM_TYPE_SERIES,
                    ApiConstants.ITEM_TYPE_EPISODE
                ).joinToString(","),
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                isFavorite = true,
                enableImageTypes = "Primary,Backdrop,Thumb",
                fields = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData,SeriesName",
                authorization = authHeader
            )

            val items = response.items.map { dto ->
                mapToMediaItem(dto)
            }
            mediaItemDao.insertMediaItems(items.map { it.toEntity(null, userId) })
            items
        }
    }

    override suspend fun getPlayedItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = listOf(
                    ApiConstants.ITEM_TYPE_MOVIE,
                    ApiConstants.ITEM_TYPE_SERIES,
                    ApiConstants.ITEM_TYPE_EPISODE
                ).joinToString(","),
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DatePlayed",
                sortOrder = "Descending",
                isPlayed = true,
                enableImageTypes = "Primary,Backdrop,Thumb",
                fields = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData,SeriesName",
                authorization = authHeader
            )

            val items = response.items.map { dto ->
                mapToMediaItem(dto)
            }
            mediaItemDao.insertMediaItems(items.map { it.toEntity(null, userId) })
            items
        }
    }

    override suspend fun getItemsByCategory(
        userId: String,
        accessToken: String,
        filters: Map<String, String>,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection,
        startIndex: Int,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = filters["IncludeItemTypes"],
                genres = filters["Genres"],
                recursive = filters["Recursive"]?.toBoolean() ?: true,
                startIndex = startIndex,
                limit = limit,
                sortBy = sortBy.joinToString(","),
                sortOrder = sortOrder.toApiValue(),
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun searchItems(
        userId: User,
        accessToken: String,
        searchTerm: String,
        includeItemTypes: String?,
        startIndex: Int,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val normalizedQuery = searchTerm.trim()
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            coroutineScope {
                val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
                val baseSearch = async {
                    val response = apiService.searchItems(
                        userId = userId.id,
                        searchTerm = normalizedQuery,
                        includeItemTypes = includeItemTypes,
                        recursive = true,
                        startIndex = startIndex,
                        limit = limit,
                        authorization = authHeader
                    )

                    response.items.map { dto ->
                        mapToMediaItem(dto)
                    }
                }

                val tmdbEnabled = preferencesManager.getTmdbEnabled().first()
                val tmdbApiKey = PreferenceConstants.DEFAULT_TMDB_API_KEY
                val shouldUseTmdb = tmdbEnabled &&
                    tmdbApiKey.isNotBlank() &&
                    normalizedQuery.length >= 3 &&
                    startIndex == 0 &&
                    !serverConnectionManager.state.value.isOffline
                val tmdbSearch: Deferred<List<String>>? = if (shouldUseTmdb) {
                    async {
                        delay(500)
                        val preferredLanguage = preferencesManager.getPreferredLanguage().first()
                        val tmdbLanguage = toTmdbLanguage(preferredLanguage)
                        runCatching {
                            fetchTmdbSearchTerms(
                                query = normalizedQuery,
                                language = tmdbLanguage,
                                maxResults = 5,
                                apiKey = tmdbApiKey
                            )
                        }.getOrDefault(emptyList())
                    }
                } else {
                    null
                }

                val baseResults = baseSearch.await()
                if (baseResults.size >= 6 || tmdbSearch == null) {
                    return@coroutineScope baseResults
                }

                val tmdbTerms = tmdbSearch.await()
                    .filterNot { it.equals(normalizedQuery, ignoreCase = true) }
                if (tmdbTerms.isEmpty()) {
                    return@coroutineScope baseResults
                }

                val mergedResults = LinkedHashMap<String, MediaItem>()
                baseResults.forEach { item ->
                    mergedResults.putIfAbsent(item.id, item)
                }

                for (term in tmdbTerms) {
                    val response = apiService.searchItems(
                        userId = userId.id,
                        searchTerm = term,
                        includeItemTypes = includeItemTypes,
                        recursive = true,
                        startIndex = startIndex,
                        limit = limit,
                        authorization = authHeader
                    )

                    response.items.map { dto ->
                        mapToMediaItem(dto)
                    }.forEach { item ->
                        mergedResults.putIfAbsent(item.id, item)
                    }
                }

                mergedResults.values.toList()
            }
        }
    }

    private fun mapToLibrary(dto: BaseItemDto): Library {
        return Library(
            id = dto.id,
            name = dto.name ?: "Unknown LibrarySection",
            type = LibraryType.fromString(dto.collectionType),
            itemCount = dto.childCount,
            primaryImageTag = dto.imageTags?.primary,
            isFolder = dto.isFolder
        )
    }

    private fun mapToMediaItem(dto: BaseItemDto): MediaItem {
        return MediaItem(
            id = dto.id,
            name = dto.name ?: "Unknown",
            title = dto.title,
            type = dto.type ?: "Unknown",
            overview = dto.overview,
            year = dto.productionYear,
            communityRating = dto.communityRating,
            runTimeTicks = dto.runTimeTicks,
            primaryImageTag = dto.imageTags?.primary,
            thumbImageTag = dto.imageTags?.thumb,
            logoImageTag = dto.imageTags?.logo,
            backdropImageTags = dto.backdropImageTags,
            genres = dto.genres,
            isFolder = dto.isFolder,
            childCount = dto.childCount,
            userData = adjustUserData(mapToUserData(dto.userData), dto.runTimeTicks),
            taglines = dto.taglines,
            people = dto.people.map { personDto ->
                Person(
                    id = personDto.id,
                    name = personDto.name ?: "Unknown",
                    role = personDto.role,
                    type = personDto.type,
                    primaryImageTag = personDto.primaryImageTag
                )
            },
            mediaSources = playbackMapper.mapMediaSourceDtoListToMediaSourceList(dto.mediaSources),
            seriesName = dto.seriesName,
            seriesId = dto.seriesId,
            seriesPrimaryImageTag = dto.seriesPrimaryImageTag,
            seasonId = dto.seasonId,
            seasonName = dto.seasonName,
            seasonPrimaryImageTag = dto.seasonPrimaryImageTag,
            parentIndexNumber = dto.parentIndexNumber,
            indexNumber = dto.indexNumber,
            tvdbId = dto.providerIds.extractTvdbId()
        )
    }

    private fun mapToUserData(dto: UserDataDto?): UserData? {
        return dto?.let {
            UserData(
                isFavorite = it.isFavorite,
                playbackPositionTicks = it.playbackPositionTicks,
                playCount = it.playCount,
                played = it.played,
                lastPlayedDate = it.lastPlayedDate,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false
            )
        }
    }

    private fun adjustUserData(userData: UserData?, runTimeTicks: Long?): UserData? {
        if (userData == null || runTimeTicks == null) return userData
        val adjustedPlayed = userData.playbackPositionTicks >= runTimeTicks - THRESHOLD
        return userData.copy(played = adjustedPlayed)
    }

    private fun LibraryEntity.toDomain(): Library {
        return Library(
            id = id,
            name = name,
            type = runCatching { LibraryType.valueOf(type) }.getOrDefault(LibraryType.UNKNOWN),
            itemCount = itemCount,
            primaryImageTag = primaryImageTag,
            isFolder = isFolder
        )
    }

    private fun Library.toEntity(userId: String): LibraryEntity {
        return LibraryEntity(
            id = id,
            name = name,
            type = type.name,
            itemCount = itemCount,
            primaryImageTag = primaryImageTag,
            isFolder = isFolder,
            userId = userId
        )
    }

    private fun MediaItemEntity.toDomain(): MediaItem {
        val userData = adjustUserData(
            UserData(
                isFavorite = isFavorite,
                playbackPositionTicks = playbackPositionTicks,
                playCount = playCount,
                played = played,
                lastPlayedDate = lastPlayedDate,
                isWatchlist = isWatchlist,
                pendingFavorite = pendingFavorite,
                pendingPlayed = pendingPlayed,
                pendingWatchlist = pendingWatchlist
            ),
            runTimeTicks
        )
        return MediaItem(
            id = id,
            name = name,
            title = title,
            type = type,
            overview = overview,
            year = year,
            communityRating = communityRating,
            runTimeTicks = runTimeTicks,
            primaryImageTag = primaryImageTag,
            thumbImageTag = thumbImageTag,
            tvdbId = tvdbId,
            logoImageTag = null,
            backdropImageTags = backdropImageTags,
            genres = genres,
            isFolder = isFolder,
            childCount = childCount,
            userData = userData,
            taglines = taglines
        )
    }

    private fun MediaItem.toEntity(libraryId: String?, userId: String): MediaItemEntity {
        val adjustedUserData = adjustUserData(userData, runTimeTicks)
        return MediaItemEntity(
            id = id,
            name = name,
            title = title,
            type = type,
            overview = overview,
            year = year,
            communityRating = communityRating,
            runTimeTicks = runTimeTicks,
            primaryImageTag = primaryImageTag,
            thumbImageTag = thumbImageTag,
            tvdbId = tvdbId,
            backdropImageTags = backdropImageTags,
            genres = genres,
            isFolder = isFolder,
            childCount = childCount,
            libraryId = libraryId,
            userId = userId,
            isFavorite = adjustedUserData?.isFavorite ?: false,
            playbackPositionTicks = adjustedUserData?.playbackPositionTicks ?: 0L,
            playCount = adjustedUserData?.playCount ?: 0,
            played = adjustedUserData?.played ?: false,
            lastPlayedDate = adjustedUserData?.lastPlayedDate,
            isWatchlist = adjustedUserData?.isWatchlist ?: false,
            pendingFavorite = adjustedUserData?.pendingFavorite ?: false,
            pendingPlayed = adjustedUserData?.pendingPlayed ?: false,
            pendingWatchlist = adjustedUserData?.pendingWatchlist ?: false,
            taglines = taglines
        )
    }

    private fun PendingActionEntity.toDomain(): PendingAction {
        return PendingAction(
            mediaId = mediaId,
            actionType = actionType,
            newValue = newValue,
            timestamp = timestamp
        )
    }

    private suspend fun getServerUrl(): String {
        val stored = preferencesManager.getServerUrl().first() ?: "http://localhost:8096"
        return serverConnectionManager.normalizeUrl(stored)
    }

    private suspend fun fetchTmdbSearchTerms(
        query: String,
        language: String,
        maxResults: Int,
        apiKey: String
    ): List<String> {
        val normalizedQuery = normalizeSearchText(query)
        val primaryCandidates = fetchTmdbCandidates(
            query = query,
            language = language,
            apiKey = apiKey
        )
        val relaxedCandidates = if (primaryCandidates.isEmpty()) {
            var fallbackCandidates: List<String> = emptyList()
            for (relaxedQuery in buildRelaxedQueries(normalizedQuery)) {
                if (relaxedQuery.isEmpty() ||
                    relaxedQuery.equals(normalizedQuery, ignoreCase = true)
                ) {
                    continue
                }
                fallbackCandidates = fetchTmdbCandidates(
                    query = relaxedQuery,
                    language = language,
                    apiKey = apiKey
                )
                if (fallbackCandidates.isNotEmpty()) {
                    break
                }
            }
            fallbackCandidates
        } else {
            emptyList()
        }
        val candidates = (primaryCandidates + relaxedCandidates).distinct()

        return candidates.mapNotNull { title ->
            val normalizedTitle = normalizeSearchText(title)
            if (normalizedTitle.isEmpty()) {
                null
            } else {
                title.trim() to similarityScore(normalizedQuery, normalizedTitle)
            }
        }
            .sortedByDescending { it.second }
            .map { it.first }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(maxResults)
    }

    private suspend fun fetchTmdbCandidates(
        query: String,
        language: String,
        apiKey: String
    ): List<String> {
        val response = getTmdbApiService().searchMulti(
            apiKey = apiKey.trim(),
            query = query,
            language = language
        )
        val multiResults = response.results
            .filter { it.mediaType == null || it.mediaType == "movie" || it.mediaType == "tv" }
            .mapNotNull { result ->
                result.title
                    ?: result.name
                    ?: result.originalTitle
                    ?: result.originalName
            }

        val tvResults = getTmdbApiService().searchTv(
            apiKey = apiKey.trim(),
            query = query,
            language = language
        ).results.mapNotNull { result ->
            result.name
                ?: result.title
                ?: result.originalName
                ?: result.originalTitle
        }

        val movieResults = getTmdbApiService().searchMovie(
            apiKey = apiKey.trim(),
            query = query,
            language = language
        ).results.mapNotNull { result ->
            result.title
                ?: result.name
                ?: result.originalTitle
                ?: result.originalName
        }

        return (multiResults + tvResults + movieResults)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun buildRelaxedQueries(normalizedQuery: String): List<String> {
        if (normalizedQuery.isBlank()) {
            return emptyList()
        }
        val stopWords = setOf("the", "a", "an", "and", "of", "to", "in", "on", "at", "for", "with")
        val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = tokens.filter { token ->
            token.length >= 4 && token !in stopWords
        }
        val queries = mutableListOf<String>()
        if (filtered.isNotEmpty()) {
            queries.add(filtered.joinToString(" "))
        }
        val longest = tokens.maxByOrNull { it.length }.orEmpty()
        if (longest.length >= 4) {
            queries.add(longest)
        }
        return queries.distinct()
    }

    private fun toTmdbLanguage(preferredLanguage: String): String {
        val trimmed = preferredLanguage.trim()
        if (trimmed.contains("-")) {
            return trimmed
        }
        if (trimmed.length == 2) {
            return "${trimmed}-US"
        }
        return "en-US"
    }

    private fun normalizeSearchText(text: String): String {
        val normalized = Normalizer.normalize(text.lowercase(Locale.US), Normalizer.Form.NFD)
        val stripped = normalized.replace(Regex("\\p{Mn}+"), "")
        return stripped.replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    private fun similarityScore(a: String, b: String): Double {
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        val distance = levenshteinDistance(a, b)
        return 1.0 - (distance.toDouble() / maxLen.toDouble())
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(
                    curr[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost
                )
            }
            for (j in prev.indices) {
                prev[j] = curr[j]
            }
        }
        return prev[b.length]
    }

    override suspend fun getMediaItemDetail(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<MediaItem> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemById(
                userId = userId,
                itemId = mediaId,
                authorization = authHeader
            )

            mapToMediaItem(response)
        }
    }

    override suspend fun getMediaItemDetailLocal(
        userId: String,
        mediaId: String
    ): MediaItem? {
        val normalized = normalizeUuid(mediaId)
        val download = downloadDao.getDownloadByMediaId(normalized)
            ?: downloadDao.getDownloadByMediaSourceId(mediaId)
        if (download != null) {
            val dir = File(download.filePath!!).parentFile
            val source = download.mediaSourceId ?: normalized
            val jsonFile = dir?.resolve("$source.json")
            val item = jsonFile?.let {
                runCatching { json.decodeFromString<MediaItem>(it.readText()) }.getOrNull()
            }
            if (item != null) return item
        }
        mediaItemDao.getMediaItem(normalized, userId)?.toDomain()?.let { return it }
        return mediaItemDao.getMediaItem(mediaId, userId)?.toDomain()
    }

    override suspend fun getSimilarItems(
        mediaId: String,
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getSimilarItems(
                itemId = mediaId,
                userId = userId,
                limit = limit,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getSpecialFeatures(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getSpecialFeatures(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )

            response.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getMediaCredits(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<Person>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val credits = apiService.getItemCredits(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )

            credits.map { personDto ->
                Person(
                    id = personDto.id,
                    name = personDto.name ?: "Unknown",
                    role = personDto.role,
                    type = personDto.type,
                    primaryImageTag = personDto.primaryImageTag
                )
            }
        }
    }

    override suspend fun getPersonDetail(
        userId: String,
        personId: String,
        accessToken: String
    ): NetworkResult<MediaItem> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val itemDto = apiService.getItemById(
                userId = userId,
                itemId = personId,
                authorization = authHeader
            )

            mapToMediaItem(itemDto)
        }
    }

    override suspend fun getPersonAppearances(
        userId: String,
        personId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByPerson(
                userId = userId,
                personIds = personId,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getRelatedResourcesBatch(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<RelatedResources> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val similar = apiService.getSimilarItems(
                itemId = mediaId,
                userId = userId,
                limit = 20,
                authorization = authHeader
            ).items.map { dto ->
                mapToMediaItem(dto)
            }
            val special = apiService.getSpecialFeatures(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            ).map { dto ->
                mapToMediaItem(dto)
            }
            RelatedResources(similar, special)
        }
    }

    override suspend fun getThemeSongs(
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getThemeSongs(
                itemId = mediaId,
                inheritFromParent = true,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getThemeSongIds(
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<String>> {
        return safeApiCall {
            val serverUrl = getServerUrl()
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getThemeMedia(
                itemId = mediaId,
                inheritFromParent = true,
                authorization = authHeader
            )

            response.themeSongsResult?.items?.map { it.id } ?: emptyList()
        }
    }

    override suspend fun getItemSegments(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<Segment>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemSegments(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )

            playbackMapper.mapSegmentDtoListToSegmentList(response.items)
        }
    }

    override suspend fun updateFavoriteRemote(
        userId: String,
        mediaId: String,
        accessToken: String,
        isFavorite: Boolean
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (isFavorite) {
                apiService.markAsFavorite(userId, mediaId, authHeader)
            } else {
                apiService.removeFromFavorites(userId, mediaId, authHeader)
            }
        }.await()
    }

    override suspend fun updatePlayedRemote(
        userId: String,
        mediaId: String,
        accessToken: String,
        isPlayed: Boolean
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        val result = enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (isPlayed) {
                apiService.markAsPlayed(userId, mediaId, authHeader)
            } else {
                apiService.markAsUnplayed(userId, mediaId, authHeader)
            }
        }.await()
        if (result is Success) {
            refreshNextUpItemsFlow(userId, accessToken)
        }
        return result
    }

    override suspend fun toggleFavorite(
        userId: String,
        mediaId: String,
        accessToken: String,
        newFavorite: Boolean,
        mediaItem: MediaItem?
    ): NetworkResult<Unit> {
        val previousItem = mediaItemDao.getMediaItem(mediaId, userId)
        setFavoriteLocal(userId, mediaId, newFavorite, mediaItem)

        val serverUrl = getServerUrl()
        val networkResult = enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (newFavorite) {
                apiService.markAsFavorite(userId, mediaId, authHeader)
            } else {
                apiService.removeFromFavorites(userId, mediaId, authHeader)
            }
        }.await()

        return when (networkResult) {
            is Success -> {
                mediaItemDao.updateFavoriteStatus(mediaId, userId, newFavorite, false)
                pendingActionDao.deleteAction(mediaId, "favorite")
                Success(Unit)
            }

            is NetworkResult.Error -> {
                if (previousItem != null) {
                    mediaItemDao.updateMediaItem(previousItem)
                } else {
                    mediaItemDao.getMediaItem(mediaId, userId)?.let { mediaItemDao.deleteMediaItem(it) }
                }
                pendingActionDao.deleteAction(mediaId, "favorite")
                networkResult
            }

            is NetworkResult.Loading<*> -> TODO()
        }
    }

    override suspend fun markAsPlayed(
        userId: String,
        mediaId: String,
        accessToken: String,
        isPlayed: Boolean
    ): NetworkResult<Unit> {
        val previousItem = mediaItemDao.getMediaItem(mediaId, userId)
        setPlayedLocal(userId, mediaId, isPlayed)
        val serverUrl = getServerUrl()
        val apiService = createApiService(serverUrl)
        val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)

        return try {
            if (isPlayed) {
                apiService.markAsPlayed(userId, mediaId, authHeader)
            } else {
                apiService.markAsUnplayed(userId, mediaId, authHeader)
            }
            mediaItemDao.getMediaItem(mediaId, userId)?.let { current ->
                mediaItemDao.updateMediaItem(current.copy(pendingPlayed = false))
            }
            pendingActionDao.deleteAction(mediaId, "played")
            refreshNextUpItemsFlow(userId, accessToken)
            Success(Unit)
        } catch (e: Exception) {
            if (previousItem != null) {
                mediaItemDao.updateMediaItem(previousItem)
            } else {
                mediaItemDao.getMediaItem(mediaId, userId)?.let { mediaItemDao.deleteMediaItem(it) }
            }
            pendingActionDao.deleteAction(mediaId, "played")
            val message = e.localizedMessage ?: "Unknown error"
            NetworkResult.Error(AppError.Unknown(message), e)
        }
    }

    override suspend fun setFavoriteLocal(userId: String, mediaId: String, isFavorite: Boolean, mediaItem: MediaItem?) {
        val existingItem = mediaItemDao.getMediaItem(mediaId, userId)
        if (existingItem == null) {
            var item = mediaItem
            if (item == null) {
                val token = preferencesManager.getAccessToken().first()
                if (token != null) {
                    when (val result = getMediaItemDetail(userId, mediaId, token)) {
                        is Success -> item = result.data
                        else -> Unit
                    }
                }
            }

            val entity = item?.toEntity(null, userId) ?: MediaItemEntity(
                id = mediaId,
                name = "",
                type = "",
                overview = null,
                year = null,
                communityRating = null,
                runTimeTicks = null,
                primaryImageTag = null,
                backdropImageTags = emptyList(),
                genres = emptyList(),
                isFolder = false,
                childCount = null,
                libraryId = null,
                userId = userId,
                isFavorite = false,
                playbackPositionTicks = 0,
                playCount = 0,
                played = false,
                lastPlayedDate = null,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false,
                taglines = emptyList()
            )
            mediaItemDao.insertMediaItem(entity)
        }
        mediaItemDao.updateFavoriteStatus(mediaId, userId, isFavorite, true)
        pendingActionDao.upsert(
            PendingActionEntity(
                mediaId = mediaId,
                actionType = "favorite",
                newValue = isFavorite,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setPlayedLocal(userId: String, mediaId: String, isPlayed: Boolean) {
        val existingItem = mediaItemDao.getMediaItem(mediaId, userId)
        if (existingItem == null) {
            val placeholder = MediaItemEntity(
                id = mediaId,
                name = "",
                type = "",
                overview = null,
                year = null,
                communityRating = null,
                runTimeTicks = null,
                primaryImageTag = null,
                backdropImageTags = emptyList(),
                genres = emptyList(),
                isFolder = false,
                childCount = null,
                libraryId = null,
                userId = userId,
                isFavorite = false,
                playbackPositionTicks = 0,
                playCount = 0,
                played = false,
                lastPlayedDate = null,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false,
                taglines = emptyList()
            )
            mediaItemDao.insertMediaItem(placeholder)
        }
        mediaItemDao.updatePlayedStatus(mediaId, userId, isPlayed, true)
        pendingActionDao.upsert(
            PendingActionEntity(
                mediaId = mediaId,
                actionType = "played",
                newValue = isPlayed,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setWatchlistLocal(userId: String, mediaId: String, isWatchlist: Boolean) {
        val existingItem = mediaItemDao.getMediaItem(mediaId, userId)
        if (existingItem == null) {
            val placeholder = MediaItemEntity(
                id = mediaId,
                name = "",
                type = "",
                overview = null,
                year = null,
                communityRating = null,
                runTimeTicks = null,
                primaryImageTag = null,
                backdropImageTags = emptyList(),
                genres = emptyList(),
                isFolder = false,
                childCount = null,
                libraryId = null,
                userId = userId,
                isFavorite = false,
                playbackPositionTicks = 0,
                playCount = 0,
                played = false,
                lastPlayedDate = null,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false,
                taglines = emptyList()
            )
            mediaItemDao.insertMediaItem(placeholder)
        }
        mediaItemDao.updateWatchlistStatus(mediaId, userId, isWatchlist, false)
    }

    override suspend fun getPendingActions(): List<PendingAction> {
        return pendingActionDao.getPendingActions().map { it.toDomain() }
    }

    override suspend fun getSeasons(
        userId: String,
        seriesId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = seriesId,
                startIndex = 0,
                limit = Int.MAX_VALUE,
                sortBy = "SortName",
                sortOrder = "Ascending",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
        }
    }

    override suspend fun getEpisodes(
        userId: String,
        seasonId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = seasonId,
                startIndex = 0,
                limit = Int.MAX_VALUE,
                sortBy = "SortName",
                sortOrder = "Ascending",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
        }
    }

    override suspend fun getNextUpEpisodes(
        userId: String,
        accessToken: String,
        limit: Int,
        seriesId: String?,
        disableFirstEpisode: Boolean?
    ): NetworkResult<List<MediaItem>> {
        return fetchNextUpEpisodesRemote(userId, accessToken, limit, seriesId, disableFirstEpisode)
    }

    override suspend fun invalidateNextUp(limit: Int) {
        nextUpStore.invalidate()
    }

    override suspend fun getWatchHistory(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie,Episode",
                isPlayed = true,
                sortBy = "DatePlayed",
                sortOrder = "Descending",
                limit = limit,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    private suspend fun fetchNextUpEpisodesRemote(
        userId: String,
        accessToken: String,
        limit: Int,
        seriesId: String? = null,
        disableFirstEpisode: Boolean? = null
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getNextUpItems(
                userId = userId,
                limit = limit,
                seriesId = seriesId,
                disableFirstEpisode = disableFirstEpisode,
                authorization = authHeader
            )

            response.items.filter { it.type == "Episode" }.map { dto ->
                mapToMediaItem(dto)
            }
        }
    }

    override suspend fun getPlaybackPosition(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<Long> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val userData = apiService.getItemUserData(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            )
            userData.playbackPositionTicks
        }
    }


    override suspend fun requestTranscodingUrl(
        itemId: String,
        userId: String,
        accessToken: String,
        mediaSourceId: String,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
        startTimeTicks: Long?,
        maxStreamingBitrate: Int?,
        parameters: TranscodeRequestParameters,
    ): NetworkResult<PlaybackStreamInfo> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val defaultBitrate = parameters.videoBitrate
                ?: parameters.maxBitrate
                ?: maxStreamingBitrate
                ?: 8_000_000
            val effectiveBitrate = if (defaultBitrate > 0) defaultBitrate else 8_000_000
            val videoCodecValue = parameters.videoCodec ?: "h264,hevc,av1"
            val audioCodecValue = parameters.audioCodec ?: "aac"
            val request = PlaybackInfoRequestDto(
                mediaSourceId = mediaSourceId,
                maxStreamingBitrate = effectiveBitrate,
                enableDirectPlay = false,
                enableDirectStream = false,
                enableTranscoding = true,
                allowVideoStreamCopy = parameters.allowVideoStreamCopy,
                allowAudioStreamCopy = parameters.allowAudioStreamCopy,
                enableAutoStreamCopy = parameters.enableAutoStreamCopy,
                alwaysBurnInSubtitleWhenTranscoding = subtitleStreamIndex != null,
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                startTimeTicks = startTimeTicks,
                deviceProfile = PlaybackDeviceProfileDto(
                    name = "VoidMpvProfile",
                    maxStreamingBitrate = effectiveBitrate,
                    transcodingProfiles = listOf(
                        PlaybackTranscodingProfileDto(
                            type = "Video",
                            videoCodec = videoCodecValue,
                            audioCodec = audioCodecValue,
                            profile = parameters.videoCodecProfile,
                            protocol = "hls",
                            context = "Streaming",
                            enableMpegtsM2TsMode = true,
                            transcodeSeekInfo = "Auto",
                            copyTimestamps = true,
                            enableSubtitlesInManifest = true,
                            enableAudioVbrEncoding = true,
                            breakOnNonKeyFrames = false,
                            maxAudioChannels = 6,
                            maxWidth = parameters.maxWidth,
                            maxHeight = parameters.maxHeight,
                            maxBitrate = parameters.maxBitrate ?: effectiveBitrate,
                            videoBitrate = parameters.videoBitrate,
                        )
                    ),
                    codecProfiles = listOf(
                        PlaybackCodecProfileDto(type = "Video", codec = "h264", container = "ts"),
                        PlaybackCodecProfileDto(type = "Video", codec = "hevc", container = "ts"),
                        PlaybackCodecProfileDto(type = "Video", codec = "av1", container = "ts"),
                    ),
                    subtitleProfiles = listOf(
                        PlaybackSubtitleProfileDto(format = "srt", method = "Encode")
                    )
                )
            )
            val response = apiService.getPlaybackInfo(
                itemId = itemId,
                userId = userId,
                request = request,
                authorization = authHeader
            )
            val transcodingUrl = response.mediaSources.firstNotNullOfOrNull { source ->
                val url = source.transcodingUrl
                url?.takeIf { it.contains("master.m3u8", ignoreCase = true) }
            } ?: throw IllegalStateException("Transcoding URL missing or invalid")
            val sanitizedBase = serverUrl.removeSuffix("/")
            val sanitizedPath = if (transcodingUrl.startsWith("/")) {
                transcodingUrl
            } else {
                "/$transcodingUrl"
            }
            PlaybackStreamInfo(
                url = sanitizedBase + sanitizedPath,
                playSessionId = response.playSessionId,
            )
        }
    }


    override suspend fun reportPlaybackStart(
        mediaId: String,
        userId: String,
        accessToken: String,
        playSessionId: String?
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.HIGH, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (shouldUseLegacyPlaybackApi()) {
                apiService.reportLegacyPlaybackStart(
                    itemId = mediaId,
                    userId = userId,
                    canSeek = true,
                    playSessionId = playSessionId,
                    authorization = authHeader
                )
            } else {
                val body = PlaybackStartRequest(
                    itemId = mediaId,
                    canSeek = true,
                    playSessionId = playSessionId,
                )
                apiService.reportPlaybackStart(body, authHeader)
            }
        }.await()
    }

    override suspend fun reportPlaybackProgress(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long,
        playSessionId: String?
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (shouldUseLegacyPlaybackApi()) {
                apiService.reportLegacyPlaybackProgress(
                    itemId = mediaId,
                    userId = userId,
                    positionTicks = positionTicks,
                    playSessionId = playSessionId,
                    isPaused = false,
                    authorization = authHeader
                )
            } else {
                val body = PlaybackProgressRequest(
                    itemId = mediaId,
                    positionTicks = positionTicks,
                    isPaused = false,
                    playSessionId = playSessionId,
                )
                apiService.reportPlaybackProgress(body, authHeader)
            }
        }.await()
    }

    override suspend fun reportPlaybackStop(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long,
        playSessionId: String?,
        mediaSourceId: String?
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            if (shouldUseLegacyPlaybackApi()) {
                apiService.reportLegacyPlaybackStop(
                    itemId = mediaId,
                    userId = userId,
                    positionTicks = positionTicks,
                    playSessionId = playSessionId,
                    mediaSourceId = mediaSourceId,
                    authorization = authHeader
                )
            } else {
                val body = PlaybackStopRequest(
                    itemId = mediaId,
                    positionTicks = positionTicks,
                    playSessionId = playSessionId,
                    mediaSourceId = mediaSourceId,
                )
                apiService.reportPlaybackStop(body, authHeader)
            }
        }.await()
    }

    private suspend fun shouldUseLegacyPlaybackApi(): Boolean {
        return preferencesManager.getServerLegacyPlayback().first()
    }

    override suspend fun refreshResumeItemsFlow(userId: String, accessToken: String) {
        when (val result = getResumeItems(userId, accessToken)) {
            is Success -> continueWatchingStore.update(result.data)
            else -> {} // Silently fail, keep existing data
        }
    }

    override suspend fun refreshNextUpItemsFlow(userId: String, accessToken: String) {
        when (val result = fetchNextUpEpisodesRemote(userId, accessToken, DEFAULT_NEXT_UP_LIMIT)) {
            is Success -> nextUpStore.update(result.data)
            else -> {} // Silently fail, keep existing data
        }
    }
}
