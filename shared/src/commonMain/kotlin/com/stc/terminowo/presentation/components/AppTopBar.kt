package com.stc.terminowo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .width(52.dp)
                    .height(40.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(Res.string.notifications_title),
                    modifier = Modifier.size(22.dp)
                )
            }
            if (unreadNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 10.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LocalExtendedColors.current.accentRed)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showSearchIcon) {
                IconButton(
                    onClick = { onSearchActiveChange(true) },
                    modifier = Modifier
                        .width(52.dp)
                        .height(40.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(Res.string.search_documents),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            trailingActions()
        }
    }
}
