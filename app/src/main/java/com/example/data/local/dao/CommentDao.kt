package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE contentId = :contentId ORDER BY timestamp DESC")
    fun getCommentsForContent(contentId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCommentsIfAbsent(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: String)

    @Query("DELETE FROM comments WHERE id = :id AND authorUserId = :userId")
    suspend fun deleteCommentByUser(id: String, userId: String): Int

    @Query("SELECT COUNT(*) FROM comments WHERE contentId = :contentId")
    suspend fun getCommentsCountForContent(contentId: String): Long
}
