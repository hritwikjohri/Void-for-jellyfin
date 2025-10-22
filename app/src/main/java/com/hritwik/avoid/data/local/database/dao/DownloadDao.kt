package com.hritwik.avoid.data.local.database.dao

import androidx.room.*
import com.hritwik.avoid.data.local.database.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE status = :status")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE mediaSourceId = :mediaSourceId LIMIT 1")
    suspend fun getDownloadByMediaSourceId(mediaSourceId: String): DownloadEntity?

    @Query("SELECT * FROM downloads")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads LIMIT 1")
    suspend fun closeOpenCursors(): DownloadEntity?

    @Query("UPDATE downloads SET queueIndex = :queueIndex, priority = :priority, addedAt = :addedAt WHERE mediaId = :mediaId")
    suspend fun updateQueueInfo(mediaId: String, queueIndex: Int, priority: Int, addedAt: Long)
}
