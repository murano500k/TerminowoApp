package com.stc.terminowo.data.remote

import com.stc.terminowo.platform.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DocumentAiService(
    private val httpClient: HttpClient,
    private val config: ProxyConfig
) {
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processDocument(imageBytes: ByteArray, mimeType: String): ProcessResponse {
        AppLogger.d(TAG, "processDocument called, imageBytes=${imageBytes.size}, mimeType=$mimeType")
        AppLogger.d(TAG, "endpoint=${config.url}")

        val base64Content = Base64.encode(imageBytes)
        AppLogger.d(TAG, "base64 encoded, length=${base64Content.length}")

        val request = ProcessRequest(
            rawDocument = RawDocument(
                content = base64Content,
                mimeType = mimeType
            )
        )

        AppLogger.d(TAG, "sending POST request...")

        val response = try {
            httpClient.post(config.url) {
                header("X-API-Key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "HTTP request FAILED: ${e::class.simpleName}: ${e.message}", e)
            throw e
        }

        AppLogger.d(TAG, "HTTP response status=${response.status}")
        val processResponse: ProcessResponse = try {
            response.body()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Response deserialization FAILED: ${e::class.simpleName}: ${e.message}", e)
            throw e
        }

        AppLogger.d(TAG, "Parsed response, document=${processResponse.document != null}")
        processResponse.document?.let { doc ->
            AppLogger.d(TAG, "text length=${doc.text?.length}, entities=${doc.entities?.size}")
            doc.entities?.forEach { entity ->
                AppLogger.d(TAG, "entity type=${entity.type}, mentionText=${entity.mentionText}, confidence=${entity.confidence}")
            }
        }

        return processResponse
    }

    companion object {
        private const val TAG = "DocumentAiService"
    }
}
