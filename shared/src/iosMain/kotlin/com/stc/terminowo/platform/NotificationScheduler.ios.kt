package com.stc.terminowo.platform

import com.stc.terminowo.domain.model.ReminderInterval
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSDateComponents
import platform.Foundation.NSString
import platform.UserNotifications.localizedUserNotificationStringForKey
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
            setTitle(
                NSString.localizedUserNotificationStringForKey("notification_title", null)
            )
            setBody(
                when (daysBefore) {
                    0 -> NSString.localizedUserNotificationStringForKey(
                        "notification_expires_today", listOf(documentName)
                    )
                    1 -> NSString.localizedUserNotificationStringForKey(
                        "notification_expires_tomorrow", listOf(documentName)
                    )
                    else -> NSString.localizedUserNotificationStringForKey(
                        "notification_expires_in_days", listOf(documentName, daysBefore)
                    )
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
        val identifiers = ReminderInterval.entries.map { "${documentId}_${it.days}" }
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    actual fun cancelAllReminders() {
        center.removeAllPendingNotificationRequests()
    }
}
