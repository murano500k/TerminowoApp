package com.stc.terminowo.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.model.Document
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlinx.datetime.Clock as DateTimeClock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.expired_days_ago_short
import terminowo.shared.generated.resources.expires_in_days_short
import terminowo.shared.generated.resources.expires_today
import terminowo.shared.generated.resources.no_expiry_date

@Composable
fun DocumentListItem(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
    val daysUntilExpiry = document.expiryDate?.let { today.daysUntil(it) }
    val expiryColor = when {
        daysUntilExpiry == null -> MaterialTheme.colorScheme.onSurfaceVariant
        daysUntilExpiry < 0 -> MaterialTheme.colorScheme.error
        daysUntilExpiry <= 30 -> MaterialTheme.colorScheme.error
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconCircle(category = document.category)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(document.category.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDaysText(daysUntilExpiry),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = expiryColor,
                    textAlign = TextAlign.End
                )
                Text(
                    text = document.expiryDate?.let { formatDateShort(it) } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun formatDaysText(daysUntil: Int?): String {
    return when {
        daysUntil == null -> stringResource(Res.string.no_expiry_date)
        daysUntil < 0 -> pluralStringResource(Res.plurals.expired_days_ago_short, -daysUntil, -daysUntil)
        daysUntil == 0 -> stringResource(Res.string.expires_today)
        else -> pluralStringResource(Res.plurals.expires_in_days_short, daysUntil, daysUntil)
    }
}

private fun formatDateShort(date: LocalDate): String {
    return "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
}
