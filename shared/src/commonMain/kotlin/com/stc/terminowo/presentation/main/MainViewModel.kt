package com.stc.terminowo.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.usecase.DeleteDocumentUseCase
import com.stc.terminowo.domain.usecase.GetDocumentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true,
    val documentToDelete: Document? = null
)

class MainViewModel(
    getDocuments: GetDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val _localState = MutableStateFlow<Document?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        getDocuments(),
        _localState
    ) { docs, docToDelete ->
        MainUiState(
            documents = docs,
            isLoading = false,
            documentToDelete = docToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun requestDelete(document: Document) {
        _localState.value = document
    }

    fun cancelDelete() {
        _localState.value = null
    }

    fun confirmDelete() {
        val document = _localState.value ?: return
        _localState.value = null
        viewModelScope.launch {
            try {
                deleteDocumentUseCase(document.id)
            } catch (_: Exception) {
                // Document list updates reactively via Flow
            }
        }
    }
}
