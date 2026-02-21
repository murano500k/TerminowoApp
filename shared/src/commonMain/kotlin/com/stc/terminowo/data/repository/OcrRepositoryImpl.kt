package com.stc.terminowo.data.repository

import com.stc.terminowo.data.remote.DocumentAiMapper
import com.stc.terminowo.data.remote.DocumentAiService
import com.stc.terminowo.domain.model.ScanResult
import com.stc.terminowo.domain.repository.OcrRepository
import kotlinx.serialization.json.Json

class OcrRepositoryImpl(
    private val documentAiService: DocumentAiService,
    private val mapper: DocumentAiMapper,
    private val json: Json
) : OcrRepository {

    override suspend fun processDocument(imageBytes: ByteArray, mimeType: String): ScanResult {
        val response = documentAiService.processDocument(imageBytes, mimeType)
        val rawJson = try {
            json.encodeToString(
                com.stc.terminowo.data.remote.ProcessResponse.serializer(),
                response
            )
        } catch (_: Exception) {
            null
        }
        return mapper.mapToScanResult(response, rawJson)
    }
}
