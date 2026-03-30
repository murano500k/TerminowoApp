package com.stc.terminowo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.menu_privacy_policy
import terminowo.shared.generated.resources.menu_terms_of_service
import terminowo.shared.generated.resources.settings

@Composable
fun SettingsMenu(
    extraItems: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.settings)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            extraItems { expanded = false }
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.menu_terms_of_service)) },
                onClick = {
                    expanded = false
                    uriHandler.openUri("https://terminowo.app/docs/terms-of-service.html")
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.menu_privacy_policy)) },
                onClick = {
                    expanded = false
                    uriHandler.openUri("https://terminowo.app/docs/privacy-policy.html")
                }
            )
        }
    }
}
