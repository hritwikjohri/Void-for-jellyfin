package com.hritwik.avoid.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hritwik.avoid.data.local.database.entities.PlaybackLogEntity

@Dao
interface PlaybackLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: PlaybackLogEntity)

    @Query("SELECT * FROM playback_logs WHERE isSynced = 0")
    suspend fun getUnSyncedLogs(): List<PlaybackLogEntity>

    @Query("UPDATE playback_logs SET isSynced = 1 WHERE mediaId = :mediaId")
    suspend fun markAsSynced(mediaId: String)

    @Query("SELECT * FROM playback_logs LIMIT 1")
    suspend fun closeOpenCursors(): PlaybackLogEntity?
}