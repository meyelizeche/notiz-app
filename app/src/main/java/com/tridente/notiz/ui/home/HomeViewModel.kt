package com.tridente.notiz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tridente.notiz.data.local.entities.FolderEntity
import com.tridente.notiz.data.local.entities.NoteEntity
import com.tridente.notiz.data.repository.AuthRepository
import com.tridente.notiz.data.repository.FolderRepository
import com.tridente.notiz.data.repository.NoteRepository
import com.tridente.notiz.data.repository.SyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser = authRepository.currentUser

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val folders: StateFlow<List<FolderEntity>> = folderRepository.allFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubsubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEntity>> = _selectedFolderId.flatMapLatest { folderId ->
        if (_searchQuery.value.isNotBlank()) {
            noteRepository.searchNotes(_searchQuery.value)
        } else {
            noteRepository.getNotesByFolder(folderId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubsubscribed(5000),
        initialValue = emptyList()
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun searchNotes(query: String) {
        _searchQuery.value = query
    }

    fun triggerSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncRepository.performSync()
            _isSyncing.value = false
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun moveNoteToFolder(noteId: String, folderId: String?) {
        viewModelScope.launch {
            noteRepository.moveNoteToFolder(noteId, folderId)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    class Factory(
        private val noteRepository: NoteRepository,
        private val folderRepository: FolderRepository,
        private val syncRepository: SyncRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(noteRepository, folderRepository, syncRepository, authRepository) as T
        }
    }
}
