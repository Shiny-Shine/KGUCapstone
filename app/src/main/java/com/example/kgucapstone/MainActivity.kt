package com.example.kgucapstone

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

import com.example.kgucapstone.HomeActivity


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
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.w(TAG, "Google 로그인이 실패했습니다.", e)
                    Toast.makeText(
                        this,
                        "Google 로그인 실패: ${e.statusCode}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.w(TAG, "Google 로그인이 취소되거나 실패했습니다, resultCode: " + result.resultCode)
                Toast.makeText(
                    this,
                    "Google 로그인이 취소되거나 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
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

    // firebaseAuthWithGoogle 함수도 수정
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase에 로그인 성공
                    val user = auth.currentUser
                    Log.d(TAG, "Firebase signInWithCredential success, user: ${user?.displayName}")
                    // 성공 토스트 메시지
                    Toast.makeText(
                        this,
                        "로그인 성공: ${user?.displayName}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 로그인 성공 후 HomeActivity로 이동
                    try {
                        val intent = Intent()
                        intent.setClassName("com.KGU.kgucapstone", "com.example.kgucapstone.HomeActivity")
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "HomeActivity 이동 실패", e)
                        Toast.makeText(this, "화면 이동 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Firebase에 로그인 실패
                    Log.w(TAG, "Firebase signInWithCredential failure", task.exception)
                    // 실패 토스트 메시지
                    Toast.makeText(
                        this,
                        "Firebase 로그인 실패: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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