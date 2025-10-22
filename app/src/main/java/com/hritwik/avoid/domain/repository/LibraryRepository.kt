package com.hritwik.avoid.domain.repository

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.hritwik.avoid.domain.model.library.PendingAction
import com.hritwik.avoid.domain.model.playback.Segment
import com.hritwik.avoid.domain.model.library.HomeScreenData

data class RelatedResources(
    val similar: List<MediaItem>,
    val special: List<MediaItem>
)

interface LibraryRepository {
    suspend fun getUserLibraries(userId: String, accessToken: String): NetworkResult<List<Library>>
    suspend fun getLibraryItems(
        userId: String,
        libraryId: String,
        accessToken: String,
        startIndex: Int = 1,
        limit: Int = 100,
        forceRefresh: Boolean = false,
        sortBy: List<String> = listOf("SortName"),
        sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING,
        genre: String? = null
    ): NetworkResult<List<MediaItem>>
    suspend fun getLibraryGenres(
        userId: String,
        libraryId: String,
        accessToken: String,
        sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING
    ): NetworkResult<List<String>>
    suspend fun getHomeScreenData(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<HomeScreenData>
    suspend fun getLatestItems(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getResumeItems(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    val resumeItemsFlow: StateFlow<List<MediaItem>>
    val nextUpItemsFlow: StateFlow<List<MediaItem>>
    fun startResumeItemsSync(userId: String, accessToken: String)
    fun stopResumeItemsSync()
    suspend fun getLatestEpisodes(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getLatestMovies(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getRecentlyReleasedMovies(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getRecentlyReleasedShows(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getRecommendedItems(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getCollections(
        userId: String,
        accessToken: String,
        startIndex: Int = 0,
        limit: Int = 20,
        tags: List<String>? = null
    ): NetworkResult<List<MediaItem>>
    suspend fun getCollectionItems(
        userId: String,
        accessToken: String,
        collectionId: String,
        sortBy: List<String> = listOf("SortName"),
        sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING,
        startIndex: Int = 0,
        limit: Int = 50
    ): NetworkResult<List<MediaItem>>
    suspend fun getTrendingItems(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    fun getFavoriteItems(userId: String): Flow<List<MediaItem>>
    fun getWatchlistItems(userId: String): Flow<List<MediaItem>>
    fun getPlayedItems(userId: String): Flow<List<MediaItem>>
    suspend fun getFavoriteItems(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getPlayedItems(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun getItemsByCategory(
        userId: String,
        accessToken: String,
        filters: Map<String, String>,
        sortBy: List<String> = listOf("SortName"),
        sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING,
        startIndex: Int = 0,
        limit: Int = 50
    ): NetworkResult<List<MediaItem>>
    suspend fun searchItems(
        userId: User,
        accessToken: String,
        searchTerm: String,
        includeItemTypes: String? = null,
        startIndex: Int = 0,
        limit: Int = 50
    ): NetworkResult<List<MediaItem>>
    suspend fun getMediaItemDetail(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<MediaItem>
    suspend fun getMediaItemDetailLocal(
        userId: String,
        mediaId: String
    ): MediaItem?
    suspend fun getSimilarItems(
        mediaId: String,
        userId: String,
        accessToken: String,
        limit: Int = 10
    ): NetworkResult<List<MediaItem>>
    suspend fun getSpecialFeatures(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>>
    suspend fun getRelatedResourcesBatch(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<RelatedResources>
    suspend fun getThemeSongs(
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>>
    suspend fun getThemeSongIds(
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<String>>
    suspend fun getItemSegments(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<List<Segment>>
    suspend fun updateFavoriteRemote(
        userId: String,
        mediaId: String,
        accessToken: String,
        isFavorite: Boolean
    ): NetworkResult<Unit>
    suspend fun updatePlayedRemote(
        userId: String,
        mediaId: String,
        accessToken: String,
        isPlayed: Boolean
    ): NetworkResult<Unit>
    suspend fun toggleFavorite(
        userId: String,
        mediaId: String,
        accessToken: String,
        newFavorite: Boolean,
        mediaItem: MediaItem? = null
    ): NetworkResult<Unit>
    suspend fun markAsPlayed(
        userId: String,
        mediaId: String,
        accessToken: String,
        isPlayed: Boolean
    ): NetworkResult<Unit>
    suspend fun setFavoriteLocal(
        userId: String,
        mediaId: String,
        isFavorite: Boolean,
        mediaItem: MediaItem? = null
    )
    suspend fun setPlayedLocal(
    userId: String,
    mediaId: String,
    isPlayed: Boolean
    )
    suspend fun setWatchlistLocal(
        userId: String,
        mediaId: String,
        isWatchlist: Boolean
    )
    suspend fun getPendingActions(): List<PendingAction>
    suspend fun getSeasons(
        userId: String,
        seriesId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>>
    suspend fun getEpisodes(
        userId: String,
        seasonId: String,
        accessToken: String
    ): NetworkResult<List<MediaItem>>
    suspend fun getNextUpEpisodes(
        userId: String,
        accessToken: String,
        limit: Int = 20
    ): NetworkResult<List<MediaItem>>
    suspend fun invalidateNextUp(limit: Int = 20)
    suspend fun getWatchHistory(
        userId: String,
        accessToken: String,
        limit: Int = 50
    ): NetworkResult<List<MediaItem>>
    suspend fun getPlaybackPosition(
        userId: String,
        mediaId: String,
        accessToken: String
    ): NetworkResult<Long>
    suspend fun reportPlaybackStart(
        mediaId: String,
        userId: String,
        accessToken: String
    ): NetworkResult<Unit>
    suspend fun reportPlaybackProgress(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long
    ): NetworkResult<Unit>
    suspend fun reportPlaybackStop(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionTicks: Long
    ): NetworkResult<Unit>
}