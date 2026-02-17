package com.stc.terminowo.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class Document(
    val id: String,
    val name: String,
    val imagePath: String,
    val thumbnailPath: String,
    val expiryDate: LocalDate?,
    val confidence: Float?,
    val reminderDays: List<Int>,
    val createdAt: LocalDateTime
)
