package com.hritwik.avoid.domain.repository

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.MediaItem


interface SearchRepository {

    
    suspend fun searchItems(
        userId: User,
        accessToken: String,
        searchTerm: String,
        includeItemTypes: String? = null,
        startIndex: Int = 0,
        limit: Int = 50,
        forceRefresh: Boolean = false
    ): NetworkResult<List<MediaItem>>

    
    suspend fun getItemsByCategory(
        userId: String,
        accessToken: String,
        filters: Map<String, String>,
        startIndex: Int = 0,
        limit: Int = 50,
        forceRefresh: Boolean = false
    ): NetworkResult<List<MediaItem>>

    
    suspend fun getSearchSuggestions(
        userId: String,
        accessToken: String,
        query: String,
        limit: Int = 10
    ): NetworkResult<List<String>>

    
    suspend fun invalidateSearchResults(query: String, filters: Map<String, String>)
}