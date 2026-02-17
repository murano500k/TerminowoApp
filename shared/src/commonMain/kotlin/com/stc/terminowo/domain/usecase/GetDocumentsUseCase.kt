package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetDocumentsUseCase(
    private val documentRepository: DocumentRepository
) {
    operator fun invoke(): Flow<List<Document>> {
        return documentRepository.getAllDocuments()
    }
}
