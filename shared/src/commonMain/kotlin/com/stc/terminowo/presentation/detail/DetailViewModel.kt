package com.stc.terminowo.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.model.ReminderInterval
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.domain.usecase.DeleteDocumentUseCase
import com.stc.terminowo.domain.usecase.SaveDocumentUseCase
import com.stc.terminowo.domain.usecase.ScheduleRemindersUseCase
import com.stc.terminowo.domain.usecase.UpdateDocumentUseCase
import com.stc.terminowo.platform.NotificationPermissionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.failed_to_delete
import terminowo.shared.generated.resources.failed_to_save

data class DetailUiState(
    val isNewDocument: Boolean = true,
    val documentId: String = "",
    val name: String = "",
    val expiryDate: LocalDate? = null,
    val confidence: Float? = null,
    val imagePath: String = "",
    val thumbnailPath: String = "",
    val selectedReminderDays: Set<Int> = setOf(14, 7, 1, 0),
    val category: DocumentCategory = DocumentCategory.OTHER,
    val reminderTime: LocalTime = LocalTime(9, 0),
    val rawOcrResponse: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val deletedSuccessfully: Boolean = false,
    val error: String? = null
)

class DetailViewModel(
    private val documentRepository: DocumentRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val updateDocumentUseCase: UpdateDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val scheduleRemindersUseCase: ScheduleRemindersUseCase,
    private val notificationPermissionHandler: NotificationPermissionHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadExistingDocument(documentId: String) {
        viewModelScope.launch {
            val document = documentRepository.getDocumentById(documentId) ?: return@launch
            _uiState.update {
                it.copy(
                    isNewDocument = false,
                    documentId = document.id,
                    name = document.name,
                    expiryDate = document.expiryDate,
                    confidence = document.confidence,
                    imagePath = document.imagePath,
                    thumbnailPath = document.thumbnailPath,
                    selectedReminderDays = document.reminderDays.toSet(),
                    category = document.category,
                    reminderTime = document.reminderTime
                )
            }
        }
    }

    fun initNewDocument(
        name: String?,
        defaultName: String,
        expiryDate: LocalDate?,
        confidence: Float?,
        imagePath: String,
        thumbnailPath: String,
        rawOcrResponse: String?,
        documentId: String,
        category: String? = null
    ) {
        _uiState.update {
            it.copy(
                isNewDocument = true,
                documentId = documentId,
                name = name ?: defaultName,
                expiryDate = expiryDate,
                confidence = confidence,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath,
                rawOcrResponse = rawOcrResponse,
                category = DocumentCategory.fromKey(category)
            )
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateExpiryDate(date: LocalDate?) {
        _uiState.update { it.copy(expiryDate = date) }
    }

    fun updateCategory(category: DocumentCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    fun toggleReminder(days: Int) {
        _uiState.update { state ->
            val current = state.selectedReminderDays.toMutableSet()
            if (current.contains(days)) current.remove(days) else current.add(days)
            state.copy(selectedReminderDays = current)
        }
    }

    fun save() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, error = null) }

        if (state.selectedReminderDays.isNotEmpty() && !notificationPermissionHandler.hasPermission()) {
            notificationPermissionHandler.requestPermission { _ ->
                performSave(state)
            }
        } else {
            performSave(state)
        }
    }

    private fun performSave(state: DetailUiState) {
        viewModelScope.launch {
            try {
                val now = DateTimeClock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val document = Document(
                    id = state.documentId,
                    name = state.name,
                    imagePath = state.imagePath,
                    thumbnailPath = state.thumbnailPath,
                    expiryDate = state.expiryDate,
                    confidence = state.confidence,
                    reminderDays = state.selectedReminderDays.toList().sorted(),
                    category = state.category,
                    reminderTime = state.reminderTime,
                    createdAt = now
                )

                if (state.isNewDocument) {
                    saveDocumentUseCase(document)
                } else {
                    updateDocumentUseCase(document)
                }

                scheduleRemindersUseCase(document)

                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                val fallback = getString(Res.string.failed_to_save)
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: fallback)
                }
            }
        }
    }

    fun delete() {
        val state = _uiState.value
        if (state.isNewDocument) return

        _uiState.update { it.copy(isDeleting = true, error = null) }

        viewModelScope.launch {
            try {
                deleteDocumentUseCase(state.documentId)
                _uiState.update { it.copy(isDeleting = false, deletedSuccessfully = true) }
            } catch (e: Exception) {
                val fallback = getString(Res.string.failed_to_delete)
                _uiState.update {
                    it.copy(isDeleting = false, error = e.message ?: fallback)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
