package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.platform.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.toLocalDateTime

/**
 * Re-schedules the OS notifications for all future reminders on app start.
 *
 * iOS bakes notification text at scheduling time, so reminders created by an older
 * build or before a device language change would otherwise stay in the old language.
 * Only future reminders are re-created; in-app notification records are left untouched.
 */
class ResyncRemindersUseCase(
    private val documentRepository: DocumentRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend operator fun invoke() {
        val tz = TimeZone.currentSystemDefault()
        val now = DateTimeClock.System.now().toLocalDateTime(tz)

        documentRepository.getAllDocuments().first().forEach { document ->
            val expiryDate = document.expiryDate ?: return@forEach
            notificationScheduler.cancelReminders(document.id, document.reminderDays)

            document.reminderDays.forEach { daysBefore ->
                val reminderDateTime = expiryDate
                    .minus(daysBefore, DateTimeUnit.DAY)
                    .atTime(document.reminderTime)
                if (reminderDateTime > now) {
                    notificationScheduler.scheduleReminder(
                        documentId = document.id,
                        documentName = document.name,
                        expiryDate = expiryDate,
                        reminderDate = reminderDateTime,
                        daysBefore = daysBefore
                    )
                }
            }
        }
    }
}
