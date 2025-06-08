package com.example.kgucapstone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.kgucapstone.model.MedicationManager
import com.example.kgucapstone.model.TimeSlot

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "medication_reminder_channel"
        private const val NOTIFICATION_ID = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val timeSlotOrdinal = intent.getIntExtra("TIME_SLOT", 0)
        val userId = intent.getStringExtra("USER_ID") ?: "current_user_id"
        val medicationCount = intent.getIntExtra("MEDICATION_COUNT", 0)

        val timeSlot = TimeSlot.fromOrdinal(timeSlotOrdinal)

        // 알림 채널 생성
        createNotificationChannel(context)

        // 약 복용 시간대 액티비티로 이동하는 인텐트
        val notificationIntent = Intent(context, MedicationTimeActivity::class.java).apply {
            putExtra("TIME_SLOT", timeSlotOrdinal)
            putExtra("USER_ID", userId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            timeSlotOrdinal,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 메시지 작성
        val timeSlotText = when (timeSlot) {
            TimeSlot.MORNING -> "아침"
            TimeSlot.LUNCH -> "점심"
            TimeSlot.EVENING -> "저녁"
            TimeSlot.BEDTIME -> "취침 전"
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("약 복용 시간")
            .setContentText("${timeSlotText} 약 ${medicationCount}개를 복용할 시간입니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // 알림 표시
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + timeSlotOrdinal, notificationBuilder.build())

        // 다음 날 같은 시간에 알람 재설정
        MedicationManager.setAlarmForTimeSlot(context, timeSlot, userId)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "약 복용 알림"
            val descriptionText = "약 복용 시간을 알려주는 알림"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            // 채널 등록
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}