package com.stc.terminowo.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.platform.ImageStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryItem(
    val category: DocumentCategory,
    val count: Int
)

data class CategoryListUiState(
    val allDocumentsCount: Int = 0,
    val categories: List<CategoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val showDeleteAllConfirmation: Boolean = false,
    val showDeleteFilesConfirmation: Boolean = false
)

class CategoryListViewModel(
    private val documentRepository: DocumentRepository,
    private val imageStorage: ImageStorage
) : ViewModel() {

    private val _showDeleteAllConfirmation = MutableStateFlow(false)
    private val _showDeleteFilesConfirmation = MutableStateFlow(false)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allDocuments = documentRepository.getAllDocuments()

    val uiState: StateFlow<CategoryListUiState> = combine(allDocuments, _showDeleteAllConfirmation, _showDeleteFilesConfirmation) { docs, showConfirm, showFilesConfirm ->
        val categories = docs
            .groupBy { it.category }
            .map { (category, documents) -> CategoryItem(category, documents.size) }
            .sortedBy { it.category.ordinal }
        CategoryListUiState(
            allDocumentsCount = docs.size,
            categories = categories,
            isLoading = false,
            showDeleteAllConfirmation = showConfirm,
            showDeleteFilesConfirmation = showFilesConfirm
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

    fun requestDeleteAll() {
        _showDeleteAllConfirmation.value = true
    }

    fun cancelDeleteAll() {
        _showDeleteAllConfirmation.value = false
    }

    fun confirmDeleteAll() {
        _showDeleteAllConfirmation.value = false
        viewModelScope.launch {
            val docs = allDocuments.first()
            for (doc in docs) {
                try { imageStorage.deleteImage(doc.imagePath) } catch (_: Exception) {}
                try { imageStorage.deleteImage(doc.thumbnailPath) } catch (_: Exception) {}
            }
            documentRepository.deleteAllDocuments()
        }
    }

    fun requestDeleteFiles() {
        _showDeleteFilesConfirmation.value = true
    }

    fun cancelDeleteFiles() {
        _showDeleteFilesConfirmation.value = false
    }

    fun confirmDeleteFiles() {
        _showDeleteFilesConfirmation.value = false
        viewModelScope.launch {
            val docs = allDocuments.first()
            for (doc in docs) {
                if (doc.imagePath.isNotEmpty()) {
                    try { imageStorage.deleteImage(doc.imagePath) } catch (_: Exception) {}
                }
                if (doc.thumbnailPath.isNotEmpty()) {
                    try { imageStorage.deleteImage(doc.thumbnailPath) } catch (_: Exception) {}
                }
            }
            documentRepository.clearAllFilePaths()
        }
    }
}
