package com.stc.terminowo.domain.model

import org.jetbrains.compose.resources.StringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.reminder_14_days
import terminowo.shared.generated.resources.reminder_1_day
import terminowo.shared.generated.resources.reminder_7_days
import terminowo.shared.generated.resources.reminder_day_of

enum class ReminderInterval(val days: Int, val labelRes: StringResource) {
    FOURTEEN_DAYS(14, Res.string.reminder_14_days),
    SEVEN_DAYS(7, Res.string.reminder_7_days),
    ONE_DAY(1, Res.string.reminder_1_day),
    DAY_OF(0, Res.string.reminder_day_of);

    companion object {
        val DEFAULT = listOf(FOURTEEN_DAYS, SEVEN_DAYS, ONE_DAY, DAY_OF)

        fun fromDays(days: Int): ReminderInterval? = entries.find { it.days == days }
    }
}
