package com.example.samarpan.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.samarpan.R
import com.google.android.material.button.MaterialButton

class ContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        // Toolbar back navigation
        val toolbar = findViewById<Toolbar>(R.id.contactToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onBackPressedDispatcher.onBackPressed()
        }

        // Email button logic
        findViewById<MaterialButton>(R.id.emailButton).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@samarpan.org")
                putExtra(Intent.EXTRA_SUBJECT, "SAMARPAN - Query or Feedback")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }

        // Call button logic
        findViewById<MaterialButton>(R.id.callButton).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+918919698182")
            }
            startActivity(callIntent)
        }
    }
}

