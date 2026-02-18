package com.stc.terminowo.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentCategoryTest {

    @Test
    fun `fromKey returns correct category for all known keys`() {
        assertEquals(DocumentCategory.INSURANCE, DocumentCategory.fromKey("insurance"))
        assertEquals(DocumentCategory.PAYMENT, DocumentCategory.fromKey("payment"))
        assertEquals(DocumentCategory.AGREEMENT, DocumentCategory.fromKey("agreement"))
        assertEquals(DocumentCategory.DRIVER_LICENSE, DocumentCategory.fromKey("driver_license"))
        assertEquals(DocumentCategory.TECHNICAL_INSPECTION, DocumentCategory.fromKey("technical_inspection"))
        assertEquals(DocumentCategory.OTHER, DocumentCategory.fromKey("other"))
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
