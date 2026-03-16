package com.stc.terminowo.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

enum class DocumentStatus {
    ACTIVE, URGENT, EXPIRED
}

fun Document.daysUntilExpiry(today: LocalDate): Int? =
    expiryDate?.let { today.daysUntil(it) }

fun Document.status(today: LocalDate): DocumentStatus {
    val days = daysUntilExpiry(today)
    return when {
        days == null || days > 30 -> DocumentStatus.ACTIVE
        days in 0..30 -> DocumentStatus.URGENT
        else -> DocumentStatus.EXPIRED
    }
}
