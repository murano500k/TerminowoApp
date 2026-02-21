package com.stc.terminowo.data.remote

import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.model.ScanResult
import com.stc.terminowo.domain.repository.OcrRepository
import com.stc.terminowo.domain.usecase.ScanDocumentUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration-style tests using real OCR response data from an Orange Polska telecom bill.
 * The fixture values below were captured from a single Document AI call against
 * test_data/Screenshot from 2026-02-21 22-54-40.png — no live API calls in tests.
 */
class OrangePolskaBillScanTest {

    private val mapper = DocumentAiMapper()

    // ── Real OCR fixture ────────────────────────────────────────────────

    private val fullText = """
        orange
        Orange Polska S.A.
        Aleje Jerozolimskie 160
        02-326 Warszawa
        Firma
        OK
        Twoje rozliczenie
        NIP 526-025-09-95
        Przyjazna Klientowi ★SENIOR
        TM
        Data wystawienia: 19 stycznia 2026
        Skontaktuj się z nami:
        przez aplikację Mój Orange lub
        510 100 100 w sprawie obsługi
        801 234 567 - w sprawie zakupu oferty
        MIKHAIL DZENISIUK
        UL. ŻEROMSKIEGO 36 m. D1
        21-500 BIAŁA PODLASKA
        Numer konta Klienta: 1.27403664
        2026
        Wszystkiego, co najlepsze,
        w nowym roku
        Twoje usługi
        Kwota brutto
        Usługi mobilne Orange
        50,00
        5G
        Rozliczenie dodatkowe (odsetki)
        Zapłać do: 02.02.2026
        0,20
        Nr konta bankowego: 54 1140 1241
        Opłata za bieżący okres:
        1068
        0000 2056 9922
        50,20 zł
        Tytuł przelewu: 26010714995804
        Rozliczenie poprzednich okresów
        0,00 zł
        Kwota do zapłaty z uwzględnieniem rozliczenia poprzednich okresów
        50,20 zł
        Zapłać online za bieżący okres
    """.trimIndent()

    /** Builds the ProcessResponse exactly as returned by Document AI for the Orange bill. */
    private fun buildOrangeResponse(): ProcessResponse = ProcessResponse(
        document = DocumentAiDocument(
            text = fullText,
            entities = listOf(
                DocumentAiEntity(
                    type = "document_type",
                    mentionText = "payment",
                    confidence = 0.98716801f,
                    normalizedValue = null
                ),
                DocumentAiEntity(
                    type = "document_name",
                    mentionText = "Twoje rozliczenie",
                    confidence = 0.97976679f,
                    normalizedValue = null
                ),
                DocumentAiEntity(
                    type = "expiry_date",
                    mentionText = "02.02.2026",
                    confidence = 0.97292489f,
                    normalizedValue = NormalizedValue(
                        text = "2026-02-02",
                        dateValue = DateValue(year = 2026, month = 2, day = 2)
                    )
                )
            )
        )
    )

    // ── Test 1: Extract payment deadline date ───────────────────────────

    @Test
    fun `extracts payment deadline date from structured dateValue`() {
        val result = mapper.mapToScanResult(buildOrangeResponse(), null)

        assertEquals(LocalDate(2026, 2, 2), result.expiryDate)
    }

    // ── Test 2: Detect PAYMENT category ─────────────────────────────────

    @Test
    fun `detects PAYMENT category from Polish bill text`() {
        val result = mapper.mapToScanResult(buildOrangeResponse(), null)

        assertEquals(DocumentCategory.PAYMENT, result.detectedCategory)
    }

    // ── Test 3: Extract document name ───────────────────────────────────

    @Test
    fun `extracts document name from entity`() {
        val result = mapper.mapToScanResult(buildOrangeResponse(), null)

        assertEquals("Twoje rozliczenie", result.extractedName)
    }

    // ── Test 4: Confidence score is valid ───────────────────────────────

    @Test
    fun `confidence score is present and in valid range`() {
        val result = mapper.mapToScanResult(buildOrangeResponse(), null)

        assertNotNull(result.confidence)
        assertTrue(result.confidence!! > 0f, "confidence should be positive")
        assertTrue(result.confidence!! <= 1f, "confidence should be at most 1.0")
    }

    // ── Test 5: Full OCR text preserved ─────────────────────────────────

    @Test
    fun `full OCR text contains key bill strings`() {
        val result = mapper.mapToScanResult(buildOrangeResponse(), null)

        assertNotNull(result.fullText)
        assertTrue(result.fullText!!.contains("Orange"), "should contain 'Orange'")
        assertTrue(result.fullText!!.contains("50,20"), "should contain total amount")
        assertTrue(result.fullText!!.contains("02.02.2026"), "should contain due date")
    }

    // ── Test 6: No false category match ─────────────────────────────────

    @Test
    fun `bill is not classified as insurance or driver_license or technical_inspection`() {
        val category = mapper.extractCategory(fullText)

        assertTrue(
            category != DocumentCategory.INSURANCE,
            "should not be classified as INSURANCE"
        )
        assertTrue(
            category != DocumentCategory.DRIVER_LICENSE,
            "should not be classified as DRIVER_LICENSE"
        )
        assertTrue(
            category != DocumentCategory.TECHNICAL_INSPECTION,
            "should not be classified as TECHNICAL_INSPECTION"
        )
    }

    // ── Test 7: DD.MM.YYYY regex (dot separator) ────────────────────────

    @Test
    fun `parses DD_MM_YYYY with dot separator via mentionText regex`() {
        // Strip normalizedValue so mapper falls through to regex strategy
        val response = ProcessResponse(
            document = DocumentAiDocument(
                entities = listOf(
                    DocumentAiEntity(
                        type = "expiry_date",
                        mentionText = "02.02.2026",
                        confidence = 0.97f
                    )
                )
            )
        )

        val result = mapper.mapToScanResult(response, null)
        assertEquals(LocalDate(2026, 2, 2), result.expiryDate)
    }

    // ── Test 8: ScanDocumentUseCase success path ────────────────────────

    @Test
    fun `use case wraps scan result in Result success`() = runTest {
        val expected = mapper.mapToScanResult(buildOrangeResponse(), null)
        val fakeRepo = object : OcrRepository {
            override suspend fun processDocument(imageBytes: ByteArray, mimeType: String): ScanResult = expected
        }
        val useCase = ScanDocumentUseCase(fakeRepo)

        val result = useCase(byteArrayOf(1, 2, 3), "image/png")

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    // ── Test 9: ScanDocumentUseCase failure path ────────────────────────

    @Test
    fun `use case wraps exception in Result failure`() = runTest {
        val fakeRepo = object : OcrRepository {
            override suspend fun processDocument(imageBytes: ByteArray, mimeType: String): ScanResult {
                throw RuntimeException("Network error")
            }
        }
        val useCase = ScanDocumentUseCase(fakeRepo)

        val result = useCase(byteArrayOf(1, 2, 3), "image/png")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
