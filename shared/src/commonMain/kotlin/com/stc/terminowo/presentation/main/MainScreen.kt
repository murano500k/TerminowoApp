package com.stc.terminowo.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stc.terminowo.platform.isIos
import com.stc.terminowo.presentation.components.AppTopBar
import com.stc.terminowo.presentation.components.ConfirmationDialog
import com.stc.terminowo.presentation.components.DocumentListItem
import com.stc.terminowo.presentation.components.FilterEmptyState
import com.stc.terminowo.presentation.components.SearchOverlay
import com.stc.terminowo.presentation.components.SettingsMenu
import com.stc.terminowo.presentation.components.SwipeToRevealDeleteItem
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.delete_all_confirm_message
import terminowo.shared.generated.resources.delete_all_confirm_title
import terminowo.shared.generated.resources.delete_all_documents
import terminowo.shared.generated.resources.delete_document_confirm_message
import terminowo.shared.generated.resources.delete_document_confirm_title
import terminowo.shared.generated.resources.delete_files_confirm_message
import terminowo.shared.generated.resources.delete_files_confirm_title
import terminowo.shared.generated.resources.delete_files_only
import terminowo.shared.generated.resources.nav_documents
import terminowo.shared.generated.resources.scan_document

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onScanClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    initialFilter: DocumentStatusFilter? = null,
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    viewModel: DocumentsViewModel = koinViewModel()
) {
    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            viewModel.selectFilter(initialFilter)
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    val accentRed = LocalExtendedColors.current.accentRed

    if (uiState.documentToDelete != null) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete_document_confirm_title),
            text = stringResource(Res.string.delete_document_confirm_message, uiState.documentToDelete!!.name),
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    if (uiState.showDeleteAllConfirmation) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete_all_confirm_title),
            text = stringResource(Res.string.delete_all_confirm_message),
            onConfirm = { viewModel.confirmDeleteAll() },
            onDismiss = { viewModel.cancelDeleteAll() }
        )
    }

    if (uiState.showDeleteFilesConfirmation) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete_files_confirm_title),
            text = stringResource(Res.string.delete_files_confirm_message),
            onConfirm = { viewModel.confirmDeleteFiles() },
            onDismiss = { viewModel.cancelDeleteFiles() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = if (isSearchActive) Modifier.blur(12.dp) else Modifier,
        topBar = {
            Column {
                AppTopBar(
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onSearchActiveChange = { isSearchActive = it },
                    onNotificationsClick = onNotificationsClick,
                    unreadNotificationCount = unreadNotificationCount,
                    showSearchIcon = true,
                    trailingActions = {
                        SettingsMenu(
                            extraItems = { onDismiss ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(Res.string.delete_files_only),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        viewModel.requestDeleteFiles()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(Res.string.delete_all_documents),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        viewModel.requestDeleteAll()
                                    }
                                )
                            }
                        )
                    }
                )

                if (!isSearchActive) {
                    Text(
                        text = stringResource(Res.string.nav_documents),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    if (uiState.allCount > 0) {
                    val tabs = DocumentStatusFilter.entries
                    val selectedIndex = tabs.indexOf(uiState.selectedFilter)

                    TabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = Color.Transparent,
                        indicator = { tabPositions ->
                            if (selectedIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                    color = accentRed
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, filter ->
                            val count = when (filter) {
                                DocumentStatusFilter.ALL -> uiState.allCount
                                DocumentStatusFilter.ACTIVE -> uiState.activeCount
                                DocumentStatusFilter.URGENT -> uiState.urgentCount
                                DocumentStatusFilter.EXPIRED -> uiState.expiredCount
                            }
                            val isSelected = selectedIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectFilter(filter) },
                                selectedContentColor = accentRed,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = {
                                    Text(
                                        text = "${stringResource(filter.labelRes)} ($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                    } // end if allCount > 0
                }
            }
        },
        floatingActionButton = {}
    ) { paddingValues ->
        if (uiState.documents.isEmpty() && !uiState.isLoading && !isSearchActive) {
            FilterEmptyState(
                filter = uiState.selectedFilter,
                hasAnyDocuments = uiState.allCount > 0,
                onAddClick = onScanClick,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = uiState.documents, key = { it.id }) { document ->
                    SwipeToRevealDeleteItem(
                        onDeleteClick = { viewModel.requestDelete(document) },
                        modifier = Modifier.animateItem()
                    ) {
                        DocumentListItem(
                            document = document,
                            onClick = { onDocumentClick(document.id) }
                        )
                    }
                }
            }
        }
    }

    // Search overlay on top of everything
    if (isSearchActive) {
        SearchOverlay(
            query = searchQuery,
            results = searchResults,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onClose = {
                isSearchActive = false
                viewModel.onSearchQueryChange("")
            },
            onDocumentClick = { id ->
                isSearchActive = false
                viewModel.onSearchQueryChange("")
                onDocumentClick(id)
            }
        )
    }
    } // close outer Box
}
