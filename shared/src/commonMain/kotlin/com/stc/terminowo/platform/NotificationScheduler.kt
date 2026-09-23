package com.stc.terminowo.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

expect class NotificationScheduler {
    fun scheduleReminder(
        documentId: String,
        documentName: String,
        expiryDate: LocalDate,
        reminderDate: LocalDateTime,
        daysBefore: Int
    )

    /** Cancels the standard intervals plus [reminderDays] (e.g. custom-date reminders). */
    fun cancelReminders(documentId: String, reminderDays: Collection<Int>)
    fun cancelAllReminders()
}
