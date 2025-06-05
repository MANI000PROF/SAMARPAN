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
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.navigation.navOptions
import com.example.samarpan.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val NOTIFICATION_PERMISSION_CODE = 1001
    private lateinit var binding: ActivityMainBinding
    private var currentCategoryId = R.id.homeFragment2

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

        val headerBg = findViewById<ImageView>(R.id.headerBg)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            headerBg.clipToOutline = true
        }

        setupHeaderHeight()
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

            val showChatButton = when (destination.id) {
                R.id.homeFragment2,
                R.id.homeFragmentClothes,
                R.id.homeFragmentElectronics -> true
                else -> false
            }

            val isHomeFragment = destination.id == R.id.homeFragment2 ||
                    destination.id == R.id.homeFragmentClothes ||
                    destination.id == R.id.homeFragmentElectronics

            if (isHomeFragment) {
                showTopHeaderSmooth()
            } else {
                hideTopHeaderSmooth()
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
                .scaleX(1.3f)
                .scaleY(1.3f)
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
                    itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    navController.navigate(R.id.homeFragment2, null, options)
                }
                R.id.historyFragment2 -> if (navController.currentDestination?.id != R.id.historyFragment2) {
                    itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    navController.navigate(R.id.historyFragment2, null, options)
                }
                R.id.searchFragment2 -> if (navController.currentDestination?.id != R.id.searchFragment2) {
                    itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    navController.navigate(R.id.searchFragment2, null, options)
                }
                R.id.profileFragment2 -> if (navController.currentDestination?.id != R.id.profileFragment2) {
                    itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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

    private var originalHeaderHeight = -1
    private var isHeaderVisible = true

    fun hideTopHeaderSmooth() {
        val header = binding.topHeaderContainer
        if (!isHeaderVisible || originalHeaderHeight <= 0) return

        val animator = ValueAnimator.ofInt(header.height, 0)
        animator.addUpdateListener {
            val newHeight = it.animatedValue as Int
            header.layoutParams.height = newHeight
            header.requestLayout()
            header.alpha = newHeight / originalHeaderHeight.toFloat()
        }
        animator.duration = 300
        animator.start()

        isHeaderVisible = false
    }

    fun showTopHeaderSmooth() {
        val header = binding.topHeaderContainer
        if (isHeaderVisible || originalHeaderHeight <= 0) return

        val animator = ValueAnimator.ofInt(0, originalHeaderHeight)
        animator.addUpdateListener {
            val newHeight = it.animatedValue as Int
            header.layoutParams.height = newHeight
            header.requestLayout()
            header.alpha = newHeight / originalHeaderHeight.toFloat()
        }
        animator.duration = 300
        animator.start()

        isHeaderVisible = true
    }

    fun setupHeaderHeight() {
        val header = binding.topHeaderContainer
        if (originalHeaderHeight == -1) {
            header.post {
                originalHeaderHeight = header.height
            }
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

        // Change header background
        val newDrawableId = when (active) {
            R.id.homeFragment2 -> R.drawable.food_bg_5
            R.id.homeFragmentClothes -> R.drawable.clothes_bg_6
            R.id.homeFragmentElectronics -> R.drawable.electronics_bg_1
            else -> R.drawable.food_bg_5
        }

        // Determine direction
        val direction = when {
            currentCategoryId == R.id.homeFragment2 && active == R.id.homeFragmentClothes -> "right"
            currentCategoryId == R.id.homeFragmentClothes && active == R.id.homeFragmentElectronics -> "right"
            currentCategoryId == R.id.homeFragment2 && active == R.id.homeFragmentElectronics -> "right"

            currentCategoryId == R.id.homeFragmentClothes && active == R.id.homeFragment2 -> "left"
            currentCategoryId == R.id.homeFragmentElectronics && active == R.id.homeFragmentClothes -> "left"
            currentCategoryId == R.id.homeFragmentElectronics && active == R.id.homeFragment2 -> "left"

            else -> "right" // fallback
        }

        animateHeaderBgChange(newDrawableId, direction)
    }

    private fun navigateToIfNotCurrent(destinationId: Int) {
        if (navController.currentDestination?.id != destinationId) {
            val (enterAnim, exitAnim, popEnterAnim, popExitAnim) = when {
                currentCategoryId == R.id.homeFragment2 && destinationId == R.id.homeFragmentClothes -> listOf(
                    R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right
                )
                currentCategoryId == R.id.homeFragmentClothes && destinationId == R.id.homeFragment2 -> listOf(
                    R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left
                )
                currentCategoryId == R.id.homeFragmentClothes && destinationId == R.id.homeFragmentElectronics -> listOf(
                    R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right
                )
                currentCategoryId == R.id.homeFragmentElectronics && destinationId == R.id.homeFragmentClothes -> listOf(
                    R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left
                )
                currentCategoryId == R.id.homeFragment2 && destinationId == R.id.homeFragmentElectronics -> listOf(
                    R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right
                )
                currentCategoryId == R.id.homeFragmentElectronics && destinationId == R.id.homeFragment2 -> listOf(
                    R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left
                )
                else -> listOf(0, 0, 0, 0) // fallback
            }

            val options = navOptions {
                launchSingleTop = true
                popUpTo(R.id.homeFragment2) { inclusive = false }
                anim {
                    enter = enterAnim
                    exit = exitAnim
                    popEnter = popEnterAnim
                    popExit = popExitAnim
                }
            }

            navController.navigate(destinationId, null, options)
            currentCategoryId = destinationId
        }
    }

    private fun animateHeaderBgChange(newResId: Int, direction: String) {
        val imageView = binding.headerBg
        val parent = imageView.parent as ViewGroup

        val outTranslation = if (direction == "left") -imageView.width.toFloat() else imageView.width.toFloat()
        val inTranslation = if (direction == "left") imageView.width.toFloat() else -imageView.width.toFloat()

        val oldImage = imageView.drawable

        // Create temporary overlay for old image
        val overlay = androidx.appcompat.widget.AppCompatImageView(this).apply {
            layoutParams = imageView.layoutParams
            setImageDrawable(oldImage)
            translationX = 0f
            alpha = 1f
        }

        // Add overlay BELOW the imageView to avoid flicker
        parent.addView(overlay, parent.indexOfChild(imageView))

        // Enable hardware layers for smooth animation
        ViewCompat.setLayerType(imageView, View.LAYER_TYPE_HARDWARE, null)
        ViewCompat.setLayerType(overlay, View.LAYER_TYPE_HARDWARE, null)

        // Prepare imageView for incoming animation
        imageView.translationX = inTranslation
        imageView.setImageResource(newResId)

        // Animate overlay (old image) out
        overlay.animate()
            .translationX(outTranslation)
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                parent.removeView(overlay)
                // Clear hardware layer after animation
                ViewCompat.setLayerType(imageView, View.LAYER_TYPE_NONE, null)
            }
            .start()

        // Animate imageView (new image) in
        imageView.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
    }


}
