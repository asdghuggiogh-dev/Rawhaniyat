package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Comment

@Entity(
    tableName = "comments",
    indices = [Index(value = ["contentId"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String,
    val authorUserId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "الآن",
    val likesCount: Long = 0L,
    val isLiked: Boolean = false
) {
    fun toComment(): Comment {
        return Comment(
            id = id,
            contentId = contentId,
            authorName = authorName,
            authorHandle = authorHandle,
            authorAvatarUrl = authorAvatarUrl,
            text = text,
            timeAgo = timeAgo,
            likesCount = likesCount,
            isLiked = isLiked
        )
    }

    companion object {
        fun fromComment(comment: Comment, authorUserId: String = ""): CommentEntity {
            return CommentEntity(
                id = comment.id,
                contentId = comment.contentId,
                authorName = comment.authorName,
                authorHandle = comment.authorHandle,
                authorAvatarUrl = comment.authorAvatarUrl,
                authorUserId = authorUserId,
                text = comment.text,
                timeAgo = comment.timeAgo,
                likesCount = comment.likesCount,
                isLiked = comment.isLiked
            )
        }
    }
}
