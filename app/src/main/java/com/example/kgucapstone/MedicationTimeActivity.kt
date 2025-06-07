package com.example.kgucapstone

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.MedicationRecord
import com.example.kgucapstone.model.TimeSlot
import com.example.kgucapstone.viewmodel.MedicationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MedicationTimeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var timeSlotTitleTextView: TextView
    private lateinit var currentDateTextView: TextView
    private lateinit var adapter: MedicationAdapter
    private lateinit var timeSlot: TimeSlot
    private lateinit var viewModel: MedicationViewModel
    private val medicationRecords = mutableListOf<MedicationRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_time)

        // ViewModel 초기화
        viewModel = ViewModelProvider(this)[MedicationViewModel::class.java]

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
        progressBar = findViewById(R.id.progress_bar) // 추가해야 함

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
        adapter = MedicationAdapter(emptyList()) { medication, isChecked ->
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

        // ViewModel 관찰 설정
        observeViewModel()

        // 데이터 로드
        viewModel.loadMedicationsForTimeSlot(timeSlot)
    }

    private fun observeViewModel() {
        viewModel.medications.observe(this) { medications ->
            // 빈 목록 처리
            if (medications.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE

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
            }

            // 어댑터 업데이트
            adapter = MedicationAdapter(medications) { medication, isChecked ->
                updateMedicationRecord(medication, isChecked)
            }
            recyclerView.adapter = adapter
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때 데이터 다시 로드
        viewModel.loadMedicationsForTimeSlot(timeSlot)
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

            // 복용 상태 변경 시 토스트 메시지 표시
            if (taken) {
                Toast.makeText(this, "${medication.name} 복용 완료!", Toast.LENGTH_SHORT).show()
            }

            // 여기서 실제로 Firestore에 복용 기록 저장 코드 추가 필요
        }
    }
}