package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.model.ScanResult
import com.stc.terminowo.domain.repository.OcrRepository

class ScanDocumentUseCase(
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray, mimeType: String): Result<ScanResult> {
        println("DocScanner-UseCase: invoke called, imageBytes=${imageBytes.size}, mimeType=$mimeType")
        return try {
            val result = ocrRepository.processDocument(imageBytes, mimeType)
            println("DocScanner-UseCase: SUCCESS - name=${result.extractedName}, expiryDate=${result.expiryDate}, confidence=${result.confidence}")
            Result.success(result)
        } catch (e: Exception) {
            println("DocScanner-UseCase: FAILED - ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
