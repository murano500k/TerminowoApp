package com.stc.terminowo.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CategoryItem(
    val category: DocumentCategory,
    val count: Int
)

data class CategoryListUiState(
    val allDocumentsCount: Int = 0,
    val categories: List<CategoryItem> = emptyList(),
    val isLoading: Boolean = true
)

class CategoryListViewModel(
    documentRepository: DocumentRepository
) : ViewModel() {

    val uiState: StateFlow<CategoryListUiState> = documentRepository.getAllDocuments()
        .map { docs ->
            val categories = docs
                .groupBy { it.category }
                .map { (category, documents) -> CategoryItem(category, documents.size) }
                .sortedBy { it.category.ordinal }
            CategoryListUiState(
                allDocumentsCount = docs.size,
                categories = categories,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoryListUiState()
        )
}
