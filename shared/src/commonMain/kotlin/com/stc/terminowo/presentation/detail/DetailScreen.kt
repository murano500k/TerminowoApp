package com.stc.terminowo.presentation.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stc.terminowo.presentation.components.ReminderChips
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    isNew: Boolean,
    documentId: String?,
    newDocName: String?,
    newDocExpiryDate: String?,
    newDocConfidence: Float?,
    newDocImagePath: String?,
    newDocThumbnailPath: String?,
    newDocRawResponse: String?,
    newDocId: String?,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (isNew && newDocId != null) {
            viewModel.initNewDocument(
                name = newDocName,
                expiryDate = newDocExpiryDate?.let {
                    try { LocalDate.parse(it) } catch (_: Exception) { null }
                },
                confidence = newDocConfidence,
                imagePath = newDocImagePath ?: "",
                thumbnailPath = newDocThumbnailPath ?: "",
                rawOcrResponse = newDocRawResponse,
                documentId = newDocId
            )
        } else if (!isNew && documentId != null) {
            viewModel.loadExistingDocument(documentId)
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved()
    }

    LaunchedEffect(uiState.deletedSuccessfully) {
        if (uiState.deletedSuccessfully) onDeleted()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isNew) "New Document" else "Document Details")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(
                            onClick = { viewModel.delete() },
                            enabled = !uiState.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Document name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Document Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expiry date display
            OutlinedTextField(
                value = uiState.expiryDate?.toString() ?: "",
                onValueChange = { input ->
                    val date = try { LocalDate.parse(input) } catch (_: Exception) { null }
                    viewModel.updateExpiryDate(date)
                },
                label = { Text("Expiry Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    uiState.confidence?.let {
                        Text("OCR confidence: ${(it * 100).toInt()}%")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reminder configuration
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReminderChips(
                selectedDays = uiState.selectedReminderDays,
                onToggle = { viewModel.toggleReminder(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving && uiState.name.isNotBlank()
            ) {
                Text(if (isNew) "Save Document" else "Update Document")
            }

            if (!isNew) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.delete() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Document")
                }
            }
        }
    }
}
