package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ShortItem
import com.example.data.model.VideoItem

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String,
    val authorUserId: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val viewsCount: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "الآن",
    val durationFormatted: String = "00:00",
    val durationSeconds: Int = 0,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val repostsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isReposted: Boolean = false,
    val hashtags: String = "",
    val description: String = "",
    val category: String = "تلاوات",
    val isShort: Boolean = false,
    val isVertical: Boolean = false,
    val isHorizontal: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val formattedSize: String = "",
    val soundTitle: String = "محتوى أصلي",
    val soundAuthor: String = "روحانيات",
    val lastWatchedPositionMs: Long = 0L
) {
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = id,
            title = title,
            authorName = authorName,
            authorHandle = authorHandle,
            authorAvatarUrl = authorAvatarUrl,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl,
            viewsCount = viewsCount,
            timeAgo = timeAgo,
            durationFormatted = durationFormatted,
            likesCount = likesCount,
            commentsCount = commentsCount,
            repostsCount = repostsCount,
            isLiked = isLiked,
            isSaved = isSaved,
            isReposted = isReposted,
            hashtags = if (hashtags.isNotBlank()) hashtags.split(",") else emptyList(),
            description = description,
            category = category,
            fileSizeBytes = fileSizeBytes,
            formattedSize = formattedSize,
            isVertical = isVertical,
            quality = "1080p FHD",
            lastWatchedPositionMs = lastWatchedPositionMs
        )
    }

    fun toShortItem(): ShortItem {
        return ShortItem(
            id = id,
            title = title,
            authorName = authorName,
            authorHandle = authorHandle,
            authorAvatarUrl = authorAvatarUrl,
            videoUrl = videoUrl,
            likesCount = likesCount,
            commentsCount = commentsCount,
            repostsCount = repostsCount,
            sharesCount = sharesCount,
            isLiked = isLiked,
            isSaved = isSaved,
            isReposted = isReposted,
            isFollowing = false,
            hashtags = if (hashtags.isNotBlank()) hashtags.split(",") else emptyList(),
            category = category,
            soundTitle = soundTitle,
            soundAuthor = soundAuthor,
            viewsCount = viewsCount,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            description = description,
            isHorizontal = isHorizontal,
            fileSizeBytes = fileSizeBytes,
            formattedSize = formattedSize
        )
    }

    /**
     * Smart score combining views, likes, comments, reposts and freshness.
     * Prevents views from being the sole sorting factor.
     */
    fun calculateScore(): Double {
        val daysOld = ((System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24.0)).coerceAtLeast(0.1)
        val freshnessFactor = (1.0 / daysOld).coerceIn(0.5, 3.0)
        return ((viewsCount * 1.0) + (likesCount * 3.5) + (commentsCount * 5.0) + (repostsCount * 4.0)) * freshnessFactor
    }
}
