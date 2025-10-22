package com.hritwik.avoid.domain.model.jellyseer

enum class JellyseerAvailabilityStatus(val code: Int) {
    UNKNOWN(1),
    PENDING(2),
    PROCESSING(3),
    PARTIALLY_AVAILABLE(4),
    AVAILABLE(5),
    BLACKLISTED(6),
    DELETED(7);

    val isAvailable: Boolean
        get() = this == AVAILABLE || this == PARTIALLY_AVAILABLE

    companion object {
        fun from(code: Int?): JellyseerAvailabilityStatus = when (code) {
            2 -> PENDING
            3 -> PROCESSING
            4 -> PARTIALLY_AVAILABLE
            5 -> AVAILABLE
            6 -> BLACKLISTED
            7 -> DELETED
            else -> UNKNOWN
        }
    }
}
