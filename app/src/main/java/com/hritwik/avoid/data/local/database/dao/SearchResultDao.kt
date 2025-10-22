package com.hritwik.avoid.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hritwik.avoid.data.local.database.entities.SearchResultEntity

@Dao
interface SearchResultDao {
    @Query("SELECT * FROM search_results WHERE query = :query AND filters = :filters")
    suspend fun getSearchResult(query: String, filters: String): SearchResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResult(result: SearchResultEntity)

    @Query("DELETE FROM search_results WHERE query = :query AND filters = :filters")
    suspend fun deleteSearchResult(query: String, filters: String)

    @Query("DELETE FROM search_results")
    suspend fun clearAll()

    @Query("SELECT * FROM search_results LIMIT 1")
    suspend fun closeOpenCursors(): SearchResultEntity?
}