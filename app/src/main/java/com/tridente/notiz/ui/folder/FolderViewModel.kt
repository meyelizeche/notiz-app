package com.tridente.notiz.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tridente.notiz.data.local.entities.FolderEntity
import com.tridente.notiz.data.repository.FolderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderViewModel(
    private val folderRepository: FolderRepository
) : ViewModel() {

    val allFolders: StateFlow<List<FolderEntity>> = folderRepository.allFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubsubscribed(5000),
        initialValue = emptyList()
    )

    fun createFolder(name: String, parentFolderId: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderRepository.createFolder(name, parentFolderId)
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            folderRepository.updateFolder(folder)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId)
        }
    }

    class Factory(private val folderRepository: FolderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FolderViewModel(folderRepository) as T
        }
    }
}
