package com.stc.terminowo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.data.remote.UserProfileService
import com.stc.terminowo.domain.model.UserProfile
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
    val loginError: String? = null,
    val userProfile: UserProfile? = null,
    val isLoadingProfile: Boolean = false
)

class AuthViewModel(
    private val googleAuthProvider: GoogleAuthProvider,
    private val userProfileService: UserProfileService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            try {
                val token = googleAuthProvider.getAccessToken()
                _uiState.update {
                    it.copy(
                        dialogDismissed = true,
                        isAuthenticated = true,
                        isLoggingIn = false,
                        isLoadingProfile = true
                    )
                }
                fetchProfile(token)
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

    fun logout() {
        googleAuthProvider.clearToken()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                userProfile = null,
                isLoadingProfile = false,
                dialogDismissed = true
            )
        }
    }

    fun skip() {
        _uiState.update { it.copy(dialogDismissed = true) }
    }

    private suspend fun fetchProfile(token: String) {
        try {
            val profile = userProfileService.fetchProfile(token)
            _uiState.update {
                it.copy(userProfile = profile, isLoadingProfile = false)
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(isLoadingProfile = false) }
        }
    }
}
