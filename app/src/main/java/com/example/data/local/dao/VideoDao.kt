package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY timestamp DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isShort = :isShort ORDER BY timestamp DESC")
    fun getVideosByType(isShort: Boolean): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE authorUserId = :userId OR authorHandle = :handle ORDER BY timestamp DESC")
    fun getUserVideos(userId: String, handle: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isLiked = 1 ORDER BY timestamp DESC")
    fun getLikedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isSaved = 1 ORDER BY timestamp DESC")
    fun getSavedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isReposted = 1 ORDER BY timestamp DESC")
    fun getRepostedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideosCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideosIfAbsent(videos: List<VideoEntity>)

    @Query("UPDATE videos SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViews(id: String)

    @Query("UPDATE videos SET isLiked = :isLiked, likesCount = likesCount + :likesDelta WHERE id = :id")
    suspend fun updateLike(id: String, likesDelta: Long, isLiked: Boolean)

    @Query("UPDATE videos SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSave(id: String, isSaved: Boolean)

    @Query("UPDATE videos SET isReposted = :isReposted, repostsCount = repostsCount + :repostsDelta WHERE id = :id")
    suspend fun updateRepost(id: String, repostsDelta: Long, isReposted: Boolean)

    @Query("UPDATE videos SET lastWatchedPositionMs = :positionMs WHERE id = :id")
    suspend fun updateWatchPosition(id: String, positionMs: Long)

    @Query("UPDATE videos SET title = :title, description = :description, thumbnailUrl = CASE WHEN :thumbnailUrl != '' THEN :thumbnailUrl ELSE thumbnailUrl END WHERE id = :id")
    suspend fun updateVideoMetadata(id: String, title: String, description: String, thumbnailUrl: String)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)
}
