package com.stc.terminowo.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.usecase.DeleteDocumentUseCase
import com.stc.terminowo.domain.usecase.GetDocumentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DocumentListUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true,
    val documentToDelete: Document? = null,
    val categoryKey: String? = null
)

class DocumentListViewModel(
    getDocuments: GetDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val _categoryKey = MutableStateFlow<String?>(null)
    private val _documentToDelete = MutableStateFlow<Document?>(null)

    val uiState: StateFlow<DocumentListUiState> = combine(
        getDocuments(),
        _categoryKey,
        _documentToDelete
    ) { docs, catKey, docToDelete ->
        val filtered = if (catKey != null) {
            val category = DocumentCategory.fromKey(catKey)
            docs.filter { it.category == category }
        } else {
            docs
        }
        DocumentListUiState(
            documents = filtered,
            isLoading = false,
            documentToDelete = docToDelete,
            categoryKey = catKey
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DocumentListUiState()
    )

    fun init(categoryKey: String?) {
        _categoryKey.value = categoryKey
    }

    fun requestDelete(document: Document) {
        _documentToDelete.value = document
    }

    fun cancelDelete() {
        _documentToDelete.value = null
    }

    fun confirmDelete() {
        val document = _documentToDelete.value ?: return
        _documentToDelete.value = null
        viewModelScope.launch {
            try {
                deleteDocumentUseCase(document.id)
            } catch (_: Exception) {
                // Document list updates reactively via Flow
            }
        }
    }
}
