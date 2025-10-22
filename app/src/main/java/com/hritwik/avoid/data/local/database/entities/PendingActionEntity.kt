package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity

@Entity(tableName = "pending_actions", primaryKeys = ["mediaId", "actionType"])
data class PendingActionEntity(
    val mediaId: String,
    val actionType: String,
    val newValue: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)