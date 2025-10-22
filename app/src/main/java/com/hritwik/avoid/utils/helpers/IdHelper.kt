package com.hritwik.avoid.utils.helpers

import java.util.UUID

fun normalizeUuid(id: String): String {
    return when {
        id.length == 32 -> buildString {
            append(id.substring(0, 8))
            append('-')
            append(id.substring(8, 12))
            append('-')
            append(id.substring(12, 16))
            append('-')
            append(id.substring(16, 20))
            append('-')
            append(id.substring(20))
        }
        else -> runCatching { UUID.fromString(id).toString() }.getOrElse { id }
    }
}
