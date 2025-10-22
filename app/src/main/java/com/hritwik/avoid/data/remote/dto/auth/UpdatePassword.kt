package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePassword(
    val currentPassword: String? = null,
    @SerialName("CurrentPw")
    val currentPw: String? = null,
    @SerialName("NewPw")
    val newPw: String,
    @SerialName("ResetPassword")
    val resetPassword: Boolean = false
)
