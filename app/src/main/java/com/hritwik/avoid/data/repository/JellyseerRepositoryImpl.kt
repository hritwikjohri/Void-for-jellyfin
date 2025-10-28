package com.hritwik.avoid.data.repository

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.remote.jellyseer.JellyseerApiService
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerCastDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerCreateRequestDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerJellyfinLoginRequestDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerMediaInfoDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerMediaRequestDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerMovieDetailDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerSearchItemDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerSearchResponseDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerTvDetailDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerUserDto
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.jellyseer.JellyseerAvailabilityStatus
import com.hritwik.avoid.domain.model.jellyseer.JellyseerCastMember
import com.hritwik.avoid.domain.model.jellyseer.JellyseerConfig
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaDetail
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaInfo
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.domain.model.jellyseer.JellyseerPagedResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerRequest
import com.hritwik.avoid.domain.model.jellyseer.JellyseerRequestStatus
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerUser
import com.hritwik.avoid.domain.repository.JellyseerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyseerRepositoryImpl @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val preferencesManager: PreferencesManager
) : JellyseerRepository {

    @Volatile
    private var apiService: JellyseerApiService? = null
    @Volatile
    private var cachedBaseUrl: String? = null

    override fun jellyseerConfig(): Flow<JellyseerConfig> = preferencesManager.getJellyseerConfig()

    private fun normalizeBaseUrl(raw: String): String {
        var base = raw.trim()
        if (base.isEmpty()) return base

        if (!base.endsWith("/")) {
            base += "/"
        }

        val lowerBase = base.lowercase()

        val normalized = when {
            lowerBase.endsWith("/api/v1/") -> base
            lowerBase.endsWith("/api/") -> base + "v1/"
            lowerBase.contains("/api/") -> base
            else -> base + "api/v1/"
        }

        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    private fun getService(baseUrl: String): JellyseerApiService {
        val normalized = normalizeBaseUrl(baseUrl)
        if (normalized.isBlank()) {
            throw IllegalStateException("Invalid Jellyseerr base URL")
        }
        if (apiService == null || cachedBaseUrl != normalized) {
            apiService = retrofitBuilder
                .baseUrl(normalized)
                .build()
                .create(JellyseerApiService::class.java)
            cachedBaseUrl = normalized
        }
        return apiService!!
    }

    private suspend fun requireConfiguration(): Result<Pair<JellyseerApiService, JellyseerConfig>> {
        val config = preferencesManager.getJellyseerConfig().first()
        if (!config.isConfigured) {
            return Result.failure(
                IllegalStateException(
                    "Configure Jellyseerr with a base URL and sign in or provide an API key"
                )
            )
        }
        return runCatching {
            getService(config.baseUrl) to config
        }
    }

    override suspend fun search(
        query: String,
        page: Int,
        language: String?
    ): NetworkResult<JellyseerPagedResult<JellyseerSearchResult>> {
        if (query.isBlank()) {
            return NetworkResult.Error(AppError.Validation("Search query cannot be empty"))
        }

        val (service, config) = requireConfiguration().getOrElse { error ->
            return NetworkResult.Error(AppError.Validation(error.message ?: "Invalid configuration"), error)
        }

        return runCatching {
            service.search(
                apiKey = config.apiKeyForRequest(),
                cookie = config.cookieForRequest(),
                query = query,
                page = page.takeIf { it > 0 },
                language = language
            )
        }.mapCatching { dto ->
            dto.toDomain()
        }.fold(
            onSuccess = { NetworkResult.Success(it) },
            onFailure = { throwable -> throwable.toNetworkError() }
        )
    }

    override suspend fun getMediaDetails(
        id: Long,
        type: JellyseerMediaType,
        language: String?
    ): NetworkResult<JellyseerMediaDetail> {
        val (service, config) = requireConfiguration().getOrElse { error ->
            return NetworkResult.Error(AppError.Validation(error.message ?: "Invalid configuration"), error)
        }

        return runCatching {
            when (type) {
                JellyseerMediaType.MOVIE -> service.getMovieDetails(
                    apiKey = config.apiKeyForRequest(),
                    cookie = config.cookieForRequest(),
                    movieId = id,
                    language = language
                ).toDomain()

                JellyseerMediaType.TV -> service.getTvDetails(
                    apiKey = config.apiKeyForRequest(),
                    cookie = config.cookieForRequest(),
                    tvId = id,
                    language = language
                ).toDomain()
            }
        }.fold(
            onSuccess = { NetworkResult.Success(it) },
            onFailure = { throwable -> throwable.toNetworkError() }
        )
    }

    override suspend fun createRequest(
        id: Long,
        type: JellyseerMediaType,
        is4k: Boolean,
        seasons: List<Int>?,
        tvdbId: Long?
    ): NetworkResult<JellyseerRequest> {
        val (service, config) = requireConfiguration().getOrElse { error ->
            return NetworkResult.Error(AppError.Validation(error.message ?: "Invalid configuration"), error)
        }

        val body = JellyseerCreateRequestDto(
            mediaType = type.value,
            mediaId = id,
            tvdbId = tvdbId,
            seasons = seasons,
            is4k = is4k
        )

        return runCatching {
            service.createRequest(
                apiKey = config.apiKeyForRequest(),
                cookie = config.cookieForRequest(),
                body = body
            ).toDomain()
        }.fold(
            onSuccess = { request ->
                if (request != null) {
                    NetworkResult.Success(request)
                } else {
                    NetworkResult.Error(AppError.Unknown("Invalid response from Jellyseerr"))
                }
            },
            onFailure = { throwable -> throwable.toNetworkError() }
        )
    }

    override suspend fun loginWithJellyfin(
        baseUrl: String,
        username: String,
        password: String,
    ): NetworkResult<JellyseerUser> {
        if (baseUrl.isBlank()) {
            return NetworkResult.Error(AppError.Validation("Jellyseerr base URL is required"))
        }
        if (username.isBlank()) {
            return NetworkResult.Error(AppError.Validation("Username is required"))
        }
        val service = runCatching { getService(baseUrl) }.getOrElse { error ->
            return NetworkResult.Error(AppError.Validation(error.message ?: "Invalid Jellyseerr base URL"), error)
        }

        val response = try {
            service.loginWithJellyfin(
                JellyseerJellyfinLoginRequestDto(
                    username = username.trim(),
                    password = password
                )
            )
        } catch (throwable: Throwable) {
            return throwable.toNetworkError()
        }

        if (!response.isSuccessful) {
            val message = when (response.code()) {
                401, 403 -> "Invalid Jellyfin credentials"
                else -> "Failed to sign in to Jellyseerr (HTTP ${response.code()})"
            }
            return NetworkResult.Error(AppError.Auth(message))
        }

        val userDto = response.body()
            ?: return NetworkResult.Error(AppError.Unknown("Jellyseerr returned an empty response"))

        val sessionCookie = extractSessionCookie(response.headers().values("set-cookie"))
            ?: return NetworkResult.Error(AppError.Unknown("Jellyseerr did not return a session cookie"))

        val user = userDto.toDomain()

        preferencesManager.updateJellyseerBaseUrl(baseUrl.trim())
        preferencesManager.updateJellyseerSessionCookie(sessionCookie)
        preferencesManager.updateJellyseerUser(user.id, user.displayName, user.email)

        return NetworkResult.Success(user)
    }

    override suspend fun logout(): NetworkResult<Unit> {
        val config = preferencesManager.getJellyseerConfig().first()
        if (config.sessionCookie.isBlank() || config.baseUrl.isBlank()) {
            preferencesManager.clearJellyseerSession()
            return NetworkResult.Success(Unit)
        }

        val service = runCatching { getService(config.baseUrl) }.getOrElse { error ->
            preferencesManager.clearJellyseerSession()
            return NetworkResult.Error(AppError.Validation(error.message ?: "Invalid Jellyseerr base URL"), error)
        }

        val response = try {
            service.logout(cookie = config.cookieForRequest())
        } catch (throwable: Throwable) {
            preferencesManager.clearJellyseerSession()
            return throwable.toNetworkError()
        }

        preferencesManager.clearJellyseerSession()

        return if (response.isSuccessful) {
            NetworkResult.Success(Unit)
        } else {
            val message = "Failed to sign out of Jellyseerr (HTTP ${response.code()})"
            NetworkResult.Error(AppError.Unknown(message))
        }
    }

    private fun JellyseerSearchResponseDto.toDomain(): JellyseerPagedResult<JellyseerSearchResult> {
        val items = results.mapNotNull { it.toDomain() }
        return JellyseerPagedResult(
            page = page,
            totalPages = totalPages,
            totalResults = totalResults,
            results = items
        )
    }

    private fun JellyseerSearchItemDto.toDomain(): JellyseerSearchResult? {
        val type = JellyseerMediaType.from(mediaType) ?: return null
        val titleValue = when (type) {
            JellyseerMediaType.MOVIE -> title ?: name
            JellyseerMediaType.TV -> name ?: title
        } ?: return null

        val release = when (type) {
            JellyseerMediaType.MOVIE -> releaseDate
            JellyseerMediaType.TV -> firstAirDate
        }

        return JellyseerSearchResult(
            id = id,
            mediaType = type,
            title = titleValue,
            overview = overview,
            releaseDate = release,
            posterPath = posterPath,
            backdropPath = backdropPath,
            voteAverage = voteAverage,
            mediaInfo = mediaInfo?.toDomain()
        )
    }

    private fun JellyseerMovieDetailDto.toDomain(): JellyseerMediaDetail {
        return JellyseerMediaDetail(
            id = id,
            mediaType = JellyseerMediaType.MOVIE,
            title = title ?: "Untitled Movie",
            overview = overview,
            tagline = tagline,
            releaseDate = releaseDate,
            runtimeMinutes = runtime,
            posterPath = posterPath,
            backdropPath = backdropPath,
            genres = genres.map { it.name },
            voteAverage = voteAverage,
            mediaInfo = mediaInfo?.toDomain(),
            episodeRunTime = null,
            numberOfSeasons = null,
            numberOfEpisodes = null,
            cast = credits?.cast?.map { it.toDomain() } ?: emptyList()
        )
    }

    private fun JellyseerTvDetailDto.toDomain(): JellyseerMediaDetail {
        val episodeRuntime = episodeRunTime?.firstOrNull()
        return JellyseerMediaDetail(
            id = id,
            mediaType = JellyseerMediaType.TV,
            title = name ?: "Untitled Series",
            overview = overview,
            tagline = tagline,
            releaseDate = firstAirDate ?: lastAirDate,
            runtimeMinutes = episodeRuntime,
            posterPath = posterPath,
            backdropPath = backdropPath,
            genres = genres.map { it.name },
            voteAverage = voteAverage,
            mediaInfo = mediaInfo?.toDomain(),
            episodeRunTime = episodeRuntime,
            numberOfSeasons = numberOfSeasons,
            numberOfEpisodes = numberOfEpisodes,
            cast = credits?.cast?.map { it.toDomain() } ?: emptyList()
        )
    }

    private fun JellyseerMediaInfoDto.toDomain(): JellyseerMediaInfo {
        return JellyseerMediaInfo(
            status = JellyseerAvailabilityStatus.from(status),
            requests = requests.mapNotNull { it.toDomain() },
            tmdbId = tmdbId,
            tvdbId = tvdbId
        )
    }

    private fun JellyseerMediaRequestDto.toDomain(): JellyseerRequest? {
        val statusValue = JellyseerRequestStatus.from(status) ?: return null
        val requester = requestedBy?.displayName
            ?: requestedBy?.username
            ?: requestedBy?.email
        return JellyseerRequest(
            id = id,
            status = statusValue,
            is4k = is4k,
            requestedBy = requester
        )
    }

    private fun JellyseerCastDto.toDomain(): JellyseerCastMember {
        return JellyseerCastMember(
            name = name,
            character = character,
            profilePath = profilePath
        )
    }

    private fun extractSessionCookie(headers: List<String>): String? {
        return headers.asSequence()
            .mapNotNull { header ->
                header.split(';')
                    .map { it.trim() }
                    .firstOrNull { it.startsWith("connect.sid=") }
            }
            .firstOrNull()
    }

    private fun JellyseerConfig.apiKeyForRequest(): String? =
        apiKey.takeIf { it.isNotBlank() && sessionCookie.isBlank() }

    private fun JellyseerConfig.cookieForRequest(): String? =
        sessionCookie.takeIf { it.isNotBlank() }

    private fun JellyseerUserDto.toDomain(): JellyseerUser {
        val preferredName = listOfNotNull(displayName, username, plexUsername, email)
            .firstOrNull { it.isNotBlank() }
        val normalizedEmail = email?.takeIf { it.isNotBlank() }
        val normalizedUsername = username?.takeIf { it.isNotBlank() } ?: plexUsername?.takeIf { it.isNotBlank() }
        return JellyseerUser(
            id = id ?: 0L,
            email = normalizedEmail,
            username = normalizedUsername,
            displayName = preferredName
        )
    }

    private fun <T> Throwable.toNetworkError(): NetworkResult.Error<T> {
        val error = when (this) {
            is HttpException -> AppError.Network(message())
            is IOException -> AppError.Network(message ?: "Network error")
            is IllegalStateException -> AppError.Validation(message ?: "Invalid configuration")
            else -> AppError.Unknown(message ?: "Unknown error")
        }
        return NetworkResult.Error(error, this)
    }
}
