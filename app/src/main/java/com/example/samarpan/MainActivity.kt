package com.example.samarpan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import android.view.View
import com.example.samarpan.Fragment.BottomAlertsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.samarpan.Fragment.MenuFragment

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val NOTIFICATION_PERMISSION_CODE = 1001

    fun checkNotificationPermission() {
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


    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this) // Apply stored theme before setting content view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appName = findViewById<TextView>(R.id.textView4)
        appName.alpha = 0f
        appName.translationY = -30f
        appName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(150)
            .start()

        val iconsLayout = findViewById<LinearLayout>(R.id.categoryIcons)
        iconsLayout.alpha = 0f
        iconsLayout.translationY = 50f
        iconsLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(200)
            .start()

        checkNotificationPermission()

        // Initialize NavController
        navController = findNavController(R.id.fragmentContainerView4)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            highlightCategory(destination.id)
        }

        // Set up BottomNavigationView with NavController
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNav.translationY = 300f
        bottomNav.alpha = 0f
        bottomNav.animate().translationY(0f).alpha(1f).setDuration(500).start()
        bottomNav.setupWithNavController(navController)

        // Handle bottom menu item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment2 -> navController.navigate(R.id.homeFragment2) // Home (Food)
                R.id.historyFragment2 -> navController.navigate(R.id.historyFragment2)
                R.id.leaderBoardFragment2 -> navController.navigate(R.id.leaderBoardFragment2)
                R.id.searchFragment2 -> navController.navigate(R.id.searchFragment2)
                R.id.profileFragment2 -> navController.navigate(R.id.profileFragment2)
            }
            true
        }

        // Handle Alert Button Click (Show Bottom Sheet)
        findViewById<View>(R.id.alertBtn).setOnClickListener {
            val bottomSheet = BottomAlertsFragment()
            bottomSheet.show(supportFragmentManager, "BottomAlertsFragment")
        }

        findViewById<View>(R.id.menuBtn).setOnClickListener {
            val sideMenu = MenuFragment()
            sideMenu.show(supportFragmentManager, "MenuFragment")
        }


        // Category buttons navigation (Keep "Home" highlighted)
        findViewById<View>(R.id.foodBtn).setOnClickListener {
            navController.navigate(R.id.homeFragment2)  // Open HomeFragment (Food)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true // Keep Home highlighted
            highlightCategory(R.id.homeFragment2)
        }
        findViewById<View>(R.id.clothesBtn).setOnClickListener {
            navController.navigate(R.id.homeFragmentClothes) // Open Clothes Fragment
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true // Keep Home highlighted
            highlightCategory(R.id.homeFragmentClothes)
        }
        findViewById<View>(R.id.electronicsBtn).setOnClickListener {
            navController.navigate(R.id.homeFragmentElectronics) // Open Electronics Fragment
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true // Keep Home highlighted
            highlightCategory(R.id.homeFragmentElectronics)
        }
    }
}
