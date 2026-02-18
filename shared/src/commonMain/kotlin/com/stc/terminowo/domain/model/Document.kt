package com.stc.terminowo.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

data class Document(
    val id: String,
    val name: String,
    val imagePath: String,
    val thumbnailPath: String,
    val expiryDate: LocalDate?,
    val confidence: Float?,
    val reminderDays: List<Int>,
    val category: DocumentCategory = DocumentCategory.OTHER,
    val reminderTime: LocalTime = LocalTime(9, 0),
    val createdAt: LocalDateTime
)
