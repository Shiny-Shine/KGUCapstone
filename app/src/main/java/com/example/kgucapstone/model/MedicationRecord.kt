package com.example.kgucapstone.model

import java.util.Date

data class MedicationRecord(
    val id: String = "",
    val medicationId: String = "",
    val userId: String = "",
    val takenAt: Date = Date(),
    val timeSlot: TimeSlot = TimeSlot.MORNING,
    val wasTaken: Boolean = false
) {
    // Firestore용 Map 변환 함수
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "medicationId" to medicationId,
            "userId" to userId,
            "takenAt" to takenAt,
            "timeSlot" to timeSlot.ordinal,
            "wasTaken" to wasTaken
        )
    }

    companion object {
        // Firestore 문서에서 MedicationRecord 객체 생성
        fun fromMap(map: Map<String, Any?>): MedicationRecord {
            return MedicationRecord(
                id = map["id"] as? String ?: "",
                medicationId = map["medicationId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                takenAt = (map["takenAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                timeSlot = TimeSlot.fromOrdinal((map["timeSlot"] as? Long)?.toInt() ?: 0),
                wasTaken = map["wasTaken"] as? Boolean ?: false
            )
        }
    }
}