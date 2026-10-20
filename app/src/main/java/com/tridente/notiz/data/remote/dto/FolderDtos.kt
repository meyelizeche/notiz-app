package com.tridente.notiz.data.remote.dto

data class FolderDto(
    val id: String,
    val name: String,
    val parentFolderId: String?,
    val isDeleted: Boolean = false,
    val updatedAt: Long
)

data class CreateFolderRequest(
    val id: String,
    val name: String,
    val parentFolderId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
