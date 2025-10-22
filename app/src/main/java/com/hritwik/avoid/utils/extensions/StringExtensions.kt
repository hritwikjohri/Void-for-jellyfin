package com.hritwik.avoid.utils.extensions

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit




fun Long.formatRuntime(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this / 10000)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this / 10000) % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}


fun Long.formatFileSize(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "%.1f %s".format(size, units[unitIndex])
}


fun String.cleanServerUrl(): String {
    var cleanUrl = this.trim()

    
    if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
        cleanUrl = "http://$cleanUrl"
    }

    
    if (cleanUrl.endsWith("/")) {
        cleanUrl = cleanUrl.dropLast(1)
    }

    return cleanUrl
}


fun String.isValidServerUrl(): Boolean {
    val urlRegex = Regex(
        "^(https?://)?" + 
                "((([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,})|" + 
                "(\\d{1,3}\\.){3}\\d{1,3})" + 
                "(:\\d{2,5})?" + 
                "(/.*)?$" 
    )
    return urlRegex.matches(this.trim())
}


fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) {
        this
    } else {
        "${take(maxLength - 3)}..."
    }
}


fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}


fun String.toRelativeTime(): String {
    return try {
        val date = Instant.from(serverDateFormatter.parse(this))
        val diff = Duration.between(date, Instant.now())

        val days = diff.toDays()
        val hours = diff.toHours()
        val minutes = diff.toMinutes()

        when {
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes minutes ago"
            else -> "Just now"
        }
    } catch (e: Exception) {
        this
    }
}

private val serverDateFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    .withZone(ZoneOffset.UTC)

