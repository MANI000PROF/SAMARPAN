package com.example.samarpan

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {
    private val typingDelay: Long = 35
    private lateinit var textView: TextView
    private lateinit var logoHand: ImageView
    private lateinit var logoHeart: ImageView

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        textView = findViewById(R.id.tagLine)
        logoHand = findViewById(R.id.logoHand)
        logoHeart = findViewById(R.id.logoHeart)

        window.setDecorFitsSystemWindows(false)

        // Initial states
        logoHand.alpha = 0f
        logoHeart.alpha = 0f
        logoHeart.scaleX = 0.5f
        logoHeart.scaleY = 0.5f

        // Step 1: Fade in hand
        logoHand.animate()
            .alpha(1f)
            .setDuration(700)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Step 2: Pop in heart
                logoHeart.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        // Step 3: Start typing tagline
                        val fullText = "Supporting Assistance Through Meals And Resources for People Across Needs"
                        animateTyping(textView, fullText)
                    }
                    .start()
            }
            .start()
    }

    private fun animateTyping(textView: TextView, text: String, index: Int = 0) {
        if (index <= text.length) {
            textView.text = text.substring(0, index)
            textView.postDelayed({
                animateTyping(textView, text, index + 1)
            }, typingDelay)
        } else {
            // Optional: Delay then move to next activity WITHOUT fading views
            textView.postDelayed({
                startActivity(Intent(this@SplashScreen, IntroActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }, 600) // Wait for a moment before moving
        }
    }
}
