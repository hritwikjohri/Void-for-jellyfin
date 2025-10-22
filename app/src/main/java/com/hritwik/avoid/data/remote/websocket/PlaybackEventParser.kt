package com.hritwik.avoid.data.remote.websocket

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull


class PlaybackEventParser(private val json: Json = Json) {
    fun parse(message: String, userId: String, deviceId: String): PlaybackEvent? {
        val element = runCatching { json.parseToJsonElement(message) }.getOrNull() ?: return null
        val obj = element.jsonObject
        val msgType = obj["MessageType"]?.jsonPrimitive?.contentOrNull ?: return null
        val data = obj["Data"]?.jsonObject ?: return null
        
        val uid = data["UserId"]?.jsonPrimitive?.contentOrNull
        val did = obj["DeviceId"]?.jsonPrimitive?.contentOrNull
        if (uid != null && uid != userId) return null
        if (did != null && did != deviceId) return null

        val itemId = data["ItemId"]?.jsonPrimitive?.content ?: return null
        val playState = data["PlayState"]?.jsonObject ?: return null
        val position = playState["PositionTicks"]?.jsonPrimitive?.longOrNull ?: 0L
        val runtime = playState["RunTimeTicks"]?.jsonPrimitive?.longOrNull ?: 0L
        val date = data["DatePlayed"]?.jsonPrimitive?.contentOrNull
            ?: playState["PlaybackStartTime"]?.jsonPrimitive?.contentOrNull

        return when (msgType) {
            "PlayState" -> PlaybackEvent.Progress(itemId, position, runtime, date)
            "PlaybackStopped" -> PlaybackEvent.Stop(itemId, position, runtime, date)
            else -> null
        }
    }
}
