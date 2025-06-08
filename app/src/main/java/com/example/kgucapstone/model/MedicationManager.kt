package com.example.kgucapstone.model

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.kgucapstone.AlarmReceiver
import android.app.AlarmManager
import java.util.*

object MedicationManager {
    private val medications = mutableListOf<Medication>()
    private val medicationRecords = mutableListOf<MedicationRecord>()

    // 알람 관련 상수
    private const val ALARM_REQUEST_CODE_MORNING = 1001
    private const val ALARM_REQUEST_CODE_LUNCH = 1002
    private const val ALARM_REQUEST_CODE_EVENING = 1003
    private const val ALARM_REQUEST_CODE_BEDTIME = 1004

    // 알람 시간 저장 (24시간 형식)
    private var alarmTimes = mutableMapOf(
        TimeSlot.MORNING to Pair(8, 0),  // 오전 8시
        TimeSlot.LUNCH to Pair(12, 0),   // 오후 12시
        TimeSlot.EVENING to Pair(18, 0), // 오후 6시
        TimeSlot.BEDTIME to Pair(22, 0)  // 오후 10시
    )

    // 알람 시간 설정
    fun setAlarmTime(timeSlot: TimeSlot, hour: Int, minute: Int) {
        alarmTimes[timeSlot] = Pair(hour, minute)
    }

    // 알람 시간 조회
    fun getAlarmTime(timeSlot: TimeSlot): Pair<Int, Int> {
        return alarmTimes[timeSlot] ?: Pair(0, 0)
    }

    // 모든 알람 설정
    fun setAllAlarms(context: Context, userId: String) {
        for (timeSlot in TimeSlot.values()) {
            setAlarmForTimeSlot(context, timeSlot, userId)
        }
    }

    // 특정 시간대 알람 설정
    @SuppressLint("ScheduleExactAlarm")
    fun setAlarmForTimeSlot(context: Context, timeSlot: TimeSlot, userId: String) {
        val medications = getMedicationsForTimeSlot(timeSlot, userId)

        // 해당 시간대에 복용할 약이 있는 경우에만 알람 설정
        if (medications.isNotEmpty()) {
            val (hour, minute) = alarmTimes[timeSlot] ?: return

            // 알람 인텐트 생성
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("TIME_SLOT", timeSlot.ordinal)
                putExtra("USER_ID", userId)
                putExtra("MEDICATION_COUNT", medications.size)
            }

            // 알람 ID 설정 (시간대별로 다른 ID 사용)
            val requestCode = when (timeSlot) {
                TimeSlot.MORNING -> ALARM_REQUEST_CODE_MORNING
                TimeSlot.LUNCH -> ALARM_REQUEST_CODE_LUNCH
                TimeSlot.EVENING -> ALARM_REQUEST_CODE_EVENING
                TimeSlot.BEDTIME -> ALARM_REQUEST_CODE_BEDTIME
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알람 시간 설정
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)

                // 현재 시간보다 이전이면 다음 날로 설정
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // 알람 설정
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            Log.d("MedicationManager", "${timeSlot.name} 알람 설정: ${hour}시 ${minute}분, 약품 수: ${medications.size}")
        } else {
            Log.d("MedicationManager", "${timeSlot.name} 시간대에 약품이 없어 알람을 설정하지 않음")
        }
    }

    // 알람 취소
    fun cancelAlarm(context: Context, timeSlot: TimeSlot) {
        val requestCode = when (timeSlot) {
            TimeSlot.MORNING -> ALARM_REQUEST_CODE_MORNING
            TimeSlot.LUNCH -> ALARM_REQUEST_CODE_LUNCH
            TimeSlot.EVENING -> ALARM_REQUEST_CODE_EVENING
            TimeSlot.BEDTIME -> ALARM_REQUEST_CODE_BEDTIME
        }

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        Log.d("MedicationManager", "${timeSlot.name} 알람 취소됨")
    }

    // 모든 알람 취소
    fun cancelAllAlarms(context: Context) {
        for (timeSlot in TimeSlot.values()) {
            cancelAlarm(context, timeSlot)
        }
    }

    // 모든 약 조회
    fun getAllMedications(): List<Medication> {
        return medications.toList()
    }

    // 특정 사용자의 약 조회
    fun getMedicationsForUser(userId: String): List<Medication> {
        return medications.filter { it.userId == userId }
    }

    // 특정 시간대의 약 조회 (특정 사용자의)
    fun getMedicationsForTimeSlot(timeSlot: TimeSlot, userId: String): List<Medication> {
        return medications.filter {
            it.timeSlots.contains(timeSlot) && it.userId == userId
        }
    }

    // 약 추가
    fun addMedication(medication: Medication) {
        // 이미 있는 약인지 확인
        val existingIndex = medications.indexOfFirst { it.id == medication.id }
        if (existingIndex >= 0) {
            medications[existingIndex] = medication
        } else {
            medications.add(medication)
        }
    }

    // 약 업데이트
    fun updateMedication(medication: Medication) {
        val index = medications.indexOfFirst { it.id == medication.id }
        if (index >= 0) {
            medications[index] = medication
        }
    }

    // 약 삭제
    fun deleteMedication(medicationId: String) {
        medications.removeIf { it.id == medicationId }
    }

    // 복용 기록 추가
    fun addMedicationRecord(record: MedicationRecord) {
        // 이미 있는 기록인지 확인
        val existingIndex = medicationRecords.indexOfFirst {
            it.medicationId == record.medicationId &&
                    it.timeSlot == record.timeSlot &&
                    isSameDay(it.date, record.date) &&
                    it.userId == record.userId
        }

        if (existingIndex >= 0) {
            medicationRecords[existingIndex] = record
        } else {
            medicationRecords.add(record)
        }
    }

    // 복용 기록 추가 또는 업데이트
    fun addOrUpdateMedicationRecord(record: MedicationRecord) {
        val index = medicationRecords.indexOfFirst {
            isSameDay(it.date, record.date) &&
                    it.userId == record.userId &&
                    it.medicationId == record.medicationId && // 약 ID도 비교
                    it.timeSlot == record.timeSlot // 시간대도 비교
        }

        if (index >= 0) {
            medicationRecords[index] = record
        } else {
            // 기록이 없으면 새로 추가
            medicationRecords.add(record)
        }
    }

    // 복용 기록 조회
    fun getMedicationRecords(medicationId: String, userId: String): List<MedicationRecord> {
        return medicationRecords.filter {
            it.medicationId == medicationId && it.userId == userId
        }
    }

    // 특정 사용자, 날짜, 시간대에 대한 복용 기록 조회
    fun getMedicationRecordsForUserDateAndSlot(userId: String, date: Date, timeSlot: TimeSlot): List<MedicationRecord> {
        // 해당 날짜의 시작과 끝 시간 계산
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.time

        // 해당 사용자, 날짜, 시간대에 대한 복용 기록 필터링
        return medicationRecords.filter { record ->
            record.userId == userId &&
                    record.timeSlot == timeSlot &&
                    record.date >= startOfDay &&
                    record.date <= endOfDay
        }
    }

    // 특정 날짜의 복용 기록 조회
    fun getMedicationRecordsForDate(date: Date, userId: String): List<MedicationRecord> {
        return medicationRecords.filter {
            isSameDay(it.date, date) && it.userId == userId
        }
    }

    // 복용 기록 업데이트
    fun updateMedicationRecord(record: MedicationRecord) {
        val index = medicationRecords.indexOfFirst {
            it.medicationId == record.medicationId &&
                    it.timeSlot == record.timeSlot &&
                    isSameDay(it.date, record.date) &&
                    it.userId == record.userId
        }

        if (index >= 0) {
            medicationRecords[index] = record
        } else {
            // 기록이 없으면 새로 추가
            medicationRecords.add(record)
        }
    }

    // 약 완전히 삭제 (ID로 삭제)
    fun removeMedication(medicationId: String) {
        medications.removeIf { it.id == medicationId }

        // 관련된 복용 기록도 함께 삭제
        medicationRecords.removeIf { it.medicationId == medicationId }
    }

    // 날짜 비교 헬퍼 함수
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    fun getMedicationById(medicationId: String): Medication? {
        return medications.find { it.id == medicationId }
    }

    // 샘플 데이터 추가 - 계정 상관없이 기본 샘플 데이터 추가
    fun addSampleData() {
        // 모든 사용자가 볼 수 있는 공통 약품들
        val commonUserId = "common_medicines"

        // 1. 타이레놀
        addMedication(
            Medication(
                id = "sample_tylenol",
                name = "타이레놀",
                description = "두통, 근육통, 발열에 효과적인 진통제",
                dosage = "1회 1-2정, 1일 3-4회",
                timesPerDay = 3,
                timeSlots = listOf(TimeSlot.MORNING, TimeSlot.LUNCH, TimeSlot.EVENING),
                startDate = Date(),
                userId = commonUserId
            )
        )

        // 2. 판콜
        addMedication(
            Medication(
                id = "sample_pancol",
                name = "판콜",
                description = "감기 증상 완화제",
                dosage = "1회 1포, 1일 3회",
                timesPerDay = 3,
                timeSlots = listOf(TimeSlot.MORNING, TimeSlot.LUNCH, TimeSlot.EVENING),
                startDate = Date(),
                userId = commonUserId
            )
        )

        // 3. 닥터베아제
        addMedication(
            Medication(
                id = "sample_bearse",
                name = "닥터베아제",
                description = "소화불량, 속쓰림에 효과적인 소화제",
                dosage = "1회 1정, 식후",
                timesPerDay = 3,
                timeSlots = listOf(TimeSlot.MORNING, TimeSlot.LUNCH, TimeSlot.EVENING),
                startDate = Date(),
                userId = commonUserId
            )
        )

        // 4. 알레르기 약
        addMedication(
            Medication(
                id = "sample_allergy",
                name = "알레그라",
                description = "알레르기 증상 완화제",
                dosage = "1회 1정, 1일 2회",
                timesPerDay = 2,
                timeSlots = listOf(TimeSlot.MORNING, TimeSlot.EVENING),
                startDate = Date(),
                userId = commonUserId
            )
        )

        // 5. 수면제
        addMedication(
            Medication(
                id = "sample_sleep",
                name = "수면유도제",
                description = "수면 장애 개선에 도움",
                dosage = "1회 1정, 취침 30분 전",
                timesPerDay = 1,
                timeSlots = listOf(TimeSlot.BEDTIME),
                startDate = Date(),
                userId = commonUserId
            )
        )

        // 6. 영양제
        addMedication(
            Medication(
                id = "sample_vitamin",
                name = "종합비타민",
                description = "일일 필수 영양소 보충",
                dosage = "1회 1정, 아침 식후",
                timesPerDay = 1,
                timeSlots = listOf(TimeSlot.MORNING),
                startDate = Date(),
                userId = commonUserId
            )
        )
    }
}