package com.stc.terminowo.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class AppNotification(
    val id: String,
    val documentId: String,
    val documentName: String,
    val category: DocumentCategory,
    val expiryDate: LocalDate?,
    val daysBefore: Int,
    val scheduledAt: LocalDateTime,
    val isRead: Boolean
)
