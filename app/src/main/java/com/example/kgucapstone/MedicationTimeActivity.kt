package com.example.kgucapstone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.MedicationManager
import com.example.kgucapstone.model.MedicationRecord
import com.example.kgucapstone.model.TimeSlot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MedicationTimeActivity : AppCompatActivity() {

    private lateinit var timeSlotTitle: TextView
    private lateinit var medicationsRecyclerView: RecyclerView
    private lateinit var adapter: MedicationAdapter
    private lateinit var emptyView: TextView
    private lateinit var addMedicationButton: Button
    private lateinit var backButton: Button

    private var timeSlot: TimeSlot = TimeSlot.MORNING
    private var userId: String = "current_user_id"
    private var medications: MutableList<Medication> = mutableListOf()
    private var medicationRecords: MutableList<MedicationRecord> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_time)

        // UI 초기화
        timeSlotTitle = findViewById(R.id.tv_time_slot_title)
        medicationsRecyclerView = findViewById(R.id.rv_medications)
        emptyView = findViewById(R.id.tv_empty_medications)
        addMedicationButton = findViewById(R.id.btn_add_medication)
        backButton = findViewById(R.id.btn_back)

        // 인텐트에서 시간대와 사용자 ID 가져오기
        timeSlot = TimeSlot.fromOrdinal(intent.getIntExtra("TIME_SLOT", 0))
        userId = intent.getStringExtra("USER_ID") ?: "current_user_id"

        // 화면 제목 설정
        val titleText = when (timeSlot) {
            TimeSlot.MORNING -> "아침 약"
            TimeSlot.LUNCH -> "점심 약"
            TimeSlot.EVENING -> "저녁 약"
            TimeSlot.BEDTIME -> "취침 약"
        }
        timeSlotTitle.text = titleText

        // RecyclerView 설정
        medicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MedicationAdapter(
            medications = medications,
            medicationRecords = medicationRecords,
            onCheckedChangeListener = { medication, isChecked ->
                updateMedicationRecord(medication, isChecked)
            },
            onDeleteClickListener = { medication ->
                deleteMedication(medication)
            }
        )
        medicationsRecyclerView.adapter = adapter

        // 약 추가 버튼 클릭 리스너
        addMedicationButton.setOnClickListener {
            val intent = Intent(this, SelectMedicationActivity::class.java)
            intent.putExtra("TIME_SLOT", timeSlot.ordinal)
            intent.putExtra("USER_ID", userId)
            startActivityForResult(intent, ADD_MEDICATION_REQUEST_CODE)
        }

        // 뒤로 가기 버튼 클릭 리스너
        backButton.setOnClickListener {
            finish()
        }

        // 약 목록 로드
        loadMedicationsForTimeSlot()
    }

    private fun loadMedicationsForTimeSlot() {
        // 해당 시간대와 사용자에 맞는 약 목록 가져오기
        medications.clear()
        medications.addAll(MedicationManager.getMedicationsForTimeSlot(timeSlot, userId))

        // 약 복용 기록 가져오기
        loadMedicationRecords()

        // 빈 상태 표시
        if (medications.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            medicationsRecyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            medicationsRecyclerView.visibility = View.VISIBLE
        }

        adapter.notifyDataSetChanged()
    }

    private fun loadMedicationRecords() {
        // 오늘 날짜에 대한 약 복용 기록 가져오기
        val today = Date()
        medicationRecords.clear()

        // 기존 기록 가져오기
        val existingRecords = MedicationManager.getMedicationRecordsForDate(today, userId)
        medicationRecords.addAll(existingRecords)

        // 누락된 약에 대해 기록 생성
        for (medication in medications) {
            if (medicationRecords.none { it.medicationId == medication.id }) {
                // 해당 약에 대한 기록이 없으면 새로 생성
                val newRecord = MedicationRecord(
                    id = "record_" + UUID.randomUUID().toString(),
                    medicationId = medication.id,
                    timeSlot = timeSlot,
                    date = today,
                    taken = false,
                    takenTime = null,
                    userId = userId
                )
                medicationRecords.add(newRecord)
                MedicationManager.addOrUpdateMedicationRecord(newRecord)
            }
        }

        adapter.updateMedicationRecords(medicationRecords)
    }

    private fun updateMedicationRecord(medication: Medication, isTaken: Boolean) {
        // 해당 약에 대한 기록 찾기
        val recordToUpdate = medicationRecords.find { it.medicationId == medication.id }

        if (recordToUpdate != null) {
            // 기존 기록 업데이트
            val updatedRecord = recordToUpdate.copy(
                taken = isTaken,
                takenTime = if (isTaken) Date() else null
            )

            // 기록 업데이트
            MedicationManager.addOrUpdateMedicationRecord(updatedRecord)

            // 로컬 목록 업데이트
            val index = medicationRecords.indexOf(recordToUpdate)
            if (index >= 0) {
                medicationRecords[index] = updatedRecord
                adapter.updateMedicationRecords(medicationRecords)
            }
        } else {
            // 기록이 없으면 새로 생성
            val newRecord = MedicationRecord(
                id = "record_" + UUID.randomUUID().toString(),
                medicationId = medication.id,
                timeSlot = timeSlot,
                date = Date(),
                taken = isTaken,
                takenTime = if (isTaken) Date() else null,
                userId = userId
            )

            medicationRecords.add(newRecord)
            MedicationManager.addOrUpdateMedicationRecord(newRecord)
            adapter.updateMedicationRecords(medicationRecords)
        }

        // 디버그 로그
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Log.d("MedicationTimeActivity", "약 상태 업데이트: ${medication.name}, 복용: $isTaken, 시간: ${dateFormat.format(Date())}")
    }

    private fun deleteMedication(medication: Medication) {
        // 약 삭제
        MedicationManager.removeMedication(medication.id)

        // 해당 약의 복용 기록도 삭제
        medicationRecords.removeIf { it.medicationId == medication.id }

        // 목록에서 제거
        medications.remove(medication)

        // UI 업데이트
        if (medications.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            medicationsRecyclerView.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()

        Log.d("MedicationTimeActivity", "약 삭제됨: ${medication.name}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_MEDICATION_REQUEST_CODE && resultCode == RESULT_OK) {
            // 약이 추가되었다면 리스트 업데이트
            loadMedicationsForTimeSlot()
            Log.d("MedicationTimeActivity", "약 추가 후 목록 새로고침")
        }
    }

    companion object {
        private const val ADD_MEDICATION_REQUEST_CODE = 1001
    }
}