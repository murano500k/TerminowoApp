package com.stc.terminowo.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.stc.terminowo.data.local.db.DocumentDatabase
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class DocumentRepositoryImpl(
    private val database: DocumentDatabase
) : DocumentRepository {

    private val queries get() = database.documentQueries

    override fun getAllDocuments(): Flow<List<Document>> {
        return queries.getAllDocuments()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getDocumentById(id: String): Document? =
        withContext(Dispatchers.Default) {
            queries.getDocumentById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun insertDocument(document: Document) {
        withContext(Dispatchers.Default) {
            queries.insertDocument(
                id = document.id,
                name = document.name,
                imagePath = document.imagePath,
                thumbnailPath = document.thumbnailPath,
                expiryDate = document.expiryDate?.toString(),
                rawOcrResponse = null,
                confidence = document.confidence?.toDouble(),
                reminderDays = document.reminderDays.joinToString(","),
                createdAt = document.createdAt.toString()
            )
        }
    }

    override suspend fun updateDocument(document: Document) {
        withContext(Dispatchers.Default) {
            queries.updateDocument(
                name = document.name,
                expiryDate = document.expiryDate?.toString(),
                reminderDays = document.reminderDays.joinToString(","),
                id = document.id
            )
        }
    }

    override suspend fun deleteDocument(id: String) {
        withContext(Dispatchers.Default) {
            queries.deleteDocument(id)
        }
    }
}

private fun com.stc.terminowo.data.local.db.DocumentEntity.toDomain(): Document {
    return Document(
        id = id,
        name = name,
        imagePath = imagePath,
        thumbnailPath = thumbnailPath,
        expiryDate = expiryDate?.let { LocalDate.parse(it) },
        confidence = confidence?.toFloat(),
        reminderDays = reminderDays.split(",").mapNotNull { it.trim().toIntOrNull() },
        createdAt = LocalDateTime.parse(createdAt)
    )
}
