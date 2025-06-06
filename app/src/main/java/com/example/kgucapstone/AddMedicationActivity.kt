package com.example.kgucapstone

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.TimeSlot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddMedicationActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var dosageEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var morningCheckBox: CheckBox
    private lateinit var lunchCheckBox: CheckBox
    private lateinit var eveningCheckBox: CheckBox
    private lateinit var bedtimeCheckBox: CheckBox
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button

    private var startDate: Date = Date()
    private var endDate: Date? = null
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medication)

        // UI 초기화
        nameEditText = findViewById(R.id.et_medication_name)
        dosageEditText = findViewById(R.id.et_medication_dosage)
        descriptionEditText = findViewById(R.id.et_medication_description)
        morningCheckBox = findViewById(R.id.cb_morning)
        lunchCheckBox = findViewById(R.id.cb_lunch)
        eveningCheckBox = findViewById(R.id.cb_evening)
        bedtimeCheckBox = findViewById(R.id.cb_bedtime)
        startDateButton = findViewById(R.id.btn_start_date)
        endDateButton = findViewById(R.id.btn_end_date)

        // 특정 시간대가 선택되었다면 해당 체크박스 선택
        val selectedTimeSlot = intent.getIntExtra("TIME_SLOT", -1)
        if (selectedTimeSlot != -1) {
            when (TimeSlot.fromOrdinal(selectedTimeSlot)) {
                TimeSlot.MORNING -> morningCheckBox.isChecked = true
                TimeSlot.LUNCH -> lunchCheckBox.isChecked = true
                TimeSlot.EVENING -> eveningCheckBox.isChecked = true
                TimeSlot.BEDTIME -> bedtimeCheckBox.isChecked = true
            }
        }

        // 시작일 버튼 설정
        startDateButton.text = dateFormat.format(startDate)
        startDateButton.setOnClickListener {
            showDatePicker(true)
        }

        // 종료일 버튼 설정
        endDateButton.text = "종료일 선택 (선택사항)"
        endDateButton.setOnClickListener {
            showDatePicker(false)
        }

        // 저장 버튼 클릭 리스너
        findViewById<Button>(R.id.btn_save_medication).setOnClickListener {
            saveMedication()
        }

        // 취소 버튼 클릭 리스너
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) startDate else (endDate ?: Date())
        calendar.time = currentDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val selectedDate = calendar.time

                if (isStartDate) {
                    startDate = selectedDate
                    startDateButton.text = dateFormat.format(startDate)

                    // 종료일이 시작일보다 빠르면 종료일도 변경
                    if (endDate != null && endDate!! < startDate) {
                        endDate = null
                        endDateButton.text = "종료일 선택 (선택사항)"
                    }
                } else {
                    // 종료일이 시작일보다 빠르면 경고
                    if (selectedDate < startDate) {
                        Toast.makeText(this, "종료일은 시작일 이후여야 합니다", Toast.LENGTH_SHORT).show()
                    } else {
                        endDate = selectedDate
                        endDateButton.text = dateFormat.format(endDate!!)
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveMedication() {
        val name = nameEditText.text.toString().trim()
        val dosage = dosageEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        // 유효성 검사
        if (name.isEmpty()) {
            Toast.makeText(this, "약 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        if (dosage.isEmpty()) {
            Toast.makeText(this, "복용량을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 선택된 시간대 확인
        val timeSlots = mutableListOf<TimeSlot>()
        if (morningCheckBox.isChecked) timeSlots.add(TimeSlot.MORNING)
        if (lunchCheckBox.isChecked) timeSlots.add(TimeSlot.LUNCH)
        if (eveningCheckBox.isChecked) timeSlots.add(TimeSlot.EVENING)
        if (bedtimeCheckBox.isChecked) timeSlots.add(TimeSlot.BEDTIME)

        if (timeSlots.isEmpty()) {
            Toast.makeText(this, "적어도 하나의 복용 시간을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 약 객체 생성
        val medication = Medication(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            dosage = dosage,
            timesPerDay = timeSlots.size,
            timeSlots = timeSlots,
            startDate = startDate,
            endDate = endDate,
            userId = "current_user_id" // 실제 앱에서는 로그인한 사용자 ID 사용
        )

        // 여기서 실제로 데이터베이스에 저장해야 합니다.
        // 예: Firebase Firestore나 Room 데이터베이스에 저장

        Toast.makeText(this, "약이 추가되었습니다", Toast.LENGTH_SHORT).show()
        finish()
    }
}