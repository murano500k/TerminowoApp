package com.stc.terminowo.domain.model

enum class ReminderInterval(val days: Int, val label: String) {
    NINETY_DAYS(90, "90 days before"),
    THIRTY_DAYS(30, "30 days before"),
    SEVEN_DAYS(7, "7 days before"),
    ONE_DAY(1, "1 day before"),
    DAY_OF(0, "Day of expiry");

    companion object {
        val DEFAULT = listOf(NINETY_DAYS, THIRTY_DAYS, SEVEN_DAYS, ONE_DAY)

        fun fromDays(days: Int): ReminderInterval? = entries.find { it.days == days }
    }
}
