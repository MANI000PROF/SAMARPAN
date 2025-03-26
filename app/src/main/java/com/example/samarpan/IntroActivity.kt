package com.example.samarpan

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class IntroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()

        // ✅ Check if user is already logged in
        if (auth.currentUser != null) {
            // User is logged in, redirect to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()  // Close IntroActivity so user can't go back to it
            return  // Exit onCreate early
        }

        setContentView(R.layout.activity_intro)

        // Find buttons
        val signUpButton = findViewById<Button>(R.id.button)
        val loginButton = findViewById<Button>(R.id.button2)

        // Set click listeners
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
