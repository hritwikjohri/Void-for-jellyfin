package com.hritwik.avoid.utils.extensions

fun Map<String, String?>?.extractTvdbId(): String? {
    return this?.get("Tvdb")
}
