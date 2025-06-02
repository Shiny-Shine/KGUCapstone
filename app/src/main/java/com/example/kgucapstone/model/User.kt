package com.example.kgucapstone.model
import com.google.firebase.Timestamp

data class User(
    val name: String = "",
    val password: String = "",
    val phone: String = "",
    val medicalHistoryList: List<MedicalHistory> = emptyList(),
    val sickedSideEffect: List<SideEffect> = emptyList(),
    val nowMedicineCount: Int = 0,
)

data class MedicalHistory(
    val diseaseName: String = "",
    val sickedDate: Timestamp = Timestamp.now(), // 기본값 : 오늘 날짜
)

data class SideEffect(
    val medicineName: String = "",
    val sideEffectList: List<String> = emptyList(),
)