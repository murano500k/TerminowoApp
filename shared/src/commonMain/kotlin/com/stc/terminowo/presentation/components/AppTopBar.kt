package com.stc.terminowo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import org.jetbrains.compose.resources.stringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.notifications_title
import terminowo.shared.generated.resources.search_documents

@Composable
fun AppTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    showSearchIcon: Boolean = true,
    trailingActions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(Res.string.notifications_title)
                )
            }
            if (unreadNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(LocalExtendedColors.current.accentRed)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showSearchIcon) {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(Res.string.search_documents)
                    )
                }
            }
            trailingActions()
        }
    }
}
