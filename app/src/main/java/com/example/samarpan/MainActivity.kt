package com.example.samarpan

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Set up NavController and BottomNavigationView
        val navController: NavController = findNavController(R.id.fragmentContainerView4)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNav.setupWithNavController(navController)

        // Set up onClickListeners for the buttons
        findViewById<View>(R.id.foodBtn).setOnClickListener {
            navController.navigate(R.id.homeFragment2) // Replace with the correct destination ID for the food fragment
        }
        findViewById<View>(R.id.clothesBtn).setOnClickListener {
            navController.navigate(R.id.homeFragmentClothes) // Replace with the correct destination ID for the clothes fragment
        }
        findViewById<View>(R.id.electronicsBtn).setOnClickListener {
            navController.navigate(R.id.homeFragmentElectronics) // Replace with the correct destination ID for the electronics fragment
        }

        // Edge-to-Edge Window Insets Handling (optional)
        // ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        //     val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //     v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //     insets
        // }
    }
}
