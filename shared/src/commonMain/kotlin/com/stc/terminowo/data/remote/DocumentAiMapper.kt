package com.stc.terminowo.data.remote

import com.stc.terminowo.domain.model.ScanResult
import kotlinx.datetime.LocalDate

class DocumentAiMapper {

    fun mapToScanResult(response: ProcessResponse, rawJson: String?): ScanResult {
        val entities = response.document?.entities.orEmpty()

        val expiryEntity = entities.firstOrNull { it.type == "expiry_date" }
        val nameEntity = entities.firstOrNull { it.type == "document_name" }

        val expiryDate = expiryEntity?.let { extractDate(it) }
        val documentName = nameEntity?.let { extractName(it) }

        return ScanResult(
            extractedName = documentName,
            expiryDate = expiryDate,
            confidence = expiryEntity?.confidence,
            fullText = response.document?.text,
            rawResponse = rawJson
        )
    }

    private fun extractDate(entity: DocumentAiEntity): LocalDate? {
        // Strategy 1: Use structured dateValue from normalizedValue
        entity.normalizedValue?.dateValue?.let { dateValue ->
            val year = dateValue.year ?: return@let null
            val month = dateValue.month ?: return@let null
            val day = dateValue.day ?: return@let null
            return try {
                LocalDate(year, month, day)
            } catch (_: Exception) {
                null
            }
        }

        // Strategy 2: Parse normalizedValue.text as ISO date
        entity.normalizedValue?.text?.let { text ->
            return tryParseIsoDate(text)
        }

        // Strategy 3: Regex patterns on mentionText
        entity.mentionText?.let { text ->
            return tryParseDatePatterns(text)
        }

        return null
    }

    private fun extractName(entity: DocumentAiEntity): String? {
        return entity.normalizedValue?.text
            ?: entity.mentionText
    }

    private fun tryParseIsoDate(text: String): LocalDate? {
        return try {
            LocalDate.parse(text.trim().take(10))
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseDatePatterns(text: String): LocalDate? {
        val trimmed = text.trim()

        // DD/MM/YYYY or DD-MM-YYYY
        val ddmmyyyy = Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})""")
        ddmmyyyy.find(trimmed)?.let { match ->
            val (d, m, y) = match.destructured
            return tryBuildDate(y.toInt(), m.toInt(), d.toInt())
        }

        // YYYY-MM-DD or YYYY/MM/DD
        val yyyymmdd = Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""")
        yyyymmdd.find(trimmed)?.let { match ->
            val (y, m, d) = match.destructured
            return tryBuildDate(y.toInt(), m.toInt(), d.toInt())
        }

        return null
    }

    private fun tryBuildDate(year: Int, month: Int, day: Int): LocalDate? {
        return try {
            LocalDate(year, month, day)
        } catch (_: Exception) {
            null
        }
    }
}
