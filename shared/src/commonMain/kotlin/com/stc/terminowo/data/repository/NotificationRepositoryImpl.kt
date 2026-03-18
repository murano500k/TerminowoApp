package com.stc.terminowo.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.stc.terminowo.data.local.db.DocumentDatabase
import com.stc.terminowo.domain.model.AppNotification
import com.stc.terminowo.domain.model.DocumentCategory
import com.stc.terminowo.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NotificationRepositoryImpl(
    private val database: DocumentDatabase
) : NotificationRepository {

    private val queries get() = database.documentQueries

    private fun nowIso(): String =
        DateTimeClock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()

    override fun getDeliveredNotifications(): Flow<List<AppNotification>> {
        return queries.getDeliveredNotifications(nowIso())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getUnreadCount(): Flow<Long> {
        return queries.getUnreadCount(nowIso())
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    override suspend fun insertNotification(notification: AppNotification) {
        withContext(Dispatchers.Default) {
            queries.insertNotification(
                id = notification.id,
                documentId = notification.documentId,
                documentName = notification.documentName,
                category = notification.category.key,
                expiryDate = notification.expiryDate?.toString(),
                daysBefore = notification.daysBefore.toLong(),
                scheduledAt = notification.scheduledAt.toString(),
                isRead = if (notification.isRead) 1L else 0L
            )
        }
    }

    override suspend fun markAllAsRead() {
        withContext(Dispatchers.Default) {
            queries.markAllAsRead(nowIso())
        }
    }

    override suspend fun deleteByDocumentId(documentId: String) {
        withContext(Dispatchers.Default) {
            queries.deleteNotificationsByDocumentId(documentId)
        }
    }
}

private fun com.stc.terminowo.data.local.db.NotificationEntity.toDomain(): AppNotification {
    return AppNotification(
        id = id,
        documentId = documentId,
        documentName = documentName,
        category = DocumentCategory.fromKey(category),
        expiryDate = expiryDate?.let { LocalDate.parse(it) },
        daysBefore = daysBefore.toInt(),
        scheduledAt = LocalDateTime.parse(scheduledAt),
        isRead = isRead != 0L
    )
}
