package com.tridente.notiz.data.repository

import com.tridente.notiz.data.local.dao.FolderDao
import com.tridente.notiz.data.local.dao.SyncQueueDao
import com.tridente.notiz.data.local.entities.FolderEntity
import com.tridente.notiz.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FolderRepository(
    private val folderDao: FolderDao,
    private val syncQueueDao: SyncQueueDao
) {

    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val rootFolders: Flow<List<FolderEntity>> = folderDao.getRootFolders()

    fun getSubfolders(parentId: String): Flow<List<FolderEntity>> {
        return folderDao.getSubfolders(parentId)
    }

    suspend fun createFolder(name: String, parentFolderId: String? = null): FolderEntity {
        val folder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            parentFolderId = parentFolderId,
            isDeleted = false,
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        folderDao.insertFolder(folder)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "FOLDER",
                entityId = folder.id,
                operation = "CREATE",
                payloadJson = "{\"name\":\"${folder.name}\"}"
            )
        )
        return folder
    }

    suspend fun updateFolder(folder: FolderEntity) {
        val updated = folder.copy(updatedAt = System.currentTimeMillis(), synced = false)
        folderDao.updateFolder(updated)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "FOLDER",
                entityId = updated.id,
                operation = "UPDATE",
                payloadJson = "{\"name\":\"${updated.name}\"}"
            )
        )
    }

    suspend fun deleteFolder(folderId: String) {
        folderDao.markAsDeleted(folderId)

        syncQueueDao.enqueueOperation(
            SyncQueueEntity(
                entityType = "FOLDER",
                entityId = folderId,
                operation = "DELETE",
                payloadJson = "{}"
            )
        )
    }
}
