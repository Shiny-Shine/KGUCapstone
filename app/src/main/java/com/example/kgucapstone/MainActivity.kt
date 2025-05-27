package com.example.kgucapstone

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts


// Firebase AI imports for Gemini API
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

// Firebase and Google Sign-In imports
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "MainActivity" // 로그 태그
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        val responseTextView = findViewById<TextView>(R.id.test)
        val btnLoginButton = findViewById<Button>(R.id.btnLogin) // btnLogin 버튼 ID 사용

        // Firebase Auth 인스턴스 초기화
        auth = Firebase.auth

        // Firebase 콘솔에서 웹 클라이언트 ID를 가져와야 합니다.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // 중요!
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ActivityResultLauncher 초기화
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!) // firebaseAuthWithGoogle 함수 호출
                } catch (e: ApiException) {
                    Log.w(TAG, "Google sign in failed", e)
                    // 로그인 실패 UI 처리
                    responseTextView.text = "Google 로그인 실패: ${e.statusCode}"
                }
            } else {
                Log.w(TAG, "Google sign in cancelled or failed, resultCode: " + result.resultCode)
                // 로그인 취소 또는 다른 이유로 실패 UI 처리
                responseTextView.text = "Google 로그인이 취소되거나 실패했습니다."
            }
        }

        // btnLogin 버튼에 Google 로그인 기능 연결
        btnLoginButton.setOnClickListener {
            signInWithGoogle()
        }

        // Initialize the Gemini Developer API backend service
        // Create a `GenerativeModel` instance with a model that supports your use case
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")

        // Provide a prompt that contains text
        val prompt = "안녕? 나는 경기대학교에서 캡스톤디자인 프로젝트를 하고 있는 대학생이야. 너는 나의 AI 도우미야. 내가 질문하면 대답해줘."

        // To generate text output, call generateContent with the text input
        lifecycleScope.launch {
            //val response = model.generateContent(prompt)
            //responseTextView.text = response.text
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase에 로그인 성공
                    val user = auth.currentUser
                    Log.d(TAG, "Firebase signInWithCredential success, user: ${user?.displayName}")
                    // UI 업데이트 또는 다음 액티비티로 이동
                    // 예: findViewById<TextView>(R.id.test).text = "로그인 성공: ${user?.displayName}"
                } else {
                    // Firebase에 로그인 실패
                    Log.w(TAG, "Firebase signInWithCredential failure", task.exception)
                    // UI 업데이트 (예: 에러 메시지 표시)
                }
            }
    }

    // 앱 시작 시 이미 로그인된 사용자가 있는지 확인 (선택 사항)
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 이미 로그인된 사용자 처리
            Log.d(TAG, "User already signed in: ${currentUser.displayName}")
            // 예: findViewById<TextView>(R.id.test).text = "환영합니다, ${currentUser.displayName}님!"
        }
    }
}