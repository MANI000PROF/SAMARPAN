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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkNotificationPermission()

        // Initialize NavController
        navController = findNavController(R.id.fragmentContainerView4)

        // Set up BottomNavigationView with NavController
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
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

        // Category buttons navigation (Keep "Home" highlighted)
        findViewById<View>(R.id.foodBtn).setOnClickListener {
            navController.navigate(R.id.homeFragment2)  // Open HomeFragment (Food)
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true // Keep Home highlighted
        }
        findViewById<View>(R.id.clothesBtn).setOnClickListener {
            navController.navigate(R.id.homeFragmentClothes) // Open Clothes Fragment
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true // Keep Home highlighted
        }
        findViewById<View>(R.id.electronicsBtn).setOnClickListener {
            navController.navigate(R.id.homeFragmentElectronics) // Open Electronics Fragment
            bottomNav.menu.findItem(R.id.homeFragment2).isChecked = true // Keep Home highlighted
        }
    }
}
