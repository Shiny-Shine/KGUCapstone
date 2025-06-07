package com.example.kgucapstone.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

enum class TimeSlot {
    MORNING, LUNCH, EVENING, BEDTIME;

    companion object {
        fun fromOrdinal(ordinal: Int): TimeSlot {
            return values()[ordinal]
        }
    }
}

@Parcelize
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
) : Parcelable {
    // Firestore용 Map 변환 함수
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "dosage" to dosage,
            "timesPerDay" to timesPerDay,
            "timeSlots" to timeSlots.map { it.ordinal },
            "startDate" to startDate,
            "endDate" to endDate,
            "userId" to userId
        )
    }

    companion object {
        // Firestore 문서에서 Medication 객체 생성
        fun fromMap(map: Map<String, Any?>): Medication {
            return Medication(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                dosage = map["dosage"] as? String ?: "",
                timesPerDay = (map["timesPerDay"] as? Long)?.toInt() ?: 1,
                timeSlots = (map["timeSlots"] as? List<*>)?.mapNotNull {
                    (it as? Long)?.toInt()?.let { ordinal -> TimeSlot.fromOrdinal(ordinal) }
                } ?: listOf(),
                startDate = (map["startDate"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                endDate = (map["endDate"] as? com.google.firebase.Timestamp)?.toDate(),
                userId = map["userId"] as? String ?: ""
            )
        }
    }
}