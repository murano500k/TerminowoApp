package com.stc.terminowo.data.remote

import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.model.ScanResult
import kotlinx.datetime.LocalDate

class DocumentAiMapper {

    fun mapToScanResult(response: ProcessResponse, rawJson: String?): ScanResult {
        val entities = response.document?.entities.orEmpty()

        val expiryEntity = entities.firstOrNull { it.type == "expiry_date" }
        val nameEntity = entities.firstOrNull { it.type == "document_name" }

        val expiryDate = expiryEntity?.let { extractDate(it) }
        val documentName = nameEntity?.let { extractName(it) }

        val detectedCategory = extractCategory(response.document?.text)

        return ScanResult(
            extractedName = documentName,
            expiryDate = expiryDate,
            confidence = expiryEntity?.confidence,
            fullText = response.document?.text,
            rawResponse = rawJson,
            detectedCategory = detectedCategory
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

    fun extractCategory(fullText: String?): DocumentCategory? {
        if (fullText.isNullOrBlank()) return null
        val text = fullText.lowercase()

        // Priority order: more specific categories first
        val categoryKeywords = listOf(
            DocumentCategory.TECHNICAL_INSPECTION to listOf(
                "przegląd techniczny", "badanie techniczne", "stacja kontroli pojazdów",
                "diagnostic station", "technical inspection", "vehicle inspection", "mot test",
                "технический осмотр", "техосмотр", "діагностична картка", "технічний огляд"
            ),
            DocumentCategory.DRIVER_LICENSE to listOf(
                "prawo jazdy", "prawa jazdy", "driver's license", "driver license", "driving licence",
                "водительское удостоверение", "водійське посвідчення", "посвідчення водія"
            ),
            DocumentCategory.INSURANCE to listOf(
                "ubezpieczenie", "polisa", "oc ", " oc ", "ac ", " ac ", "polisa ubezpieczeniowa",
                "insurance", "policy", "insurer", "coverage",
                "страхование", "страховка", "полис", "страхування", "поліс"
            ),
            DocumentCategory.AGREEMENT to listOf(
                "umowa", "kontrakt", "porozumienie",
                "agreement", "contract",
                "договор", "контракт", "договір"
            ),
            DocumentCategory.PAYMENT to listOf(
                "faktura", "rachunek", "płatność", "opłata",
                "invoice", "receipt", "payment", "bill",
                "счёт", "оплата", "платёж", "рахунок", "оплата"
            )
        )

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { it in text }) {
                return category
            }
        }

        return null
    }
}
