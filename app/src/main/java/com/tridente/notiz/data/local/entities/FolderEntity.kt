package com.tridente.notiz.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val remoteId: String? = null,
    val name: String,
    val parentFolderId: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
