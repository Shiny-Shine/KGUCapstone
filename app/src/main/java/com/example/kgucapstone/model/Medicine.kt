package com.example.kgucapstone.model

import java.util.Date

enum class TimeSlot {
    MORNING, LUNCH, EVENING, BEDTIME;

    companion object {
        fun fromOrdinal(ordinal: Int): TimeSlot {
            return values()[ordinal]
        }
    }
}

data class Medication(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val dosage: String = "",
    val timesPerDay: Int = 1,
    val timeSlots: List<TimeSlot> = listOf(),
    val startDate: Date = Date(),
    val endDate: Date? = null,
    val userId: String = ""
)

data class MedicationRecord(
    val id: String = "",
    val medicationId: String = "",
    val userId: String = "",
    val timeSlot: TimeSlot = TimeSlot.MORNING,
    val date: Date = Date(),
    val taken: Boolean = false,
    val takenTime: Date? = null
)