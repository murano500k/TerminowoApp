package com.stc.terminowo.domain.repository

import com.stc.terminowo.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    suspend fun getDocumentById(id: String): Document?
    suspend fun insertDocument(document: Document)
    suspend fun updateDocument(document: Document)
    suspend fun deleteDocument(id: String)
}
