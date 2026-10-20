package com.tridente.notiz.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_shares")
data class NoteShareEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val sharedWithEmail: String,
    val permissionRole: String = "READ", // "READ" or "WRITE"
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
