package com.hritwik.avoid.data.local.database.dao

import androidx.room.*
import com.hritwik.avoid.data.local.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY lastLoginTime DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE serverUrl = :serverUrl ORDER BY lastLoginTime DESC")
    fun getUsersByServer(serverUrl: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY lastLoginTime DESC LIMIT 1")
    suspend fun getLastLoginUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("UPDATE users SET lastLoginTime = :loginTime WHERE id = :id")
    suspend fun updateLastLoginTime(id: String, loginTime: Long)
}