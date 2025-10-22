package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    @SerialName("Name")
    val name: String,
    @SerialName("Id")
    val id: String,
    @SerialName("HasPassword")
    val hasPassword: Boolean,
    @SerialName("HasConfiguredPassword")
    val hasConfiguredPassword: Boolean,
    @SerialName("HasConfiguredEasyPassword")
    val hasConfiguredEasyPassword: Boolean,
    @SerialName("EnableAutoLogin")
    val enableAutoLogin: Boolean? = false
)