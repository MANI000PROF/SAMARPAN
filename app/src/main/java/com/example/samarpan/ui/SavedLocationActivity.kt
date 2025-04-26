package com.example.samarpan.ui

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Model.SavedLocation
import com.example.samarpan.R
import com.example.samarpan.adapters.SavedLocationAdapter
import com.example.samarpan.databinding.ActivitySavedLocationBinding

class SavedLocationActivity : AppCompatActivity() {

    // ViewBinding instance
    private lateinit var binding: ActivitySavedLocationBinding

    private lateinit var locationAdapter: SavedLocationAdapter
    private val savedLocationsList = mutableListOf<SavedLocation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySavedLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView
        setupRecyclerView()

        // Back button click listener
        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onBackPressed()
        }

        // Add New Location Button click listener
        binding.addNewLocationLayout.setOnClickListener {
            // Simulate adding a new location
            addNewLocation()
        }
    }

    private fun setupRecyclerView() {
        // Initialize adapter and set it to RecyclerView
        locationAdapter = SavedLocationAdapter(savedLocationsList)
        binding.savedLocationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SavedLocationActivity)
            adapter = locationAdapter
        }

        // For demonstration, adding some dummy data
        savedLocationsList.add(SavedLocation("Location 1", "Address 1"))
        savedLocationsList.add(SavedLocation("Location 2", "Address 2"))
        savedLocationsList.add(SavedLocation("Location 3", "Address 3"))
        locationAdapter.notifyDataSetChanged()
    }

    private fun addNewLocation() {
        // This could open a dialog or another activity for adding a new location
        // For now, just add a dummy location
        savedLocationsList.add(SavedLocation("New Location", "New Address"))
        locationAdapter.notifyItemInserted(savedLocationsList.size - 1)
    }
}
