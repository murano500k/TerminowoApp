package com.stc.terminowo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.platform.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val dialogDismissed: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null
)

class AuthViewModel(
    private val googleAuthProvider: GoogleAuthProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            try {
                googleAuthProvider.getAccessToken()
                _uiState.update {
                    it.copy(
                        dialogDismissed = true,
                        isAuthenticated = true,
                        isLoggingIn = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        loginError = e.message ?: "Login failed"
                    )
                }
            }
        }
    }

    fun skip() {
        _uiState.update { it.copy(dialogDismissed = true) }
    }
}
