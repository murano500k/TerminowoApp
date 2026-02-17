package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.repository.DocumentRepository

class DeleteDocumentUseCase(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(id: String) {
        documentRepository.deleteDocument(id)
    }
}
