package com.hritwik.avoid.domain.model.jellyseer

enum class JellyseerRequestStatus(val code: Int) {
    PENDING(1),
    APPROVED(2),
    DECLINED(3),
    FAILED(4),
    COMPLETED(5),
    PROCESSING(6);

    val isActiveRequest: Boolean
        get() = when (this) {
            PENDING, APPROVED, PROCESSING -> true
            else -> false
        }

    val shouldDisplayBadge: Boolean
        get() = this != DECLINED

    companion object {
        fun from(code: Int?): JellyseerRequestStatus? = when (code) {
            1 -> PENDING
            2 -> APPROVED
            3 -> DECLINED
            4 -> FAILED
            5 -> COMPLETED
            6 -> PROCESSING
            else -> null
        }
    }
}
