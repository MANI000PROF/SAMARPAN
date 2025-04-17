package com.example.samarpan

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.samarpan.Fragment.BottomAlertsFragment
import com.example.samarpan.Fragment.MenuFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val NOTIFICATION_PERMISSION_CODE = 1001
    private lateinit var gestureDetector: GestureDetector

    private val TAG = "SwipeGesture"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            Log.d("SwipeGesture", "Swipe Right")
                            onSwipeRight()
                        } else {
                            Log.d("SwipeGesture", "Swipe Left")
                            onSwipeLeft()
                        }
                        return true
                    }
                }
                return false
            }
        })


        val appName = findViewById<TextView>(R.id.textView4)
        appName.alpha = 0f
        appName.translationY = -30f
        appName.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(150).start()

        val iconsLayout = findViewById<LinearLayout>(R.id.categoryIcons)
        iconsLayout.alpha = 0f
        iconsLayout.translationY = 50f
        iconsLayout.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(200).start()

        checkNotificationPermission()

        navController = findNavController(R.id.fragmentContainerView4)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            highlightCategory(destination.id)
        }

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNav.translationY = 300f
        bottomNav.alpha = 0f
        bottomNav.animate().translationY(0f).alpha(1f).setDuration(500).start()
        bottomNav.setupWithNavController(navController)

        bottomNav.setOnItemSelectedListener { item ->
            // Haptic feedback on click
            bottomNav.findViewById<View>(item.itemId)?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            when (item.itemId) {
                R.id.homeFragment2 -> navController.navigate(R.id.homeFragment2)
                R.id.historyFragment2 -> navController.navigate(R.id.historyFragment2)
                R.id.leaderBoardFragment2 -> navController.navigate(R.id.leaderBoardFragment2)
                R.id.searchFragment2 -> navController.navigate(R.id.searchFragment2)
                R.id.profileFragment2 -> navController.navigate(R.id.profileFragment2)
            }
            true
        }

        findViewById<View>(R.id.alertBtn).setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            BottomAlertsFragment().show(supportFragmentManager, "BottomAlertsFragment")
        }

        findViewById<View>(R.id.menuBtn).setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            MenuFragment().show(supportFragmentManager, "MenuFragment")
        }

        findViewById<View>(R.id.foodBtn).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            navController.navigate(R.id.homeFragment2)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true
            highlightCategory(R.id.homeFragment2)
        }

        findViewById<View>(R.id.clothesBtn).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            navController.navigate(R.id.homeFragmentClothes)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true
            highlightCategory(R.id.homeFragmentClothes)
        }

        findViewById<View>(R.id.electronicsBtn).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            navController.navigate(R.id.homeFragmentElectronics)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true
            highlightCategory(R.id.homeFragmentElectronics)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun highlightCategory(active: Int) {
        val foodBtn = findViewById<ImageView>(R.id.foodBtn)
        val clothesBtn = findViewById<ImageView>(R.id.clothesBtn)
        val electronicsBtn = findViewById<ImageView>(R.id.electronicsBtn)

        val activeColor = ContextCompat.getColor(this, R.color.teal_700)
        val defaultColor = ContextCompat.getColor(this, R.color.colorPrimary)

        foodBtn.setColorFilter(if (active == R.id.homeFragment2) activeColor else defaultColor)
        clothesBtn.setColorFilter(if (active == R.id.homeFragmentClothes) activeColor else defaultColor)
        electronicsBtn.setColorFilter(if (active == R.id.homeFragmentElectronics) activeColor else defaultColor)
    }

    private fun onSwipeLeft() {
        val current = navController.currentDestination?.id
        val options = androidx.navigation.navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        when (current) {
            R.id.homeFragment2 -> {
                navController.navigate(R.id.homeFragmentClothes, null, options)
                highlightCategory(R.id.homeFragmentClothes)
            }
            R.id.homeFragmentClothes -> {
                navController.navigate(R.id.homeFragmentElectronics, null, options)
                highlightCategory(R.id.homeFragmentElectronics)
            }
        }
    }

    private fun onSwipeRight() {
        val current = navController.currentDestination?.id
        val options = androidx.navigation.navOptions {
            anim {
                enter = R.anim.slide_in_left
                exit = R.anim.slide_out_right
                popEnter = R.anim.slide_in_right
                popExit = R.anim.slide_out_left
            }
        }

        when (current) {
            R.id.homeFragmentElectronics -> {
                navController.navigate(R.id.homeFragmentClothes, null, options)
                highlightCategory(R.id.homeFragmentClothes)
            }
            R.id.homeFragmentClothes -> {
                navController.navigate(R.id.homeFragment2, null, options)
                highlightCategory(R.id.homeFragment2)
            }
        }
    }

}
