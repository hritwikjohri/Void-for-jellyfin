package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libraries")
data class LibraryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val itemCount: Int?,
    val primaryImageTag: String?,
    val isFolder: Boolean,
    val userId: String,
    val lastUpdated: Long = System.currentTimeMillis()
)