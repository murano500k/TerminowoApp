package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.platform.NotificationScheduler
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.todayIn

class ScheduleRemindersUseCase(
    private val notificationScheduler: NotificationScheduler
) {
    operator fun invoke(document: Document) {
        // Cancel existing reminders for this document
        notificationScheduler.cancelReminders(document.id)

        val expiryDate = document.expiryDate ?: return
        val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
        val reminderTime = LocalTime(9, 0) // 9:00 AM

        document.reminderDays.forEach { daysBefore ->
            val reminderDate = expiryDate.minus(daysBefore, DateTimeUnit.DAY)
            // Only schedule future reminders
            if (reminderDate >= today) {
                notificationScheduler.scheduleReminder(
                    documentId = document.id,
                    documentName = document.name,
                    expiryDate = expiryDate,
                    reminderDate = reminderDate.atTime(reminderTime),
                    daysBefore = daysBefore
                )
            }
        }
    }
}
