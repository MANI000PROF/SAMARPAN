package com.example.samarpan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SplashScreen : AppCompatActivity() {
    private val typingDelay: Long = 25 // ms per character (can tune)
    private val splashDuration: Long = 3000 // total splash time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val logoImage = findViewById<CardView>(R.id.cardView)
        val taglineView = findViewById<TextView>(R.id.textView)

        // Animate Logo
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_zoom_fade)
        logoImage.startAnimation(logoAnim)

        // Typing effect
        val fullText = taglineView.text.toString()
        taglineView.text = ""
        typeText(taglineView, fullText)

        // Move to next screen after splashDuration
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, IntroActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, splashDuration)
    }

    private fun typeText(textView: TextView, text: String, index: Int = 0) {
        if (index <= text.length) {
            textView.text = text.substring(0, index)
            Handler(Looper.getMainLooper()).postDelayed({
                typeText(textView, text, index + 1)
            }, typingDelay)
        }
    }
}
