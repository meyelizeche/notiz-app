package com.tridente.notiz.data.remote.dto

data class NoteDto(
    val id: String,
    val folderId: String?,
    val title: String,
    val contentMarkdown: String,
    val dueDateTimestamp: Long?,
    val isDeleted: Boolean = false,
    val isPublic: Boolean = false,
    val publicToken: String? = null,
    val updatedAt: Long
)

data class CreateNoteRequest(
    val id: String,
    val folderId: String?,
    val title: String,
    val contentMarkdown: String,
    val dueDateTimestamp: Long?,
    val updatedAt: Long = System.currentTimeMillis()
)

data class UpdateNoteRequest(
    val folderId: String?,
    val title: String,
    val contentMarkdown: String,
    val dueDateTimestamp: Long?,
    val updatedAt: Long = System.currentTimeMillis()
)
