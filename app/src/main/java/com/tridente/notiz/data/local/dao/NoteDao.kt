package com.tridente.notiz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tridente.notiz.data.local.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId IS NULL AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getUncategorizedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    fun getNoteByIdFlow(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE synced = 0")
    suspend fun getUnsyncedNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :updatedAt, synced = 0 WHERE id = :id")
    suspend fun markAsDeleted(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET folderId = :folderId, updatedAt = :updatedAt, synced = 0 WHERE id = :noteId")
    suspend fun moveNoteToFolder(noteId: String, folderId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deletePermanently(id: String)
}
