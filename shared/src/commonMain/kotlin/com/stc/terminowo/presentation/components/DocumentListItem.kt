package com.stc.terminowo.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.model.DocumentCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlinx.datetime.Clock as DateTimeClock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.expired_with_date
import terminowo.shared.generated.resources.expires_in_days
import terminowo.shared.generated.resources.expires_on_date
import terminowo.shared.generated.resources.expires_today
import terminowo.shared.generated.resources.expires_tomorrow
import terminowo.shared.generated.resources.no_expiry_date

@Composable
fun DocumentListItem(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
    val daysUntilExpiry = document.expiryDate?.let { today.daysUntil(it) }
    val isExpired = daysUntilExpiry != null && daysUntilExpiry < 0
    val expiryColor = when {
        daysUntilExpiry == null -> MaterialTheme.colorScheme.onSurfaceVariant
        daysUntilExpiry < 0 -> MaterialTheme.colorScheme.error
        daysUntilExpiry <= 7 -> MaterialTheme.colorScheme.error
        daysUntilExpiry <= 30 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isExpired) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Card(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                // Thumbnail image would go here
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (document.category != DocumentCategory.OTHER) {
                    Text(
                        text = stringResource(document.category.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = document.expiryDate?.let { formatExpiryDate(it, daysUntilExpiry) }
                        ?: stringResource(Res.string.no_expiry_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = expiryColor
                )
            }
        }
    }
}

@Composable
private fun formatExpiryDate(date: LocalDate, daysUntil: Int?): String {
    val dateStr = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
    return when {
        daysUntil == null -> dateStr
        daysUntil < 0 -> stringResource(Res.string.expired_with_date, dateStr)
        daysUntil == 0 -> stringResource(Res.string.expires_today)
        daysUntil == 1 -> stringResource(Res.string.expires_tomorrow)
        daysUntil <= 30 -> pluralStringResource(Res.plurals.expires_in_days, daysUntil, daysUntil)
        else -> stringResource(Res.string.expires_on_date, dateStr)
    }
}
