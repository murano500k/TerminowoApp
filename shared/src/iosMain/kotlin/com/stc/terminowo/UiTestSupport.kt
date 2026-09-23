package com.stc.terminowo

import com.stc.terminowo.domain.model.Document
import com.stc.terminowo.domain.repository.AppSettingsRepository
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.domain.usecase.CancelRemindersUseCase
import com.stc.terminowo.domain.usecase.ScheduleRemindersUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.koin.mp.KoinPlatform
import kotlinx.datetime.Clock as DateTimeClock

/**
 * Deterministic app state for the iOS UI tests (iosAppUITests). Called from
 * AppDelegate only in DEBUG builds launched with `-uiTestSeedReminders`, after Koin starts.
 *
 * Seeds two documents with scheduled reminders:
 * - [DOC_A]: expires in 30 days, standard reminders plus a custom one 23 days before.
 * - [DOC_B]: expires in 40 days, reminders 7 and 25 (custom) days before; a control
 *   that must survive actions on DOC_A.
 */
object UiTestSupport {
    const val DOC_A = "uitest-a"
    const val DOC_B = "uitest-b"

    fun resetAndSeed() = runBlocking {
        val koin = KoinPlatform.getKoin()
        val documents = koin.get<DocumentRepository>()
        val cancelReminders = koin.get<CancelRemindersUseCase>()
        val scheduleReminders = koin.get<ScheduleRemindersUseCase>()

        documents.getAllDocuments().first().forEach { cancelReminders(it.id, it.reminderDays) }
        documents.deleteAllDocuments()
        koin.get<AppSettingsRepository>().setTermsAccepted(true)

        val tz = TimeZone.currentSystemDefault()
        val today = DateTimeClock.System.todayIn(tz)
        val now = DateTimeClock.System.now().toLocalDateTime(tz)
        listOf(
            seedDocument(DOC_A, "UITest A", today.plus(30, DateTimeUnit.DAY), listOf(0, 1, 7, 14, 23), now),
            seedDocument(DOC_B, "UITest B", today.plus(40, DateTimeUnit.DAY), listOf(7, 25), now)
        ).forEach {
            documents.insertDocument(it)
            scheduleReminders(it)
        }
    }

    private fun seedDocument(
        id: String,
        name: String,
        expiryDate: kotlinx.datetime.LocalDate,
        reminderDays: List<Int>,
        now: kotlinx.datetime.LocalDateTime
    ) = Document(
        id = id,
        name = name,
        imagePath = "",
        thumbnailPath = "",
        expiryDate = expiryDate,
        confidence = null,
        reminderDays = reminderDays,
        createdAt = now
    )
}
