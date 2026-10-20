package com.tridente.notiz.data.remote.dto

data class ShareNoteRequest(
    val sharedWithEmail: String,
    val permissionRole: String // "READ" or "WRITE"
)

data class ShareResponse(
    val id: String,
    val noteId: String,
    val sharedWithEmail: String,
    val permissionRole: String
)

data class PublicLinkResponse(
    val publicUrl: String,
    val token: String
)
