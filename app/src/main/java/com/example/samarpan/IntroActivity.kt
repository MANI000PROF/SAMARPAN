package com.example.samarpan

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class IntroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var viewPager: ViewPager2
    private lateinit var introAdapter: IntroPagerAdapter
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_intro)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        introAdapter = IntroPagerAdapter(this)
        viewPager.adapter = introAdapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        findViewById<Button>(R.id.button).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        findViewById<Button>(R.id.button2).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
