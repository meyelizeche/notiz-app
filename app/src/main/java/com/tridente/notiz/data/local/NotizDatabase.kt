package com.tridente.notiz.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tridente.notiz.data.local.dao.AttachmentDao
import com.tridente.notiz.data.local.dao.FolderDao
import com.tridente.notiz.data.local.dao.NoteDao
import com.tridente.notiz.data.local.dao.NoteShareDao
import com.tridente.notiz.data.local.dao.SyncQueueDao
import com.tridente.notiz.data.local.dao.UserDao
import com.tridente.notiz.data.local.entities.AttachmentEntity
import com.tridente.notiz.data.local.entities.FolderEntity
import com.tridente.notiz.data.local.entities.NoteEntity
import com.tridente.notiz.data.local.entities.NoteShareEntity
import com.tridente.notiz.data.local.entities.SyncQueueEntity
import com.tridente.notiz.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        FolderEntity::class,
        NoteEntity::class,
        AttachmentEntity::class,
        NoteShareEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NotizDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun noteShareDao(): NoteShareDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: NotizDatabase? = null

        fun getDatabase(context: Context): NotizDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotizDatabase::class.java,
                    "notiz_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
