package com.hritwik.avoid.data.repository

import android.content.Context
import android.provider.Settings
import com.hritwik.avoid.data.common.BaseRepository
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.SearchResultDao
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import com.hritwik.avoid.data.local.database.entities.SearchResultEntity
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.data.remote.dto.library.BaseItemDto
import com.hritwik.avoid.data.remote.dto.library.UserDataDto
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.mapper.PlaybackMapper
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.domain.repository.SearchRepository
import com.hritwik.avoid.utils.constants.ApiConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import java.net.URI

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val preferencesManager: PreferencesManager,
    private val playbackMapper: PlaybackMapper,
    private val searchResultDao: SearchResultDao,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context,
    private val serverConnectionManager: ServerConnectionManager,
    priorityDispatcher: PriorityDispatcher
) : BaseRepository(priorityDispatcher, serverConnectionManager), SearchRepository {

    private var apiService: JellyfinApiService? = null
    private var lastServerUrl: String? = null

    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private fun createApiService(serverUrl: String): JellyfinApiService {
        val baseUrl = if (!serverUrl.endsWith("/")) "$serverUrl/" else serverUrl
        return retrofitBuilder
            .baseUrl(baseUrl)
            .build()
            .create(JellyfinApiService::class.java)
    }

    private fun getApiService(serverUrl: String): JellyfinApiService {
        if (apiService == null || lastServerUrl != serverUrl) {
            apiService = createApiService(serverUrl)
            lastServerUrl = serverUrl
        }
        return apiService!!
    }

    override suspend fun searchItems(
        userId: User,
        accessToken: String,
        searchTerm: String,
        includeItemTypes: String?,
        startIndex: Int,
        limit: Int,
        forceRefresh: Boolean
    ): NetworkResult<List<MediaItem>> {
        val filterMap = mutableMapOf(
            "includeItemTypes" to (includeItemTypes ?: ""),
            "startIndex" to startIndex.toString(),
            "limit" to limit.toString()
        )
        val filtersKey = filterMap.toSortedMap().toString()
        val cached = searchResultDao.getSearchResult(searchTerm, filtersKey)
        if (!networkMonitor.isConnected.value) {
            return if (cached != null) {
                NetworkResult.Success(cached.results.map { it.toDomain() })
            } else {
                NetworkResult.Error<List<MediaItem>>(AppError.Network("No network connection"))
            }
        }

        if (!forceRefresh && cached != null) {
            return NetworkResult.Success(cached.results.map { it.toDomain() })
        }

        val serverUrl = getServerUrl()
        val result = enqueue(PriorityDispatcher.Priority.HIGH, serverUrl) {
            val apiService = getApiService(serverUrl)
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
        }.await()

        if (result is NetworkResult.Success) {
            val entities = result.data.map { it.toEntity(userId.id) }
            searchResultDao.insertSearchResult(
                SearchResultEntity(
                    query = searchTerm,
                    filters = filtersKey,
                    results = entities
                )
            )
        }

        return result
    }

    override suspend fun getItemsByCategory(
        userId: String,
        accessToken: String,
        filters: Map<String, String>,
        startIndex: Int,
        limit: Int,
        forceRefresh: Boolean
    ): NetworkResult<List<MediaItem>> {
        val filterMap = filters.toMutableMap().apply {
            this["startIndex"] = startIndex.toString()
            this["limit"] = limit.toString()
        }
        val filtersKey = filterMap.toSortedMap().toString()
        val cached = searchResultDao.getSearchResult("", filtersKey)
        if (!networkMonitor.isConnected.value) {
            return if (cached != null) {
                NetworkResult.Success(cached.results.map { it.toDomain() })
            } else {
                NetworkResult.Error<List<MediaItem>>(AppError.Network("No network connection"))
            }
        }

        if (!forceRefresh && cached != null) {
            return NetworkResult.Success(cached.results.map { it.toDomain() })
        }

        val serverUrl = getServerUrl()
        val enableImageTypes = enableImageTypesFor(serverUrl, includeLegacyFallback = true)
        val result = safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getItemsByFilters(
                userId = userId,
                includeItemTypes = filters["IncludeItemTypes"],
                genres = filters["Genres"],
                recursive = filters["Recursive"]?.toBoolean() ?: true,
                startIndex = startIndex,
                limit = limit,
                enableImageTypes = enableImageTypes,
                authorization = authHeader
            )

            response.items.map { dto ->
                mapToMediaItem(dto, serverUrl)
            }
        }
        if (result is NetworkResult.Success) {
            val entities = result.data.map { it.toEntity(userId) }
            searchResultDao.insertSearchResult(
                SearchResultEntity(
                    query = "",
                    filters = filtersKey,
                    results = entities
                )
            )
        }

        return result
    }

    override suspend fun getSearchSuggestions(
        userId: String,
        accessToken: String,
        query: String,
        limit: Int
    ): NetworkResult<List<String>> {
        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error<List<String>>(AppError.Network("No network connection"))
        }

        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)

            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val response = apiService.getSearchSuggestions(
                userId = userId,
                searchTerm = query,
                limit = limit,
                authorization = authHeader
            )

            response.searchHints.mapNotNull { it.name }
        }
    }

    override suspend fun invalidateSearchResults(query: String, filters: Map<String, String>) {
        val filtersKey = filters.toSortedMap().toString()
        searchResultDao.deleteSearchResult(query, filtersKey)
    }

    private fun MediaItemEntity.toDomain(): MediaItem {
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
            userData = UserData(
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
            taglines = taglines
        )
    }

    private fun MediaItem.toEntity(userId: String): MediaItemEntity {
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
            libraryId = null,
            userId = userId,
            taglines = taglines,
            isFavorite = userData?.isFavorite ?: false,
            playbackPositionTicks = userData?.playbackPositionTicks ?: 0,
            playCount = userData?.playCount ?: 0,
            played = userData?.played ?: false,
            lastPlayedDate = userData?.lastPlayedDate,
            isWatchlist = userData?.isWatchlist ?: false,
            pendingFavorite = userData?.pendingFavorite ?: false,
            pendingPlayed = userData?.pendingPlayed ?: false,
            pendingWatchlist = userData?.pendingWatchlist ?: false
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
            userData = mapToUserData(dto.userData),
            taglines = dto.taglines,
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

    private suspend fun getServerUrl(): String {
        val stored = preferencesManager.getServerUrl().first() ?: "http://localhost:8096"
        return serverConnectionManager.normalizeUrl(stored)
    }
    private fun enableImageTypesFor(serverUrl: String, includeLegacyFallback: Boolean = false): String? {
        return if (supportsBundledImages(serverUrl)) {
            ApiConstants.DEFAULT_LIBRARY_IMAGE_TYPES
        } else if (includeLegacyFallback) {
            ApiConstants.LEGACY_LIBRARY_IMAGE_TYPES
        } else {
            null
        }
    }

    private fun supportsBundledImages(serverUrl: String): Boolean {
        return runCatching {
            val uri = URI(serverUrl)
            uri.scheme.equals("https", ignoreCase = true)
        }.getOrElse {
            serverUrl.startsWith("https://", ignoreCase = true)
        }
    }
}
