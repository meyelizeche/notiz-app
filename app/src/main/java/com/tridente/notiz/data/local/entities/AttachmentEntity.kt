package com.tridente.notiz.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val remoteId: String? = null,
    val noteId: String,
    val fileName: String,
    val mimeType: String = "image/jpeg",
    val imageBlob: ByteArray? = null,
    val localPath: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentEntity

        if (id != other.id) return false
        if (noteId != other.noteId) return false
        if (fileName != other.fileName) return false
        if (imageBlob != null) {
            if (other.imageBlob == null) return false
            if (!imageBlob.contentEquals(other.imageBlob)) return false
        } else if (other.imageBlob != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + noteId.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + (imageBlob?.contentHashCode() ?: 0)
        return result
    }
}
