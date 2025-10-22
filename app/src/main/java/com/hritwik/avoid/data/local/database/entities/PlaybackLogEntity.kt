package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_logs")
data class PlaybackLogEntity(
    @PrimaryKey val mediaId: String,
    val positionTicks: Long,
    val isCompleted: Boolean,
    val isSynced: Boolean = false
)