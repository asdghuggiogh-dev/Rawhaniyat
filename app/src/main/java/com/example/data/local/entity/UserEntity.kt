package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val handle: String,
    val id: String,
    val name: String,
    val bio: String,
    val avatarUrl: String,
    val coverUrl: String,
    val followersCount: Long,
    val followingCount: Long,
    val likesCount: Long,
    val viewsCount: Long,
    val isFollowing: Boolean = false,
    val country: String = "المملكة العربية السعودية",
    val websiteUrl: String = "",
    val email: String = "",
    val isCurrentUser: Boolean = false
) {
    // Verified automatically if 1,000 followers or more
    val isVerified: Boolean
        get() = followersCount >= 1000
}
