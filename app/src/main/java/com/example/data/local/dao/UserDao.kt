package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY followersCount DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE handle = :handle LIMIT 1")
    fun getUserByHandle(handle: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE handle = :handle LIMIT 1")
    suspend fun getUserDirect(handle: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET followersCount = :followersCount, isFollowing = :isFollowing WHERE handle = :handle")
    suspend fun updateFollowState(handle: String, followersCount: Long, isFollowing: Boolean)

    @Query("UPDATE users SET name = :name, bio = :bio, avatarUrl = :avatarUrl, coverUrl = :coverUrl WHERE handle = :handle")
    suspend fun updateProfile(handle: String, name: String, bio: String, avatarUrl: String, coverUrl: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int

    @Query("DELETE FROM users WHERE handle = :handle")
    suspend fun deleteUserByHandle(handle: String)
}
