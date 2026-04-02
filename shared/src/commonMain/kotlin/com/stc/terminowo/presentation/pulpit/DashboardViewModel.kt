package com.stc.terminowo.presentation.pulpit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.model.DocumentStatus
import com.stc.terminowo.domain.model.status
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.platform.NotificationScheduler
import com.stc.terminowo.presentation.components.DocumentSearchHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.Clock as DateTimeClock

data class DashboardUiState(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val urgentCount: Int = 0,
    val expiredCount: Int = 0,
    val scorePercent: Int = 0,
    val upcomingDocuments: List<Document> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val documentRepository: DocumentRepository,
    private val imageStorage: ImageStorage,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val allDocuments = documentRepository.getAllDocuments()
    private val searchHelper = DocumentSearchHelper(allDocuments, viewModelScope)
    val searchQuery: StateFlow<String> = searchHelper.searchQuery
    val searchResults: StateFlow<List<Document>> = searchHelper.searchResults

    fun onSearchQueryChange(query: String) {
        searchHelper.onSearchQueryChange(query)
    }

    private val _showDeleteAllConfirmation = MutableStateFlow(false)
    val showDeleteAllConfirmation: StateFlow<Boolean> = _showDeleteAllConfirmation.asStateFlow()

    private val _showDeleteFilesConfirmation = MutableStateFlow(false)
    val showDeleteFilesConfirmation: StateFlow<Boolean> = _showDeleteFilesConfirmation.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = allDocuments
        .map { docs ->
            val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())

            val grouped = docs.groupBy { it.status(today) }
            val activeCount = grouped[DocumentStatus.ACTIVE]?.size ?: 0
            val urgentCount = grouped[DocumentStatus.URGENT]?.size ?: 0
            val expiredCount = grouped[DocumentStatus.EXPIRED]?.size ?: 0

            val upcoming = (grouped[DocumentStatus.URGENT].orEmpty() + grouped[DocumentStatus.EXPIRED].orEmpty())
                .sortedBy { it.expiryDate }

            val scorePercent = if (docs.isNotEmpty()) {
                (activeCount * 100) / docs.size
            } else 0

            DashboardUiState(
                totalCount = docs.size,
                activeCount = activeCount,
                urgentCount = urgentCount,
                expiredCount = expiredCount,
                scorePercent = scorePercent,
                upcomingDocuments = upcoming,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )

    fun requestDeleteAll() { _showDeleteAllConfirmation.value = true }
    fun cancelDeleteAll() { _showDeleteAllConfirmation.value = false }
    fun confirmDeleteAll() {
        _showDeleteAllConfirmation.value = false
        viewModelScope.launch {
            val docs = allDocuments.first()
            for (doc in docs) {
                try { notificationScheduler.cancelReminders(doc.id) } catch (_: Exception) {}
                try { imageStorage.deleteImage(doc.imagePath) } catch (_: Exception) {}
                try { imageStorage.deleteImage(doc.thumbnailPath) } catch (_: Exception) {}
            }
            documentRepository.deleteAllDocuments()
        }
    }

    fun requestDeleteFiles() { _showDeleteFilesConfirmation.value = true }
    fun cancelDeleteFiles() { _showDeleteFilesConfirmation.value = false }
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
