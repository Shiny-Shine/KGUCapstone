package com.example.kgucapstone

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.MedicationManager
import com.example.kgucapstone.model.TimeSlot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.TextPart
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicineAnalysisResultActivity : AppCompatActivity() {

    private lateinit var resultTextView: TextView
    private lateinit var medicineImageView: ImageView
    private lateinit var addToMorningBtn: Button
    private lateinit var addToLunchBtn: Button
    private lateinit var addToEveningBtn: Button
    private lateinit var addToBedtimeBtn: Button
    private lateinit var backButton: Button

    private var analysisResult: String = ""
    private var extractedMedicineName: String = ""
    private var matchedMedication: Medication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_analysis_result)

        // UI 초기화
        resultTextView = findViewById(R.id.tv_analysis_result)
        medicineImageView = findViewById(R.id.iv_medicine_image)
        addToMorningBtn = findViewById(R.id.btn_add_to_morning)
        addToLunchBtn = findViewById(R.id.btn_add_to_lunch)
        addToEveningBtn = findViewById(R.id.btn_add_to_evening)
        addToBedtimeBtn = findViewById(R.id.btn_add_to_bedtime)
        backButton = findViewById(R.id.btn_back)

        // 분석 결과 가져오기
        analysisResult = intent.getStringExtra("ANALYSIS_RESULT") ?: "분석 결과가 없습니다."
        resultTextView.text = analysisResult

        // 이미지 가져오기
        val imageBytes = intent.getByteArrayExtra("MEDICINE_IMAGE_BYTES")
        if (imageBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            medicineImageView.setImageBitmap(bitmap)
            medicineImageView.visibility = View.VISIBLE
        } else {
            medicineImageView.visibility = View.GONE
        }

        // 분석 결과에서 약품 이름 추출 및 데이터베이스 검색
        extractMedicineNameAndFindMatch(analysisResult)

        // 시간대별 약 추가 버튼 이벤트 설정
        setupAddMedicationButtons()

        // 뒤로 가기 버튼
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun extractMedicineNameAndFindMatch(analysisText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Gemini 모델을 사용해 약품 이름 추출
                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel("gemini-2.0-flash")

                val prompt = "다음 약품 분석 결과에서 약품의 정확한 이름만 추출해서 한 단어로 알려주세요. 다른 설명은 하지 말고 약품 이름만 답변해주세요: $analysisText"
                val textPart = TextPart(prompt)
                val content = Content(parts = listOf(textPart))

                val response = model.generateContent(content)
                extractedMedicineName = response.text?.trim() ?: ""

                Log.d("MedicineAnalysis", "추출된 약품 이름: $extractedMedicineName")

                // 추출된 이름을 기반으로 기존 데이터베이스에서 유사한 약품 검색
                matchedMedication = findSimilarMedicationInDatabase(extractedMedicineName)

                withContext(Dispatchers.Main) {
                    if (matchedMedication != null) {
                        Toast.makeText(
                            this@MedicineAnalysisResultActivity,
                            "기존 데이터베이스에서 '${matchedMedication?.name}' 약품을 찾았습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MedicineAnalysis", "약품 이름 추출 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MedicineAnalysisResultActivity,
                        "약품 이름 추출에 실패했습니다: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun findSimilarMedicationInDatabase(medicineName: String): Medication? {
        // 모든 약품 목록 가져오기 (공통 약품 목록)
        val allMedications = MedicationManager.getMedicationsForUser("common_medicines")

        // 정확히 일치하는 약품 찾기
        val exactMatch = allMedications.find {
            it.name.equals(medicineName, ignoreCase = true)
        }

        if (exactMatch != null) {
            return exactMatch
        }

        // 부분 일치하는 약품 찾기
        return allMedications.find { medication ->
            medication.name.contains(medicineName, ignoreCase = true) ||
                    medicineName.contains(medication.name, ignoreCase = true)
        }
    }

    private fun setupAddMedicationButtons() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "current_user_id"

        // 아침 시간대에 추가
        addToMorningBtn.setOnClickListener {
            navigateToAddMedicationActivity(TimeSlot.MORNING, userId)
        }

        // 점심 시간대에 추가
        addToLunchBtn.setOnClickListener {
            navigateToAddMedicationActivity(TimeSlot.LUNCH, userId)
        }

        // 저녁 시간대에 추가
        addToEveningBtn.setOnClickListener {
            navigateToAddMedicationActivity(TimeSlot.EVENING, userId)
        }

        // 취침 시간대에 추가
        addToBedtimeBtn.setOnClickListener {
            navigateToAddMedicationActivity(TimeSlot.BEDTIME, userId)
        }
    }

    private fun navigateToAddMedicationActivity(timeSlot: TimeSlot, userId: String) {
        val intent = Intent(this, AddMedicationActivity::class.java)
        intent.putExtra("TIME_SLOT", timeSlot.ordinal)
        intent.putExtra("USER_ID", userId)

        // 분석된 약품 정보 전달
        intent.putExtra("MEDICINE_NAME", extractedMedicineName)
        intent.putExtra("MEDICINE_DESCRIPTION", analysisResult)

        // 매칭된 약품이 있으면 해당 정보도 전달
        if (matchedMedication != null) {
            intent.putExtra("MATCHED_MEDICINE_ID", matchedMedication!!.id)
            intent.putExtra("MATCHED_MEDICINE_NAME", matchedMedication!!.name)
            intent.putExtra("MATCHED_MEDICINE_DESCRIPTION", matchedMedication!!.description)
            intent.putExtra("MATCHED_MEDICINE_DOSAGE", matchedMedication!!.dosage)
        }

        // 이미지 전달 (선택적)
        val imageBytes = intent.getByteArrayExtra("MEDICINE_IMAGE_BYTES")
        if (imageBytes != null) {
            intent.putExtra("MEDICINE_IMAGE_BYTES", imageBytes)
        }

        startActivity(intent)
    }
}