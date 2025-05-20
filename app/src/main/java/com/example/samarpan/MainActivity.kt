package com.example.samarpan

import android.os.Bundle
import android.util.Log
import android.view.*
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
import androidx.navigation.navOptions
import com.example.samarpan.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val NOTIFICATION_PERMISSION_CODE = 1001
    private lateinit var gestureDetector: GestureDetector
    private lateinit var binding: ActivityMainBinding
    private val TAG = "SwipeGesture"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(currentUserId)
                        .child("fcmToken")
                        .setValue(token)
                }
            }
        }

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


        val appName = binding.textView4
        appName.alpha = 0f
        appName.translationY = -30f
        appName.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(150).start()

        val iconsLayout = binding.categoryIcons
        val menuBtn = binding.menuBtn
        val alertBtn = binding.alertBtn

        iconsLayout.alpha = 0f
        iconsLayout.translationY = 50f
        iconsLayout.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(200).start()

        checkNotificationPermission()

        navController = findNavController(R.id.fragmentContainerView4)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            highlightCategory(destination.id)

            val showIcons = when (destination.id) {
                R.id.homeFragment2,
                R.id.homeFragmentClothes,
                R.id.homeFragmentElectronics -> true
                else -> false
            }
            val showChatButton = when (destination.id) {
                R.id.homeFragment2,
                R.id.homeFragmentClothes,
                R.id.homeFragmentElectronics -> true
                else -> false
            }

            if (showIcons) {
                if (iconsLayout.visibility != View.VISIBLE) {
                    iconsLayout.visibility = View.VISIBLE
                    iconsLayout.animate().alpha(1f).translationY(0f).setDuration(300).start()
                }
                if (appName.visibility != View.VISIBLE) {
                    appName.visibility = View.VISIBLE
                    appName.animate().alpha(1f).translationY(0f).setDuration(300).start()
                }
                if (menuBtn.visibility != View.VISIBLE) {
                    menuBtn.visibility = View.VISIBLE
                    menuBtn.animate().alpha(1f).translationY(0f).setDuration(300).start()
                }
                if (alertBtn.visibility != View.VISIBLE) {
                    alertBtn.visibility = View.VISIBLE
                    alertBtn.animate().alpha(1f).translationY(0f).setDuration(300).start()
                }

            } else {
                iconsLayout.animate().alpha(0f).translationY(-iconsLayout.height.toFloat()).setDuration(300)
                    .withEndAction { iconsLayout.visibility = View.GONE }.start()

                appName.animate().alpha(0f).translationY(-30f).setDuration(300)
                    .withEndAction { appName.visibility = View.GONE }.start()

                menuBtn.animate().alpha(0f).translationY(-30f).setDuration(300)
                    .withEndAction { menuBtn.visibility = View.GONE }.start()

                alertBtn.animate().alpha(0f).translationY(-30f).setDuration(300)
                    .withEndAction { alertBtn.visibility = View.GONE }.start()
            }

            // Control visibility of chat button based on the fragment
            val chatBtn = binding.chatBtn
            if (showChatButton) {
                if (chatBtn.visibility != View.VISIBLE) {
                    chatBtn.visibility = View.VISIBLE
                    chatBtn.animate().alpha(1f).translationY(0f).setDuration(300).start()
                }
            } else {
                chatBtn.animate().alpha(0f).translationY(-30f).setDuration(300)
                    .withEndAction { chatBtn.visibility = View.GONE }.start()
            }
        }

        val bottomNav: BottomNavigationView = binding.bottomNavigationView
        bottomNav.translationY = 300f
        bottomNav.alpha = 0f
        bottomNav.animate().translationY(0f).alpha(1f).setDuration(500).start()
        bottomNav.setupWithNavController(navController)

        bottomNav.setOnItemSelectedListener { item ->

            val menuView = binding.bottomNavigationView.getChildAt(0) as ViewGroup
            val itemView = menuView.getChildAt(menuIndexForItem(item.itemId))

            val iconView = (itemView as ViewGroup).getChildAt(0)

            iconView.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction {
                    iconView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
                .start()

            val options = navOptions {
                launchSingleTop = true
                popUpTo(R.id.homeFragment2) { inclusive = false }
            }

            when (item.itemId) {
                R.id.homeFragment2 -> if (navController.currentDestination?.id != R.id.homeFragment2) {
                    navController.navigate(R.id.homeFragment2, null, options)
                }
                R.id.historyFragment2 -> if (navController.currentDestination?.id != R.id.historyFragment2) {
                    navController.navigate(R.id.historyFragment2, null, options)
                }
                R.id.searchFragment2 -> if (navController.currentDestination?.id != R.id.searchFragment2) {
                    navController.navigate(R.id.searchFragment2, null, options)
                }
                R.id.profileFragment2 -> if (navController.currentDestination?.id != R.id.profileFragment2) {
                    navController.navigate(R.id.profileFragment2, null, options)
                }
            }
            true
        }


        binding.fabLeaderboardIcon.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val options = navOptions {
                launchSingleTop = true
                popUpTo(R.id.homeFragment2) { inclusive = false }
            }
            navController.navigate(R.id.leaderBoardFragment2, null, options)
            binding.bottomNavigationView.selectedItemId = R.id.leaderBoardFragment2
        }

        binding.chatBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            navController.navigate(R.id.chatFragment2)
        }

        alertBtn.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            BottomAlertsFragment().show(supportFragmentManager, "BottomAlertsFragment")
        }

        menuBtn.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            MenuFragment().show(supportFragmentManager, "MenuFragment")
        }

        binding.foodBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            navigateToIfNotCurrent(R.id.homeFragment2)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true
            highlightCategory(R.id.homeFragment2)
        }

        binding.clothesBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            navigateToIfNotCurrent(R.id.homeFragmentClothes)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true
            highlightCategory(R.id.homeFragmentClothes)
        }

        binding.electronicsBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            navigateToIfNotCurrent(R.id.homeFragmentElectronics)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true
            highlightCategory(R.id.homeFragmentElectronics)
        }
    }
    private fun menuIndexForItem(itemId: Int): Int {
        return when(itemId) {
            R.id.homeFragment2 -> 0
            R.id.historyFragment2 -> 1
            R.id.leaderBoardFragment2 -> 2
            R.id.searchFragment2 -> 3
            R.id.profileFragment2 -> 4
            else -> 0
        }
    }
    override fun onResume() {
        super.onResume()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .child("isOnline").setValue(true)
        }
    }

    override fun onPause() {
        super.onPause()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .child("isOnline").setValue(false)
        }
    }

    fun setCategoryIconsVisibility(show: Boolean) {
        val iconsLayout = binding.categoryIcons ?: return

        Log.d("CategoryIcons", "setCategoryIconsVisibility called with show = $show")

        val isVisible = iconsLayout.visibility == View.VISIBLE

        if (show && !isVisible) {
            iconsLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .withStartAction { iconsLayout.visibility = View.VISIBLE }
                .start()
        } else if (!show && isVisible) {
            iconsLayout.animate()
                .alpha(0f)
                .translationY(-iconsLayout.height.toFloat())
                .setDuration(300)
                .withEndAction { iconsLayout.visibility = View.GONE }
                .start()
        }
    }
    fun updateAlertAnimation(hasAlerts: Boolean) {
        val alertBtn = binding.alertBtn
        if (hasAlerts) {
            alertBtn.loop(true)
            alertBtn.playAnimation()
        } else {
            alertBtn.pauseAnimation()
            alertBtn.progress = 0f // Reset to start frame
            alertBtn.repeatCount = 0
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
        val foodBtn = binding.foodBtn
        val clothesBtn = binding.clothesBtn
        val electronicsBtn = binding.electronicsBtn

        val activeColor = ContextCompat.getColor(this, R.color.teal_700)
        val defaultColor = ContextCompat.getColor(this, R.color.colorPrimary)

        foodBtn.setColorFilter(if (active == R.id.homeFragment2) activeColor else defaultColor)
        clothesBtn.setColorFilter(if (active == R.id.homeFragmentClothes) activeColor else defaultColor)
        electronicsBtn.setColorFilter(if (active == R.id.homeFragmentElectronics) activeColor else defaultColor)
    }

    private fun onSwipeLeft() {
        val current = navController.currentDestination?.id
        val options = navOptions {
            launchSingleTop = true
            popUpTo(R.id.homeFragment2) { inclusive = false } // Update popUpTo as needed
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
        if (current == R.id.homeFragmentElectronics) {
            // maybe show a toast or visual feedback it's the last category
            return
        }
        when (current) {
            R.id.homeFragment2 -> {
                if (current != R.id.homeFragmentClothes) {
                    navController.navigate(R.id.homeFragmentClothes, null, options)
                }
                highlightCategory(R.id.homeFragmentClothes)
            }
            R.id.homeFragmentClothes -> {
                if (navController.currentDestination?.id != R.id.homeFragmentElectronics) {
                    navController.navigate(R.id.homeFragmentElectronics, null, options)
                }
                highlightCategory(R.id.homeFragmentElectronics)
            }

        }
    }

    private fun onSwipeRight() {
        val current = navController.currentDestination?.id
        val options = navOptions {
            launchSingleTop = true
            anim {
                enter = R.anim.slide_in_left
                exit = R.anim.slide_out_right
                popEnter = R.anim.slide_in_right
                popExit = R.anim.slide_out_left
            }
        }
        if (current == R.id.homeFragment2) {
            // maybe show a toast or visual feedback it's the last category
            return
        }

        when (current) {
            R.id.homeFragmentElectronics -> {
                if (current != R.id.homeFragmentClothes) {
                    navController.navigate(R.id.homeFragmentClothes, null, options)
                }
                highlightCategory(R.id.homeFragmentClothes)
            }
            R.id.homeFragmentClothes -> {
                if (current != R.id.homeFragment2) {
                    navController.navigate(R.id.homeFragment2, null, options)
                }
                highlightCategory(R.id.homeFragment2)
            }
        }
    }

    private fun navigateToIfNotCurrent(destinationId: Int) {
        if (navController.currentDestination?.id != destinationId) {
            val options = navOptions {
                launchSingleTop = true
                popUpTo(R.id.homeFragment2) { inclusive = false } // Update popUpTo as needed
            }
            navController.navigate(destinationId, null, options)
        }
    }
}
