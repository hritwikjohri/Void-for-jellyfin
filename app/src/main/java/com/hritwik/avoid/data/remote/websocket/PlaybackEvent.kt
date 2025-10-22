package com.hritwik.avoid.data.remote.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


sealed class PlaybackEvent {
    
    @Serializable
    data class Progress(
        @SerialName("itemId") val itemId: String,
        @SerialName("positionTicks") val positionTicks: Long,
        @SerialName("runTimeTicks") val runTimeTicks: Long,
        @SerialName("datePlayed") val datePlayed: String?
    ) : PlaybackEvent()

    
    @Serializable
    data class Stop(
        @SerialName("itemId") val itemId: String,
        @SerialName("positionTicks") val positionTicks: Long,
        @SerialName("runTimeTicks") val runTimeTicks: Long,
        @SerialName("datePlayed") val datePlayed: String?
    ) : PlaybackEvent()
}
