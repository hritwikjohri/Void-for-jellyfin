package com.hritwik.avoid.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings.Secure.ANDROID_ID
import android.provider.Settings.System.getString
import java.util.Locale
import androidx.annotation.RequiresApi
import com.hritwik.avoid.data.common.BaseRepository
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.common.NetworkResult.*
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.LibraryDao
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.database.dao.PendingActionDao
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.entities.LibraryEntity
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import com.hritwik.avoid.data.local.database.entities.PendingActionEntity
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.data.remote.dto.library.BaseItemDto
import com.hritwik.avoid.data.remote.dto.library.UserDataDto
import com.hritwik.avoid.domain.mapper.PlaybackMapper
import com.hritwik.avoid.data.remote.dto.playback.PlaybackProgressRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStartRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStopRequest
import com.hritwik.avoid.data.remote.websocket.PlaybackEvent
import com.hritwik.avoid.data.remote.websocket.PlaybackWebSocketClient
import com.hritwik.avoid.data.repository.ContinueWatchingStore
import com.hritwik.avoid.data.repository.NextUpStore
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.PendingAction
import com.hritwik.avoid.domain.model.library.Person
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.domain.model.library.HomeScreenData
import com.hritwik.avoid.domain.repository.RelatedResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import com.hritwik.avoid.domain.repository.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hritwik.avoid.di.WebSocketScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.playback.Segment
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File
import com.hritwik.avoid.utils.helpers.normalizeUuid

private const val THRESHOLD = 10_000_000L
private const val DEFAULT_NEXT_UP_LIMIT = 20

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class LibraryRepositoryImpl @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val preferencesManager: PreferencesManager,
    private val playbackMapper: PlaybackMapper,
    private val libraryDao: LibraryDao,
    private val mediaItemDao: MediaItemDao,
    private val pendingActionDao: PendingActionDao,
    private val downloadDao: DownloadDao,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context,
    private val webSocketClient: PlaybackWebSocketClient,
    private val continueWatchingStore: ContinueWatchingStore,
    private val nextUpStore: NextUpStore,
    @WebSocketScope private val wsScope: CoroutineScope,
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

    private var wsJob: Job? = null

    private fun createApiService(serverUrl: String): JellyfinApiService {
        val baseUrl = if (!serverUrl.endsWith("/")) "$serverUrl/" else serverUrl
        return retrofitBuilder
            .baseUrl(baseUrl)
            .build()
            .create(JellyfinApiService::class.java)
    }

    override suspend fun getUserLibraries(
        userId: String,
        accessToken: String
    ): NetworkResult<List<Library>> {
        val cached = libraryDao.getAllLibraries(userId).first()
        if (!networkMonitor.isConnected.value) {
            return if (cached.isNotEmpty()) {
                NetworkResult.Success(cached.map { it.toDomain() })
            } else {
                NetworkResult.Error<List<Library>>(AppError.Network("No network connection"))
            }
        }

        if (cached.isNotEmpty()) {
            return NetworkResult.Success(cached.map { it.toDomain() })
        }

        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getUserLibraries(userId, authHeader)

            val libraries = response.items.map { dto ->
                mapToLibrary(dto, serverUrl)
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
        val cached = mediaItemDao.getMediaItemsByLibrary(libraryId, userId).first()
        val isDefaultSort =
            sortBy == listOf("SortName") && sortOrder == LibrarySortDirection.ASCENDING && genre.isNullOrEmpty()

        if (!networkMonitor.isConnected.value) {
            return if (cached.isNotEmpty()) {
                NetworkResult.Success(cached.map { it.toDomain() })
            } else {
                NetworkResult.Error<List<MediaItem>>(AppError.Network("No network connection"))
            }
        }

        if (isDefaultSort && !forceRefresh && cached.isNotEmpty() && startIndex == 0) {
            return NetworkResult.Success(cached.map { it.toDomain() })
        }

        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
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

            val items = response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
            }

            if (isDefaultSort && startIndex == 0) {
                mediaItemDao.deleteMediaItemsByLibrary(libraryId, userId)
            }
            mediaItemDao.insertMediaItems(items.map { it.toEntity(libraryId, userId) })

            items
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
            val cachedLibraries = libraryDao.getAllLibraries(userId).first()

            val resultPair = coroutineScope {
                val librariesDeferred = async {
                    try {
                        val response = apiService.getUserLibraries(userId, authHeader)
                        val libraries = response.items.map { dto -> mapToLibrary(dto, serverUrl) }
                        libraryDao.insertLibraries(libraries.map { it.toEntity(userId) })
                        Result.success(libraries)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val latestItemsDeferred = async {
                    try {
                        val items = apiService.getLatestItems(userId, limit, authorization = authHeader)
                            .map { dto -> mapToMediaItem(dto, serverUrl) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val resumeItemsDeferred = async {
                    try {
                        val items = apiService.getResumeItems(userId, limit, authorization = authHeader)
                            .items.map { dto -> mapToMediaItem(dto, serverUrl) }
                        continueWatchingStore.setInitial(items)
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
                            .map { dto -> mapToMediaItem(dto, serverUrl) }
                        nextUpStore.setInitial(items, limit)
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
                            enableImageTypes = "Primary,Backdrop",
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto, serverUrl) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val latestMoviesDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Movie",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "DateCreated",
                            sortOrder = "Descending",
                            enableImageTypes = "Primary,Backdrop",
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto, serverUrl) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val recentlyReleasedMoviesDeferred = async {
                    try {
                        val minPremiereDate = LocalDate.now().minusMonths(6).format(movieDateFormatter)
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Movie",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "PremiereDate",
                            sortOrder = "Descending",
                            minPremiereDate = minPremiereDate,
                            enableImageTypes = "Primary,Backdrop",
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto, serverUrl) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val recentlyReleasedShowsDeferred = async {
                    try {
                        val minPremiereDate = LocalDate.now().minusMonths(3).format(movieDateFormatter)
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Series",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "PremiereDate",
                            sortOrder = "Descending",
                            minPremiereDate = minPremiereDate,
                            enableImageTypes = "Primary,Backdrop",
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto, serverUrl) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val recommendedItemsDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "Movie,Series",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "CommunityRating",
                            sortOrder = "Descending",
                            isPlayed = false,
                            minCommunityRating = 7.0,
                            enableImageTypes = "Primary,Backdrop",
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto, serverUrl) }
                        Result.success(items)
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                }
                val collectionsDeferred = async {
                    try {
                        val items = apiService.getItemsByFilters(
                            userId = userId,
                            includeItemTypes = "BoxSet",
                            recursive = true,
                            startIndex = 0,
                            limit = limit,
                            sortBy = "SortName",
                            sortOrder = "Ascending",
                            enableImageTypes = "Primary,Backdrop",
                            authorization = authHeader
                        ).items.map { dto -> mapToMediaItem(dto, serverUrl) }
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
                        continueWatchingStore.items.value
                    },
                    nextUpItems = nextUpItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load next up items: ${throwable.message ?: "Unknown error"}"
                        nextUpStore.snapshot(limit)
                    },
                    latestEpisodes = latestEpisodesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load latest episodes: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    latestMovies = latestMoviesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load latest movies: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    recentlyReleasedMovies = recentlyReleasedMoviesDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load recently released movies: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    recentlyReleasedShows = recentlyReleasedShowsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load recently released shows: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    recommendedItems = recommendedItemsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load recommended items: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    collections = collectionsDeferred.await().getOrElse { throwable ->
                        errors += "Failed to load collections: ${throwable.message ?: "Unknown error"}"
                        emptyList()
                    },
                    errors = emptyList()
                )

                homeData to librariesDeferred.await()
            }

            val (homeData, librariesResult) = resultPair
            val libraries = if (librariesResult.isSuccess) {
                librariesResult.getOrThrow()
            } else {
                errors += "Failed to load libraries: ${librariesResult.exceptionOrNull()?.message ?: "Unknown error"}"
                cachedLibraries.map { it.toDomain() }
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getLatestItems(userId, limit, authorization = authHeader)

            response.map { dto ->
                mapToMediaItem(dto, serverUrl)
            }
        }
    }

    override suspend fun getResumeItems(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        val result = safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getResumeItems(userId, limit, authorization = authHeader)

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
            }
        }
        if (result is NetworkResult.Success) {
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Episode",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getRecentlyReleasedMovies(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val minPremiereDate = LocalDate.now().minusMonths(6).format(movieDateFormatter)

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Movie",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PremiereDate",
                sortOrder = "Descending",
                minPremiereDate = minPremiereDate,
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "Series",
                recursive = true,
                startIndex = 0,
                limit = limit,
                sortBy = "PremiereDate",
                sortOrder = "Descending",
                minPremiereDate = minPremiereDate,
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)

            
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
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = "BoxSet",
                recursive = true,
                tags = tags?.joinToString(","),
                startIndex = startIndex,
                limit = limit,
                sortBy = "SortName",
                sortOrder = "Ascending",
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                parentId = collectionId,
                recursive = true,
                startIndex = startIndex,
                limit = limit,
                sortBy = sortBy.joinToString(","),
                sortOrder = sortOrder.toApiValue(),
                enableImageTypes = "Primary,Backdrop",
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)

            
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
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
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
                mapToMediaItem(dto, serverUrl)
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
                mapToMediaItem(dto, serverUrl)
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

            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
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
                mapToMediaItem(dto, serverUrl)
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
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.searchItems(
                userId = userId.id,
                searchTerm = searchTerm,
                includeItemTypes = includeItemTypes,
                recursive = true,
                startIndex = startIndex,
                limit = limit,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
            }
        }
    }

    private fun mapToLibrary(dto: BaseItemDto, serverUrl: String): Library {
        return Library(
            id = dto.id,
            name = dto.name ?: "Unknown LibrarySection",
            type = LibraryType.Companion.fromString(dto.collectionType),
            itemCount = dto.childCount,
            primaryImageTag = dto.imageTags?.primary,
            isFolder = dto.isFolder
        )
    }

    private fun mapToMediaItem(dto: BaseItemDto, serverUrl: String): MediaItem {
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
            indexNumber = dto.indexNumber
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

            mapToMediaItem(response, serverUrl)
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
            val dir = File(download.filePath).parentFile
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
                mapToMediaItem(dto, serverUrl)
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
                mapToMediaItem(dto, serverUrl)
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
                mapToMediaItem(dto, serverUrl)
            }
            val special = apiService.getSpecialFeatures(
                itemId = mediaId,
                userId = userId,
                authorization = authHeader
            ).map { dto ->
                mapToMediaItem(dto, serverUrl)
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
                mapToMediaItem(dto, serverUrl)
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
        if (result is NetworkResult.Success) {
            scheduleNextUpRefresh(userId, accessToken, invalidate = true)
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
            val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)
            if (newFavorite) {
                apiService.markAsFavorite(userId, mediaId, authHeader)
            } else {
                apiService.removeFromFavorites(userId, mediaId, authHeader)
            }
        }.await()

        return when (networkResult) {
            is NetworkResult.Success -> {
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
        val authHeader = JellyfinApiService.Companion.createAuthHeader(deviceId, token = accessToken)

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
            scheduleNextUpRefresh(userId, accessToken, invalidate = true)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            if (previousItem != null) {
                mediaItemDao.updateMediaItem(previousItem)
            } else {
                mediaItemDao.getMediaItem(mediaId, userId)?.let { mediaItemDao.deleteMediaItem(it) }
            }
            pendingActionDao.deleteAction(mediaId, "played")
            val message = e.localizedMessage ?: "Unknown error"
            NetworkResult.Error<Unit>(AppError.Unknown(message), e)
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
                        is NetworkResult.Success -> item = result.data
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
                mapToMediaItem(dto, serverUrl)
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
                mapToMediaItem(dto, serverUrl)
            }.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
        }
    }

    override suspend fun getNextUpEpisodes(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        return if (nextUpStore.canServe(limit)) {
            NetworkResult.Success(nextUpStore.snapshot(limit))
        } else {
            when (val result = fetchNextUpEpisodesRemote(userId, accessToken, limit)) {
                is NetworkResult.Success -> {
                    nextUpStore.setInitial(result.data, limit)
                    result
                }

                else -> result
            }
        }
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
                mapToMediaItem(dto, serverUrl)
            }
        }
    }

    private suspend fun fetchNextUpEpisodesRemote(
        userId: String,
        accessToken: String,
        limit: Int
    ): NetworkResult<List<MediaItem>> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getNextUpItems(
                userId = userId,
                limit = limit,
                authorization = authHeader
            )

            response.items.filter { it.type == "Episode" }.map { dto ->
                mapToMediaItem(dto, serverUrl)
            }
        }
    }

    private suspend fun scheduleNextUpRefresh(
        userId: String,
        accessToken: String,
        limit: Int = DEFAULT_NEXT_UP_LIMIT,
        invalidate: Boolean = false
    ) {
        if (userId.isBlank() || accessToken.isBlank()) return
        if (invalidate) {
            nextUpStore.invalidate()
        }
        nextUpStore.requestRefresh(limit) { requestedLimit ->
            when (val result = fetchNextUpEpisodesRemote(userId, accessToken, requestedLimit)) {
                is NetworkResult.Success -> result.data
                else -> null
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


    override suspend fun reportPlaybackStart(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.HIGH, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val body = PlaybackStartRequest(
                itemId = mediaId,
                canSeek = true
            )
            apiService.reportPlaybackStart(body, authHeader)
        }.await()
    }

    override suspend fun reportPlaybackProgress(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val body = PlaybackProgressRequest(
                itemId = mediaId,
                positionTicks = positionTicks,
                isPaused = false
            )
            apiService.reportPlaybackProgress(body, authHeader)
        }.await()
    }

    override suspend fun reportPlaybackStop(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long
    ): NetworkResult<Unit> {
        val serverUrl = getServerUrl()
        return enqueue(PriorityDispatcher.Priority.LOW, serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val body = PlaybackStopRequest(
                itemId = mediaId,
                positionTicks = positionTicks
            )
            apiService.reportPlaybackStop(body, authHeader)
        }.await()
    }

    override fun startResumeItemsSync(userId: String, accessToken: String) {
        if (wsJob != null) return
        wsJob = wsScope.launch {
            val serverUrl = getServerUrl()
            val wsUrl = if (serverUrl.endsWith("/")) "${serverUrl}socket" else "$serverUrl/socket"
            var currentToken = accessToken
            var refreshed = false
            fun connect() {
                val header = JellyfinApiService.createAuthHeader(deviceId, token = currentToken)
                webSocketClient.start(wsUrl, header, userId, deviceId) {
                    if (!refreshed) {
                        refreshed = true
                        wsScope.launch {
                            
                            currentToken = preferencesManager.getAccessToken().first() ?: currentToken
                            connect()
                        }
                    }
                }
            }
            connect()
            webSocketClient.events.collect { event ->
                continueWatchingStore.handle(event)
                when (event) {
                    is PlaybackEvent.Progress,
                    is PlaybackEvent.Stop -> scheduleNextUpRefresh(userId, currentToken)
                }
            }
        }
    }

    override fun stopResumeItemsSync() {
        wsJob?.cancel()
        wsJob = null
        webSocketClient.stop()
    }
}
