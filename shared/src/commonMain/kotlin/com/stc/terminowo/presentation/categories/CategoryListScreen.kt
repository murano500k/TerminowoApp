package com.stc.terminowo.presentation.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stc.terminowo.presentation.auth.AuthViewModel
import com.stc.terminowo.presentation.components.AccountIconButton
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.all_documents
import terminowo.shared.generated.resources.app_title
import terminowo.shared.generated.resources.documents_count
import terminowo.shared.generated.resources.empty_state_hint
import terminowo.shared.generated.resources.no_documents_yet
import terminowo.shared.generated.resources.scan_document

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    authViewModel: AuthViewModel,
    onScanClick: () -> Unit,
    onCategoryClick: (String?) -> Unit,
    viewModel: CategoryListViewModel = koinViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    AccountIconButton(
                        authState = authState,
                        onSignIn = authViewModel::login,
                        onSignOut = authViewModel::logout
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.scan_document)
                )
            }
        }
    ) { paddingValues ->
        if (uiState.allDocumentsCount == 0 && !uiState.isLoading) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else if (!uiState.isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "all") {
                    CategoryCard(
                        label = stringResource(Res.string.all_documents),
                        count = uiState.allDocumentsCount,
                        onClick = { onCategoryClick(null) }
                    )
                }
                items(
                    items = uiState.categories,
                    key = { it.category.key }
                ) { item ->
                    CategoryCard(
                        label = stringResource(item.category.labelRes),
                        count = item.count,
                        onClick = { onCategoryClick(item.category.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(Res.string.documents_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.no_documents_yet),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.empty_state_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
