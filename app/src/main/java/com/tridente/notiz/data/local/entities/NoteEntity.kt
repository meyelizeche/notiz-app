package com.tridente.notiz.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val remoteId: String? = null,
    val folderId: String? = null,
    val title: String,
    val contentMarkdown: String,
    val dueDateTimestamp: Long? = null,
    val isDeleted: Boolean = false,
    val isPublic: Boolean = false,
    val publicToken: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
