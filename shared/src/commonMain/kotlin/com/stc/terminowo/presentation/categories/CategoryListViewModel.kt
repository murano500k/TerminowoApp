package com.stc.terminowo.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.domain.model.Document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allDocuments = documentRepository.getAllDocuments()

    val uiState: StateFlow<CategoryListUiState> = allDocuments
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

    val searchResults: StateFlow<List<Document>> = combine(allDocuments, _searchQuery) { docs, query ->
        if (query.isBlank()) emptyList()
        else docs.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
