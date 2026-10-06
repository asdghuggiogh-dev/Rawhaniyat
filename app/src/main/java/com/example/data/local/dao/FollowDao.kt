package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.FollowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {

    @Query("SELECT * FROM follows WHERE followerHandle = :followerHandle ORDER BY timestamp DESC")
    fun getFollowing(followerHandle: String): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE followingHandle = :followingHandle ORDER BY timestamp DESC")
    fun getFollowers(followingHandle: String): Flow<List<FollowEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerHandle = :followerHandle AND followingHandle = :followingHandle)")
    suspend fun isFollowing(followerHandle: String, followingHandle: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerHandle = :followerHandle AND followingHandle = :followingHandle")
    suspend fun deleteFollow(followerHandle: String, followingHandle: String)

    @Query("SELECT COUNT(*) FROM follows WHERE followingHandle = :followingHandle")
    suspend fun getFollowersCount(followingHandle: String): Long

    @Query("SELECT COUNT(*) FROM follows WHERE followerHandle = :followerHandle")
    suspend fun getFollowingCount(followerHandle: String): Long
}
