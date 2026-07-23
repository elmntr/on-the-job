package com.example.onthejob.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.onthejob.data.auth.AuthRepository
import com.example.onthejob.data.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SignInUiState {
    object Idle : SignInUiState()
    object Loading : SignInUiState()
    data class SignedIn(val displayName: String?) : SignInUiState()
    data class Error(val message: String) : SignInUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<SignInUiState>(
        repository.currentUser?.let { SignInUiState.SignedIn(it.displayName) }
            ?: SignInUiState.Idle
    )
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(webClientId: String) {
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            when (val result = repository.signInWithGoogle(webClientId)) {
                is AuthResult.Success -> _uiState.value = SignInUiState.SignedIn(result.user.displayName)
                is AuthResult.Failure -> _uiState.value = SignInUiState.Error(result.message)
                AuthResult.Cancelled -> _uiState.value = SignInUiState.Idle
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.value = SignInUiState.Idle
        }
    }
}