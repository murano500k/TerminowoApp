package com.stc.terminowo.presentation.pulpit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

    val uiState: StateFlow<DashboardUiState> = allDocuments
        .map { docs ->
            val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())

            var activeCount = 0
            var urgentCount = 0
            var expiredCount = 0
            val upcoming = mutableListOf<Document>()

            for (doc in docs) {
                val days = doc.expiryDate?.let { today.daysUntil(it) }
                when {
                    days == null || days > 30 -> activeCount++
                    days in 0..30 -> {
                        urgentCount++
                        upcoming.add(doc)
                    }
                    else -> {
                        expiredCount++
                        upcoming.add(doc)
                    }
                }
            }

            upcoming.sortBy { it.expiryDate }

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
