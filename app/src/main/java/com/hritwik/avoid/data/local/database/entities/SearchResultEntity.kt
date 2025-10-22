package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "search_results", primaryKeys = ["query", "filters"])
data class SearchResultEntity(
    val query: String,
    val filters: String,
    val results: List<MediaItemEntity>,
    val lastUpdated: Long = System.currentTimeMillis()
)