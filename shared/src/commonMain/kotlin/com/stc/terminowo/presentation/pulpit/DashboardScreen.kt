package com.stc.terminowo.presentation.pulpit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stc.terminowo.platform.isIos
import androidx.compose.material3.DropdownMenuItem
import com.stc.terminowo.presentation.components.AppTopBar
import com.stc.terminowo.presentation.components.ConfirmationDialog
import com.stc.terminowo.presentation.components.SettingsMenu
import com.stc.terminowo.presentation.components.DocumentListItem
import com.stc.terminowo.presentation.main.DocumentStatusFilter
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.add_document_button
import terminowo.shared.generated.resources.all_documents_ok
import terminowo.shared.generated.resources.all_documents_ok_subtitle
import terminowo.shared.generated.resources.document_score
import terminowo.shared.generated.resources.documents_count_label
import terminowo.shared.generated.resources.delete_all_confirm_message
import terminowo.shared.generated.resources.delete_all_confirm_title
import terminowo.shared.generated.resources.delete_all_documents
import terminowo.shared.generated.resources.delete_files_confirm_message
import terminowo.shared.generated.resources.delete_files_confirm_title
import terminowo.shared.generated.resources.delete_files_only
import terminowo.shared.generated.resources.no_documents_pulpit_subtitle
import terminowo.shared.generated.resources.no_documents_pulpit_title
import terminowo.shared.generated.resources.no_results
import terminowo.shared.generated.resources.pulpit_title
import terminowo.shared.generated.resources.scan_document
import terminowo.shared.generated.resources.status_expired
import terminowo.shared.generated.resources.status_over_30_days
import terminowo.shared.generated.resources.status_within_30_days
import terminowo.shared.generated.resources.upcoming_deadlines

@Composable
fun DashboardScreen(
    onDocumentClick: (String) -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToDocumentsWithFilter: (DocumentStatusFilter) -> Unit,
    onAddDocumentClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0
) {
    val viewModel: DashboardViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val showDeleteAllConfirmation by viewModel.showDeleteAllConfirmation.collectAsState()
    val showDeleteFilesConfirmation by viewModel.showDeleteFilesConfirmation.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    if (showDeleteAllConfirmation) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete_all_confirm_title),
            text = stringResource(Res.string.delete_all_confirm_message),
            onConfirm = { viewModel.confirmDeleteAll() },
            onDismiss = { viewModel.cancelDeleteAll() }
        )
    }

    if (showDeleteFilesConfirmation) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete_files_confirm_title),
            text = stringResource(Res.string.delete_files_confirm_message),
            onConfirm = { viewModel.confirmDeleteFiles() },
            onDismiss = { viewModel.cancelDeleteFiles() }
        )
    }

    Scaffold(
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
                        text = stringResource(Res.string.pulpit_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        },
        floatingActionButton = {}
    ) { paddingValues ->
        if (isSearchActive) {
            if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it.id }
                    ) { document ->
                        DocumentListItem(
                            document = document,
                            onClick = { onDocumentClick(document.id) }
                        )
                    }
                }
            }
        } else {
            DashboardContent(
                uiState = uiState,
                onDocumentClick = onDocumentClick,
                onNavigateToDocuments = onNavigateToDocuments,
                onNavigateToDocumentsWithFilter = onNavigateToDocumentsWithFilter,
                onAddDocumentClick = onAddDocumentClick,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onDocumentClick: (String) -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToDocumentsWithFilter: (DocumentStatusFilter) -> Unit,
    onAddDocumentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.isLoading && uiState.totalCount == 0) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(onAddClick = onAddDocumentClick)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> { /* show nothing while loading */ }

                else -> {
                    item {
                        ScoreCard(
                            scorePercent = uiState.scorePercent,
                            totalCount = uiState.totalCount
                        )
                    }

                    item {
                        StatusBoxesRow(
                            expiredCount = uiState.expiredCount,
                            urgentCount = uiState.urgentCount,
                            activeCount = uiState.activeCount,
                            onExpiredClick = { onNavigateToDocumentsWithFilter(DocumentStatusFilter.EXPIRED) },
                            onUrgentClick = { onNavigateToDocumentsWithFilter(DocumentStatusFilter.URGENT) },
                            onActiveClick = { onNavigateToDocumentsWithFilter(DocumentStatusFilter.ACTIVE) }
                        )
                    }

                    if (uiState.upcomingDocuments.isNotEmpty()) {
                        item {
                            UpcomingDeadlinesHeader(onClick = onNavigateToDocuments)
                        }

                        items(
                            items = uiState.upcomingDocuments,
                            key = { it.id }
                        ) { document ->
                            DocumentListItem(
                                document = document,
                                onClick = { onDocumentClick(document.id) }
                            )
                        }
                    } else {
                        item {
                            AllDocumentsOkState()
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScoreCard(
    scorePercent: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularScoreIndicator(
                scorePercent = scorePercent,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(Res.string.document_score),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = pluralStringResource(Res.plurals.documents_count_label, totalCount, totalCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CircularScoreIndicator(
    scorePercent: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = scorePercent / 100f,
        animationSpec = tween(durationMillis = 800)
    )

    val scoreColor = when {
        scorePercent < 40 -> Color(0xFFE53935)
        scorePercent < 70 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val arcSize = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - arcSize) / 2f,
                (size.height - arcSize) / 2f
            )

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = scoreColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$scorePercent%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scoreColor
        )
    }
}

@Composable
private fun StatusBoxesRow(
    expiredCount: Int,
    urgentCount: Int,
    activeCount: Int,
    onExpiredClick: () -> Unit,
    onUrgentClick: () -> Unit,
    onActiveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusBox(
            label = stringResource(Res.string.status_expired),
            count = expiredCount,
            iconColor = Color(0xFFE53935),
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) },
            highlighted = expiredCount > 0,
            onClick = onExpiredClick,
            modifier = Modifier.weight(1f)
        )
        StatusBox(
            label = stringResource(Res.string.status_within_30_days),
            count = urgentCount,
            iconColor = Color(0xFFFF9800),
            icon = {
                Text(
                    text = "!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF9800)
                )
            },
            highlighted = false,
            onClick = onUrgentClick,
            modifier = Modifier.weight(1f)
        )
        StatusBox(
            label = stringResource(Res.string.status_over_30_days),
            count = activeCount,
            iconColor = Color(0xFF4CAF50),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) },
            highlighted = false,
            onClick = onActiveClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusBox(
    label: String,
    count: Int,
    iconColor: Color,
    icon: @Composable () -> Unit,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (highlighted) iconColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    val borderColor = if (highlighted) iconColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .then(
                            if (highlighted) Modifier.background(iconColor, RoundedCornerShape(4.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 28.dp)
            )
        }
    }
}

@Composable
private fun UpcomingDeadlinesHeader(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.upcoming_deadlines),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.no_documents_pulpit_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.no_documents_pulpit_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalExtendedColors.current.accentRed,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(text = stringResource(Res.string.add_document_button))
        }
    }
}

@Composable
private fun AllDocumentsOkState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.all_documents_ok),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.all_documents_ok_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
