package com.tridente.notiz.data.repository

import com.tridente.notiz.data.local.dao.AttachmentDao
import com.tridente.notiz.data.local.dao.NoteDao
import com.tridente.notiz.data.local.dao.SyncQueueDao
import com.tridente.notiz.data.local.entities.AttachmentEntity
import com.tridente.notiz.data.local.entities.NoteEntity
import com.tridente.notiz.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val attachmentDao: AttachmentDao,
    private val syncQueueDao: SyncQueueDao
) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByFolder(folderId: String?): Flow<List<NoteEntity>> {
        return if (folderId == null) {
            noteDao.getAllNotes()
        } else {
            noteDao.getNotesByFolder(folderId)
        }
    }

    fun getNoteByIdFlow(noteId: String): Flow<NoteEntity?> {
        return noteDao.getNoteByIdFlow(noteId)
    }

    suspend fun getNoteById(noteId: String): NoteEntity? {
        return noteDao.getNoteById(noteId)
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return noteDao.searchNotes(query)
    }

    suspend fun createNote(
        title: String,
        contentMarkdown: String,
        folderId: String? = null,
        dueDateTimestamp: Long? = null
    ): NoteEntity {
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            folderId = folderId,
            title = title,
            contentMarkdown = contentMarkdown,
            dueDateTimestamp = dueDateTimestamp,
            isDeleted = false,
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        noteDao.insertNote(note)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "NOTE",
                entityId = note.id,
                operation = "CREATE",
                payloadJson = "{\"title\":\"${note.title}\"}"
            )
        )
        return note
    }

    suspend fun updateNote(note: NoteEntity) {
        val updated = note.copy(updatedAt = System.currentTimeMillis(), synced = false)
        noteDao.updateNote(updated)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "NOTE",
                entityId = updated.id,
                operation = "UPDATE",
                payloadJson = "{\"title\":\"${updated.title}\"}"
            )
        )
    }

    suspend fun deleteNote(noteId: String) {
        noteDao.markAsDeleted(noteId)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "NOTE",
                entityId = noteId,
                operation = "DELETE",
                payloadJson = "{}"
            )
        )
    }

    suspend fun moveNoteToFolder(noteId: String, folderId: String?) {
        noteDao.moveNoteToFolder(noteId, folderId)
    }

    // Attachments
    fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>> {
        return attachmentDao.getAttachmentsForNote(noteId)
    }

    suspend fun addAttachment(
        noteId: String,
        fileName: String,
        mimeType: String,
        imageBlob: ByteArray?,
        localPath: String?
    ): AttachmentEntity {
        val attachment = AttachmentEntity(
            id = UUID.randomUUID().toString(),
            noteId = noteId,
            fileName = fileName,
            mimeType = mimeType,
            imageBlob = imageBlob,
            localPath = localPath,
            isDeleted = false,
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        attachmentDao.insertAttachment(attachment)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "ATTACHMENT",
                entityId = attachment.id,
                operation = "CREATE",
                payloadJson = "{\"fileName\":\"$fileName\"}"
            )
        )
        return attachment
    }

    suspend fun deleteAttachment(attachmentId: String) {
        attachmentDao.markAsDeleted(attachmentId)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "ATTACHMENT",
                entityId = attachmentId,
                operation = "DELETE",
                payloadJson = "{}"
            )
        )
    }
}
