package com.hritwik.avoid.data.local.database.entities

import androidx.annotation.FloatRange
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.download.DownloadQuality

@Entity(
    tableName = "downloads",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["status"])]
)
data class DownloadEntity(
    @PrimaryKey
    val mediaId: String,
    val requestUri: String,
    val title: String,
    val type: String,
    @ColumnInfo(defaultValue = "")
    val serverUrl: String = "",
    @ColumnInfo(defaultValue = "")
    val accessToken: String = "",
    @ColumnInfo(defaultValue = "0")
    @FloatRange(from = 0.0, to = 100.0)
    val progress: Float,
    val status: String,
    @ColumnInfo(defaultValue = "0")
    val downloadedBytes: Long = 0L,
    val filePath: String? = null,
    val checksum: String? = null,
    val mediaSourceId: String? = null,
    val defaultVideoStream: MediaStream? = null,
    val audioStreams: List<MediaStream> = emptyList(),
    val subtitleStreams: List<MediaStream> = emptyList(),
    val subtitleFilePaths: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "0")
    val priority: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val addedAt: Long = 0L,
    @ColumnInfo(defaultValue = "-1")
    val queueIndex: Int = -1,
    @ColumnInfo(defaultValue = "FHD_1080")
    val quality: String = DownloadQuality.FHD_1080.name
)
