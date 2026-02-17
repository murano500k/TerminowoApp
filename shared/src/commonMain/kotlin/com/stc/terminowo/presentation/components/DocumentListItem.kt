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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn

@Composable
fun DocumentListItem(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = kotlinx.datetime.Clock.System.todayIn(TimeZone.currentSystemDefault())
    val daysUntilExpiry = document.expiryDate?.let { today.daysUntil(it) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                Text(
                    text = document.expiryDate?.let { formatExpiryDate(it, daysUntilExpiry) }
                        ?: "No expiry date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = expiryColor
                )
            }
        }
    }
}

private fun formatExpiryDate(date: LocalDate, daysUntil: Int?): String {
    val dateStr = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
    return when {
        daysUntil == null -> dateStr
        daysUntil < 0 -> "Expired ($dateStr)"
        daysUntil == 0 -> "Expires today"
        daysUntil == 1 -> "Expires tomorrow"
        daysUntil <= 30 -> "Expires in $daysUntil days"
        else -> "Expires $dateStr"
    }
}
