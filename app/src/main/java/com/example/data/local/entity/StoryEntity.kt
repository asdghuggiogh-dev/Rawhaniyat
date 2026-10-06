package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.StoryItem

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String,
    val authorUserId: String,
    val mediaUri: String,
    val isVideo: Boolean = true,
    val textCaption: String = "",
    val isViewed: Boolean = false,
    val isMyStory: Boolean = false,
    val durationSeconds: Int = 15,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toStoryItem(): StoryItem {
        return StoryItem(
            id = id,
            authorName = authorName,
            authorHandle = authorHandle,
            authorAvatarUrl = authorAvatarUrl,
            mediaUri = mediaUri,
            isVideo = isVideo,
            textCaption = textCaption,
            isViewed = isViewed,
            isMyStory = isMyStory,
            durationSeconds = durationSeconds
        )
    }
}
