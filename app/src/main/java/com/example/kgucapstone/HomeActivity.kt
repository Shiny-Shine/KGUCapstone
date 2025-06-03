package com.example.kgucapstone // 본인의 패키지명으로 변경

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 내 정보 버튼
        findViewById<Button>(R.id.btn_my_info).setOnClickListener {
            showToast("내 정보")
        }

        // 식사 시간 버튼들
        findViewById<LinearLayout>(R.id.btn_morning).setOnClickListener {
            showToast("아침 복용")
        }

        findViewById<LinearLayout>(R.id.btn_lunch).setOnClickListener {
            showToast("점심 복용")
        }

        findViewById<LinearLayout>(R.id.btn_evening).setOnClickListener {
            showToast("저녁 복용")
        }

        findViewById<LinearLayout>(R.id.btn_bedtime).setOnClickListener {
            showToast("취침 전 복용")
        }

        // 내 복용 기록 버튼
        findViewById<Button>(R.id.btn_medication_record).setOnClickListener {
            showToast("내 복용 기록")
        }

        // 카메라 버튼
        findViewById<LinearLayout>(R.id.btn_camera).setOnClickListener {
            showToast("카메라로 검색")
        }

        // 음성 버튼
        findViewById<LinearLayout>(R.id.btn_voice).setOnClickListener {
            showToast("음성으로 질문")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}