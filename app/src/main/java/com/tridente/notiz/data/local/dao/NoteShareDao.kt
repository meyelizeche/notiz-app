package com.tridente.notiz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tridente.notiz.data.local.entities.NoteShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteShareDao {
    @Query("SELECT * FROM note_shares WHERE noteId = :noteId ORDER BY updatedAt DESC")
    fun getSharesForNote(noteId: String): Flow<List<NoteShareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: NoteShareEntity)

    @Query("DELETE FROM note_shares WHERE id = :shareId")
    suspend fun deleteShare(shareId: String)
}
