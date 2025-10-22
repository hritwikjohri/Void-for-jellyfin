package com.hritwik.avoid.domain.model.library

data class PendingAction(
    val mediaId: String,
    val actionType: String,
    val newValue: Boolean,
    val timestamp: Long
)