package com.example.samarpan

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.samarpan.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private val database by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Navigate to LoginActivity
        binding.signInBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Handle Sign Up
        binding.signUpBtn.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val fullName = binding.fullnameET.text.toString().trim()
        val email = binding.emailET.text.toString().trim()
        val mobile = binding.mobileET.text.toString().trim()
        val password = binding.passwordET.text.toString()
        val confirmPassword = binding.repasswordET.text.toString()

        // Input Validation
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

        // Register User with Firebase Authentication
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
            "mobile" to mobile
        )

        userRef.setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                Toast.makeText(this, "Failed to save user data: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
