package com.stc.terminowo.domain.usecase

import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.platform.NotificationScheduler
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

class ScheduleRemindersUseCase(
    private val notificationScheduler: NotificationScheduler
) {
    operator fun invoke(document: Document) {
        // Cancel existing reminders for this document
        notificationScheduler.cancelReminders(document.id)

        val expiryDate = document.expiryDate ?: return
        val tz = TimeZone.currentSystemDefault()
        val today = DateTimeClock.System.todayIn(tz)
        val nowTime = DateTimeClock.System.now().toLocalDateTime(tz).time
        val reminderTime = document.reminderTime

        document.reminderDays.forEach { daysBefore ->
            val reminderDate = expiryDate.minus(daysBefore, DateTimeUnit.DAY)

            val scheduledDateTime = when {
                reminderDate > today -> reminderDate.atTime(reminderTime)
                reminderDate == today && reminderTime > nowTime -> reminderDate.atTime(reminderTime)
                reminderDate == today -> {
                    // Time already passed today — schedule 5 seconds from now
                    val nowDateTime = DateTimeClock.System.now().toLocalDateTime(tz)
                    val futureSecond = nowDateTime.second + 5
                    val futureMinute = nowDateTime.minute + futureSecond / 60
                    val futureHour = nowDateTime.hour + futureMinute / 60
                    kotlinx.datetime.LocalDateTime(
                        today.year, today.monthNumber, today.dayOfMonth,
                        futureHour % 24, futureMinute % 60, futureSecond % 60
                    )
                }
                else -> return@forEach // Past date, skip
            }

            notificationScheduler.scheduleReminder(
                documentId = document.id,
                documentName = document.name,
                expiryDate = expiryDate,
                reminderDate = scheduledDateTime,
                daysBefore = daysBefore
            )
        }
    }
}
