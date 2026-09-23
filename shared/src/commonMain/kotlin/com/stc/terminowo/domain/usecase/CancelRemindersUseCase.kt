package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.repository.NotificationRepository
import com.stc.terminowo.platform.NotificationScheduler

/**
 * Cancels every OS reminder scheduled for a document and clears its in-app
 * notification records.
 *
 * Custom-date reminders can use any number of days, so the days actually scheduled
 * are read back from the notification records (written by [ScheduleRemindersUseCase])
 * and combined with [knownReminderDays], e.g. the document's current reminder days.
 */
class CancelRemindersUseCase(
    private val notificationScheduler: NotificationScheduler,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(documentId: String, knownReminderDays: Collection<Int> = emptyList()) {
        val scheduledDays = notificationRepository.getScheduledDaysBefore(documentId)
        notificationScheduler.cancelReminders(documentId, knownReminderDays + scheduledDays)
        notificationRepository.deleteByDocumentId(documentId)
    }
}
