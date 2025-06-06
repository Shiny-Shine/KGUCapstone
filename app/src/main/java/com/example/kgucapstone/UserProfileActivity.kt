package com.example.kgucapstone

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.URL

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "UserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // UI 요소 초기화
        val profileImageView = findViewById<ImageView>(R.id.iv_profile_image)
        val userNameTextView = findViewById<TextView>(R.id.tv_user_name)
        val userEmailTextView = findViewById<TextView>(R.id.tv_user_email)
        val accountIdTextView = findViewById<TextView>(R.id.tv_account_id)
        val loginTypeTextView = findViewById<TextView>(R.id.tv_login_type)
        val logoutButton = findViewById<Button>(R.id.btn_logout)
        val backButton = findViewById<Button>(R.id.btn_back)

        // 현재 로그인된 사용자 정보 가져오기
        val currentUser = auth.currentUser
        val loginType = intent.getStringExtra("LOGIN_TYPE") ?: "일반 로그인"

        if (currentUser != null) {
            // 사용자 이름 설정
            val displayName = currentUser.displayName ?: "사용자"
            userNameTextView.text = displayName

            // 이메일 설정
            userEmailTextView.text = currentUser.email ?: "이메일 정보 없음"

            // 계정 ID 설정
            accountIdTextView.text = currentUser.uid.take(10) + "..."

            // 로그인 유형 설정
            loginTypeTextView.text = loginType

            // 프로필 이미지 설정 (Google 계정인 경우 프로필 이미지 로드)
            val photoUrl = currentUser.photoUrl
            if (photoUrl != null) {
                // 백그라운드 스레드에서 이미지 로딩
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val inputStream: InputStream = URL(photoUrl.toString()).openStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        // UI 스레드에서 이미지 설정
                        runOnUiThread {
                            profileImageView.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "프로필 이미지 로딩 실패", e)
                    }
                }
            } else {
                // 기본 프로필 이미지 설정
                profileImageView.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            // 일반 로그인 정보 표시 (MainActivity에서 전달받은 데이터 사용)
            val username = intent.getStringExtra("USERNAME") ?: "kgucapstone"
            userNameTextView.text = username
            userEmailTextView.text = "사용자 이메일 없음"
            accountIdTextView.text = "일반 계정"
            loginTypeTextView.text = "일반 로그인"

            // 기본 프로필 이미지 설정
            profileImageView.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // 로그아웃 버튼 클릭 리스너
        logoutButton.setOnClickListener {
            // Firebase 로그아웃
            auth.signOut()

            // 메인 화면으로 이동
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, "로그아웃되었습니다", Toast.LENGTH_SHORT).show()
        }

        // 뒤로 가기 버튼 클릭 리스너
        backButton.setOnClickListener {
            finish()
        }
    }
}