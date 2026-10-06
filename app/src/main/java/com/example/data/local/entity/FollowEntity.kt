package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val followerHandle: String,
    val followingHandle: String,
    val timestamp: Long = System.currentTimeMillis()
)
