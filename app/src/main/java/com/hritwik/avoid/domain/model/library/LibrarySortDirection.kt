package com.hritwik.avoid.domain.model.library


enum class LibrarySortDirection {
    ASCENDING,
    DESCENDING;

    fun toApiValue(): String = when (this) {
        ASCENDING -> "Ascending"
        DESCENDING -> "Descending"
    }

    companion object {
        fun fromApi(value: String?): LibrarySortDirection =
            if (value.equals("Descending", ignoreCase = true)) DESCENDING else ASCENDING
    }
}
