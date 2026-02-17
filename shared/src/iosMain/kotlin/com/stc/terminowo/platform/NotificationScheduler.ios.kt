package com.stc.terminowo.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

actual class NotificationScheduler {
    actual fun scheduleReminder(
        documentId: String,
        documentName: String,
        expiryDate: LocalDate,
        reminderDate: LocalDateTime,
        daysBefore: Int
    ) {
        // TODO: Implement iOS notification scheduling via UNUserNotificationCenter
    }

    actual fun cancelReminders(documentId: String) {
        // TODO: Implement iOS notification cancellation
    }

    actual fun cancelAllReminders() {
        // TODO: Implement iOS cancel all notifications
    }
}
