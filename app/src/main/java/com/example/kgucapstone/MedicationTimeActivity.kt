package com.example.kgucapstone

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.MedicationRecord
import com.example.kgucapstone.model.TimeSlot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MedicationTimeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var timeSlotTitleTextView: TextView
    private lateinit var currentDateTextView: TextView
    private lateinit var adapter: MedicationAdapter
    private lateinit var timeSlot: TimeSlot
    private val medications = mutableListOf<Medication>()
    private val medicationRecords = mutableListOf<MedicationRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_time)

        // 현재 날짜 설정
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)

        // 시간대 정보 가져오기
        val timeSlotOrdinal = intent.getIntExtra("TIME_SLOT", 0)
        timeSlot = TimeSlot.fromOrdinal(timeSlotOrdinal)

        // UI 초기화
        recyclerView = findViewById(R.id.rv_medications)
        emptyTextView = findViewById(R.id.tv_empty_medications)
        timeSlotTitleTextView = findViewById(R.id.tv_time_slot_title)
        currentDateTextView = findViewById(R.id.tv_current_date)

        // 시간대에 따른 제목 설정
        val timeSlotTitle = when(timeSlot) {
            TimeSlot.MORNING -> "아침 복용 약"
            TimeSlot.LUNCH -> "점심 복용 약"
            TimeSlot.EVENING -> "저녁 복용 약"
            TimeSlot.BEDTIME -> "취침 전 복용 약"
        }
        timeSlotTitleTextView.text = timeSlotTitle
        currentDateTextView.text = formattedDate

        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MedicationAdapter(medications) { medication, isChecked ->
            updateMedicationRecord(medication, isChecked)
        }
        recyclerView.adapter = adapter

        // 약 추가 버튼 클릭 리스너
        findViewById<Button>(R.id.btn_add_medication).setOnClickListener {
            val intent = Intent(this, AddMedicationActivity::class.java)
            intent.putExtra("TIME_SLOT", timeSlot.ordinal)
            startActivity(intent)
        }

        // 뒤로 가기 버튼 클릭 리스너
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // 데이터 로드
        loadMedicationsForTimeSlot()
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때 데이터 다시 로드
        loadMedicationsForTimeSlot()
    }

    private fun loadMedicationsForTimeSlot() {
        // 여기서는 임시 데이터를 사용합니다.
        // 실제 앱에서는 Firebase Firestore나 Room 데이터베이스에서 데이터를 가져와야 합니다.
        medications.clear()

        // 임시 데이터 생성 (샘플)
        val sampleMedications = listOf(
            Medication(
                id = "1",
                name = "타이레놀",
                description = "두통 및 근육통 완화",
                dosage = "1회 1정",
                timeSlots = listOf(timeSlot)
            ),
            Medication(
                id = "2",
                name = "비타민C",
                description = "면역력 강화 및 피로 회복",
                dosage = "1회 1정",
                timeSlots = listOf(timeSlot)
            )
        )

        // 현재 시간대에 맞는 약만 필터링
        medications.addAll(sampleMedications.filter { it.timeSlots.contains(timeSlot) })

        // 기록 초기화
        medicationRecords.clear()
        for (medication in medications) {
            medicationRecords.add(
                MedicationRecord(
                    id = UUID.randomUUID().toString(),
                    medicationId = medication.id,
                    timeSlot = timeSlot,
                    date = Date()
                )
            )
        }

        // 빈 목록 처리
        if (medications.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
        }

        // 어댑터에 데이터 변경 알림
        adapter.notifyDataSetChanged()
    }

    private fun updateMedicationRecord(medication: Medication, taken: Boolean) {
        // 해당 약의 복용 기록 찾기
        val record = medicationRecords.find { it.medicationId == medication.id }
        record?.let {
            // 복용 여부 및 시간 업데이트
            val updatedRecord = it.copy(
                taken = taken,
                takenTime = if (taken) Date() else null
            )

            // 목록에서 기존 기록 제거 및 새 기록 추가
            medicationRecords.remove(it)
            medicationRecords.add(updatedRecord)

            // 여기서 실제로 데이터베이스에 저장해야 합니다.
            // 예: Firebase Firestore나 Room 데이터베이스에 저장
        }
    }
}