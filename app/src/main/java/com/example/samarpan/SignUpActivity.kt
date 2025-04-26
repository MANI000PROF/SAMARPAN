package com.example.samarpan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.samarpan.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }

    private var isPasswordVisible: Boolean = false
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val database by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Google Sign-In config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Sign Up button
        binding.signUpBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            registerUser()
        }

        // Navigate to LoginActivity
        binding.signInBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            startActivity(Intent(this, LoginActivity::class.java))
        }

        val loadingOverlay = findViewById<FrameLayout>(R.id.loadingOverlay)
        // Google Sign-Up button
        binding.signUpWithGoogle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            signInWithGoogle()
            // After success:
            loadingOverlay.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                loadingOverlay.visibility = View.GONE
                // Continue to next activity
            }, 2500)
        }

        // Toggle password visibility
        binding.passwordIcon.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            togglePasswordVisibility()
        }
        binding.repasswordIcon.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            togglePasswordVisibility1()
        }
    }

    private fun registerUser() {
        val fullName = binding.fullnameET.text.toString().trim()
        val email = binding.emailET.text.toString().trim()
        val mobile = binding.mobileET.text.toString().trim()
        val password = binding.passwordET.text.toString()
        val confirmPassword = binding.repasswordET.text.toString()

        if (fullName.isEmpty()) {
            binding.fullnameET.error = "Full Name is required"
            binding.fullnameET.requestFocus()
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailET.error = "Valid Email is required"
            binding.emailET.requestFocus()
            return
        }

        if (mobile.isEmpty() || mobile.length != 10 || !mobile.all { it.isDigit() }) {
            binding.mobileET.error = "Valid Mobile Number is required"
            binding.mobileET.requestFocus()
            return
        }

        if (password.isEmpty() || password.length < 6) {
            binding.passwordET.error = "Password must be at least 6 characters"
            binding.passwordET.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.repasswordET.error = "Passwords do not match"
            binding.repasswordET.requestFocus()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserData(fullName, email, mobile)
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserData(fullName: String, email: String, mobile: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        val user = mapOf(
            "id" to userId,
            "fullName" to fullName,
            "email" to email,
            "mobile" to mobile,
            "profileImageUrl" to null,
            "score" to 0
        )

        userRef.setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else {
                Toast.makeText(this, "Failed to save user data: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account)
            } else {
                Toast.makeText(this, "Failed to retrieve account info", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userRef = database.getReference("users").child(user.uid)

                        userRef.get().addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()) {
                                val newUser = mapOf(
                                    "id" to user.uid,
                                    "fullName" to user.displayName,
                                    "email" to user.email,
                                    "mobile" to user.phoneNumber,
                                    "profileImageUrl" to user.photoUrl?.toString(),
                                    "score" to 0
                                )
                                userRef.setValue(newUser)
                            }
                        }

                        Toast.makeText(this, "Sign-up successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.passwordET.inputType =
            if (isPasswordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.passwordET.setSelection(binding.passwordET.text.length)
    }

    private fun togglePasswordVisibility1() {
        isPasswordVisible = !isPasswordVisible
        binding.repasswordET.inputType =
            if (isPasswordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.repasswordET.setSelection(binding.passwordET.text.length)
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
