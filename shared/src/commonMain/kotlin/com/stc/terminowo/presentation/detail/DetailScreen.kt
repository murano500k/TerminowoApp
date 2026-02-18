package com.stc.terminowo.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.presentation.components.ReminderChips
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlinx.datetime.Clock as DateTimeClock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.back
import terminowo.shared.generated.resources.category
import terminowo.shared.generated.resources.confirm
import terminowo.shared.generated.resources.cancel
import terminowo.shared.generated.resources.default_document_name
import terminowo.shared.generated.resources.delete
import terminowo.shared.generated.resources.delete_document
import terminowo.shared.generated.resources.document_details
import terminowo.shared.generated.resources.document_name
import terminowo.shared.generated.resources.expired_days_ago
import terminowo.shared.generated.resources.expiry_date_format
import terminowo.shared.generated.resources.new_document
import terminowo.shared.generated.resources.notification_time
import terminowo.shared.generated.resources.ocr_confidence
import terminowo.shared.generated.resources.reminders
import terminowo.shared.generated.resources.save_document
import terminowo.shared.generated.resources.update_document

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
    newDocCategory: String? = null,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultDocName = stringResource(Res.string.default_document_name)

    LaunchedEffect(Unit) {
        if (isNew && newDocId != null) {
            viewModel.initNewDocument(
                name = newDocName,
                defaultName = defaultDocName,
                expiryDate = newDocExpiryDate?.let {
                    try { LocalDate.parse(it) } catch (_: Exception) { null }
                },
                confidence = newDocConfidence,
                imagePath = newDocImagePath ?: "",
                thumbnailPath = newDocThumbnailPath ?: "",
                rawOcrResponse = newDocRawResponse,
                documentId = newDocId,
                category = newDocCategory
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

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isNew) stringResource(Res.string.new_document) else stringResource(Res.string.document_details))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
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
                                contentDescription = stringResource(Res.string.delete),
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
                label = { Text(stringResource(Res.string.document_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = stringResource(uiState.category.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    DocumentCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(stringResource(category.labelRes)) },
                            onClick = {
                                viewModel.updateCategory(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expiry date display
            OutlinedTextField(
                value = uiState.expiryDate?.toString() ?: "",
                onValueChange = { input ->
                    val date = try { LocalDate.parse(input) } catch (_: Exception) { null }
                    viewModel.updateExpiryDate(date)
                },
                label = { Text(stringResource(Res.string.expiry_date_format)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    uiState.confidence?.let {
                        Text(stringResource(Res.string.ocr_confidence, (it * 100).toInt()))
                    }
                }
            )

            // Expired date warning
            uiState.expiryDate?.let { expiryDate ->
                val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
                val daysOverdue = today.daysUntil(expiryDate)
                if (daysOverdue < 0) {
                    val daysAgo = -daysOverdue
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pluralStringResource(Res.plurals.expired_days_ago, daysAgo, daysAgo),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reminder configuration
            Text(
                text = stringResource(Res.string.reminders),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReminderChips(
                selectedDays = uiState.selectedReminderDays,
                onToggle = { viewModel.toggleReminder(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notification time picker
            Text(
                text = stringResource(Res.string.notification_time),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = "%02d:%02d".format(uiState.reminderTime.hour, uiState.reminderTime.minute),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true },
                enabled = false
            )

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = uiState.reminderTime.hour,
                    initialMinute = uiState.reminderTime.minute,
                    is24Hour = true
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateReminderTime(
                                LocalTime(timePickerState.hour, timePickerState.minute)
                            )
                            showTimePicker = false
                        }) {
                            Text(stringResource(Res.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving && uiState.name.isNotBlank()
            ) {
                Text(if (isNew) stringResource(Res.string.save_document) else stringResource(Res.string.update_document))
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
                    Text(stringResource(Res.string.delete_document))
                }
            }
        }
    }
}
