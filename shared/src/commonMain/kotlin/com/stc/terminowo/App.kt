package com.stc.terminowo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stc.terminowo.presentation.auth.AuthViewModel
import com.stc.terminowo.presentation.navigation.NavGraph
import com.stc.terminowo.presentation.theme.TerminowoTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.auth_dialog_message
import terminowo.shared.generated.resources.auth_dialog_title
import terminowo.shared.generated.resources.auth_login
import terminowo.shared.generated.resources.auth_login_failed
import terminowo.shared.generated.resources.auth_skip

@Composable
fun App() {
    TerminowoTheme(darkTheme = isSystemInDarkTheme()) {
        val authViewModel: AuthViewModel = koinViewModel()
        val authState by authViewModel.uiState.collectAsState()

        if (!authState.dialogDismissed) {
            AuthDialog(
                isLoggingIn = authState.isLoggingIn,
                loginError = authState.loginError,
                onLogin = authViewModel::login,
                onSkip = authViewModel::skip
            )
        }

        NavGraph(isAuthenticated = authState.isAuthenticated)
    }
}

@Composable
private fun AuthDialog(
    isLoggingIn: Boolean,
    loginError: String?,
    onLogin: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(Res.string.auth_dialog_title)) },
        text = {
            if (loginError != null) {
                Text(
                    text = stringResource(Res.string.auth_login_failed),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(stringResource(Res.string.auth_dialog_message))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onLogin,
                enabled = !isLoggingIn
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text(stringResource(Res.string.auth_login))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !isLoggingIn
            ) {
                Text(stringResource(Res.string.auth_skip))
            }
        }
    )
}
