package com.stc.terminowo.domain.model

import com.stc.terminowo.domain.model.DocumentCategory
import kotlinx.datetime.LocalDate

data class ScanResult(
    val extractedName: String?,
    val expiryDate: LocalDate?,
    val confidence: Float?,
    val fullText: String?,
    val rawResponse: String?,
    val detectedCategory: DocumentCategory? = null
)
