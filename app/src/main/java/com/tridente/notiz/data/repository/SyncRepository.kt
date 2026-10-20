package com.tridente.notiz.data.repository

import android.util.Base64
import com.tridente.notiz.data.local.dao.AttachmentDao
import com.tridente.notiz.data.local.dao.FolderDao
import com.tridente.notiz.data.local.dao.NoteDao
import com.tridente.notiz.data.local.dao.SyncQueueDao
import com.tridente.notiz.data.local.entities.AttachmentEntity
import com.tridente.notiz.data.local.entities.FolderEntity
import com.tridente.notiz.data.local.entities.NoteEntity
import com.tridente.notiz.data.remote.RetrofitClient
import com.tridente.notiz.data.remote.dto.AttachmentDto
import com.tridente.notiz.data.remote.dto.FolderDto
import com.tridente.notiz.data.remote.dto.NoteDto
import com.tridente.notiz.data.remote.dto.SyncRequest
import com.tridente.notiz.data.security.TokenManager

class SyncRepository(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao,
    private val attachmentDao: AttachmentDao,
    private val syncQueueDao: SyncQueueDao,
    private val tokenManager: TokenManager
) {

    private var lastSyncTimestamp: Long = 0

    suspend fun performSync(): Result<Unit> {
        return try {
            val api = RetrofitClient.getNotizApi(tokenManager)

            // Gather local unsynced changes
            val unsyncedFolders = folderDao.getUnsyncedFolders()
            val unsyncedNotes = noteDao.getUnsyncedNotes()
            val unsyncedAttachments = attachmentDao.getUnsyncedAttachments()

            val folderDtos = unsyncedFolders.map {
                FolderDto(
                    id = it.id,
                    name = it.name,
                    parentFolderId = it.parentFolderId,
                    isDeleted = it.isDeleted,
                    updatedAt = it.updatedAt
                )
            }

            val noteDtos = unsyncedNotes.map {
                NoteDto(
                    id = it.id,
                    folderId = it.folderId,
                    title = it.title,
                    contentMarkdown = it.contentMarkdown,
                    dueDateTimestamp = it.dueDateTimestamp,
                    isDeleted = it.isDeleted,
                    isPublic = it.isPublic,
                    publicToken = it.publicToken,
                    updatedAt = it.updatedAt
                )
            }

            val attachmentDtos = unsyncedAttachments.map {
                val b64 = it.imageBlob?.let { blob -> Base64.encodeToString(blob, Base64.NO_WRAP) }
                AttachmentDto(
                    id = it.id,
                    noteId = it.noteId,
                    fileName = it.fileName,
                    mimeType = it.mimeType,
                    imageBlobBase64 = b64,
                    isDeleted = it.isDeleted,
                    updatedAt = it.updatedAt
                )
            }

            val syncRequest = SyncRequest(
                lastSyncTimestamp = lastSyncTimestamp,
                folderChanges = folderDtos,
                noteChanges = noteDtos,
                attachmentChanges = attachmentDtos
            )

            val response = api.sync(syncRequest)
            if (response.isSuccessful && response.body() != null) {
                val syncResponse = response.body()!!
                lastSyncTimestamp = syncResponse.serverSyncTimestamp

                // Mark local pushed changes as synced
                unsyncedFolders.forEach { folderDao.insertFolder(it.copy(synced = true)) }
                unsyncedNotes.forEach { noteDao.insertNote(it.copy(synced = true)) }
                unsyncedAttachments.forEach { attachmentDao.insertAttachment(it.copy(synced = true)) }

                // Process Server Folders with Last-Write-Wins
                syncResponse.serverFolderChanges.forEach { serverFolder ->
                    val local = folderDao.getFolderById(serverFolder.id)
                    if (local == null || serverFolder.updatedAt > local.updatedAt) {
                        folderDao.insertFolder(
                            FolderEntity(
                                id = serverFolder.id,
                                name = serverFolder.name,
                                parentFolderId = serverFolder.parentFolderId,
                                isDeleted = serverFolder.isDeleted,
                                updatedAt = serverFolder.updatedAt,
                                synced = true
                            )
                        )
                    }
                }

                // Process Server Notes with Last-Write-Wins
                syncResponse.serverNoteChanges.forEach { serverNote ->
                    val local = noteDao.getNoteById(serverNote.id)
                    if (local == null || serverNote.updatedAt > local.updatedAt) {
                        noteDao.insertNote(
                            NoteEntity(
                                id = serverNote.id,
                                folderId = serverNote.folderId,
                                title = serverNote.title,
                                contentMarkdown = serverNote.contentMarkdown,
                                dueDateTimestamp = serverNote.dueDateTimestamp,
                                isDeleted = serverNote.isDeleted,
                                isPublic = serverNote.isPublic,
                                publicToken = serverNote.publicToken,
                                updatedAt = serverNote.updatedAt,
                                synced = true
                            )
                        )
                    }
                }

                // Process Server Attachments
                syncResponse.serverAttachmentChanges.forEach { serverAtt ->
                    val local = attachmentDao.getAttachmentById(serverAtt.id)
                    if (local == null || serverAtt.updatedAt > local.updatedAt) {
                        val blob = serverAtt.imageBlobBase64?.let { b64 -> Base64.decode(b64, Base64.NO_WRAP) }
                        attachmentDao.insertAttachment(
                            AttachmentEntity(
                                id = serverAtt.id,
                                noteId = serverAtt.noteId,
                                fileName = serverAtt.fileName,
                                mimeType = serverAtt.mimeType,
                                imageBlob = blob,
                                isDeleted = serverAtt.isDeleted,
                                updatedAt = serverAtt.updatedAt,
                                synced = true
                            )
                        )
                    }
                }

                syncQueueDao.clearAll()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error en servidor al sincronizar: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Offline scenario - sync fails gracefully, data remains locally safe in Room
            Result.failure(e)
        }
    }
}
