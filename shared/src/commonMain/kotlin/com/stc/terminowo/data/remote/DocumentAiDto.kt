package com.stc.terminowo.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessRequest(
    val rawDocument: RawDocument
)

@Serializable
data class RawDocument(
    val content: String,
    val mimeType: String
)

@Serializable
data class ProcessResponse(
    val document: DocumentAiDocument? = null
)

@Serializable
data class DocumentAiDocument(
    val text: String? = null,
    val entities: List<DocumentAiEntity>? = null
)

@Serializable
data class DocumentAiEntity(
    val type: String? = null,
    val mentionText: String? = null,
    val confidence: Float? = null,
    val normalizedValue: NormalizedValue? = null
)

@Serializable
data class NormalizedValue(
    val text: String? = null,
    val dateValue: DateValue? = null
)

@Serializable
data class DateValue(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)
