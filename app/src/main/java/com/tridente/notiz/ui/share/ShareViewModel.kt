package com.tridente.notiz.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tridente.notiz.data.local.entities.NoteShareEntity
import com.tridente.notiz.data.repository.ShareRepository
import com.tridente.notiz.data.security.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ShareUiState {
    object Idle : ShareUiState()
    object Loading : ShareUiState()
    data class Success(val message: String) : ShareUiState()
    data class PublicLinkGenerated(val link: String) : ShareUiState()
    data class Error(val message: String) : ShareUiState()
}

class ShareViewModel(
    private val shareRepository: ShareRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun getSharesForNote(noteId: String) = shareRepository.getSharesForNote(noteId)

    fun shareNoteInternally(noteId: String, email: String, permissionRole: String) {
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = ShareUiState.Error("Ingresa un correo electrónico válido")
            return
        }

        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            val result = shareRepository.shareNoteInternally(noteId, email, permissionRole, tokenManager)
            result.fold(
                onSuccess = {
                    _uiState.value = ShareUiState.Success("Nota compartida exitosamente con $email")
                },
                onFailure = { error ->
                    _uiState.value = ShareUiState.Error(error.message ?: "Error al compartir la nota")
                }
            )
        }
    }

    fun generatePublicLink(noteId: String) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            val result = shareRepository.generatePublicLink(noteId, tokenManager)
            result.fold(
                onSuccess = { link ->
                    _uiState.value = ShareUiState.PublicLinkGenerated(link)
                },
                onFailure = { error ->
                    _uiState.value = ShareUiState.Error(error.message ?: "Error al generar enlace público")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = ShareUiState.Idle
    }

    class Factory(
        private val shareRepository: ShareRepository,
        private val tokenManager: TokenManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShareViewModel(shareRepository, tokenManager) as T
        }
    }
}
