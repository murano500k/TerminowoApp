package com.stc.terminowo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.model.DocumentCategory

private data class CategoryIconStyle(
    val icon: ImageVector,
    val backgroundColor: Color
)

private val categoryStyles = mapOf(
    DocumentCategory.INSURANCE to CategoryIconStyle(Icons.Default.Shield, Color(0xFF4CAF50)),
    DocumentCategory.PAYMENTS to CategoryIconStyle(Icons.Default.Receipt, Color(0xFFE91E63)),
    DocumentCategory.AGREEMENT to CategoryIconStyle(Icons.Default.Edit, Color(0xFF009688)),
    DocumentCategory.DOCUMENTS to CategoryIconStyle(Icons.Default.Article, Color(0xFF2196F3)),
    DocumentCategory.TECHNICAL_INSPECTION to CategoryIconStyle(Icons.Default.DirectionsCar, Color(0xFFFF9800)),
    DocumentCategory.SUBSCRIPTIONS to CategoryIconStyle(Icons.Default.Autorenew, Color(0xFF7C4DFF)),
    DocumentCategory.HEALTH to CategoryIconStyle(Icons.Default.LocalHospital, Color(0xFFE53935)),
    DocumentCategory.OTHER to CategoryIconStyle(Icons.Default.Description, Color(0xFF9E9E9E))
)

@Composable
fun CategoryIconCircle(
    category: DocumentCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val style = categoryStyles[category] ?: categoryStyles[DocumentCategory.OTHER]!!
    Box(
        modifier = modifier
            .size(size)
            .background(style.backgroundColor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.backgroundColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
