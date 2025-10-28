package com.hritwik.avoid.utils.constants

import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.PlayerType

object PreferenceConstants {

    const val DATASTORE_NAME = "void_preferences"

    const val KEY_SERVER_URL = "server_url"
    const val KEY_SERVER_NAME = "server_name"
    const val KEY_SERVER_VERSION = "server_version"
    const val KEY_SERVER_CONNECTED = "server_connected"
    const val KEY_SERVER_CONNECTIONS = "server_connections"
    const val KEY_MTLS_ENABLED = "mtls_enabled"
    const val KEY_MTLS_CERTIFICATE_NAME = "mtls_certificate_name"
    const val KEY_MTLS_CERTIFICATE_PASSWORD = "mtls_certificate_password"
    const val KEY_OFFLINE_MODE = "offline_mode"

    const val KEY_USERNAME = "username"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_SERVER_ID = "server_id"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_LAST_LOGIN_TIME = "last_login_time"
    const val KEY_SESSION_VALID = "session_valid"

    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_DYNAMIC_COLORS = "dynamic_colors"
    const val KEY_AUTO_PLAY = "auto_play"
    const val KEY_CONTINUE_WATCHING = "continue_watching"
    const val KEY_DOWNLOAD_QUALITY = "download_quality"
    const val KEY_DOWNLOAD_CODEC = "download_codec"
    const val KEY_STREAMING_QUALITY = "streaming_quality"
    const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
    const val KEY_AUTO_DELETE_DOWNLOADS = "auto_delete_downloads"
    const val KEY_DOWNLOAD_LIMIT = "download_limit"
    const val KEY_DOWNLOAD_LOCATION = "download_location"

    const val KEY_SHOW_FEATURED_HEADER = "show_featured_header"
    const val KEY_AMBIENT_BACKGROUND = "ambient_background_enabled"
    const val KEY_NAVIGATE_EPISODES_TO_SEASON = "navigate_episodes_to_season"
    const val KEY_FONT_SCALE = "font_scale"
    const val KEY_PREFERRED_LANGUAGE = "preferred_language"
    const val KEY_GESTURE_CONTROLS = "gesture_controls"
    const val KEY_HIGH_CONTRAST = "high_contrast"

    const val KEY_FIRST_RUN_COMPLETED = "first_run_completed"

    const val KEY_RECENT_SEARCHES = "recent_searches"
    const val KEY_SEARCH_HISTORY_ENABLED = "search_history_enabled"

    const val KEY_PLAYBACK_SPEED = "playback_speed"
    const val KEY_SUBTITLE_ENABLED = "subtitle_enabled"
    const val KEY_SUBTITLE_SIZE = "subtitle_size"
    const val KEY_AUDIO_TRACK_LANGUAGE = "audio_track_language"
    const val KEY_SUBTITLE_LANGUAGE = "subtitle_language"

    const val KEY_PLAY_THEME_SONGS = "play_theme_songs"
    const val KEY_DISPLAY_MODE = "display_mode"
    const val KEY_DECODER_MODE = "decoder_mode"
    const val KEY_PREFERRED_VIDEO_CODEC = "preferred_video_codec"
    const val KEY_PREFERRED_AUDIO_CODEC = "preferred_audio_codec"
    const val KEY_AUTO_SKIP_SEGMENTS = "auto_skip_segments"
    const val KEY_PLAYER_TYPE = "player_type"

    const val KEY_LIBRARY_VIEW_TYPE = "library_view_type"
    const val KEY_LIBRARY_SORT_ORDER = "library_sort_order"
    const val KEY_LIBRARY_FILTER_WATCHED = "library_filter_watched"

    const val KEY_IMAGE_CACHE_SIZE = "image_cache_size"
    const val KEY_VIDEO_CACHE_SIZE = "video_cache_size"
    const val KEY_CACHE_WIFI_ONLY = "cache_wifi_only"
    const val KEY_MAX_STALE_DAYS = "max_stale_days"
    const val KEY_PREFETCH_ENABLED = "prefetch_enabled"

    const val KEY_SYNC_ENABLED = "sync_enabled"
    const val KEY_HEARTBEAT_ENABLED = "heartbeat_enabled"
    const val KEY_CLEANUP_ENABLED = "cleanup_enabled"

    const val KEY_JELLYSEER_BASE_URL = "jellyseer_base_url"
    const val KEY_JELLYSEER_API_KEY = "jellyseer_api_key"
    const val KEY_JELLYSEER_SESSION = "jellyseer_session_cookie"
    const val KEY_JELLYSEER_USER_ID = "jellyseer_user_id"
    const val KEY_JELLYSEER_USER_NAME = "jellyseer_user_name"
    const val KEY_JELLYSEER_USER_EMAIL = "jellyseer_user_email"

    const val KEY_DATA_USAGE_RX = "data_usage_rx"
    const val KEY_DATA_USAGE_TX = "data_usage_tx"
    const val KEY_DAILY_DATA_CAP = "daily_data_cap"
    const val KEY_MONTHLY_DATA_CAP = "monthly_data_cap"
    const val KEY_DAILY_DATA_USAGE = "daily_data_usage"
    const val KEY_MONTHLY_DATA_USAGE = "monthly_data_usage"
    const val KEY_LAST_DAILY_RESET = "last_daily_reset"
    const val KEY_LAST_MONTHLY_RESET = "last_monthly_reset"

    const val DEFAULT_THEME_MODE = "dark"
    const val DEFAULT_DYNAMIC_COLORS = false
    const val DEFAULT_AUTO_PLAY = true
    const val DEFAULT_CONTINUE_WATCHING = true
    const val DEFAULT_DOWNLOAD_QUALITY = "1080p"
    const val DEFAULT_DOWNLOAD_CODEC = "h264"
    const val DEFAULT_STREAMING_QUALITY = "auto"
    const val DEFAULT_DOWNLOAD_LOCATION = "internal"
    const val DEFAULT_SUBTITLE_SIZE = "medium"
    const val DEFAULT_LIBRARY_VIEW_TYPE = "grid"
    const val DEFAULT_LIBRARY_SORT_ORDER = "name"
    const val DEFAULT_IMAGE_CACHE_SIZE = 100 
    const val DEFAULT_VIDEO_CACHE_SIZE = 500 
    const val DEFAULT_CACHE_WIFI_ONLY = false
    const val DEFAULT_MAX_STALE_DAYS = 7 
    const val DEFAULT_PREFETCH_ENABLED = true
    const val DEFAULT_DOWNLOAD_WIFI_ONLY = true
    const val DEFAULT_AUTO_DELETE_DOWNLOADS = false
    const val DEFAULT_OFFLINE_MODE = false
    const val DEFAULT_SEARCH_HISTORY_ENABLED = true
    const val DEFAULT_PLAY_THEME_SONGS = false
    const val DEFAULT_SHOW_FEATURED_HEADER = false
    const val DEFAULT_AMBIENT_BACKGROUND = true
    const val DEFAULT_NAVIGATE_EPISODES_TO_SEASON = true
    const val DEFAULT_FONT_SCALE = 1.0f
    const val DEFAULT_PREFERRED_LANGUAGE = "en"
    const val DEFAULT_GESTURE_CONTROLS = true
    const val DEFAULT_HIGH_CONTRAST = false
    const val DEFAULT_MTLS_ENABLED = false
    const val DEFAULT_FIRST_RUN_COMPLETED = false
    const val DEFAULT_DISPLAY_MODE = "Fit Screen"
    const val SKIP_PROMPT_FLOATING_DURATION_MS = 5_000L
    val DEFAULT_DECODER_MODE = DecoderMode.AUTO.value
    val DEFAULT_PLAYER_TYPE = PlayerType.MPV.value
    const val DEFAULT_AUTO_SKIP_SEGMENTS = false
    const val DEFAULT_PREFERRED_VIDEO_CODEC = "h264"
    const val DEFAULT_PREFERRED_AUDIO_CODEC = "aac"

    const val DEFAULT_SYNC_ENABLED = true
    const val DEFAULT_HEARTBEAT_ENABLED = true
    const val DEFAULT_CLEANUP_ENABLED = true

    const val DEFAULT_JELLYSEER_BASE_URL = ""
    const val DEFAULT_JELLYSEER_API_KEY = ""
    const val DEFAULT_JELLYSEER_SESSION = ""
    const val DEFAULT_JELLYSEER_USER_NAME = ""
    const val DEFAULT_JELLYSEER_USER_EMAIL = ""


    
    const val QUALITY_AUTO = "auto"
    const val QUALITY_4K = "4k"
    const val QUALITY_1080P = "1080p"
    const val QUALITY_720P = "720p"
    const val QUALITY_480P = "480p"
    const val QUALITY_360P = "360p"

    
    const val SUBTITLE_SIZE_SMALL = "small"
    const val SUBTITLE_SIZE_MEDIUM = "medium"
    const val SUBTITLE_SIZE_LARGE = "large"
    const val SUBTITLE_SIZE_EXTRA_LARGE = "extra_large"

    
    const val VIEW_TYPE_GRID = "grid"
    const val VIEW_TYPE_LIST = "list"
    const val VIEW_TYPE_CARD = "card"

    
    const val SORT_BY_NAME = "name"
    const val SORT_BY_DATE_ADDED = "date_added"
    const val SORT_BY_RELEASE_DATE = "release_date"
    const val SORT_BY_RATING = "rating"
    const val SORT_BY_RUNTIME = "runtime"
}
