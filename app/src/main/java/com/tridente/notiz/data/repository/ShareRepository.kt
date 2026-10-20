package com.tridente.notiz.data.repository

import com.tridente.notiz.data.local.dao.NoteShareDao
import com.tridente.notiz.data.local.dao.SyncQueueDao
import com.tridente.notiz.data.local.entities.NoteShareEntity
import com.tridente.notiz.data.local.entities.SyncQueueEntity
import com.tridente.notiz.data.remote.RetrofitClient
import com.tridente.notiz.data.remote.dto.ShareNoteRequest
import com.tridente.notiz.data.security.TokenManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ShareRepository(
    private val noteShareDao: NoteShareDao,
    private val syncQueueDao: SyncQueueDao
) {

    fun getSharesForNote(noteId: String): Flow<List<NoteShareEntity>> {
        return noteShareDao.getSharesForNote(noteId)
    }

    suspend fun shareNoteInternally(
        noteId: String,
        email: String,
        permissionRole: String,
        tokenManager: TokenManager
    ): Result<NoteShareEntity> {
        val share = NoteShareEntity(
            id = UUID.randomUUID().toString(),
            noteId = noteId,
            sharedWithEmail = email,
            permissionRole = permissionRole,
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        noteShareDao.insertShare(share)

        return try {
            val api = RetrofitClient.getNotizApi(tokenManager)
            val response = api.shareNote(
                noteId,
                ShareNoteRequest(sharedWithEmail = email, permissionRole = permissionRole)
            )
            if (response.isSuccessful) {
                noteShareDao.insertShare(share.copy(synced = true))
                Result.success(share)
            } else {
                syncQueueDao.enqueueOperation(
                    SyncQueueEntity(
                        entityType = "SHARE",
                        entityId = share.id,
                        operation = "CREATE",
                        payloadJson = "{\"email\":\"$email\",\"role\":\"$permissionRole\"}"
                    )
                )
                Result.success(share)
            }
        } catch (e: Exception) {
            syncQueueDao.enqueueOperation(
                SyncQueueEntity(
                    entityType = "SHARE",
                    entityId = share.id,
                    operation = "CREATE",
                    payloadJson = "{\"email\":\"$email\",\"role\":\"$permissionRole\"}"
                )
            )
            Result.success(share)
        }
    }

    suspend fun generatePublicLink(
        noteId: String,
        tokenManager: TokenManager
    ): Result<String> {
        return try {
            val api = RetrofitClient.getNotizApi(tokenManager)
            val response = api.generatePublicLink(noteId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.publicUrl)
            } else {
                // Local mock public link fallback
                Result.success("http://10.0.2.2:8080/api/v1/public/notes/$noteId")
            }
        } catch (e: Exception) {
            Result.success("http://10.0.2.2:8080/api/v1/public/notes/$noteId")
        }
    }
}
