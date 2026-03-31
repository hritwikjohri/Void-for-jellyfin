package com.hritwik.avoid.domain.model.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ActivityLogEntry(
    val id: Long,
    val name: String,
    val overview: String?,
    val shortOverview: String?,
    val type: ActivityType,
    val itemId: String?,
    val date: String,
    val userId: String?,
    val userPrimaryImageTag: String?,
    val severity: LogSeverity
) : Parcelable {
    fun getUserImageUrl(serverUrl: String): String? {
        if (userId == null || userPrimaryImageTag == null) return null
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return "${baseUrl}Users/$userId/Images/Primary?tag=$userPrimaryImageTag&quality=90"
    }
}

@Serializable
enum class ActivityType(val displayName: String) {
    USER_AUTHENTICATION("Authentication"),
    USER_CREATED("User Created"),
    USER_DELETED("User Deleted"),
    USER_PASSWORD_CHANGED("Password Changed"),
    USER_LOCKED_OUT("User Locked Out"),
    SUBTITLES_DOWNLOADED("Subtitles Downloaded"),
    PLUGIN_INSTALLED("Plugin Installed"),
    PLUGIN_UNINSTALLED("Plugin Uninstalled"),
    PLUGIN_UPDATED("Plugin Updated"),
    PACKAGE_INSTALLATION_CANCELLED("Installation Cancelled"),
    PACKAGE_INSTALLATION_FAILED("Installation Failed"),
    TASK_COMPLETED("Task Completed"),
    VIDEO_PLAYBACK("Video Playback"),
    AUDIO_PLAYBACK("Audio Playback"),
    ITEM_ADDED("Item Added"),
    ITEM_REMOVED("Item Removed"),
    ITEM_UPDATED("Item Updated"),
    SESSION_STARTED("Session Started"),
    SESSION_ENDED("Session Ended"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(value: String): ActivityType {
            return when {
                value.contains("Authentication", ignoreCase = true) -> USER_AUTHENTICATION
                value.contains("UserCreated", ignoreCase = true) -> USER_CREATED
                value.contains("UserDeleted", ignoreCase = true) -> USER_DELETED
                value.contains("UserPasswordChanged", ignoreCase = true) -> USER_PASSWORD_CHANGED
                value.contains("UserLockedOut", ignoreCase = true) -> USER_LOCKED_OUT
                value.contains("SubtitlesDownloaded", ignoreCase = true) -> SUBTITLES_DOWNLOADED
                value.contains("PluginInstalled", ignoreCase = true) -> PLUGIN_INSTALLED
                value.contains("PluginUninstalled", ignoreCase = true) -> PLUGIN_UNINSTALLED
                value.contains("PluginUpdated", ignoreCase = true) -> PLUGIN_UPDATED
                value.contains("PackageInstallationCancelled", ignoreCase = true) -> PACKAGE_INSTALLATION_CANCELLED
                value.contains("PackageInstallationFailed", ignoreCase = true) -> PACKAGE_INSTALLATION_FAILED
                value.contains("TaskCompleted", ignoreCase = true) -> TASK_COMPLETED
                value.contains("VideoPlayback", ignoreCase = true) -> VIDEO_PLAYBACK
                value.contains("AudioPlayback", ignoreCase = true) -> AUDIO_PLAYBACK
                value.contains("ItemAdded", ignoreCase = true) -> ITEM_ADDED
                value.contains("ItemRemoved", ignoreCase = true) -> ITEM_REMOVED
                value.contains("ItemUpdated", ignoreCase = true) -> ITEM_UPDATED
                value.contains("SessionStarted", ignoreCase = true) -> SESSION_STARTED
                value.contains("SessionEnded", ignoreCase = true) -> SESSION_ENDED
                else -> UNKNOWN
            }
        }
    }
}

@Serializable
enum class LogSeverity {
    INFO,
    WARN,
    ERROR,
    DEBUG;

    companion object {
        fun fromString(value: String?): LogSeverity {
            return when (value?.uppercase()) {
                "INFO" -> INFO
                "WARN", "WARNING" -> WARN
                "ERROR" -> ERROR
                "DEBUG" -> DEBUG
                else -> INFO
            }
        }
    }
}

@Serializable
@Parcelize
data class ActivityLogResult(
    val items: List<ActivityLogEntry>,
    val totalRecordCount: Int,
    val startIndex: Int
) : Parcelable
