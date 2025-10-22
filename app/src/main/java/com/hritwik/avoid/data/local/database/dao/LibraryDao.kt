package com.hritwik.avoid.data.local.database.dao

import androidx.room.*
import com.hritwik.avoid.data.local.database.entities.LibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM libraries WHERE userId = :userId ORDER BY name ASC")
    fun getAllLibraries(userId: String): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM libraries WHERE id = :id AND userId = :userId")
    suspend fun getLibrary(id: String, userId: String): LibraryEntity?

    @Query("SELECT * FROM libraries WHERE type = :type AND userId = :userId ORDER BY name ASC")
    fun getLibrariesByType(type: String, userId: String): Flow<List<LibraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibrary(library: LibraryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraries(libraries: List<LibraryEntity>)

    @Update
    suspend fun updateLibrary(library: LibraryEntity)

    @Delete
    suspend fun deleteLibrary(library: LibraryEntity)

    @Query("DELETE FROM libraries WHERE userId = :userId")
    suspend fun deleteAllLibraries(userId: String)

    @Query("UPDATE libraries SET itemCount = :itemCount WHERE id = :id AND userId = :userId")
    suspend fun updateItemCount(id: String, userId: String, itemCount: Int)

    @Query("SELECT * FROM libraries LIMIT 1")
    suspend fun closeOpenCursors(): LibraryEntity?
}