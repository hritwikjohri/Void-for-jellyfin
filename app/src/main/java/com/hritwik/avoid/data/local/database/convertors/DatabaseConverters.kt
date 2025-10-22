package com.hritwik.avoid.data.local.database.convertors

import androidx.room.TypeConverter
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import com.hritwik.avoid.domain.model.media.MediaStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class DatabaseConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    
    @TypeConverter
    fun fromNullableStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    
    @TypeConverter
    fun toNullableStringList(value: String?): List<String>? {
        return value?.let {
            try {
                json.decodeFromString<List<String>>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    
    @TypeConverter
    fun fromMediaItemList(value: List<MediaItemEntity>): String {
        return json.encodeToString(value)
    }

    
    @TypeConverter
    fun toMediaItemList(value: String): List<MediaItemEntity> {
        return try {
            json.decodeFromString<List<MediaItemEntity>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    
    @TypeConverter
    fun fromMediaStreamList(value: List<MediaStream>): String {
        return json.encodeToString(value)
    }

    
    @TypeConverter
    fun toMediaStreamList(value: String): List<MediaStream> {
        return try {
            json.decodeFromString<List<MediaStream>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    
    @TypeConverter
    fun fromMediaStream(value: MediaStream?): String? {
        return value?.let { json.encodeToString(it) }
    }

    
    @TypeConverter
    fun toMediaStream(value: String?): MediaStream? {
        return value?.let {
            try {
                json.decodeFromString<MediaStream>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}