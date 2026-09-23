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
                    // Passed as a string: localizedUserNotificationString only
                    // substitutes object (%@) placeholders, %d renders empty.
                    else -> NSString.localizedUserNotificationStringForKey(
                        expiresInDaysKey(daysBefore), listOf(documentName, daysBefore.toString())
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

// localizedUserNotificationString has no plural support and resolves the
// language at delivery time, so pick a key by Slavic plural form (ru/uk/pl);
// daysBefore >= 2 here, so "one" only covers 21, 31, ... Each language's
// Localizable.strings fills all three keys with its own wording.
private fun expiresInDaysKey(days: Int): String {
    val mod10 = days % 10
    val mod100 = days % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "notification_expires_in_days_one"
        mod10 in 2..4 && mod100 !in 12..14 -> "notification_expires_in_days_few"
        else -> "notification_expires_in_days"
    }
}
