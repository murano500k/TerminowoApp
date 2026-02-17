package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.repository.DocumentRepository

class UpdateDocumentUseCase(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(document: Document) {
        documentRepository.updateDocument(document)
    }
}
