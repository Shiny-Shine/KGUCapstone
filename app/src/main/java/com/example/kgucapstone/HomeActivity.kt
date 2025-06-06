package com.example.kgucapstone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.LinearLayout
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.TextPart
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.ai.type.Content
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.createBitmap
import com.example.kgucapstone.model.TimeSlot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.FirebaseStorage


class HomeActivity : AppCompatActivity() {

    private lateinit var currentPhotoPath: String
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var speechRecognizerLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupClickListeners()
        setupActivityResultLaunchers()
    }

    private fun setupActivityResultLaunchers() {
        // 카메라 퍼미션 요청 결과 처리
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 카메라 앱에서 돌아왔을 때 결과 처리
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    // 미리보기 이미지 가져오기 시도 (에뮬레이터에서 더 잘 작동)
                    val bitmap = if (result.data?.extras?.containsKey("data") == true) {
                        // 미리보기 이미지 사용
                        result.data?.extras?.get("data") as? Bitmap
                    } else {
                        // 파일에서 이미지 로드
                        BitmapFactory.decodeFile(currentPhotoPath)
                    }

                    if (bitmap != null) {
                        analyzeImageWithGemini(bitmap)
                    } else {
                        Log.e(TAG, "비트맵 생성 실패. 파일 경로: $currentPhotoPath")
                        Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "이미지 처리 중 오류 발생", e)
                    Toast.makeText(this, "이미지 처리 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "사진 촬영이 취소되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 음성 인식 결과 처리
        speechRecognizerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
                if (spokenText.isNotEmpty()) {
                    // 음성으로 인식된 텍스트를 Gemini에 전송
                    askGeminiWithText(spokenText + ", 일반인이 알아들을 수 있게 쉽고 간단하게 정보를 제공해 주세요, 주요 성분, 효과, 용법, 주의사항이 포함되야 합니다.")
                    Log.d(TAG, "음성 인식 결과: $spokenText")
                } else {
                    Toast.makeText(this, "음성이 인식되지 않았습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "음성 인식이 취소되었습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition()
            } else {
                Toast.makeText(this, "음성 인식을 위한 마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        // 내 정보 버튼
        // setupClickListeners() 함수 내부에 있는 내 정보 버튼 클릭 핸들러 수정
        findViewById<Button>(R.id.btn_my_info).setOnClickListener {
            // 내 정보 화면으로 이동
            val intent = Intent(this, UserProfileActivity::class.java)

            // 현재 사용자 정보 확인
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // Firebase 인증 제공자 확인
                val providerData = currentUser.providerData
                var loginType = "일반 로그인"

                for (profile in providerData) {
                    if (profile.providerId == GoogleAuthProvider.PROVIDER_ID) {
                        loginType = "Google 로그인"
                        break
                    }
                }

                intent.putExtra("LOGIN_TYPE", loginType)
            } else {
                // Firebase 로그인이 아닌 경우, 일반 로그인으로 간주
                intent.putExtra("USERNAME", "kgucapstone")
                intent.putExtra("LOGIN_TYPE", "일반 로그인")
            }

            startActivity(intent)
        }

        // 식사 시간 버튼들
        findViewById<LinearLayout>(R.id.btn_morning).setOnClickListener {
            val intent = Intent(this, MedicationTimeActivity::class.java)
            intent.putExtra("TIME_SLOT", TimeSlot.MORNING.ordinal)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btn_lunch).setOnClickListener {
            val intent = Intent(this, MedicationTimeActivity::class.java)
            intent.putExtra("TIME_SLOT", TimeSlot.LUNCH.ordinal)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btn_evening).setOnClickListener {
            val intent = Intent(this, MedicationTimeActivity::class.java)
            intent.putExtra("TIME_SLOT", TimeSlot.EVENING.ordinal)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btn_bedtime).setOnClickListener {
            val intent = Intent(this, MedicationTimeActivity::class.java)
            intent.putExtra("TIME_SLOT", TimeSlot.BEDTIME.ordinal)
            startActivity(intent)
        }

        // 내 복용 기록 버튼
        findViewById<Button>(R.id.btn_medication_record).setOnClickListener {
            showToast("내 복용 기록")
        }

        // 카메라 버튼
        findViewById<LinearLayout>(R.id.btn_camera).setOnClickListener {
            // 카메라 권한 확인 및 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // 음성 버튼
        findViewById<LinearLayout>(R.id.btn_voice).setOnClickListener {
            // 마이크 권한 확인 및 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    1001
                )
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // 에뮬레이터에서는 간소화된 방식으로 진행 (미리보기 이미지 사용)
            if (isEmulator()) {
                takePictureLauncher.launch(takePictureIntent)
                return@also
            }

            // 실제 기기에서는 원래 방식대로 파일 생성
            takePictureIntent.resolveActivity(packageManager)?.also {
                // 이미지 파일 생성
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e(TAG, "이미지 파일 생성 중 오류", ex)
                    null
                }

                // 파일이 성공적으로 생성되었다면 진행
                photoFile?.also { file ->
                    try {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.KGU.kgucapstone.fileprovider",
                            file
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    } catch (e: Exception) {
                        Log.e(TAG, "FileProvider URI 생성 중 오류", e)
                    }
                    takePictureLauncher.launch(takePictureIntent)
                } ?: takePictureLauncher.launch(takePictureIntent)
            } ?: takePictureLauncher.launch(takePictureIntent)
        }
    }

    // 에뮬레이터 여부 확인 함수
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // 이미지 파일명 생성
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // 파일 경로 저장
            currentPhotoPath = absolutePath
        }
    }

    private fun analyzeImageWithGemini(bitmap: Bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "비트맵이 null입니다. 파일 경로: $currentPhotoPath")
            Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        // 로딩 메시지 표시
        Toast.makeText(this, "사진을 분석하는 중입니다...", Toast.LENGTH_SHORT).show()

        try {
            // Gemini 모델 초기화
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-2.0-flash")

            // 코루틴으로 실행
            lifecycleScope.launch {
                try {
                    // 프롬프트 텍스트
                    val prompt = "이 사진에 있는 약품에 대해 일반인이 알아들을 수 있게 쉽고 간단하게 정보를 제공해 주세요, 주요 성분, 효과, 용법, 주의사항이 포함되야 합니다."

                    // 텍스트 파트와 이미지 파트 생성
                    val textPart = TextPart(prompt)
                    val imagePart = ImagePart(bitmap)

                    // Content 객체 생성 방식 수정
                    val content1 = Content(parts = listOf(textPart))
                    val content2 = Content(parts = listOf(imagePart))

                    // Gemini API 호출
                    val response = model.generateContent(content1, content2)
                    val resultText = response.text ?: "분석 결과를 가져올 수 없습니다."

                    // 결과와 함께 이미지 전달
                    showAnalysisResultWithImage(resultText, bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Gemini API 호출 실패", e)
                    Toast.makeText(
                        this@HomeActivity,
                        "약품 분석에 실패했습니다: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 분석 초기화 중 오류 발생", e)
            Toast.makeText(
                this,
                "이미지 분석 초기화 중 오류가 발생했습니다: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "약에 대해 질문해 보세요")
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "음성 인식을 지원하지 않는 기기입니다", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "음성 인식 시작 실패", e)
        }
    }

    private fun askGeminiWithText(text: String) {
        // 로딩 메시지 표시
        Toast.makeText(this, "질문을 처리하는 중입니다...", Toast.LENGTH_SHORT).show()

        try {
            // Gemini 모델 초기화
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-2.0-flash")

            // 코루틴으로 실행
            lifecycleScope.launch {
                try {
                    // 약 관련 프롬프트 추가
                    val prompt = "다음은 약에 관한 질문입니다. 일반인이 알아들을 수 있게 쉽고 간단하게 정보를 제공해 주세요, 주요 성분, 효과, 용법, 주의사항이 포함되야 합니다.: $text"

                    // 텍스트 파트 생성
                    val textPart = TextPart(prompt)
                    val content = Content(parts = listOf(textPart))

                    // Gemini API 호출
                    val response = model.generateContent(content)
                    val resultText = response.text ?: "답변을 가져올 수 없습니다."

                    // 약품 이름 추출 (키워드 추출)
                    val keywordPrompt = "다음 질문에서 약품 이름만 간단히 한 단어로 추출해줘. 약품 이름이 없으면 '없음'이라고 답변해줘: $text"
                    val keywordTextPart = TextPart(keywordPrompt)
                    val keywordContent = Content(parts = listOf(keywordTextPart))
                    val keywordResponse = model.generateContent(keywordContent)
                    val keyword = keywordResponse.text?.trim() ?: "없음"

                    Log.d(TAG, "추출된 약품 키워드: $keyword")

                    if (keyword != "없음") {
                        // 약품 이미지 검색
                        fetchMedicineImage(keyword, resultText)
                    } else {
                        // 키워드가 없으면 기본 이미지 사용
                        val defaultMedicineImage = createDefaultMedicineImage()
                        showAnalysisResultWithImage(resultText, defaultMedicineImage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gemini API 호출 실패", e)
                    Toast.makeText(
                        this@HomeActivity,
                        "질문 처리에 실패했습니다: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 초기화 중 오류 발생", e)
            Toast.makeText(this, "Gemini 초기화 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchMedicineImage(medicineKeyword: String, resultText: String) {
        // 파이어베이스 스토리지 레퍼런스
        val storageRef = FirebaseStorage.getInstance().reference.child("medicine_images")

        // 대체 키워드 목록 (일반적인 약품명 변형 처리)
        val keywordVariations = listOf(
            medicineKeyword,
            medicineKeyword.lowercase(),
            medicineKeyword.replace(" ", "_"),
            medicineKeyword.replace(" ", ""),
            "${medicineKeyword.lowercase()}.jpg",
            "${medicineKeyword.lowercase()}.png"
        )

        // 이미지 검색 결과 리스너
        var imageFound = false

        // 모든 변형 키워드에 대해 순차적으로 검색 시도
        lifecycleScope.launch {
            try {
                for (keyword in keywordVariations) {
                    if (imageFound) break

                    try {
                        val imageRef = storageRef.child("$keyword")
                        // 최대 파일 크기 5MB로 제한
                        val MAX_SIZE: Long = 5 * 1024 * 1024

                        imageRef.getBytes(MAX_SIZE).addOnSuccessListener { bytes ->
                            // 이미지 데이터 로드 성공
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                imageFound = true
                                showAnalysisResultWithImage(resultText, bitmap)
                            }
                        }.addOnFailureListener {
                            // 다음 키워드 시도
                            Log.d(TAG, "키워드 '$keyword'로 이미지 검색 실패")
                        }

                        // 비동기 작업이 완료될 시간을 주기 위해 잠시 대기
                        kotlinx.coroutines.delay(500)
                    } catch (e: Exception) {
                        Log.e(TAG, "이미지 검색 중 오류: ${e.message}")
                    }
                }

                // 이미지를 찾지 못한 경우 백업 검색 수행
                if (!imageFound) {
                    searchCommonMedicineImage(medicineKeyword, resultText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "이미지 검색 실패", e)
                // 오류 발생 시 기본 이미지 사용
                val defaultImage = createDefaultMedicineImage()
                showAnalysisResultWithImage(resultText, defaultImage)
            }
        }
    }

    private fun searchCommonMedicineImage(keyword: String, resultText: String) {
        // 일반적인 약품 카테고리에 맞는 이미지 로드
        val resourceId = when {
            keyword.contains("타이레놀") || keyword.contains("해열") ||
                    keyword.contains("진통") || keyword.contains("아세트아미노펜") ||
                    keyword.contains("acetaminophen") ->
                R.drawable.med_painkiller

            keyword.contains("아스피린") || keyword.contains("aspirin") ->
                R.drawable.med_aspirin

            keyword.contains("항생제") || keyword.contains("antibiotic") ->
                R.drawable.med_antibiotic

            keyword.contains("위장약") || keyword.contains("소화제") ->
                R.drawable.med_digestive

            keyword.contains("감기약") || keyword.contains("cold") ->
                R.drawable.med_cold

            keyword.contains("비타민") || keyword.contains("vitamin") ->
                R.drawable.med_vitamin

            else -> 0
        }

        if (resourceId != 0) {
            try {
                val bitmap = BitmapFactory.decodeResource(resources, resourceId)
                showAnalysisResultWithImage(resultText, bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "리소스 이미지 로드 실패", e)
                val defaultImage = createDefaultMedicineImage()
                showAnalysisResultWithImage(resultText, defaultImage)
            }
        } else {
            // 일치하는 카테고리도 없으면 기본 이미지 사용
            val defaultImage = createDefaultMedicineImage()
            showAnalysisResultWithImage(resultText, defaultImage)
        }
    }

    private fun createDefaultMedicineImage(): Bitmap {
        // 기본 정보 표시용 이미지 생성
        val width = 800
        val height = 400
        val bitmap = createBitmap(width, height)
        val canvas = android.graphics.Canvas(bitmap)

        // 배경 그리기
        val paint = android.graphics.Paint()
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 약 정보 텍스트 추가
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 40f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("약품 정보", width / 2f, 100f, paint)

        // 간단한 약 아이콘
        paint.color = android.graphics.Color.rgb(255, 165, 0) // 주황색
        canvas.drawCircle(width / 2f, height / 2f, 100f, paint)

        return bitmap
    }

    private fun showAnalysisResultWithImage(result: String, image: Bitmap) {
        try {
            val dialogIntent = Intent(this, MedicineAnalysisResultActivity::class.java)
            dialogIntent.putExtra("ANALYSIS_RESULT", result)

            // 비트맵 크기 줄이기
            val resizedBitmap = getResizedBitmap(image, 500) // 최대 가로 500px로 조정

            // 비트맵을 바이트 배열로 변환
            val stream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val byteArray = stream.toByteArray()

            // 바이트 배열로 전달
            dialogIntent.putExtra("MEDICINE_IMAGE_BYTES", byteArray)

            startActivity(dialogIntent)
        } catch (e: Exception) {
            Log.e(TAG, "이미지 전달 실패", e)
            // 이미지 없이 결과만 표시
            showAnalysisResult(result)
        }
    }

    // 비트맵 크기 조정 함수
    private fun getResizedBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth = maxWidth
        val newHeight = (newWidth / ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun showAnalysisResult(result: String) {
        val dialogIntent = Intent(this, MedicineAnalysisResultActivity::class.java)
        dialogIntent.putExtra("ANALYSIS_RESULT", result)
        startActivity(dialogIntent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}