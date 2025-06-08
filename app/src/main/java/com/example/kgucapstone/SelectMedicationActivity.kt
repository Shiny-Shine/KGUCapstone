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
import com.example.kgucapstone.model.TimeSlot

class SelectMedicationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var adapter: SelectMedicationAdapter
    private lateinit var medications: MutableList<Medication>
    private var timeSlot: TimeSlot = TimeSlot.MORNING
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_medication)

        // 초기화
        timeSlot = TimeSlot.fromOrdinal(intent.getIntExtra("TIME_SLOT", 0))
        userId = intent.getStringExtra("USER_ID") ?: "current_user_id"

        Log.d("SelectMedicationActivity", "시작: 사용자 ID = $userId, 시간대 = $timeSlot")

        // UI 요소 초기화
        recyclerView = findViewById(R.id.rv_medications)
        emptyTextView = findViewById(R.id.tv_empty_medications)

        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
        medications = mutableListOf()
        adapter = SelectMedicationAdapter(medications) { medication ->
            // 약 선택 시 처리
            selectMedication(medication)
        }
        recyclerView.adapter = adapter

        // 새 약 추가 버튼
        findViewById<Button>(R.id.btn_add_new_medication).setOnClickListener {
            val intent = Intent(this, AddMedicationActivity::class.java)
            intent.putExtra("TIME_SLOT", timeSlot.ordinal)
            intent.putExtra("USER_ID", userId)
            startActivityForResult(intent, ADD_MEDICATION_REQUEST_CODE)
        }

        // 취소 버튼
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // 약 목록 로드
        loadAvailableMedications()
    }

    private fun loadAvailableMedications() {
        medications.clear()

        // 샘플 데이터가 없으면 추가 (최초 실행 시)
        if (MedicationManager.getAllMedications().isEmpty()) {
            MedicationManager.addSampleData()
            Log.d("SelectMedicationActivity", "샘플 데이터 추가됨")
        }

        // 1. 공통 약들 가져오기 (common_medicines 사용자 ID를 가진)
        val commonMeds = MedicationManager.getMedicationsForUser("common_medicines")

        // 2. 사용자의 약들 가져오기
        val userMeds = MedicationManager.getMedicationsForUser(userId)

        // 3. 시간대에 맞는 약이 아니면 필터링
        val allAvailableMeds = (commonMeds + userMeds).filter { medication ->
            // 이미 현재 시간대에 할당된 약은 필터링 (중복 방지)
            !MedicationManager.getMedicationsForTimeSlot(timeSlot, userId)
                .any { it.id == medication.id }
        }

        medications.addAll(allAvailableMeds)
        Log.d("SelectMedicationActivity", "사용 가능한 약 수: ${medications.size}")

        // UI 업데이트
        if (medications.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        adapter.notifyDataSetChanged()
    }

    private fun selectMedication(medication: Medication) {
        // 선택한 약을 현재 사용자의 해당 시간대에 복제하여 추가
        val userMedication = medication.copy(
            id = medication.id + "_" + userId + "_" + timeSlot.name,
            timeSlots = listOf(timeSlot),
            userId = userId
        )

        // 약 추가
        MedicationManager.addMedication(userMedication)
        Log.d("SelectMedicationActivity", "약 선택됨: ${medication.name}, 사용자 ID: $userId")

        // 결과 설정 및 종료
        setResult(RESULT_OK)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_MEDICATION_REQUEST_CODE && resultCode == RESULT_OK) {
            // 새 약이 추가되면 목록 업데이트
            loadAvailableMedications()
            setResult(RESULT_OK) // 상위 액티비티에도 변경 알림
        }
    }

    companion object {
        private const val ADD_MEDICATION_REQUEST_CODE = 1001
    }
}