package com.example.kgucapstone

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.TimeSlot
import com.example.kgucapstone.viewmodel.MedicationViewModel
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
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var viewModel: MedicationViewModel
    private var selectedTimeSlot: TimeSlot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medication)

        // ViewModel 초기화
        viewModel = ViewModelProvider(this)[MedicationViewModel::class.java]

        // UI 초기화
        initializeUI()

        // 선택된 시간대 가져오기
        val timeSlotOrdinal = intent.getIntExtra("TIME_SLOT", -1)
        if (timeSlotOrdinal >= 0) {
            selectedTimeSlot = TimeSlot.fromOrdinal(timeSlotOrdinal)

            // 선택된 시간대 체크박스 자동 선택
            when (selectedTimeSlot) {
                TimeSlot.MORNING -> morningCheckBox.isChecked = true
                TimeSlot.LUNCH -> lunchCheckBox.isChecked = true
                TimeSlot.EVENING -> eveningCheckBox.isChecked = true
                TimeSlot.BEDTIME -> bedtimeCheckBox.isChecked = true
                else -> {}
            }
        }

        // 저장 버튼 클릭 리스너
        saveButton.setOnClickListener {
            saveMedication()
        }

        // 취소 버튼 클릭 리스너
        cancelButton.setOnClickListener {
            finish()
        }

        // ViewModel 관찰
        observeViewModel()
    }

    private fun initializeUI() {
        nameEditText = findViewById(R.id.et_medication_name)
        descriptionEditText = findViewById(R.id.et_medication_description)
        dosageEditText = findViewById(R.id.et_medication_dosage)
        morningCheckBox = findViewById(R.id.cb_morning)
        lunchCheckBox = findViewById(R.id.cb_lunch)
        eveningCheckBox = findViewById(R.id.cb_evening)
        bedtimeCheckBox = findViewById(R.id.cb_bedtime)
        saveButton = findViewById(R.id.btn_save)
        cancelButton = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            saveButton.isEnabled = !isLoading
            cancelButton.isEnabled = !isLoading
        }
    }

    private fun saveMedication() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val dosage = dosageEditText.text.toString().trim()

        // 유효성 검사
        if (name.isEmpty()) {
            nameEditText.error = "약 이름을 입력해주세요"
            return
        }

        if (dosage.isEmpty()) {
            dosageEditText.error = "복용량을 입력해주세요"
            return
        }

        // 시간대 수집
        val timeSlots = mutableListOf<TimeSlot>()
        if (morningCheckBox.isChecked) timeSlots.add(TimeSlot.MORNING)
        if (lunchCheckBox.isChecked) timeSlots.add(TimeSlot.LUNCH)
        if (eveningCheckBox.isChecked) timeSlots.add(TimeSlot.EVENING)
        if (bedtimeCheckBox.isChecked) timeSlots.add(TimeSlot.BEDTIME)

        if (timeSlots.isEmpty()) {
            Toast.makeText(this, "최소 한 개의 복용 시간을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // Medication 객체 생성
        val medication = Medication(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            dosage = dosage,
            timesPerDay = timeSlots.size,
            timeSlots = timeSlots,
            startDate = Date()
        )

        // ViewModel을 통해 저장
        viewModel.addMedication(
            medication = medication,
            onSuccess = {
                Toast.makeText(this, "약이 성공적으로 등록되었습니다", Toast.LENGTH_SHORT).show()
                finish() // 화면 종료
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }
}