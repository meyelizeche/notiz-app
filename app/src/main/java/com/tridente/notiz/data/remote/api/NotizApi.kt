package com.tridente.notiz.data.remote.api

import com.tridente.notiz.data.remote.dto.CreateFolderRequest
import com.tridente.notiz.data.remote.dto.CreateNoteRequest
import com.tridente.notiz.data.remote.dto.FolderDto
import com.tridente.notiz.data.remote.dto.NoteDto
import com.tridente.notiz.data.remote.dto.PublicLinkResponse
import com.tridente.notiz.data.remote.dto.ShareNoteRequest
import com.tridente.notiz.data.remote.dto.ShareResponse
import com.tridente.notiz.data.remote.dto.SyncRequest
import com.tridente.notiz.data.remote.dto.SyncResponse
import com.tridente.notiz.data.remote.dto.UpdateNoteRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotizApi {

    // Folders
    @GET("api/v1/folders")
    suspend fun getFolders(): Response<List<FolderDto>>

    @POST("api/v1/folders")
    suspend fun createFolder(@Body request: CreateFolderRequest): Response<FolderDto>

    @DELETE("api/v1/folders/{id}")
    suspend fun deleteFolder(@Path("id") id: String): Response<Unit>

    // Notes
    @GET("api/v1/notes")
    suspend fun getNotes(): Response<List<NoteDto>>

    @POST("api/v1/notes")
    suspend fun createNote(@Body request: CreateNoteRequest): Response<NoteDto>

    @PUT("api/v1/notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body request: UpdateNoteRequest): Response<NoteDto>

    @DELETE("api/v1/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Unit>

    // Sharing & Collaboration
    @POST("api/v1/notes/{noteId}/share")
    suspend fun shareNote(
        @Path("noteId") noteId: String,
        @Body request: ShareNoteRequest
    ): Response<ShareResponse>

    @POST("api/v1/notes/{noteId}/public-link")
    suspend fun generatePublicLink(@Path("noteId") noteId: String): Response<PublicLinkResponse>

    // Synchronization
    @POST("api/v1/sync")
    suspend fun sync(@Body request: SyncRequest): Response<SyncResponse>
}
