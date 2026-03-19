package com.stc.terminowo.presentation.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.model.AppNotification
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.close
import terminowo.shared.generated.resources.notifications_empty
import terminowo.shared.generated.resources.notifications_expires_label
import terminowo.shared.generated.resources.notifications_title
import terminowo.shared.generated.resources.time_ago_days
import terminowo.shared.generated.resources.time_ago_hours
import terminowo.shared.generated.resources.time_ago_just_now
import terminowo.shared.generated.resources.time_ago_minutes
import terminowo.shared.generated.resources.time_ago_weeks

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.markAllAsReadAfterDelay()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close)
                )
            }
            Text(
                text = stringResource(Res.string.notifications_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.notifications_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationItem(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: AppNotification) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val unreadColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    val targetColor = if (notification.isRead) surfaceColor else unreadColor
    val animatedBg by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600)
    )

    val accentRed = LocalExtendedColors.current.accentRed
    val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
    val isExpired = notification.expiryDate != null &&
            today.daysUntil(notification.expiryDate) < 0

    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .background(animatedBg)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Severity icon
        if (isExpired) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = accentRed,
                modifier = Modifier.size(20.dp)
            )
        } else {
            // Urgent — exclamation mark style
            Text(
                text = "!",
                color = accentRed,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.documentName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (notification.category != DocumentCategory.OTHER) {
                Text(
                    text = stringResource(notification.category.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            notification.expiryDate?.let { date ->
                Text(
                    text = "${stringResource(Res.string.notifications_expires_label)} $date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Time ago
        Text(
            text = formatTimeAgo(notification.scheduledAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun formatTimeAgo(dateTime: LocalDateTime): String {
    val now = DateTimeClock.System.now()
    val tz = TimeZone.currentSystemDefault()
    val nowLocal = now.toLocalDateTime(tz)
    val today = DateTimeClock.System.todayIn(tz)
    val scheduledDate = LocalDate(dateTime.year, dateTime.monthNumber, dateTime.dayOfMonth)
    val daysDiff = scheduledDate.daysUntil(today)

    return when {
        daysDiff < 0 -> stringResource(Res.string.time_ago_just_now)
        daysDiff == 0 -> {
            val hoursDiff = nowLocal.hour - dateTime.hour
            val minutesDiff = (nowLocal.hour * 60 + nowLocal.minute) - (dateTime.hour * 60 + dateTime.minute)
            when {
                minutesDiff < 1 -> stringResource(Res.string.time_ago_just_now)
                minutesDiff < 60 -> stringResource(Res.string.time_ago_minutes, minutesDiff)
                else -> stringResource(Res.string.time_ago_hours, hoursDiff)
            }
        }
        daysDiff in 1..6 -> stringResource(Res.string.time_ago_days, daysDiff)
        else -> stringResource(Res.string.time_ago_weeks, daysDiff / 7)
    }
}
