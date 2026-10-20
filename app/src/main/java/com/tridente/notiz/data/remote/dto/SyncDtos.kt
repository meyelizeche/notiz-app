package com.tridente.notiz.data.remote.dto

data class SyncRequest(
    val lastSyncTimestamp: Long,
    val folderChanges: List<FolderDto>,
    val noteChanges: List<NoteDto>,
    val attachmentChanges: List<AttachmentDto>
)

data class SyncResponse(
    val serverSyncTimestamp: Long,
    val serverFolderChanges: List<FolderDto>,
    val serverNoteChanges: List<NoteDto>,
    val serverAttachmentChanges: List<AttachmentDto>
)
