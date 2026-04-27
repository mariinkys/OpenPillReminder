package dev.mariinkys.openPillReminder.model

import java.time.LocalDate

data class PillLog(
    val date: LocalDate,
    val taken: Boolean = false,
    val note: String = ""
)