package com.hritwik.avoid.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.hritwik.avoid.data.local.model.PlaybackPreferences
import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod
import com.hritwik.avoid.domain.model.jellyseer.JellyseerConfig
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.domain.model.playback.PreferredAudioCodec
import com.hritwik.avoid.domain.model.playback.PreferredVideoCodec
import com.hritwik.avoid.utils.constants.PreferenceConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PreferenceConstants.DATASTORE_NAME)

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_preferences"
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    private val authDataStore = context.authDataStore

    init {
        runBlocking { migrateAuthDataIfNeeded() }
    }

    companion object {
        
        private val SERVER_URL = stringPreferencesKey(PreferenceConstants.KEY_SERVER_URL)
        private val SERVER_NAME = stringPreferencesKey(PreferenceConstants.KEY_SERVER_NAME)
        private val SERVER_VERSION = stringPreferencesKey(PreferenceConstants.KEY_SERVER_VERSION)
        private val SERVER_CONNECTED = stringPreferencesKey(PreferenceConstants.KEY_SERVER_CONNECTED)
        private val SERVER_CONNECTIONS = stringPreferencesKey(PreferenceConstants.KEY_SERVER_CONNECTIONS)

        
        private val USERNAME = stringPreferencesKey(PreferenceConstants.KEY_USERNAME)
        private val ACCESS_TOKEN = stringPreferencesKey(PreferenceConstants.KEY_ACCESS_TOKEN)
        private val USER_ID = stringPreferencesKey(PreferenceConstants.KEY_USER_ID)
        private val SERVER_ID = stringPreferencesKey(PreferenceConstants.KEY_SERVER_ID)
        private val DEVICE_ID = stringPreferencesKey(PreferenceConstants.KEY_DEVICE_ID)
        private val LAST_LOGIN_TIME = stringPreferencesKey(PreferenceConstants.KEY_LAST_LOGIN_TIME)
        private val SESSION_VALID = stringPreferencesKey(PreferenceConstants.KEY_SESSION_VALID)
        private val AUTH_MIGRATED = booleanPreferencesKey("auth_migrated")
        private val THEME_MODE = stringPreferencesKey(PreferenceConstants.KEY_THEME_MODE)
        private val DYNAMIC_COLORS = booleanPreferencesKey(PreferenceConstants.KEY_DYNAMIC_COLORS)
        private val SHOW_FEATURED_HEADER = booleanPreferencesKey(PreferenceConstants.KEY_SHOW_FEATURED_HEADER)
        private val AMBIENT_BACKGROUND = booleanPreferencesKey(PreferenceConstants.KEY_AMBIENT_BACKGROUND)
        private val NAVIGATE_EPISODES_TO_SEASON =
            booleanPreferencesKey(PreferenceConstants.KEY_NAVIGATE_EPISODES_TO_SEASON)
        private val FONT_SCALE = floatPreferencesKey(PreferenceConstants.KEY_FONT_SCALE)
        private val PREFERRED_LANGUAGE = stringPreferencesKey(PreferenceConstants.KEY_PREFERRED_LANGUAGE)
        private val GESTURE_CONTROLS = booleanPreferencesKey(PreferenceConstants.KEY_GESTURE_CONTROLS)
        private val HIGH_CONTRAST = booleanPreferencesKey(PreferenceConstants.KEY_HIGH_CONTRAST)
        private val FIRST_RUN_COMPLETED = booleanPreferencesKey(PreferenceConstants.KEY_FIRST_RUN_COMPLETED)
        private val RECENT_SEARCHES = stringPreferencesKey(PreferenceConstants.KEY_RECENT_SEARCHES)

        
        private val AUTO_PLAY = booleanPreferencesKey(PreferenceConstants.KEY_AUTO_PLAY)
        private val CONTINUE_WATCHING = booleanPreferencesKey(PreferenceConstants.KEY_CONTINUE_WATCHING)
        private val STREAMING_QUALITY = stringPreferencesKey(PreferenceConstants.KEY_STREAMING_QUALITY)
        private val DOWNLOAD_QUALITY = stringPreferencesKey(PreferenceConstants.KEY_DOWNLOAD_QUALITY)
        private val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey(PreferenceConstants.KEY_DOWNLOAD_WIFI_ONLY)
        private val AUTO_DELETE_DOWNLOADS = booleanPreferencesKey(PreferenceConstants.KEY_AUTO_DELETE_DOWNLOADS)
        private val DOWNLOAD_LIMIT = longPreferencesKey(PreferenceConstants.KEY_DOWNLOAD_LIMIT)
        private val DOWNLOAD_LOCATION = stringPreferencesKey(PreferenceConstants.KEY_DOWNLOAD_LOCATION)
        private val DOWNLOAD_CODEC = stringPreferencesKey(PreferenceConstants.KEY_DOWNLOAD_CODEC)
        private val PLAYBACK_SPEED = stringPreferencesKey(PreferenceConstants.KEY_PLAYBACK_SPEED)
        private val SUBTITLE_ENABLED = booleanPreferencesKey(PreferenceConstants.KEY_SUBTITLE_ENABLED)
        private val SUBTITLE_SIZE = stringPreferencesKey(PreferenceConstants.KEY_SUBTITLE_SIZE)
        private val AUDIO_TRACK_LANGUAGE = stringPreferencesKey(PreferenceConstants.KEY_AUDIO_TRACK_LANGUAGE)
        private val SUBTITLE_LANGUAGE = stringPreferencesKey(PreferenceConstants.KEY_SUBTITLE_LANGUAGE)
        private val PLAY_THEME_SONGS = booleanPreferencesKey(PreferenceConstants.KEY_PLAY_THEME_SONGS)
        private val DISPLAY_MODE = stringPreferencesKey(PreferenceConstants.KEY_DISPLAY_MODE)
        private val DECODER_MODE = stringPreferencesKey(PreferenceConstants.KEY_DECODER_MODE)
        private val PREFERRED_VIDEO_CODEC = stringPreferencesKey(PreferenceConstants.KEY_PREFERRED_VIDEO_CODEC)
        private val PREFERRED_AUDIO_CODEC = stringPreferencesKey(PreferenceConstants.KEY_PREFERRED_AUDIO_CODEC)
        private val AUTO_SKIP_SEGMENTS = booleanPreferencesKey(PreferenceConstants.KEY_AUTO_SKIP_SEGMENTS)
        private val PLAYER_TYPE = stringPreferencesKey(PreferenceConstants.KEY_PLAYER_TYPE)

        
        private val IMAGE_CACHE_SIZE = longPreferencesKey(PreferenceConstants.KEY_IMAGE_CACHE_SIZE)
        private val VIDEO_CACHE_SIZE = longPreferencesKey(PreferenceConstants.KEY_VIDEO_CACHE_SIZE)
        private val CACHE_WIFI_ONLY = booleanPreferencesKey(PreferenceConstants.KEY_CACHE_WIFI_ONLY)
        private val MAX_STALE_DAYS = intPreferencesKey(PreferenceConstants.KEY_MAX_STALE_DAYS)
        private val PREFETCH_ENABLED = booleanPreferencesKey(PreferenceConstants.KEY_PREFETCH_ENABLED)

        
        private val DATA_USAGE_RX = longPreferencesKey(PreferenceConstants.KEY_DATA_USAGE_RX)
        private val DATA_USAGE_TX = longPreferencesKey(PreferenceConstants.KEY_DATA_USAGE_TX)
        private val DAILY_DATA_CAP = longPreferencesKey(PreferenceConstants.KEY_DAILY_DATA_CAP)
        private val MONTHLY_DATA_CAP = longPreferencesKey(PreferenceConstants.KEY_MONTHLY_DATA_CAP)
        private val DAILY_DATA_USAGE = longPreferencesKey(PreferenceConstants.KEY_DAILY_DATA_USAGE)
        private val MONTHLY_DATA_USAGE = longPreferencesKey(PreferenceConstants.KEY_MONTHLY_DATA_USAGE)
        private val LAST_DAILY_RESET = stringPreferencesKey(PreferenceConstants.KEY_LAST_DAILY_RESET)
        private val LAST_MONTHLY_RESET = stringPreferencesKey(PreferenceConstants.KEY_LAST_MONTHLY_RESET)

        private val SYNC_ENABLED = booleanPreferencesKey(PreferenceConstants.KEY_SYNC_ENABLED)
        private val HEARTBEAT_ENABLED = booleanPreferencesKey(PreferenceConstants.KEY_HEARTBEAT_ENABLED)
        private val CLEANUP_ENABLED = booleanPreferencesKey(PreferenceConstants.KEY_CLEANUP_ENABLED)

        private val JELLYSEER_BASE_URL = stringPreferencesKey(PreferenceConstants.KEY_JELLYSEER_BASE_URL)
        private val JELLYSEER_API_KEY = stringPreferencesKey(PreferenceConstants.KEY_JELLYSEER_API_KEY)
        private val JELLYSEER_SESSION = stringPreferencesKey(PreferenceConstants.KEY_JELLYSEER_SESSION)
        private val JELLYSEER_USER_ID = longPreferencesKey(PreferenceConstants.KEY_JELLYSEER_USER_ID)
        private val JELLYSEER_USER_NAME = stringPreferencesKey(PreferenceConstants.KEY_JELLYSEER_USER_NAME)
        private val JELLYSEER_USER_EMAIL = stringPreferencesKey(PreferenceConstants.KEY_JELLYSEER_USER_EMAIL)

        
        private const val PLAYBACK_POSITION_PREFIX = "playback_position_"
        private const val PLAYBACK_PREFERENCES_PREFIX = "playback_preferences"
    }

    

    fun getThemeMode(): Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: PreferenceConstants.DEFAULT_THEME_MODE
    }

    fun getDynamicColors(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLORS] ?: PreferenceConstants.DEFAULT_DYNAMIC_COLORS
    }

    fun getShowFeaturedHeader(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHOW_FEATURED_HEADER] ?: PreferenceConstants.DEFAULT_SHOW_FEATURED_HEADER
    }

    fun getAmbientBackgroundEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AMBIENT_BACKGROUND] ?: PreferenceConstants.DEFAULT_AMBIENT_BACKGROUND
    }

    fun getNavigateEpisodesToSeason(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NAVIGATE_EPISODES_TO_SEASON]
            ?: PreferenceConstants.DEFAULT_NAVIGATE_EPISODES_TO_SEASON
    }

    fun getFontScale(): Flow<Float> = dataStore.data.map { prefs ->
        prefs[FONT_SCALE] ?: PreferenceConstants.DEFAULT_FONT_SCALE
    }

    fun getPreferredLanguage(): Flow<String> = dataStore.data.map { prefs ->
        prefs[PREFERRED_LANGUAGE] ?: PreferenceConstants.DEFAULT_PREFERRED_LANGUAGE
    }

    fun getGestureControlsEnabled(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[GESTURE_CONTROLS] ?: PreferenceConstants.DEFAULT_GESTURE_CONTROLS
    }

    fun getHighContrastEnabled(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HIGH_CONTRAST] ?: PreferenceConstants.DEFAULT_HIGH_CONTRAST
    }

    fun getJellyseerConfig(): Flow<JellyseerConfig> = dataStore.data.map { preferences ->
        val url = preferences[JELLYSEER_BASE_URL] ?: PreferenceConstants.DEFAULT_JELLYSEER_BASE_URL
        val apiKey = preferences[JELLYSEER_API_KEY] ?: PreferenceConstants.DEFAULT_JELLYSEER_API_KEY
        val session = preferences[JELLYSEER_SESSION] ?: PreferenceConstants.DEFAULT_JELLYSEER_SESSION
        val userId = preferences[JELLYSEER_USER_ID]
        val userName = preferences[JELLYSEER_USER_NAME] ?: PreferenceConstants.DEFAULT_JELLYSEER_USER_NAME
        val userEmail = preferences[JELLYSEER_USER_EMAIL] ?: PreferenceConstants.DEFAULT_JELLYSEER_USER_EMAIL
        JellyseerConfig(
            baseUrl = url.trim(),
            apiKey = apiKey.trim(),
            sessionCookie = session.trim(),
            userId = userId,
            userEmail = userEmail.trim().ifBlank { null },
            userDisplayName = userName.trim().ifBlank { null }
        )
    }

    suspend fun initializeThemePreferences() {
        dataStore.edit { preferences ->
            if (!preferences.contains(THEME_MODE)) {
                preferences[THEME_MODE] = PreferenceConstants.DEFAULT_THEME_MODE
            }
            if (!preferences.contains(DYNAMIC_COLORS)) {
                preferences[DYNAMIC_COLORS] = PreferenceConstants.DEFAULT_DYNAMIC_COLORS
            }
            if (!preferences.contains(AUTO_PLAY)) {
                preferences[AUTO_PLAY] = PreferenceConstants.DEFAULT_AUTO_PLAY
            }
            if (!preferences.contains(CONTINUE_WATCHING)) {
                preferences[CONTINUE_WATCHING] = PreferenceConstants.DEFAULT_CONTINUE_WATCHING
            }
            if (!preferences.contains(STREAMING_QUALITY)) {
                preferences[STREAMING_QUALITY] = PreferenceConstants.DEFAULT_STREAMING_QUALITY
            }
            if (!preferences.contains(DOWNLOAD_QUALITY)) {
                preferences[DOWNLOAD_QUALITY] = PreferenceConstants.DEFAULT_DOWNLOAD_QUALITY
            }
            if (!preferences.contains(DOWNLOAD_CODEC)) {
                preferences[DOWNLOAD_CODEC] = PreferenceConstants.DEFAULT_DOWNLOAD_CODEC
            }
            if (!preferences.contains(DOWNLOAD_LOCATION)) {
                preferences[DOWNLOAD_LOCATION] = PreferenceConstants.DEFAULT_DOWNLOAD_LOCATION
            }
            if (!preferences.contains(PLAY_THEME_SONGS)) {
                preferences[PLAY_THEME_SONGS] = PreferenceConstants.DEFAULT_PLAY_THEME_SONGS
            }
            if (!preferences.contains(PREFERRED_VIDEO_CODEC)) {
                preferences[PREFERRED_VIDEO_CODEC] = PreferenceConstants.DEFAULT_PREFERRED_VIDEO_CODEC
            }
            if (!preferences.contains(PREFERRED_AUDIO_CODEC)) {
                preferences[PREFERRED_AUDIO_CODEC] = PreferenceConstants.DEFAULT_PREFERRED_AUDIO_CODEC
            }
            if (!preferences.contains(AUTO_SKIP_SEGMENTS)) {
                preferences[AUTO_SKIP_SEGMENTS] = PreferenceConstants.DEFAULT_AUTO_SKIP_SEGMENTS
            }
            if (!preferences.contains(DISPLAY_MODE)) {
                preferences[DISPLAY_MODE] = PreferenceConstants.DEFAULT_DISPLAY_MODE
            }
            if (!preferences.contains(DECODER_MODE)) {
                preferences[DECODER_MODE] = PreferenceConstants.DEFAULT_DECODER_MODE
            }
            if (!preferences.contains(PLAYER_TYPE)) {
                preferences[PLAYER_TYPE] = PreferenceConstants.DEFAULT_PLAYER_TYPE
            }
            if (!preferences.contains(SHOW_FEATURED_HEADER)) {
                preferences[SHOW_FEATURED_HEADER] = PreferenceConstants.DEFAULT_SHOW_FEATURED_HEADER
            }
            if (!preferences.contains(AMBIENT_BACKGROUND)) {
                preferences[AMBIENT_BACKGROUND] = PreferenceConstants.DEFAULT_AMBIENT_BACKGROUND
            }
            if (!preferences.contains(FONT_SCALE)) {
                preferences[FONT_SCALE] = PreferenceConstants.DEFAULT_FONT_SCALE
            }
            if (!preferences.contains(PREFERRED_LANGUAGE)) {
                preferences[PREFERRED_LANGUAGE] = PreferenceConstants.DEFAULT_PREFERRED_LANGUAGE
            }
            if (!preferences.contains(GESTURE_CONTROLS)) {
                preferences[GESTURE_CONTROLS] = PreferenceConstants.DEFAULT_GESTURE_CONTROLS
            }
            if (!preferences.contains(HIGH_CONTRAST)) {
                preferences[HIGH_CONTRAST] = PreferenceConstants.DEFAULT_HIGH_CONTRAST
            }
            if (!preferences.contains(SYNC_ENABLED)) {
                preferences[SYNC_ENABLED] = PreferenceConstants.DEFAULT_SYNC_ENABLED
            }
            if (!preferences.contains(HEARTBEAT_ENABLED)) {
                preferences[HEARTBEAT_ENABLED] = PreferenceConstants.DEFAULT_HEARTBEAT_ENABLED
            }
            if (!preferences.contains(CLEANUP_ENABLED)) {
                preferences[CLEANUP_ENABLED] = PreferenceConstants.DEFAULT_CLEANUP_ENABLED
            }
            if (!preferences.contains(PREFETCH_ENABLED)) {
                preferences[PREFETCH_ENABLED] = PreferenceConstants.DEFAULT_PREFETCH_ENABLED
            }
        }
    }

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun updateJellyseerBaseUrl(baseUrl: String) {
        val sanitized = baseUrl.trim()
        dataStore.edit { it[JELLYSEER_BASE_URL] = sanitized }
    }

    suspend fun updateJellyseerApiKey(apiKey: String) {
        dataStore.edit { it[JELLYSEER_API_KEY] = apiKey.trim() }
    }

    suspend fun updateJellyseerSessionCookie(cookie: String) {
        dataStore.edit { it[JELLYSEER_SESSION] = cookie.trim() }
    }

    suspend fun updateJellyseerUser(id: Long?, displayName: String?, email: String?) {
        dataStore.edit { preferences ->
            if (id != null) {
                preferences[JELLYSEER_USER_ID] = id
            } else {
                preferences.remove(JELLYSEER_USER_ID)
            }
            preferences[JELLYSEER_USER_NAME] = displayName?.trim().orEmpty()
            preferences[JELLYSEER_USER_EMAIL] = email?.trim().orEmpty()
        }
    }

    suspend fun clearJellyseerSession() {
        dataStore.edit { preferences ->
            preferences[JELLYSEER_SESSION] = PreferenceConstants.DEFAULT_JELLYSEER_SESSION
            preferences.remove(JELLYSEER_USER_ID)
            preferences[JELLYSEER_USER_NAME] = PreferenceConstants.DEFAULT_JELLYSEER_USER_NAME
            preferences[JELLYSEER_USER_EMAIL] = PreferenceConstants.DEFAULT_JELLYSEER_USER_EMAIL
        }
    }

    suspend fun saveFontScale(scale: Float) {
        dataStore.edit { it[FONT_SCALE] = scale }
    }

    suspend fun savePreferredLanguage(language: String) {
        dataStore.edit { it[PREFERRED_LANGUAGE] = language }
    }

    suspend fun saveGestureControlsEnabled(enabled: Boolean) {
        dataStore.edit { it[GESTURE_CONTROLS] = enabled }
    }

    suspend fun saveHighContrastEnabled(enabled: Boolean) {
        dataStore.edit { it[HIGH_CONTRAST] = enabled }
    }

    

    fun isFirstRunCompleted(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[FIRST_RUN_COMPLETED] ?: PreferenceConstants.DEFAULT_FIRST_RUN_COMPLETED
    }

    suspend fun setFirstRunCompleted(completed: Boolean) {
        dataStore.edit { it[FIRST_RUN_COMPLETED] = completed }
    }

    

    fun getSyncEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SYNC_ENABLED] ?: PreferenceConstants.DEFAULT_SYNC_ENABLED
    }

    suspend fun saveSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[SYNC_ENABLED] = enabled }
    }

    fun getHeartbeatEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HEARTBEAT_ENABLED] ?: PreferenceConstants.DEFAULT_HEARTBEAT_ENABLED
    }

    suspend fun saveHeartbeatEnabled(enabled: Boolean) {
        dataStore.edit { it[HEARTBEAT_ENABLED] = enabled }
    }

    fun getCleanupEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CLEANUP_ENABLED] ?: PreferenceConstants.DEFAULT_CLEANUP_ENABLED
    }

    suspend fun saveCleanupEnabled(enabled: Boolean) {
        dataStore.edit { it[CLEANUP_ENABLED] = enabled }
    }

    

    suspend fun saveServerConfig(url: String, name: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
            preferences[SERVER_NAME] = name
        }
    }

    suspend fun saveServerUrlOnly(url: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }

    suspend fun clearServerUrl() {
        dataStore.edit { preferences ->
            preferences.remove(SERVER_URL)
        }
    }

    suspend fun saveServerDetails(serverVersion: String, serverConnected: Boolean) {
        dataStore.edit { preferences ->
            preferences[SERVER_VERSION] = serverVersion
            preferences[SERVER_CONNECTED] = serverConnected.toString()
        }
    }

    fun getServerUrl(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[SERVER_URL]
    }

    fun getServerName(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[SERVER_NAME]
    }

    fun getServerVersion(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[SERVER_VERSION]
    }

    fun getServerConnected(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SERVER_CONNECTED]?.toBoolean() ?: false
    }

    fun getServerConnections(): Flow<List<ServerConnectionMethod>> = dataStore.data.map { preferences ->
        preferences[SERVER_CONNECTIONS]?.let { json ->
            runCatching { Json.decodeFromString<List<ServerConnectionMethod>>(json) }
                .getOrDefault(emptyList())
                .distinctBy { it.url.lowercase() }
        } ?: emptyList()
    }

    suspend fun saveServerConnections(methods: List<ServerConnectionMethod>) {
        dataStore.edit { preferences ->
            preferences[SERVER_CONNECTIONS] = Json.encodeToString(methods)
        }
    }

    suspend fun clearServerConfiguration() {
        dataStore.edit { preferences ->
            preferences.remove(SERVER_URL)
            preferences.remove(SERVER_NAME)
            preferences.remove(SERVER_VERSION)
            preferences.remove(SERVER_CONNECTED)
            preferences[SERVER_CONNECTIONS] = Json.encodeToString<List<ServerConnectionMethod>>(emptyList())
        }
    }

    fun getOfflineMode(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[OFFLINE_MODE] ?: PreferenceConstants.DEFAULT_OFFLINE_MODE
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[OFFLINE_MODE] = enabled
        }
    }

    

    suspend fun saveAuthData(
        username: String,
        accessToken: String,
        userId: String,
        serverId: String
    ) {
        authDataStore.edit { preferences ->
            preferences[USERNAME] = username
            preferences[ACCESS_TOKEN] = accessToken
            preferences[USER_ID] = userId
            preferences[SERVER_ID] = serverId
            preferences[LAST_LOGIN_TIME] = System.currentTimeMillis().toString()
            preferences[SESSION_VALID] = "true"
        }
    }

    fun getUsername(): Flow<String?> = authDataStore.data.map { preferences ->
        preferences[USERNAME]
    }

    fun getAccessToken(): Flow<String?> = authDataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }

    fun getUserId(): Flow<String?> = authDataStore.data.map { preferences ->
        preferences[USER_ID]
    }

    fun getServerId(): Flow<String?> = authDataStore.data.map { preferences ->
        preferences[SERVER_ID]
    }

    fun getLastLoginTime(): Flow<Long?> = authDataStore.data.map { preferences ->
        preferences[LAST_LOGIN_TIME]?.toLongOrNull()
    }

    fun isSessionValid(): Flow<Boolean> = authDataStore.data.map { preferences ->
        preferences[SESSION_VALID] == "true"
    }

    suspend fun invalidateSession() {
        authDataStore.edit { preferences ->
            preferences[SESSION_VALID] = "false"
        }
    }

    fun isLoggedIn(): Flow<Boolean> = combine(authDataStore.data, dataStore.data) { authPrefs, prefs ->
        val hasToken = authPrefs[ACCESS_TOKEN] != null
        val hasUserId = authPrefs[USER_ID] != null
        val hasServerUrl = prefs[SERVER_URL] != null
        val isSessionValid = authPrefs[SESSION_VALID] == "true"
        hasToken && hasUserId && hasServerUrl && isSessionValid
    }

    suspend fun clearAuthData() {
        authDataStore.edit { preferences ->
            preferences.remove(USERNAME)
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(USER_ID)
            preferences.remove(SERVER_ID)
            preferences.remove(LAST_LOGIN_TIME)
            preferences.remove(SESSION_VALID)
        }
    }

    private suspend fun migrateAuthDataIfNeeded() {
        val migrated = dataStore.data.map { it[AUTH_MIGRATED] ?: false }.first()
        if (!migrated) {
            val oldPrefs = dataStore.data.first()
            authDataStore.edit { enc ->
                oldPrefs[USERNAME]?.let { enc[USERNAME] = it }
                oldPrefs[ACCESS_TOKEN]?.let { enc[ACCESS_TOKEN] = it }
                oldPrefs[USER_ID]?.let { enc[USER_ID] = it }
                oldPrefs[SERVER_ID]?.let { enc[SERVER_ID] = it }
                oldPrefs[LAST_LOGIN_TIME]?.let { enc[LAST_LOGIN_TIME] = it }
                oldPrefs[SESSION_VALID]?.let { enc[SESSION_VALID] = it }
            }
            dataStore.edit { prefs ->
                prefs.remove(USERNAME)
                prefs.remove(ACCESS_TOKEN)
                prefs.remove(USER_ID)
                prefs.remove(SERVER_ID)
                prefs.remove(LAST_LOGIN_TIME)
                prefs.remove(SESSION_VALID)
                prefs[AUTH_MIGRATED] = true
            }
        }
    }

    

    fun getRecentSearches(): Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[RECENT_SEARCHES]?.let { json ->
            runCatching { Json.decodeFromString<List<String>>(json) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun saveRecentSearches(searches: List<String>) {
        dataStore.edit { preferences ->
            preferences[RECENT_SEARCHES] = Json.encodeToString(searches)
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCHES)
        }
    }

    

    fun getAutoPlay(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_PLAY] ?: PreferenceConstants.DEFAULT_AUTO_PLAY
    }

    fun getContinueWatching(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CONTINUE_WATCHING] ?: PreferenceConstants.DEFAULT_CONTINUE_WATCHING
    }

    fun getStreamingQuality(): Flow<String> = dataStore.data.map { preferences ->
        preferences[STREAMING_QUALITY] ?: PreferenceConstants.DEFAULT_STREAMING_QUALITY
    }

    fun getDownloadQuality(): Flow<String> = dataStore.data.map { preferences ->
        preferences[DOWNLOAD_QUALITY] ?: PreferenceConstants.DEFAULT_DOWNLOAD_QUALITY
    }

    fun getDownloadWifiOnly(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DOWNLOAD_WIFI_ONLY] ?: PreferenceConstants.DEFAULT_DOWNLOAD_WIFI_ONLY
    }

    fun getDownloadCodec(): Flow<String> = dataStore.data.map { preferences ->
        preferences[DOWNLOAD_CODEC] ?: PreferenceConstants.DEFAULT_DOWNLOAD_CODEC
    }

    fun getAutoDeleteDownloads(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_DELETE_DOWNLOADS] ?: PreferenceConstants.DEFAULT_AUTO_DELETE_DOWNLOADS
    }

    fun getDownloadLocation(): Flow<String> = dataStore.data.map { preferences ->
        preferences[DOWNLOAD_LOCATION] ?: PreferenceConstants.DEFAULT_DOWNLOAD_LOCATION
    }

    fun getPlaybackSpeed(): Flow<Float> = dataStore.data.map { preferences ->
        preferences[PLAYBACK_SPEED]?.toFloatOrNull() ?: 1.0f
    }

    fun getSubtitleEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_ENABLED] ?: false
    }

    fun getSubtitleSize(): Flow<String> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_SIZE] ?: PreferenceConstants.DEFAULT_SUBTITLE_SIZE
    }

    fun getAudioTrackLanguage(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUDIO_TRACK_LANGUAGE]
    }

    fun getSubtitleLanguage(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[SUBTITLE_LANGUAGE]
    }

    fun getPlayThemeSongs(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PLAY_THEME_SONGS] ?: PreferenceConstants.DEFAULT_PLAY_THEME_SONGS
    }

    fun getAutoSkipSegments(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_SKIP_SEGMENTS] ?: PreferenceConstants.DEFAULT_AUTO_SKIP_SEGMENTS
    }

    fun getDisplayMode(): Flow<DisplayMode> = dataStore.data.map { preferences ->
        DisplayMode.fromValue(preferences[DISPLAY_MODE] ?: PreferenceConstants.DEFAULT_DISPLAY_MODE)
    }

    fun getDecoderMode(): Flow<DecoderMode> = dataStore.data.map { preferences ->
        DecoderMode.fromValue(preferences[DECODER_MODE] ?: PreferenceConstants.DEFAULT_DECODER_MODE)
    }

    fun getPreferredVideoCodec(): Flow<PreferredVideoCodec> = dataStore.data.map { preferences ->
        PreferredVideoCodec.fromPreferenceValue(
            preferences[PREFERRED_VIDEO_CODEC] ?: PreferenceConstants.DEFAULT_PREFERRED_VIDEO_CODEC
        )
    }

    fun getPreferredAudioCodec(): Flow<PreferredAudioCodec> = dataStore.data.map { preferences ->
        PreferredAudioCodec.fromPreferenceValue(
            preferences[PREFERRED_AUDIO_CODEC] ?: PreferenceConstants.DEFAULT_PREFERRED_AUDIO_CODEC
        )
    }

    fun getPlayerType(): Flow<PlayerType> = dataStore.data.map { preferences ->
        PlayerType.fromValue(preferences[PLAYER_TYPE] ?: PreferenceConstants.DEFAULT_PLAYER_TYPE)
    }

    

    fun getImageCacheSize(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[IMAGE_CACHE_SIZE] ?: PreferenceConstants.DEFAULT_IMAGE_CACHE_SIZE.toLong()
    }

    fun getVideoCacheSize(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[VIDEO_CACHE_SIZE] ?: PreferenceConstants.DEFAULT_VIDEO_CACHE_SIZE.toLong()
    }

    fun getCacheWifiOnly(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CACHE_WIFI_ONLY] ?: PreferenceConstants.DEFAULT_CACHE_WIFI_ONLY
    }

    fun getMaxStaleDays(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[MAX_STALE_DAYS] ?: PreferenceConstants.DEFAULT_MAX_STALE_DAYS
    }

    fun getPrefetchEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PREFETCH_ENABLED] ?: PreferenceConstants.DEFAULT_PREFETCH_ENABLED
    }

    suspend fun savePrefetchEnabled(enabled: Boolean) {
        dataStore.edit { it[PREFETCH_ENABLED] = enabled }
    }

    suspend fun saveAutoPlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_PLAY] = enabled
        }
    }

    suspend fun saveContinueWatching(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CONTINUE_WATCHING] = enabled
        }
    }

    suspend fun saveStreamingQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[STREAMING_QUALITY] = quality
        }
    }

    suspend fun saveDownloadQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[DOWNLOAD_QUALITY] = quality
        }
    }

    suspend fun saveDownloadCodec(codec: String) {
        dataStore.edit { preferences ->
            preferences[DOWNLOAD_CODEC] = codec
        }
    }

    suspend fun saveDownloadWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DOWNLOAD_WIFI_ONLY] = enabled
        }
    }

    suspend fun saveAutoDeleteDownloads(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_DELETE_DOWNLOADS] = enabled
        }
    }

    suspend fun saveDownloadLimit(limit: Long) {
        dataStore.edit { preferences ->
            preferences[DOWNLOAD_LIMIT] = limit
        }
    }

    suspend fun saveDownloadLocation(location: String) {
        dataStore.edit { preferences ->
            preferences[DOWNLOAD_LOCATION] = location
        }
    }

    suspend fun savePlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED] = speed.toString()
        }
    }

    suspend fun saveSubtitleEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_ENABLED] = enabled
        }
    }

    suspend fun saveSubtitleSize(size: String) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_SIZE] = size
        }
    }

    suspend fun saveAudioTrackLanguage(language: String?) {
        dataStore.edit { preferences ->
            if (language != null) {
                preferences[AUDIO_TRACK_LANGUAGE] = language
            } else {
                preferences.remove(AUDIO_TRACK_LANGUAGE)
            }
        }
    }

    suspend fun saveSubtitleLanguage(language: String?) {
        dataStore.edit { preferences ->
            if (language != null) {
                preferences[SUBTITLE_LANGUAGE] = language
            } else {
                preferences.remove(SUBTITLE_LANGUAGE)
            }
        }
    }

    suspend fun savePlayThemeSongs(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PLAY_THEME_SONGS] = enabled
        }
    }

    suspend fun saveAutoSkipSegments(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_SKIP_SEGMENTS] = enabled
        }
    }

    suspend fun saveDisplayMode(mode: DisplayMode) {
        dataStore.edit { preferences ->
            preferences[DISPLAY_MODE] = mode.value
        }
    }

    suspend fun saveDecoderMode(mode: DecoderMode) {
        dataStore.edit { preferences ->
            preferences[DECODER_MODE] = mode.value
        }
    }

    suspend fun savePreferredVideoCodec(codec: PreferredVideoCodec) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_VIDEO_CODEC] = codec.preferenceValue
        }
    }

    suspend fun savePreferredAudioCodec(codec: PreferredAudioCodec) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_AUDIO_CODEC] = codec.preferenceValue
        }
    }

    suspend fun savePlayerType(playerType: PlayerType) {
        dataStore.edit { preferences ->
            preferences[PLAYER_TYPE] = playerType.value
        }
    }

    suspend fun saveShowFeaturedHeader(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_FEATURED_HEADER] = enabled
        }
    }

    suspend fun saveAmbientBackgroundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AMBIENT_BACKGROUND] = enabled
        }
    }

    suspend fun saveNavigateEpisodesToSeason(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NAVIGATE_EPISODES_TO_SEASON] = enabled
        }
    }

    suspend fun saveImageCacheSize(sizeMb: Long) {
        dataStore.edit { preferences ->
            preferences[IMAGE_CACHE_SIZE] = sizeMb
        }
    }

    suspend fun saveVideoCacheSize(sizeMb: Long) {
        dataStore.edit { preferences ->
            preferences[VIDEO_CACHE_SIZE] = sizeMb
        }
    }

    suspend fun saveCacheWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CACHE_WIFI_ONLY] = enabled
        }
    }

    suspend fun saveMaxStaleDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[MAX_STALE_DAYS] = days
        }
    }

    

    fun getTotalRxBytes(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[DATA_USAGE_RX] ?: 0L
    }

    fun getTotalTxBytes(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[DATA_USAGE_TX] ?: 0L
    }

    fun getDailyDataUsage(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[DAILY_DATA_USAGE] ?: 0L
    }

    fun getMonthlyDataUsage(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[MONTHLY_DATA_USAGE] ?: 0L
    }

    fun getDailyDataCap(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[DAILY_DATA_CAP] ?: 0L
    }

    fun getMonthlyDataCap(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[MONTHLY_DATA_CAP] ?: 0L
    }

    suspend fun saveDailyDataCap(capBytes: Long) {
        dataStore.edit { preferences ->
            preferences[DAILY_DATA_CAP] = capBytes
        }
    }

    suspend fun saveMonthlyDataCap(capBytes: Long) {
        dataStore.edit { preferences ->
            preferences[MONTHLY_DATA_CAP] = capBytes
        }
    }

    suspend fun updateDataUsage(rxBytes: Long, txBytes: Long) {
        dataStore.edit { preferences ->
            val today = LocalDate.now().toString()
            val currentMonth = YearMonth.now().toString()

            val lastDay = preferences[LAST_DAILY_RESET]
            val lastMonth = preferences[LAST_MONTHLY_RESET]

            val dailyUsage = if (lastDay == today) {
                (preferences[DAILY_DATA_USAGE] ?: 0L) + rxBytes + txBytes
            } else {
                preferences[LAST_DAILY_RESET] = today
                rxBytes + txBytes
            }
            val monthlyUsage = if (lastMonth == currentMonth) {
                (preferences[MONTHLY_DATA_USAGE] ?: 0L) + rxBytes + txBytes
            } else {
                preferences[LAST_MONTHLY_RESET] = currentMonth
                rxBytes + txBytes
            }

            preferences[DAILY_DATA_USAGE] = dailyUsage
            preferences[MONTHLY_DATA_USAGE] = monthlyUsage

            val currentRx = preferences[DATA_USAGE_RX] ?: 0L
            val currentTx = preferences[DATA_USAGE_TX] ?: 0L
            preferences[DATA_USAGE_RX] = currentRx + rxBytes
            preferences[DATA_USAGE_TX] = currentTx + txBytes
        }
    }

    

    suspend fun savePlaybackPosition(itemId: String, positionTicks: Long) {
        val key = longPreferencesKey("${PLAYBACK_POSITION_PREFIX}$itemId")
        dataStore.edit { preferences ->
            preferences[key] = positionTicks
        }
    }

    fun getPlaybackPosition(itemId: String): Flow<Long?> {
        val key = longPreferencesKey("${PLAYBACK_POSITION_PREFIX}$itemId")
        return dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun clearPlaybackPosition(itemId: String) {
        val key = longPreferencesKey("${PLAYBACK_POSITION_PREFIX}$itemId")
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    suspend fun clearAllPlaybackPositions() {
        dataStore.edit { preferences ->
            val keysToRemove = preferences.asMap().keys.filter {
                it.name.startsWith(PLAYBACK_POSITION_PREFIX)
            }
            keysToRemove.forEach { preferences.remove(it) }
        }
    }

    

    suspend fun savePlaybackPreferences(mediaId: String, prefs: PlaybackPreferences) {
        val key = stringPreferencesKey("${PLAYBACK_PREFERENCES_PREFIX}$mediaId")
        dataStore.edit { preferences ->
            preferences[key] = Json.encodeToString(prefs)
        }
    }

    fun getPlaybackPreferences(mediaId: String): Flow<PlaybackPreferences?> {
        val key = stringPreferencesKey("${PLAYBACK_PREFERENCES_PREFIX}$mediaId")
        return dataStore.data.map { preferences ->
            preferences[key]?.let { json ->
                runCatching { Json.decodeFromString<PlaybackPreferences>(json) }.getOrNull()
            }
        }
    }

    

    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            val preservedFirstRun = if (preferences.contains(FIRST_RUN_COMPLETED)) {
                preferences[FIRST_RUN_COMPLETED] ?: true
            } else {
                null
            }
            preferences.clear()
            preservedFirstRun?.let { preferences[FIRST_RUN_COMPLETED] = it }
        }
    }
}
        private val OFFLINE_MODE = booleanPreferencesKey(PreferenceConstants.KEY_OFFLINE_MODE)