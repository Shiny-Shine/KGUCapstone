package com.example.kgucapstone

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MedicineAnalysisResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_analysis_result)

        // 결과 텍스트 표시
        val resultTextView = findViewById<TextView>(R.id.tv_analysis_result)
        val analysisResult = intent.getStringExtra("ANALYSIS_RESULT") ?: "분석 결과를 가져올 수 없습니다."
        resultTextView.text = analysisResult

        // 이미지가 있으면 표시
        val imageView = findViewById<ImageView>(R.id.iv_medicine_image)

        try {
            // 바이트 배열에서 비트맵 복원
            val imageBytes = intent.getByteArrayExtra("MEDICINE_IMAGE_BYTES")
            if (imageBytes != null) {
                val medicineImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(medicineImage)
                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("MedicineAnalysisResultActivity", "이미지 로딩 실패", e)
            imageView.visibility = View.GONE
        }

        // 뒤로가기 버튼
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }
}