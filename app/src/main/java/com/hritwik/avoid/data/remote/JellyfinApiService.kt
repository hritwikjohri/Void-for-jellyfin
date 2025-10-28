package com.hritwik.avoid.data.remote

import com.hritwik.avoid.BuildConfig
import com.hritwik.avoid.data.remote.dto.auth.AuthResponse
import com.hritwik.avoid.data.remote.dto.auth.LoginRequest
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectDto
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectInitiateResponse
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectResult
import com.hritwik.avoid.data.remote.dto.auth.ServerInfo
import com.hritwik.avoid.data.remote.dto.auth.UpdatePassword
import com.hritwik.avoid.data.remote.dto.auth.UserInfo
import com.hritwik.avoid.data.remote.dto.library.AllThemeMediaResultDto
import com.hritwik.avoid.data.remote.dto.library.BaseItemDto
import com.hritwik.avoid.data.remote.dto.library.LibraryResponse
import com.hritwik.avoid.data.remote.dto.library.PersonDto
import com.hritwik.avoid.data.remote.dto.library.UserDataDto
import com.hritwik.avoid.data.remote.dto.playback.PlaybackProgressRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStartRequest
import com.hritwik.avoid.data.remote.dto.playback.PlaybackStopRequest
import com.hritwik.avoid.data.remote.dto.playback.SegmentResponse
import com.hritwik.avoid.data.remote.dto.search.SearchHintsResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer

interface JellyfinApiService {

    @GET("System/Info/Public")
    suspend fun getServerInfo(): ServerInfo

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Body loginRequest: LoginRequest,
        @Header("X-Emby-Authorization") authorization: String
    ): AuthResponse

    @POST("Sessions/Logout")
    suspend fun logout(
        @Header("X-Emby-Authorization") authorization: String
    )

    @GET("Users/Me")
    suspend fun getCurrentUser(
        @Header("X-Emby-Authorization") authorization: String
    ): UserInfo

    @POST("Users/Password")
    suspend fun updatePassword(
        @Query("userId") userId: String? = null,
        @Body request: UpdatePassword,
        @Header("X-Emby-Authorization") authorization: String
    )

    @POST("QuickConnect/Initiate")
    suspend fun initiateQuickConnect(
        @Header("X-Emby-Authorization") authorization: String
    ): QuickConnectInitiateResponse

    @GET("QuickConnect/Connect")
    suspend fun connectQuickConnect(
        @Query("secret") secret: String,
        @Header("X-Emby-Authorization") authorization: String
    ): QuickConnectResult

    @POST("Users/AuthenticateWithQuickConnect")
    suspend fun authorizeQuickConnect(
        @Body request: QuickConnectDto,
        @Header("X-Emby-Authorization") authorization: String
    ): AuthResponse

    @GET("QuickConnect/Enabled")
    suspend fun isQuickConnectEnabled(
        @Header("X-Emby-Authorization") authorization: String
    ): Boolean

    @GET("Users/{userId}/Views")
    suspend fun getUserLibraries(
        @Path("userId") userId: String,
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Users/{userId}/Items")
    suspend fun getLibraryItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("Genres") genres: String? = null,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData",
        @Query("EnableImageTypes") enableImageTypes: String? = null,
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Items/{itemId}/Credits")
    suspend fun getItemCredits(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary",
        @Header("X-Emby-Authorization") authorization: String
    ): List<PersonDto>

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData",
        @Header("X-Emby-Authorization") authorization: String
    ): List<BaseItemDto>

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData",
        @Query("EnableImageTypes") enableImageTypes: String? = null,
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Shows/NextUp")
    suspend fun getNextUpItems(
        @Query("UserId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData",
        @Query("EnableImageTypes") enableImageTypes: String? = null,
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Genres")
    suspend fun getGenres(
        @Query("UserId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 100,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("EnableImages") enableImages: Boolean = false,
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Users/{userId}/Items")
    suspend fun getItemsByFilters(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Genres") genres: String? = null,
        @Query("Years") years: String? = null,
        @Query("OfficialRatings") officialRatings: String? = null,
        @Query("Tags") tags: String? = null,
        @Query("Studios") studios: String? = null,
        @Query("Artists") artists: String? = null,
        @Query("Albums") albums: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("SearchTerm") searchTerm: String? = null,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 50,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("IsPlayed") isPlayed: Boolean? = null,
        @Query("IsFavorite") isFavorite: Boolean? = null,
        @Query("HasImdbId") hasImdbId: Boolean? = null,
        @Query("HasTmdbId") hasTmdbId: Boolean? = null,
        @Query("HasTvdbId") hasTvdbId: Boolean? = null,
        @Query("IsMovie") isMovie: Boolean? = null,
        @Query("IsSeries") isSeries: Boolean? = null,
        @Query("IsNews") isNews: Boolean? = null,
        @Query("IsKids") isKids: Boolean? = null,
        @Query("IsSports") isSports: Boolean? = null,
        @Query("MinCommunityRating") minCommunityRating: Double? = null,
        @Query("MinCriticRating") minCriticRating: Double? = null,
        @Query("MinPremiereDate") minPremiereDate: String? = null,
        @Query("MaxPremiereDate") maxPremiereDate: String? = null,
        @Query("MinDateLastSaved") minDateLastSaved: String? = null,
        @Query("MinDateLastSavedForUser") minDateLastSavedForUser: String? = null,
        @Query("MaxDateLastSaved") maxDateLastSaved: String? = null,
        @Query("HasOverview") hasOverview: Boolean? = null,
        @Query("HasTrailer") hasTrailer: Boolean? = null,
        @Query("HasSpecialFeature") hasSpecialFeature: Boolean? = null,
        @Query("HasSubtitles") hasSubtitles: Boolean? = null,
        @Query("HasParentalRating") hasParentalRating: Boolean? = null,
        @Query("EnableImageTypes") enableImageTypes: String? = null,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData",
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Users/{userId}/Items")
    suspend fun searchItems(
        @Path("userId") userId: String,
        @Query("SearchTerm") searchTerm: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 50,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,UserData",
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Search/Hints")
    suspend fun getSearchSuggestions(
        @Query("UserId") userId: String,
        @Query("SearchTerm") searchTerm: String,
        @Query("Limit") limit: Int = 10,
        @Header("X-Emby-Authorization") authorization: String
    ): SearchHintsResponse

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemById(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData",
        @Header("X-Emby-Authorization") authorization: String
    ): BaseItemDto

    @GET("UserItems/{itemId}/UserData")
    suspend fun getItemUserData(
        @Path("itemId") itemId: String,
        @Query("userId") userId: String,
        @Header("X-Emby-Authorization") authorization: String
    ): UserDataDto

    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markAsPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") authorization: String
    )

    @DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markAsUnplayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") authorization: String
    )

    @GET("Items/{itemId}/ThemeSongs")
    suspend fun getThemeSongs(
        @Path("itemId") itemId: String,
        @Query("inheritFromParent") inheritFromParent: Boolean = true,
        @Query("fields") fields: String = "MediaSources",
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Items/{itemId}/ThemeMedia")
    suspend fun getThemeMedia(
        @Path("itemId") itemId: String,
        @Query("inheritFromParent") inheritFromParent: Boolean = true,
        @Header("X-Emby-Authorization") authorization: String
    ): AllThemeMediaResultDto

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markAsFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") authorization: String
    )

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun removeFromFavorites(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") authorization: String
    )

    @GET("Items/{itemId}/Similar")
    suspend fun getSimilarItems(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,,ParentIndexNumber,IndexNumber,UserData",
        @Header("X-Emby-Authorization") authorization: String
    ): LibraryResponse

    @GET("Items/{itemId}/SpecialFeatures")
    suspend fun getSpecialFeatures(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Genres,Studios,People,Overview,Taglines,MediaSources,MediaStreams,ParentIndexNumber,IndexNumber,UserData",
        @Header("X-Emby-Authorization") authorization: String
    ): List<BaseItemDto>

    @GET("MediaSegments/{itemId}")
    suspend fun getItemSegments(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Header("X-Emby-Authorization") authorization: String
    ): SegmentResponse

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Body progressInfo: PlaybackProgressRequest,
        @Header("X-Emby-Authorization") authorization: String
    )

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(
        @Body playbackInfo: PlaybackStartRequest,
        @Header("X-Emby-Authorization") authorization: String
    )

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStop(
        @Body stopInfo: PlaybackStopRequest,
        @Header("X-Emby-Authorization") authorization: String
    )

    companion object {
        fun createAuthHeader(
            deviceId: String,
            device: String = "Android",
            client: String = "Void",
            version: String = BuildConfig.VERSION_NAME,
            token: String? = null
        ): String {
            val safeClient = sanitizeHeaderValue(client, "Void")
            val safeDevice = sanitizeHeaderValue(device, "Android")
            val safeDeviceId = sanitizeHeaderValue(deviceId, "VoidDevice")
            val safeVersion = sanitizeHeaderValue(version, BuildConfig.VERSION_NAME)
            val safeToken = token?.let { sanitizeHeaderValue(it) }

            val tokenPart = if (!safeToken.isNullOrEmpty()) ", Token=\"$safeToken\"" else ""
            return "MediaBrowser Client=\"$safeClient\", Device=\"$safeDevice\", DeviceId=\"$safeDeviceId\", Version=\"$safeVersion\"$tokenPart"
        }

        private fun sanitizeHeaderValue(value: String, fallback: String? = null): String {
            val processed = value
                .replace('"', '\'')
                .trim()

            printableAsciiOrNull(processed)?.let { return it }

            val encoded = encodeForHeader(processed)
            if (encoded.isNotEmpty()) return encoded

            val fallbackValue = fallback?.replace('"', '\'')?.trim().orEmpty()
            if (fallbackValue.isEmpty()) return ""

            printableAsciiOrNull(fallbackValue)?.let { return it }

            return encodeForHeader(fallbackValue)
        }

        private fun printableAsciiOrNull(value: String): String? {
            if (value.isEmpty()) return null
            return if (value.all { it.code in 0x20..0x7E }) value else null
        }

        private fun encodeForHeader(value: String): String {
            if (value.isEmpty()) return ""
            val normalized = Normalizer.normalize(value, Normalizer.Form.NFC)
            return try {
                URLEncoder.encode(normalized, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
            } catch (_: Exception) {
                ""
            }
        }
    }
}