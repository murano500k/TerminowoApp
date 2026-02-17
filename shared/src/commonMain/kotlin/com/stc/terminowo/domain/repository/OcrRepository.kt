package com.stc.terminowo.domain.repository

import com.stc.terminowo.domain.model.ScanResult

interface OcrRepository {
    suspend fun processDocument(imageBytes: ByteArray, mimeType: String): ScanResult
}
