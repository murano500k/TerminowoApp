package com.stc.terminowo.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentCategoryTest {

    @Test
    fun `fromKey returns correct category for all known keys`() {
        assertEquals(DocumentCategory.INSURANCE, DocumentCategory.fromKey("insurance"))
        assertEquals(DocumentCategory.PAYMENTS, DocumentCategory.fromKey("payments"))
        assertEquals(DocumentCategory.AGREEMENT, DocumentCategory.fromKey("agreement"))
        assertEquals(DocumentCategory.DOCUMENTS, DocumentCategory.fromKey("documents"))
        assertEquals(DocumentCategory.TECHNICAL_INSPECTION, DocumentCategory.fromKey("technical_inspection"))
        assertEquals(DocumentCategory.SUBSCRIPTIONS, DocumentCategory.fromKey("subscriptions"))
        assertEquals(DocumentCategory.HEALTH, DocumentCategory.fromKey("health"))
        assertEquals(DocumentCategory.OTHER, DocumentCategory.fromKey("other"))
    }

    @Test
    fun `fromKey returns correct category for legacy keys`() {
        assertEquals(DocumentCategory.PAYMENTS, DocumentCategory.fromKey("payment"))
        assertEquals(DocumentCategory.DOCUMENTS, DocumentCategory.fromKey("driver_license"))
    }

    @Test
    fun `fromKey returns OTHER for unknown key`() {
        assertEquals(DocumentCategory.OTHER, DocumentCategory.fromKey("unknown"))
        assertEquals(DocumentCategory.OTHER, DocumentCategory.fromKey(""))
    }

    @Test
    fun `fromKey returns OTHER for null key`() {
        assertEquals(DocumentCategory.OTHER, DocumentCategory.fromKey(null))
    }

    @Test
    fun `DEFAULT is OTHER`() {
        assertEquals(DocumentCategory.OTHER, DocumentCategory.DEFAULT)
    }
}
