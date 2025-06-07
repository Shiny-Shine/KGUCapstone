package com.example.kgucapstone.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.TimeSlot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MedicationViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val medicationsCollection = db.collection("medications")
    private val auth = FirebaseAuth.getInstance()

    private val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> = _medications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadMedicationsForTimeSlot(timeSlot: TimeSlot) {
        _isLoading.value = true
        _error.value = null

        val userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            _error.value = "사용자 정보를 찾을 수 없습니다."
            _isLoading.value = false
            return
        }

        medicationsCollection
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val medicationList = documents.mapNotNull { doc ->
                    val medicationMap = doc.data
                    try {
                        Medication.fromMap(medicationMap)
                    } catch (e: Exception) {
                        null
                    }
                }.filter { medication ->
                    // 특정 시간대에 해당하는 약만 필터링
                    medication.timeSlots.contains(timeSlot)
                }

                _medications.value = medicationList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "약 정보를 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
    }

    fun addMedication(medication: Medication, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true

        val medicationWithUserId = if (medication.userId.isEmpty()) {
            medication.copy(userId = auth.currentUser?.uid ?: "")
        } else {
            medication
        }

        medicationsCollection.document(medicationWithUserId.id)
            .set(medicationWithUserId.toMap())
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onFailure("약 등록에 실패했습니다: ${e.message}")
            }
    }

    fun deleteMedication(medicationId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true

        medicationsCollection.document(medicationId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onFailure("약 삭제에 실패했습니다: ${e.message}")
            }
    }
}