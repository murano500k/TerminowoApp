package com.stc.terminowo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stc.terminowo.presentation.auth.AuthUiState
import org.jetbrains.compose.resources.stringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.account
import terminowo.shared.generated.resources.auth_login
import terminowo.shared.generated.resources.google_api_access_granted
import terminowo.shared.generated.resources.not_signed_in
import terminowo.shared.generated.resources.sign_in_for_ocr
import terminowo.shared.generated.resources.sign_out

@Composable
fun AccountIconButton(
    authState: AuthUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            if (authState.isAuthenticated && authState.userProfile != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authState.userProfile.initials,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(Res.string.account),
                    tint = if (authState.isAuthenticated)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (authState.isAuthenticated) {
                AuthenticatedMenuContent(
                    authState = authState,
                    onSignOut = {
                        expanded = false
                        onSignOut()
                    }
                )
            } else {
                UnauthenticatedMenuContent(
                    onSignIn = {
                        expanded = false
                        onSignIn()
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedMenuContent(
    authState: AuthUiState,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        authState.userProfile?.let { profile ->
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = stringResource(Res.string.google_api_access_granted),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
    HorizontalDivider()
    TextButton(
        onClick = onSignOut,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(stringResource(Res.string.sign_out))
    }
}

@Composable
private fun UnauthenticatedMenuContent(
    onSignIn: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.not_signed_in),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(Res.string.sign_in_for_ocr),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
    TextButton(
        onClick = onSignIn,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(stringResource(Res.string.auth_login))
    }
}
