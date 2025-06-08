package com.example.kgucapstone

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.MedicationManager
import com.example.kgucapstone.model.TimeSlot
import java.util.Date
import java.util.UUID

class AddMedicationActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var dosageEditText: EditText

    private lateinit var morningCheckBox: CheckBox
    private lateinit var lunchCheckBox: CheckBox
    private lateinit var eveningCheckBox: CheckBox
    private lateinit var bedtimeCheckBox: CheckBox

    private var selectedTimeSlot: TimeSlot? = null
    private lateinit var userId: String
    private var matchedMedicationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medication)

        // UI 요소 초기화
        nameEditText = findViewById(R.id.et_medication_name)
        descriptionEditText = findViewById(R.id.et_medication_description)
        dosageEditText = findViewById(R.id.et_medication_dosage)

        morningCheckBox = findViewById(R.id.cb_morning)
        lunchCheckBox = findViewById(R.id.cb_lunch)
        eveningCheckBox = findViewById(R.id.cb_evening)
        bedtimeCheckBox = findViewById(R.id.cb_bedtime)

        val saveButton = findViewById<Button>(R.id.btn_save_medication)
        val cancelButton = findViewById<Button>(R.id.btn_cancel)

        // 인텐트에서 데이터 가져오기
        selectedTimeSlot = TimeSlot.fromOrdinal(intent.getIntExtra("TIME_SLOT", 0))
        userId = intent.getStringExtra("USER_ID") ?: "current_user_id"

        // 분석된 약품 정보가 있으면 불러오기
        val medicineName = intent.getStringExtra("MEDICINE_NAME")
        val medicineDescription = intent.getStringExtra("MEDICINE_DESCRIPTION")

        // 매칭된 약품 정보가 있으면 불러오기
        matchedMedicationId = intent.getStringExtra("MATCHED_MEDICINE_ID")
        val matchedMedicineName = intent.getStringExtra("MATCHED_MEDICINE_NAME")
        val matchedMedicineDescription = intent.getStringExtra("MATCHED_MEDICINE_DESCRIPTION")
        val matchedMedicineDosage = intent.getStringExtra("MATCHED_MEDICINE_DOSAGE")

        Log.d("AddMedicationActivity", "매칭된 약품 ID: $matchedMedicationId")
        Log.d("AddMedicationActivity", "매칭된 약품 이름: $matchedMedicineName")

        // 매칭된 약품 정보가 있으면 우선적으로 사용
        if (!matchedMedicationId.isNullOrEmpty() && !matchedMedicineName.isNullOrEmpty()) {
            nameEditText.setText(matchedMedicineName)
            descriptionEditText.setText(matchedMedicineDescription)
            dosageEditText.setText(matchedMedicineDosage)
        }
        // 아니면 분석된 정보 사용
        else if (!medicineName.isNullOrEmpty()) {
            nameEditText.setText(medicineName)
            descriptionEditText.setText(medicineDescription)
        }

        // 미리 선택된 시간대 체크
        when (selectedTimeSlot) {
            TimeSlot.MORNING -> morningCheckBox.isChecked = true
            TimeSlot.LUNCH -> lunchCheckBox.isChecked = true
            TimeSlot.EVENING -> eveningCheckBox.isChecked = true
            TimeSlot.BEDTIME -> bedtimeCheckBox.isChecked = true
            null -> TODO()
        }

        // 저장 버튼 클릭 이벤트
        saveButton.setOnClickListener {
            saveMedication()
        }

        // 취소 버튼 클릭 이벤트
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // 샘플 약품을 가져오지 못한 경우 자동으로 데이터베이스 검색
        if (matchedMedicationId.isNullOrEmpty() && !medicineName.isNullOrEmpty()) {
            searchMedicationInDatabase(medicineName)
        }
    }

    private fun searchMedicationInDatabase(medicineName: String) {
        // 샘플 데이터에서 약 이름으로 검색
        val allMedications = MedicationManager.getMedicationsForUser("common_medicines")

        // 정확히 일치하는 약품 찾기
        val exactMatch = allMedications.find {
            it.name.equals(medicineName, ignoreCase = true)
        }

        // 부분 일치하는 약품 찾기
        val partialMatch = allMedications.find { medication ->
            medication.name.contains(medicineName, ignoreCase = true) ||
                    medicineName.contains(medication.name, ignoreCase = true)
        }

        val matchedMedication = exactMatch ?: partialMatch

        if (matchedMedication != null) {
            // 찾은 약품 정보로 화면 업데이트
            nameEditText.setText(matchedMedication.name)
            descriptionEditText.setText(matchedMedication.description)
            dosageEditText.setText(matchedMedication.dosage)

            matchedMedicationId = matchedMedication.id

            Toast.makeText(
                this,
                "기존 데이터베이스에서 '${matchedMedication.name}' 약품을 찾았습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveMedication() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val dosage = dosageEditText.text.toString().trim()

        // 입력 검증
        if (name.isEmpty()) {
            Toast.makeText(this, "약 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 선택된 시간대 목록 생성
        val selectedTimeSlots = mutableListOf<TimeSlot>()
        if (morningCheckBox.isChecked) selectedTimeSlots.add(TimeSlot.MORNING)
        if (lunchCheckBox.isChecked) selectedTimeSlots.add(TimeSlot.LUNCH)
        if (eveningCheckBox.isChecked) selectedTimeSlots.add(TimeSlot.EVENING)
        if (bedtimeCheckBox.isChecked) selectedTimeSlots.add(TimeSlot.BEDTIME)

        if (selectedTimeSlots.isEmpty()) {
            Toast.makeText(this, "적어도 하나의 시간대를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 약품 ID 생성 (매칭된 약품이 있으면 해당 ID 사용)
        val medicationId = if (matchedMedicationId != null) {
            // 매칭된 약품 ID + 사용자 ID로 복제
            "${matchedMedicationId}_${userId}"
        } else {
            // 새 ID 생성
            "med_${UUID.randomUUID().toString().substring(0, 8)}"
        }

        // 약품 객체 생성
        val medication = Medication(
            id = medicationId,
            name = name,
            description = description,
            dosage = dosage,
            timesPerDay = selectedTimeSlots.size,
            timeSlots = selectedTimeSlots,
            startDate = Date(),
            userId = userId
        )

        // 데이터베이스에 저장
        MedicationManager.addMedication(medication)

        Log.d("AddMedicationActivity", "약품 저장됨: $name, 시간대: ${selectedTimeSlots.joinToString()}")

        Toast.makeText(this, "약이 추가되었습니다", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}