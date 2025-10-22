package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    @SerialName("LocalAddress")
    val localAddress: String? = null,
    @SerialName("ServerName")
    val serverName: String? = null,
    @SerialName("Version")
    val version: String? = null,
    @SerialName("ProductName")
    val productName: String? = null,
    @SerialName("OperatingSystem")
    val operatingSystem: String? = null,
    @SerialName("Id")
    val id: String? = null,
    @SerialName("StartupWizardCompleted")
    val startupWizardCompleted: Boolean? = null
)