package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CommentDao
import com.example.data.local.dao.FollowDao
import com.example.data.local.dao.StoryDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.VideoDao
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.FollowEntity
import com.example.data.local.entity.StoryEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        CommentEntity::class,
        StoryEntity::class,
        UserEntity::class,
        FollowEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class RowhaniyatDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao
    abstract fun commentDao(): CommentDao
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao
    abstract fun followDao(): FollowDao

    companion object {
        @Volatile
        private var INSTANCE: RowhaniyatDatabase? = null

        fun getInstance(context: Context): RowhaniyatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RowhaniyatDatabase::class.java,
                    "rowhaniyat_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
