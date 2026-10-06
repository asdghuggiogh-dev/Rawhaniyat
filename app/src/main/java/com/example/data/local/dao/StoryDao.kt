package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStoriesIfAbsent(stories: List<StoryEntity>)

    @Query("UPDATE stories SET isViewed = 1 WHERE id = :id")
    suspend fun markStoryViewed(id: String)

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteStory(id: String)
}
