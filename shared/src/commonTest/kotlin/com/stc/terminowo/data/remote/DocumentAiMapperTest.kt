package com.stc.terminowo.data.remote

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentAiMapperTest {

    private val mapper = DocumentAiMapper()

    @Test
    fun `maps structured dateValue correctly`() {
        val response = ProcessResponse(
            document = DocumentAiDocument(
                text = "Some text",
                entities = listOf(
                    DocumentAiEntity(
                        type = "expiry_date",
                        mentionText = "15/03/2026",
                        confidence = 0.97f,
                        normalizedValue = NormalizedValue(
                            dateValue = DateValue(year = 2026, month = 3, day = 15)
                        )
                    ),
                    DocumentAiEntity(
                        type = "document_name",
                        mentionText = "Car Insurance Policy",
                        confidence = 0.94f,
                        normalizedValue = NormalizedValue(text = "Car Insurance Policy")
                    )
                )
            )
        )

        val result = mapper.mapToScanResult(response, null)

        assertEquals(LocalDate(2026, 3, 15), result.expiryDate)
        assertEquals("Car Insurance Policy", result.extractedName)
        assertEquals(0.97f, result.confidence)
        assertEquals("Some text", result.fullText)
    }

    @Test
    fun `maps ISO date from normalizedValue text`() {
        val response = ProcessResponse(
            document = DocumentAiDocument(
                entities = listOf(
                    DocumentAiEntity(
                        type = "expiry_date",
                        mentionText = "2026-06-30",
                        confidence = 0.90f,
                        normalizedValue = NormalizedValue(text = "2026-06-30")
                    )
                )
            )
        )

        val result = mapper.mapToScanResult(response, null)
        assertEquals(LocalDate(2026, 6, 30), result.expiryDate)
    }

    @Test
    fun `parses DD_MM_YYYY from mentionText`() {
        val response = ProcessResponse(
            document = DocumentAiDocument(
                entities = listOf(
                    DocumentAiEntity(
                        type = "expiry_date",
                        mentionText = "25/12/2026",
                        confidence = 0.85f
                    )
                )
            )
        )

        val result = mapper.mapToScanResult(response, null)
        assertEquals(LocalDate(2026, 12, 25), result.expiryDate)
    }

    @Test
    fun `returns null for empty entities`() {
        val response = ProcessResponse(
            document = DocumentAiDocument(
                text = "Some text",
                entities = emptyList()
            )
        )

        val result = mapper.mapToScanResult(response, null)
        assertNull(result.expiryDate)
        assertNull(result.extractedName)
        assertNull(result.confidence)
    }

    @Test
    fun `returns null for null document`() {
        val response = ProcessResponse(document = null)

        val result = mapper.mapToScanResult(response, null)
        assertNull(result.expiryDate)
        assertNull(result.extractedName)
    }

    @Test
    fun `extracts name from mentionText when normalizedValue is null`() {
        val response = ProcessResponse(
            document = DocumentAiDocument(
                entities = listOf(
                    DocumentAiEntity(
                        type = "document_name",
                        mentionText = "Health Insurance",
                        confidence = 0.92f
                    )
                )
            )
        )

        val result = mapper.mapToScanResult(response, null)
        assertEquals("Health Insurance", result.extractedName)
    }
}
