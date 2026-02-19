package com.stc.terminowo.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual class NotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }

    actual fun scheduleReminder(
        documentId: String,
        documentName: String,
        expiryDate: LocalDate,
        reminderDate: LocalDateTime,
        daysBefore: Int
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Document Expiring Soon")
            setBody(
                when (daysBefore) {
                    0 -> "$documentName expires today!"
                    1 -> "$documentName expires tomorrow!"
                    else -> "$documentName expires in $daysBefore days"
                }
            )
            setSound(UNNotificationSound.defaultSound)
        }

        val dateComponents = NSDateComponents().apply {
            year = reminderDate.year.toLong()
            month = reminderDate.monthNumber.toLong()
            day = reminderDate.dayOfMonth.toLong()
            hour = reminderDate.hour.toLong()
            minute = reminderDate.minute.toLong()
            second = reminderDate.second.toLong()
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = false
        )

        val identifier = "${documentId}_$daysBefore"
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = trigger
        )

        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    actual fun cancelReminders(documentId: String) {
        val identifiers = listOf(0, 1, 7, 14).map { "${documentId}_$it" }
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    actual fun cancelAllReminders() {
        center.removeAllPendingNotificationRequests()
    }
}
