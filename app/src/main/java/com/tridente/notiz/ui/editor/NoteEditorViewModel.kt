package com.tridente.notiz.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tridente.notiz.data.local.entities.AttachmentEntity
import com.tridente.notiz.data.local.entities.NoteEntity
import com.tridente.notiz.data.repository.NoteRepository
import com.tridente.notiz.util.CalendarManager
import com.tridente.notiz.util.PdfExporter
import com.tridente.notiz.util.ReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class NoteEditorViewModel(
    private val noteRepository: NoteRepository,
    private val initialNoteId: String?
) : ViewModel() {

    private val _currentNote = MutableStateFlow<NoteEntity?>(null)
    val currentNote: StateFlow<NoteEntity?> = _currentNote.asStateFlow()

    private val _attachments = MutableStateFlow<List<AttachmentEntity>>(emptyList())
    val attachments: StateFlow<List<AttachmentEntity>> = _attachments.asStateFlow()

    var title = MutableStateFlow("")
    var contentMarkdown = MutableStateFlow("")
    var selectedFolderId = MutableStateFlow<String?>(null)
    var dueDateTimestamp = MutableStateFlow<Long?>(null)

    init {
        if (!initialNoteId.isNullOrBlank()) {
            viewModelScope.launch {
                val note = noteRepository.getNoteById(initialNoteId)
                if (note != null) {
                    _currentNote.value = note
                    title.value = note.title
                    contentMarkdown.value = note.contentMarkdown
                    selectedFolderId.value = note.folderId
                    dueDateTimestamp.value = note.dueDateTimestamp

                    noteRepository.getAttachmentsForNote(note.id).collect { list ->
                        _attachments.value = list
                    }
                }
            }
        }
    }

    fun saveNote(onSaved: () -> Unit) {
        viewModelScope.launch {
            val existing = _currentNote.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title.value,
                    contentMarkdown = contentMarkdown.value,
                    folderId = selectedFolderId.value,
                    dueDateTimestamp = dueDateTimestamp.value
                )
                noteRepository.updateNote(updated)
            } else {
                val created = noteRepository.createNote(
                    title = title.value,
                    contentMarkdown = contentMarkdown.value,
                    folderId = selectedFolderId.value,
                    dueDateTimestamp = dueDateTimestamp.value
                )
                _currentNote.value = created
            }
            onSaved()
        }
    }

    fun addImageAttachment(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val byteArray = stream.toByteArray()

                    val noteId = _currentNote.value?.id ?: run {
                        val newNote = noteRepository.createNote(
                            title = title.value.ifBlank { "Nueva Nota" },
                            contentMarkdown = contentMarkdown.value,
                            folderId = selectedFolderId.value,
                            dueDateTimestamp = dueDateTimestamp.value
                        )
                        _currentNote.value = newNote
                        newNote.id
                    }

                    noteRepository.addAttachment(
                        noteId = noteId,
                        fileName = "photo_${System.currentTimeMillis()}.jpg",
                        mimeType = "image/jpeg",
                        imageBlob = byteArray,
                        localPath = imageUri.toString()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch {
            noteRepository.deleteAttachment(attachmentId)
        }
    }

    fun setDueDate(timestamp: Long, context: Context) {
        dueDateTimestamp.value = timestamp
        val noteTitle = title.value.ifBlank { "Nota sin título" }
        _currentNote.value?.let { note ->
            ReminderManager.scheduleReminder(context, note.id, noteTitle, timestamp)
        }
    }

    fun syncWithNativeCalendar(context: Context) {
        val due = dueDateTimestamp.value
        if (due != null) {
            CalendarManager.addNoteToNativeCalendar(
                context = context,
                title = title.value.ifBlank { "Sin título" },
                description = contentMarkdown.value,
                dueDateTimestamp = due
            )
        }
    }

    fun exportToPdf(context: Context): Uri? {
        val blobs = _attachments.value.mapNotNull { it.imageBlob }
        return PdfExporter.generatePdfFile(
            context = context,
            title = title.value,
            contentMarkdown = contentMarkdown.value,
            imageBlobs = blobs
        )
    }

    fun printNote(context: Context) {
        PdfExporter.printNote(context, title.value, contentMarkdown.value)
    }

    class Factory(
        private val noteRepository: NoteRepository,
        private val initialNoteId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(noteRepository, initialNoteId) as T
        }
    }
}
