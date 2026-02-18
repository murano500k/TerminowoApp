package com.stc.terminowo.domain.model

import org.jetbrains.compose.resources.StringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.reminder_1_day
import terminowo.shared.generated.resources.reminder_30_days
import terminowo.shared.generated.resources.reminder_7_days
import terminowo.shared.generated.resources.reminder_90_days
import terminowo.shared.generated.resources.reminder_day_of

enum class ReminderInterval(val days: Int, val labelRes: StringResource) {
    NINETY_DAYS(90, Res.string.reminder_90_days),
    THIRTY_DAYS(30, Res.string.reminder_30_days),
    SEVEN_DAYS(7, Res.string.reminder_7_days),
    ONE_DAY(1, Res.string.reminder_1_day),
    DAY_OF(0, Res.string.reminder_day_of);

    companion object {
        val DEFAULT = listOf(NINETY_DAYS, THIRTY_DAYS, SEVEN_DAYS, ONE_DAY)

        fun fromDays(days: Int): ReminderInterval? = entries.find { it.days == days }
    }
}
