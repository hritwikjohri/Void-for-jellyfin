package com.hritwik.avoid.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hritwik.avoid.data.local.database.entities.PendingActionEntity

@Dao
interface PendingActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(action: PendingActionEntity)

    @Query("SELECT * FROM pending_actions ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<PendingActionEntity>

    @Query("DELETE FROM pending_actions WHERE mediaId = :mediaId AND actionType = :actionType")
    suspend fun deleteAction(mediaId: String, actionType: String)

    @Query("SELECT * FROM pending_actions LIMIT 1")
    suspend fun closeOpenCursors(): PendingActionEntity?
}