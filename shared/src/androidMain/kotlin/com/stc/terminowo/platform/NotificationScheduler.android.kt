package com.stc.terminowo.platform

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

actual class NotificationScheduler(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "document_reminders"
        const val CHANNEL_NAME = "Document Reminders"
        const val EXTRA_DOC_ID = "document_id"
        const val EXTRA_DOC_NAME = "document_name"
        const val EXTRA_DAYS_BEFORE = "days_before"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for expiring documents"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    actual fun scheduleReminder(
        documentId: String,
        documentName: String,
        expiryDate: LocalDate,
        reminderDate: LocalDateTime,
        daysBefore: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId = "${documentId}_$daysBefore".hashCode()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_DOC_ID, documentId)
            putExtra(EXTRA_DOC_NAME, documentName)
            putExtra(EXTRA_DAYS_BEFORE, daysBefore)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMillis = reminderDate
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Exact alarm permission not granted, fall back to inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    actual fun cancelReminders(documentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Cancel all possible reminder intervals for this document
        listOf(0, 1, 7, 30, 90).forEach { daysBefore ->
            val notificationId = "${documentId}_$daysBefore".hashCode()
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    actual fun cancelAllReminders() {
        // Note: Android doesn't have a built-in way to cancel all alarms
        // This would need tracking all scheduled alarm IDs in a database
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val documentName = intent.getStringExtra(NotificationScheduler.EXTRA_DOC_NAME) ?: "Document"
        val daysBefore = intent.getIntExtra(NotificationScheduler.EXTRA_DAYS_BEFORE, 0)
        val notificationId = intent.getIntExtra(NotificationScheduler.EXTRA_NOTIFICATION_ID, 0)

        val title = "Document Expiring Soon"
        val body = when (daysBefore) {
            0 -> "$documentName expires today!"
            1 -> "$documentName expires tomorrow!"
            else -> "$documentName expires in $daysBefore days"
        }

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
