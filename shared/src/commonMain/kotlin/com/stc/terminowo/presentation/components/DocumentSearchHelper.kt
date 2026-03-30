package com.stc.terminowo.presentation.components

import com.stc.terminowo.domain.model.Document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DocumentSearchHelper(
    allDocuments: Flow<List<Document>>,
    scope: CoroutineScope
) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Document>> = combine(allDocuments, _searchQuery) { docs, query ->
        if (query.isBlank()) emptyList()
        else docs.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.myComments.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
