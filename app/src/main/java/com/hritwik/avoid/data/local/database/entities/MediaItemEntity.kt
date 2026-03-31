package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "media_items",
    indices = [
        Index("userId"),
        Index("libraryId"),
        Index("isFavorite"),
        Index("played")
    ]
)
data class MediaItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val title: String? = null,
    val type: String,
    val overview: String?,
    val year: Int?,
    val communityRating: Double?,
    val runTimeTicks: Long?,
    val primaryImageTag: String?,
    val thumbImageTag: String? = null,
    val tvdbId: String? = null,
    val backdropImageTags: List<String>,
    val genres: List<String>,
    val isFolder: Boolean,
    val childCount: Int?,
    val libraryId: String?,
    val userId: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val playbackPositionTicks: Long = 0,
    val playCount: Int = 0,
    val played: Boolean = false,
    val lastPlayedDate: String? = null,
    val isWatchlist: Boolean = false,
    val pendingFavorite: Boolean = false,
    val pendingPlayed: Boolean = false,
    val pendingWatchlist: Boolean = false,
    val taglines: List<String>
)
