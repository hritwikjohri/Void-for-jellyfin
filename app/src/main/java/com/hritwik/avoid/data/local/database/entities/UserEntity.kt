package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val hasPassword: Boolean,
    val serverId: String,
    val serverUrl: String,
    val lastLoginTime: Long = System.currentTimeMillis()
)
