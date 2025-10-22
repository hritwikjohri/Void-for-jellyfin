package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuickConnectResult(
    @SerialName("Secret")
    val secret: String? = null,
    @SerialName("Code")
    val code: String? = null,
    @SerialName("Authenticated")
    val authenticated: Boolean,
    @SerialName("DeviceId")
    val deviceId: String? = null,
    @SerialName("DeviceName")
    val deviceName: String? = null,
    @SerialName("AppName")
    val appName: String? = null,
    @SerialName("AppVersion")
    val appVersion: String? = null,
    @SerialName("DateAdded")
    val dateAdded: String? = null
)