package com.example.kgucapstone

import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.kgucapstone.model.MedicationManager
import com.example.kgucapstone.model.MedicationRecord
import com.example.kgucapstone.model.TimeSlot
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationRecordActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var ivMorningStatus: ImageView
    private lateinit var ivLunchStatus: ImageView
    private lateinit var ivEveningStatus: ImageView
    private lateinit var ivBedtimeStatus: ImageView
    private lateinit var btnBack: Button

    private lateinit var userId: String
    private var selectedDate: Date = Date()
    private val dateFormatter = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_record)

        // 현재 사용자 ID 가져오기
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "current_user_id"

        // UI 요소 초기화
        initViews()

        // 현재 날짜 설정
        updateSelectedDate(Date())

        // 클릭 리스너 설정
        setupListeners()
    }

    private fun initViews() {
        calendarView = findViewById(R.id.calendar_view)
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        ivMorningStatus = findViewById(R.id.iv_morning_status)
        ivLunchStatus = findViewById(R.id.iv_lunch_status)
        ivEveningStatus = findViewById(R.id.iv_evening_status)
        ivBedtimeStatus = findViewById(R.id.iv_bedtime_status)
        btnBack = findViewById(R.id.btn_back)
    }

    private fun setupListeners() {
        // 달력 날짜 선택 리스너
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            updateSelectedDate(calendar.time)
        }

        // 뒤로 가기 버튼
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateSelectedDate(date: Date) {
        selectedDate = date
        tvSelectedDate.text = dateFormatter.format(date)

        // 선택된 날짜의 복용 기록 표시
        updateMedicationStatusForDate(date)
    }

    private fun updateMedicationStatusForDate(date: Date) {
        // 각 시간대별 복용 상태 가져오기
        val morningStatus = getMedicationStatusForTimeSlot(TimeSlot.MORNING, date)
        val lunchStatus = getMedicationStatusForTimeSlot(TimeSlot.LUNCH, date)
        val eveningStatus = getMedicationStatusForTimeSlot(TimeSlot.EVENING, date)
        val bedtimeStatus = getMedicationStatusForTimeSlot(TimeSlot.BEDTIME, date)

        // 상태에 따라 아이콘 업데이트
        ivMorningStatus.setImageResource(getStatusIcon(morningStatus))
        ivLunchStatus.setImageResource(getStatusIcon(lunchStatus))
        ivEveningStatus.setImageResource(getStatusIcon(eveningStatus))
        ivBedtimeStatus.setImageResource(getStatusIcon(bedtimeStatus))
    }

    private fun getMedicationStatusForTimeSlot(timeSlot: TimeSlot, date: Date): MedicationStatus {
        // 현재 날짜와 선택된 날짜 비교
        val currentDate = Date()

        // 미래 날짜인 경우
        if (date.after(currentDate) && !isSameDay(date, currentDate)) {
            return MedicationStatus.FUTURE
        }

        // 해당 시간대와 사용자에 맞는 약 목록 가져오기
        val medicationsForTimeSlot = MedicationManager.getMedicationsForTimeSlot(timeSlot, userId)

        // 해당 시간대에 약이 없는 경우
        if (medicationsForTimeSlot.isEmpty()) {
            return MedicationStatus.PENDING // 약이 없으면 대기 중(시계) 아이콘 표시
        }

        // 복용 기록 가져오기
        val records = MedicationManager.getMedicationRecordsForUserDateAndSlot(userId, date, timeSlot)

        // 기록이 없는 경우
        if (records.isEmpty()) {
            // 오늘이고 아직 해당 시간대가 되지 않았으면 PENDING 상태
            if (isSameDay(date, currentDate) && !isTimeSlotPassed(timeSlot)) {
                return MedicationStatus.PENDING
            }
            return MedicationStatus.NOT_TAKEN
        }

        // 모든 약을 복용했는지 확인 (모든 약에 대한 복용 기록이 있고, 모두 taken이 true인지)
        val allTaken = records.size == medicationsForTimeSlot.size && records.all { it.taken }
        val anyTaken = records.any { it.taken }

        return when {
            allTaken -> MedicationStatus.TAKEN
            anyTaken -> MedicationStatus.PARTIAL
            else -> MedicationStatus.NOT_TAKEN
        }
    }

    private fun getStatusIcon(status: MedicationStatus): Int {
        return when (status) {
            MedicationStatus.TAKEN -> R.drawable.ic_check_circle
            MedicationStatus.NOT_TAKEN -> R.drawable.ic_cancel
            MedicationStatus.PENDING -> R.drawable.ic_clock
            MedicationStatus.FUTURE -> R.drawable.ic_future
            MedicationStatus.PARTIAL -> R.drawable.ic_partial
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun isTimeSlotPassed(timeSlot: TimeSlot): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (timeSlot) {
            TimeSlot.MORNING -> currentHour >= 10  // 아침 시간대는 10시 이후에 지났다고 판단
            TimeSlot.LUNCH -> currentHour >= 14    // 점심 시간대는 14시 이후에 지났다고 판단
            TimeSlot.EVENING -> currentHour >= 20  // 저녁 시간대는 20시 이후에 지났다고 판단
            TimeSlot.BEDTIME -> currentHour >= 23  // 취침 시간대는 23시 이후에 지났다고 판단
        }
    }

    enum class MedicationStatus {
        TAKEN,       // 복용 완료
        NOT_TAKEN,   // 복용 안 함
        PENDING,     // 복용 시간 아직 안 됨
        FUTURE,      // 미래 날짜
        PARTIAL      // 일부만 복용
    }
}