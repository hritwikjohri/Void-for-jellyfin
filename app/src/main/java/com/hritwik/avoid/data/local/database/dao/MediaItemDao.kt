package com.hritwik.avoid.data.local.database.dao

import androidx.room.*
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    companion object {
        private const val MEDIA_ITEM_COLUMNS = "id, name, title, type, overview, year, communityRating, runTimeTicks, primaryImageTag, thumbImageTag, tvdbId, backdropImageTags, genres, isFolder, childCount, libraryId, userId, lastUpdated, isFavorite, playbackPositionTicks, playCount, played, lastPlayedDate, isWatchlist, pendingFavorite, pendingPlayed, pendingWatchlist, taglines"
    }

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE userId = :userId ORDER BY lastUpdated DESC")
    fun getAllMediaItems(userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE id = :id AND userId = :userId")
    suspend fun getMediaItem(id: String, userId: String): MediaItemEntity?

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE libraryId = :libraryId AND userId = :userId ORDER BY name ASC")
    fun getMediaItemsByLibrary(libraryId: String, userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE type = :type AND userId = :userId ORDER BY lastUpdated DESC")
    fun getMediaItemsByType(type: String, userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE playbackPositionTicks > 0 AND played = 0 AND userId = :userId ORDER BY lastPlayedDate DESC")
    fun getResumeItems(userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE isFavorite = 1 AND userId = :userId ORDER BY name ASC")
    fun getFavoriteItems(userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE isWatchlist = 1 AND userId = :userId ORDER BY name ASC")
    fun getWatchlistItems(userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE played = 1 AND userId = :userId ORDER BY lastPlayedDate DESC")
    fun getPlayedItems(userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT $MEDIA_ITEM_COLUMNS FROM media_items WHERE name LIKE '%' || :query || '%' AND userId = :userId ORDER BY name ASC")
    fun searchMediaItems(query: String, userId: String): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(mediaItem: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(mediaItems: List<MediaItemEntity>)

    @Update
    suspend fun updateMediaItem(mediaItem: MediaItemEntity)

    @Delete
    suspend fun deleteMediaItem(mediaItem: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE userId = :userId")
    suspend fun deleteAllMediaItems(userId: String)

    @Query("DELETE FROM media_items WHERE libraryId = :libraryId AND userId = :userId")
    suspend fun deleteMediaItemsByLibrary(libraryId: String, userId: String)

    @Query("UPDATE media_items SET isFavorite = :isFavorite, pendingFavorite = :pendingFavorite WHERE id = :id AND userId = :userId")
    suspend fun updateFavoriteStatus(id: String, userId: String, isFavorite: Boolean, pendingFavorite: Boolean)

    @Query("UPDATE media_items SET played = :played, playCount = playCount + 1, pendingPlayed = :pendingPlayed WHERE id = :id AND userId = :userId")
    suspend fun updatePlayedStatus(id: String, userId: String, played: Boolean, pendingPlayed: Boolean)

    @Query("UPDATE media_items SET isWatchlist = :isWatchlist, pendingWatchlist = :pendingWatchlist WHERE id = :id AND userId = :userId")
    suspend fun updateWatchlistStatus(id: String, userId: String, isWatchlist: Boolean, pendingWatchlist: Boolean)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id AND userId = :userId")
    suspend fun updateFavoriteStatus(id: String, userId: String, isFavorite: Boolean)

    @Query("UPDATE media_items SET played = :played, playCount = playCount + 1 WHERE id = :id AND userId = :userId")
    suspend fun updatePlayedStatus(id: String, userId: String, played: Boolean)

    @Query("UPDATE media_items SET playbackPositionTicks = :position WHERE id = :id AND userId = :userId")
    suspend fun updatePlaybackPosition(id: String, userId: String, position: Long)

    @Query("SELECT id FROM media_items LIMIT 1")
    suspend fun closeOpenCursors(): String?
}
