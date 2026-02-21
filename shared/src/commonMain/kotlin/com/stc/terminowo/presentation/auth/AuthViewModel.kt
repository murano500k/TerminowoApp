package com.stc.terminowo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.data.remote.OcrTokenProvider
import com.stc.terminowo.data.remote.UserProfileService
import com.stc.terminowo.domain.model.UserProfile
import com.stc.terminowo.platform.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val requiresUserAuth: Boolean = true,
    val isCheckingAuth: Boolean = true,
    val dialogDismissed: Boolean = false,
    val showLoginDialog: Boolean = false,
    val isAuthenticated: Boolean = false,
    val canUseOcr: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val userProfile: UserProfile? = null,
    val isLoadingProfile: Boolean = false
)

class AuthViewModel(
    private val googleAuthProvider: GoogleAuthProvider,
    private val userProfileService: UserProfileService,
    private val ocrTokenProvider: OcrTokenProvider
) : ViewModel() {

    private val needsUserAuth = ocrTokenProvider.requiresUserAuth

    private val _uiState = MutableStateFlow(
        AuthUiState(
            requiresUserAuth = needsUserAuth,
            isCheckingAuth = needsUserAuth,
            dialogDismissed = !needsUserAuth,
            canUseOcr = !needsUserAuth
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (needsUserAuth) {
            viewModelScope.launch {
                if (googleAuthProvider.isLoggedOut()) {
                    _uiState.update { it.copy(isCheckingAuth = false) }
                    return@launch
                }
                try {
                    val token = googleAuthProvider.getAccessToken()
                    _uiState.update {
                        it.copy(
                            isCheckingAuth = false,
                            dialogDismissed = true,
                            isAuthenticated = true,
                            canUseOcr = true,
                            isLoadingProfile = true
                        )
                    }
                    fetchProfile(token)
                } catch (_: Exception) {
                    _uiState.update { it.copy(isCheckingAuth = false) }
                }
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            googleAuthProvider.setLoggedOut(false)
            try {
                val token = googleAuthProvider.getAccessToken()
                _uiState.update {
                    it.copy(
                        dialogDismissed = true,
                        isAuthenticated = true,
                        canUseOcr = true,
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
        googleAuthProvider.setLoggedOut(true)
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                canUseOcr = !needsUserAuth,
                userProfile = null,
                isLoadingProfile = false,
                dialogDismissed = true
            )
        }
    }

    fun proceedToLogin() {
        _uiState.update { it.copy(showLoginDialog = true) }
    }

    fun skip() {
        _uiState.update { it.copy(dialogDismissed = true, showLoginDialog = false) }
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
