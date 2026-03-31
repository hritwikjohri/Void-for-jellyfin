package com.hritwik.avoid.data.remote

import com.hritwik.avoid.data.remote.dto.tmdb.TmdbSearchResponse
import com.hritwik.avoid.data.remote.dto.tmdb.TmdbTitleResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApiService {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbTitleResponse

    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbTitleResponse
}
