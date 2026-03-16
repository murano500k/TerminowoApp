package com.stc.terminowo.presentation.pulpit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.model.DocumentStatus
import com.stc.terminowo.domain.model.status
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.presentation.components.DocumentSearchHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    documentRepository: DocumentRepository
) : ViewModel() {

    private val allDocuments = documentRepository.getAllDocuments()
    private val searchHelper = DocumentSearchHelper(allDocuments, viewModelScope)
    val searchQuery: StateFlow<String> = searchHelper.searchQuery
    val searchResults: StateFlow<List<Document>> = searchHelper.searchResults

    fun onSearchQueryChange(query: String) {
        searchHelper.onSearchQueryChange(query)
    }

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
}
