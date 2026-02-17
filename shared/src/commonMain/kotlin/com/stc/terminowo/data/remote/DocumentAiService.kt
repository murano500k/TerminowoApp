package com.stc.terminowo.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DocumentAiService(
    private val httpClient: HttpClient,
    private val config: DocumentAiConfig
) {
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processDocument(imageBytes: ByteArray, mimeType: String): ProcessResponse {
        println("DocScanner-OCR: processDocument called, imageBytes=${imageBytes.size}, mimeType=$mimeType")
        println("DocScanner-OCR: endpoint=${config.endpoint}")
        println("DocScanner-OCR: projectId=${config.projectId}, location=${config.location}, processorId=${config.processorId}")
        println("DocScanner-OCR: apiKey present=${config.apiKey.isNotBlank()}, length=${config.apiKey.length}")

        val base64Content = Base64.encode(imageBytes)
        println("DocScanner-OCR: base64 encoded, length=${base64Content.length}")

        val request = ProcessRequest(
            rawDocument = RawDocument(
                content = base64Content,
                mimeType = mimeType
            )
        )

        println("DocScanner-OCR: sending POST request...")
        val response = try {
            httpClient.post(config.endpoint) {
                parameter("key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } catch (e: Exception) {
            println("DocScanner-OCR: HTTP request FAILED with exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            throw e
        }

        println("DocScanner-OCR: HTTP response status=${response.status}")
        val processResponse: ProcessResponse = try {
            response.body()
        } catch (e: Exception) {
            println("DocScanner-OCR: Response deserialization FAILED: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            throw e
        }

        println("DocScanner-OCR: Parsed response, document=${processResponse.document != null}")
        processResponse.document?.let { doc ->
            println("DocScanner-OCR: text length=${doc.text?.length}, entities=${doc.entities?.size}")
            doc.entities?.forEach { entity ->
                println("DocScanner-OCR: entity type=${entity.type}, mentionText=${entity.mentionText}, confidence=${entity.confidence}")
            }
        }

        return processResponse
    }
}
