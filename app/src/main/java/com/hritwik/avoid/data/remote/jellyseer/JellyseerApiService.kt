package com.hritwik.avoid.data.remote.jellyseer

import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerCreateRequestDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerJellyfinLoginRequestDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerMediaRequestDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerMovieDetailDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerSearchResponseDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerStatusResponseDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerTvDetailDto
import com.hritwik.avoid.data.remote.jellyseer.dto.JellyseerUserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JellyseerApiService {

    @GET("search")
    suspend fun search(
        @Header(API_KEY_HEADER) apiKey: String? = null,
        @Header(COOKIE_HEADER) cookie: String? = null,
        @Query("query") query: String,
        @Query("page") page: Int? = null,
        @Query("language") language: String? = null
    ): JellyseerSearchResponseDto

    @GET("movie/{movieId}")
    suspend fun getMovieDetails(
        @Header(API_KEY_HEADER) apiKey: String? = null,
        @Header(COOKIE_HEADER) cookie: String? = null,
        @Path("movieId") movieId: Long,
        @Query("language") language: String? = null
    ): JellyseerMovieDetailDto

    @GET("tv/{tvId}")
    suspend fun getTvDetails(
        @Header(API_KEY_HEADER) apiKey: String? = null,
        @Header(COOKIE_HEADER) cookie: String? = null,
        @Path("tvId") tvId: Long,
        @Query("language") language: String? = null
    ): JellyseerTvDetailDto

    @POST("request")
    suspend fun createRequest(
        @Header(API_KEY_HEADER) apiKey: String? = null,
        @Header(COOKIE_HEADER) cookie: String? = null,
        @Body body: JellyseerCreateRequestDto
    ): JellyseerMediaRequestDto

    @POST("auth/jellyfin")
    suspend fun loginWithJellyfin(
        @Body body: JellyseerJellyfinLoginRequestDto
    ): Response<JellyseerUserDto>

    @POST("auth/logout")
    suspend fun logout(
        @Header(COOKIE_HEADER) cookie: String? = null
    ): Response<JellyseerStatusResponseDto>

    companion object {
        const val API_KEY_HEADER = "X-Api-Key"
        const val COOKIE_HEADER = "Cookie"
    }
}
