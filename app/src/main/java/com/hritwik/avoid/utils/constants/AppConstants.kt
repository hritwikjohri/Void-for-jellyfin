package com.hritwik.avoid.utils.constants

object AppConstants {
    const val APP_NAME = "Void"
    const val APP_VERSION = "0.1.0"
    const val APP_CLIENT_NAME = "Void Client"
    const val DATABASE_NAME = "void_database"
    const val DATABASE_VERSION = 15
    const val IMAGE_CACHE_SIZE_PERCENT = 0.25
    const val DISK_CACHE_SIZE_PERCENT = 0.02
    const val DEFAULT_PAGE_SIZE = 50
    const val SMALL_PAGE_SIZE = 20
    const val LARGE_PAGE_SIZE = 100
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val IMAGE_TIMEOUT_SECONDS = 15L
    const val NETWORK_MAX_RETRY_ATTEMPTS = 3
    const val HTTP_CACHE_DIR = "http_cache"
    const val HTTP_CACHE_SIZE = 10L * 1024 * 1024 
    const val CDN_BASE_URL = "https://cdn.example.com"
    const val DOWNLOAD_CACHE_SIZE = 512L * 1024 * 1024 
    const val SPLASH_DELAY_MS = 2000L
    const val ANIMATION_DURATION_MS = 300
    const val LONG_ANIMATION_DURATION_MS = 500
    const val MIN_SEARCH_QUERY_LENGTH = 2
    const val MAX_RECENT_SEARCHES = 10
    const val POSTER_ASPECT_RATIO = 3f / 4f
    const val THUMBNAIL_ASPECT_RATIO = 16f / 9f
    const val SQUARE_ASPECT_RATIO = 1f
    const val RESUME_COMPLETION_THRESHOLD = 10_000_000L
}
