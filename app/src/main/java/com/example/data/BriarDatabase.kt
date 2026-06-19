package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MyIdentity::class,
        Contact::class,
        Message::class,
        GroupChat::class,
        GroupMessage::class,
        Forum::class,
        ForumPost::class,
        BlogPost::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BriarDatabase : RoomDatabase() {
    abstract fun briarDao(): BriarDao

    companion object {
        @Volatile
        private var INSTANCE: BriarDatabase? = null

        fun getDatabase(context: Context): BriarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BriarDatabase::class.java,
                    "briar_clone_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
