package com.stc.terminowo.presentation.components

import com.stc.terminowo.domain.model.Document
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentSearchTest {

    private fun document(
        name: String = "Badanie",
        myComments: String = "",
        ocrText: String = ""
    ) = Document(
        id = "1",
        name = name,
        imagePath = "",
        thumbnailPath = "",
        expiryDate = null,
        confidence = null,
        reminderDays = emptyList(),
        createdAt = LocalDateTime(2026, 1, 1, 9, 0),
        myComments = myComments,
        ocrText = ocrText
    )

    private val medical = document(
        ocrText = "ORZECZENIE LEKARSKIE nr 12/2026 Pan Jan Kowalski zamieszkały w Łodzi jest zdolny do pracy na stanowisku kierowca"
    )

    @Test
    fun `matches by name`() {
        assertTrue(document(name = "Car Insurance").matchesSearch("insurance"))
    }

    @Test
    fun `matches by comments`() {
        assertTrue(document(myComments = "for Anna").matchesSearch("anna"))
    }

    @Test
    fun `matches by OCR text`() {
        assertTrue(medical.matchesSearch("kowalski"))
        assertTrue(medical.matchesSearch("Jan Kowalski"))
    }

    @Test
    fun `query whitespace is normalized`() {
        assertTrue(medical.matchesSearch("  jan   kowalski "))
    }

    @Test
    fun `ignores Polish diacritics in both directions`() {
        assertTrue(medical.matchesSearch("lodzi"))
        assertTrue(medical.matchesSearch("zamieszkaly"))
        assertTrue(document(ocrText = "Zamieszkaly w Lodzi").matchesSearch("Łodzi"))
    }

    @Test
    fun `blank query or missing text does not match`() {
        assertFalse(medical.matchesSearch("   "))
        assertFalse(medical.matchesSearch("nowak"))
    }

    @Test
    fun `multi-word query matches words in any order`() {
        // Real Document AI output: form fields read out of order split the name.
        val scanned = document(ocrText = "Pan/Pani: Jan PESEL: Kowalski 85031512345 Stanowisko: kierowca")
        assertTrue(scanned.matchesSearch("jan kowalski"))
        assertTrue(scanned.matchesSearch("kowalski jan"))
    }

    @Test
    fun `multi-word query requires every word`() {
        assertFalse(medical.matchesSearch("jan nowak"))
    }

    @Test
    fun `multi-word query matches words spread across fields`() {
        val doc = document(name = "Orzeczenie", myComments = "kadry", ocrText = "Jan Kowalski")
        assertTrue(doc.matchesSearch("orzeczenie kadry kowalski"))
    }

    @Test
    fun `snippet is null when name or comments already contain every word`() {
        assertNull(document(name = "Kowalski", ocrText = "Jan Kowalski").ocrSnippet("kowalski"))
        assertNull(document(myComments = "Kowalski", ocrText = "Jan Kowalski").ocrSnippet("kowalski"))
        assertNull(document(name = "Jan", myComments = "Kowalski", ocrText = "Jan Kowalski").ocrSnippet("jan kowalski"))
    }

    @Test
    fun `snippet is shown when some words only match the OCR text`() {
        val snippet = assertNotNull(document(name = "Orzeczenie", ocrText = "Jan Kowalski").ocrSnippet("orzeczenie kowalski"))
        assertEquals(listOf("Kowalski"), snippet.highlighted())
    }

    @Test
    fun `snippet is null when nothing matches`() {
        assertNull(medical.ocrSnippet("nowak"))
    }

    @Test
    fun `snippet highlights the match in original casing`() {
        val snippet = assertNotNull(medical.ocrSnippet("lodzi"))
        assertEquals(listOf("Łodzi"), snippet.highlighted())
    }

    @Test
    fun `snippet highlights every query word in the excerpt`() {
        val scanned = document(ocrText = "Pan/Pani: Jan PESEL: Kowalski 85031512345 Stanowisko: kierowca")
        val snippet = assertNotNull(scanned.ocrSnippet("kowalski jan"))
        assertEquals(listOf("Jan", "Kowalski"), snippet.highlighted())
    }

    @Test
    fun `snippet adds ellipses and keeps whole words at trimmed edges`() {
        val snippet = assertNotNull(medical.ocrSnippet("kowalski", contextChars = 10))
        assertEquals("…Pan Jan Kowalski…", snippet.text)
        assertEquals(listOf("Kowalski"), snippet.highlighted())
    }

    @Test
    fun `short text is shown in full without ellipses`() {
        val snippet = assertNotNull(document(ocrText = "Jan Kowalski").ocrSnippet("jan"))
        assertEquals("Jan Kowalski", snippet.text)
        assertEquals(listOf(0..2), snippet.highlights)
    }

    private fun SearchSnippet.highlighted(): List<String> = highlights.map { text.substring(it) }
}
