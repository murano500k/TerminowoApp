package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.model.ScanResult
import com.stc.terminowo.domain.repository.OcrRepository
import com.stc.terminowo.platform.AppLogger

class ScanDocumentUseCase(
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray, mimeType: String): Result<ScanResult> {
        AppLogger.d(TAG, "invoke called, imageBytes=${imageBytes.size}, mimeType=$mimeType")
        return try {
            val result = ocrRepository.processDocument(imageBytes, mimeType)
            AppLogger.d(TAG, "SUCCESS - name=${result.extractedName}, expiryDate=${result.expiryDate}, confidence=${result.confidence}")
            Result.success(result)
        } catch (e: Exception) {
            AppLogger.e(TAG, "FAILED - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ScanDocumentUseCase"
    }
}
