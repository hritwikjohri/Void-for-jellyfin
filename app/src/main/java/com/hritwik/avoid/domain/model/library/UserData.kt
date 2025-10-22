package com.hritwik.avoid.domain.model.library

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UserData(
    val isFavorite: Boolean = false,
    val playbackPositionTicks: Long = 0,
    val playCount: Int = 0,
    val played: Boolean = false,
    val lastPlayedDate: String? = null,
    val name: String? = "",
    val email: String? = "",
    val serverName: String? = "",
    val serverUrl: String? = "",
    val watchedHours: Int? = 0,
    val favoriteMovies: Int? = 0,
    val favoriteShows: Int? = 0,
    val downloadsCount: Int? = 0,
    val watchlistCount: Int? = 0,
    val isWatchlist: Boolean = false,
    val pendingFavorite: Boolean = false,
    val pendingPlayed: Boolean = false,
    val pendingWatchlist: Boolean = false,
) : Parcelable
