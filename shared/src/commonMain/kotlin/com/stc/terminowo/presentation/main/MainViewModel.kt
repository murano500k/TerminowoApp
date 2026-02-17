package com.stc.terminowo.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.usecase.GetDocumentsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MainUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true
)

class MainViewModel(
    getDocuments: GetDocumentsUseCase
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = getDocuments()
        .map { docs ->
            MainUiState(
                documents = docs,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState()
        )
}
