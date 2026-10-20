package com.tridente.notiz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tridente.notiz.data.local.entities.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE noteId = :noteId AND isDeleted = 0 ORDER BY updatedAt ASC")
    fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId AND isDeleted = 0 ORDER BY updatedAt ASC")
    suspend fun getAttachmentsForNoteSync(noteId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id AND isDeleted = 0")
    suspend fun getAttachmentById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE synced = 0")
    suspend fun getUnsyncedAttachments(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("UPDATE attachments SET isDeleted = 1, updatedAt = :updatedAt, synced = 0 WHERE id = :id")
    suspend fun markAsDeleted(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deletePermanently(id: String)
}
