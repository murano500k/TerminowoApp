package com.stc.terminowo.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.platform.ImageStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlinx.datetime.Clock as DateTimeClock

data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val allCount: Int = 0,
    val activeCount: Int = 0,
    val urgentCount: Int = 0,
    val expiredCount: Int = 0,
    val selectedFilter: DocumentStatusFilter = DocumentStatusFilter.ALL,
    val isLoading: Boolean = true,
    val documentToDelete: Document? = null,
    val showDeleteAllConfirmation: Boolean = false,
    val showDeleteFilesConfirmation: Boolean = false
)

class DocumentsViewModel(
    private val documentRepository: DocumentRepository,
    private val imageStorage: ImageStorage
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DocumentStatusFilter.ALL)
    private val _documentToDelete = MutableStateFlow<Document?>(null)
    private val _showDeleteAllConfirmation = MutableStateFlow(false)
    private val _showDeleteFilesConfirmation = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allDocuments = documentRepository.getAllDocuments()

    val searchResults: StateFlow<List<Document>> = combine(allDocuments, _searchQuery) { docs, query ->
        if (query.isBlank()) emptyList()
        else docs.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<DocumentsUiState> = combine(
        allDocuments,
        _selectedFilter,
        _documentToDelete,
        _showDeleteAllConfirmation,
        _showDeleteFilesConfirmation
    ) { docs, filter, docToDelete, showDeleteAll, showDeleteFiles ->
        val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())

        val active = mutableListOf<Document>()
        val urgent = mutableListOf<Document>()
        val expired = mutableListOf<Document>()

        for (doc in docs) {
            val days = doc.expiryDate?.let { today.daysUntil(it) }
            when {
                days == null || days > 30 -> active.add(doc)
                days in 0..30 -> urgent.add(doc)
                else -> expired.add(doc)
            }
        }

        val filtered = when (filter) {
            DocumentStatusFilter.ALL -> docs
            DocumentStatusFilter.ACTIVE -> active
            DocumentStatusFilter.URGENT -> urgent
            DocumentStatusFilter.EXPIRED -> expired
        }

        DocumentsUiState(
            documents = filtered,
            allCount = docs.size,
            activeCount = active.size,
            urgentCount = urgent.size,
            expiredCount = expired.size,
            selectedFilter = filter,
            isLoading = false,
            documentToDelete = docToDelete,
            showDeleteAllConfirmation = showDeleteAll,
            showDeleteFilesConfirmation = showDeleteFiles
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DocumentsUiState()
    )

    fun selectFilter(filter: DocumentStatusFilter) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
                documentRepository.deleteDocument(document.id)
            } catch (_: Exception) {}
        }
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
