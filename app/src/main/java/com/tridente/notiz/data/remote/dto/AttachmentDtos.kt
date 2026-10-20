package com.tridente.notiz.data.remote.dto

data class AttachmentDto(
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val imageBlobBase64: String?,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)
