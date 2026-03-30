package com.stc.terminowo.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.platform.decodeImageBitmap
import com.stc.terminowo.platform.renderPdfPage
import com.stc.terminowo.presentation.components.FullScreenImageDialog
import com.stc.terminowo.presentation.components.ReminderChips
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.Clock as DateTimeClock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.add_document
import terminowo.shared.generated.resources.back
import terminowo.shared.generated.resources.category
import terminowo.shared.generated.resources.close
import terminowo.shared.generated.resources.confirm
import terminowo.shared.generated.resources.cancel
import terminowo.shared.generated.resources.default_document_name
import terminowo.shared.generated.resources.delete_document
import terminowo.shared.generated.resources.document_details
import terminowo.shared.generated.resources.document_image
import terminowo.shared.generated.resources.document_name
import terminowo.shared.generated.resources.expired_days_ago
import terminowo.shared.generated.resources.expiry_date_format
import terminowo.shared.generated.resources.expiry_date_required
import terminowo.shared.generated.resources.my_comments
import terminowo.shared.generated.resources.new_document
import terminowo.shared.generated.resources.select_expiry_date
import terminowo.shared.generated.resources.notification_time
import terminowo.shared.generated.resources.ocr_confidence
import terminowo.shared.generated.resources.reminder_custom_date
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
    onSaved: (documentName: String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultDocName = stringResource(Res.string.default_document_name)
    val imageStorage: ImageStorage = koinInject()
    val accentRed = LocalExtendedColors.current.accentRed
    val focusManager = LocalFocusManager.current

    var thumbnailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showFullImage by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.imagePath) {
        thumbnailBitmap = if (uiState.imagePath.isNotEmpty()) {
            val bytes = imageStorage.readImage(uiState.imagePath) ?: return@LaunchedEffect
            if (uiState.imagePath.endsWith(".pdf", ignoreCase = true)) {
                renderPdfPage(bytes, 0)?.let { decodeImageBitmap(it) }
            } else {
                decodeImageBitmap(bytes)
            }
        } else null
    }

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
        if (uiState.savedSuccessfully) onSaved(uiState.name)
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

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    val accentColorScheme = MaterialTheme.colorScheme.copy(
        primary = accentRed,
        onPrimary = Color.White
    )

    MaterialTheme(colorScheme = accentColorScheme) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) stringResource(Res.string.add_document)
                        else stringResource(Res.string.document_details)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = if (isNew) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isNew) stringResource(Res.string.close) else stringResource(Res.string.back)
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focusManager.clearFocus() }
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Show thumbnail only for existing documents (edit mode)
            if (!isNew && thumbnailBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFullImage = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Image(
                        bitmap = thumbnailBitmap!!,
                        contentDescription = stringResource(Res.string.document_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (!isNew && uiState.imagePath.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

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
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

            // My comments
            OutlinedTextField(
                value = uiState.myComments,
                onValueChange = { viewModel.updateMyComments(it) },
                label = { Text(stringResource(Res.string.my_comments)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expiry date picker
            Box {
                OutlinedTextField(
                    value = uiState.expiryDate?.let {
                        "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}/${it.year}"
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.expiry_date_format)) },
                    placeholder = { Text("dd/mm/yyyy") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.expiryDate == null,
                    supportingText = {
                        if (uiState.expiryDate == null) {
                            Text(
                                stringResource(Res.string.expiry_date_required),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            uiState.confidence?.let {
                                Text(stringResource(Res.string.ocr_confidence, (it * 100).toInt()))
                            }
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.expiryDate
                        ?.atStartOfDayIn(TimeZone.UTC)
                        ?.toEpochMilliseconds()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC).date
                                viewModel.updateExpiryDate(date)
                            }
                            showDatePicker = false
                        }) {
                            Text(stringResource(Res.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

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

            // Custom date reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Switch(
                    checked = uiState.customReminderEnabled,
                    onCheckedChange = { viewModel.toggleCustomReminder(it) },
                    enabled = uiState.expiryDate != null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentRed,
                        checkedBorderColor = accentRed
                    )
                )
                Text(
                    text = stringResource(Res.string.reminder_custom_date),
                    modifier = Modifier.weight(1f)
                        .then(Modifier.padding(start = 12.dp))
                )
            }

            if (uiState.customReminderEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = uiState.customReminderDate?.let {
                            "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}/${it.year}"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.reminder_custom_date)) },
                        placeholder = { Text("dd/mm/yyyy") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showCustomDatePicker = true }
                    )
                }

                if (showCustomDatePicker) {
                    val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
                    val todayMillis = today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                    val expiryMillis = uiState.expiryDate
                        ?.atStartOfDayIn(TimeZone.UTC)
                        ?.toEpochMilliseconds()
                    val customDatePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = uiState.customReminderDate
                            ?.atStartOfDayIn(TimeZone.UTC)
                            ?.toEpochMilliseconds(),
                        selectableDates = object : androidx.compose.material3.SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                return utcTimeMillis >= todayMillis &&
                                    (expiryMillis == null || utcTimeMillis <= expiryMillis)
                            }
                        }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showCustomDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                customDatePickerState.selectedDateMillis?.let { millis ->
                                    val date = Instant.fromEpochMilliseconds(millis)
                                        .toLocalDateTime(TimeZone.UTC).date
                                    viewModel.updateCustomReminderDate(date)
                                }
                                showCustomDatePicker = false
                            }) {
                                Text(stringResource(Res.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCustomDatePicker = false }) {
                                Text(stringResource(Res.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = customDatePickerState)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification time picker
            Box {
                OutlinedTextField(
                    value = "${uiState.reminderTime.hour.toString().padStart(2, '0')}:${uiState.reminderTime.minute.toString().padStart(2, '0')}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.notification_time)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showTimePicker = true }
                )
            }

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
                enabled = !uiState.isSaving && uiState.name.isNotBlank() && uiState.expiryDate != null,
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (isNew) stringResource(Res.string.save_document) else stringResource(Res.string.update_document))
            }

            if (!isNew) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.delete() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isDeleting,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.delete_document))
                }
            }
        }
    }

    // Fullscreen image viewer
    if (showFullImage && uiState.imagePath.isNotEmpty()) {
        FullScreenImageDialog(
            imagePath = uiState.imagePath,
            imageStorage = imageStorage,
            onDismiss = { showFullImage = false }
        )
    }
    }
}
