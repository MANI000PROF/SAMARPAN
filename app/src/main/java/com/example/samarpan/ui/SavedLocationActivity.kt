package com.example.samarpan.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Adapter.SavedLocationAdapter
import com.example.samarpan.LocationPickerActivity
import com.example.samarpan.Model.SavedLocation
import com.example.samarpan.databinding.ActivitySavedLocationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SavedLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedLocationBinding
    private lateinit var databaseRef: DatabaseReference
    private lateinit var locationAdapter: SavedLocationAdapter
    private val savedLocationsList = mutableListOf<SavedLocation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseRef = FirebaseDatabase.getInstance().reference
            .child("savedLocations")
            .child(FirebaseAuth.getInstance().uid ?: "")


        setupRecyclerView()
        fetchSavedLocations()

        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onBackPressed()
        }

        binding.selectLocationButton.setOnClickListener {
            locationPickerLauncher.launch(Intent(this, LocationPickerActivity::class.java))
        }
    }
    private val locationPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val lat = result.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                val lon = result.data?.getDoubleExtra("longitude", 0.0) ?: 0.0

                // Step 1: Ask for a custom name
                val input = EditText(this)
                input.hint = "Enter location name"
                input.maxLines = 1

                AlertDialog.Builder(this)
                    .setTitle("Save Location")
                    .setView(input)
                    .setPositiveButton("Save") { dialog, _ ->
                        val name = input.text.toString().ifBlank { "Unnamed Location" }
                        saveLocationToFirebase(name, lat, lon)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    private fun setPrimaryAddress(location: SavedLocation) {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val userRef = FirebaseDatabase.getInstance().reference.child("savedLocations").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            val isCurrentlyPrimary = location.primary
            val newPrimaryState = !isCurrentlyPrimary

            for (locSnap in snapshot.children) {
                val key = locSnap.key ?: continue
                val isThisOne = key == location.key

                // Set only the clicked one to newPrimaryState, others to false
                userRef.child(key).child("primary").setValue(if (isThisOne) newPrimaryState else false)
            }
            fetchSavedLocations()
        }
    }

    private fun editAddress(location: SavedLocation) {
        val input = EditText(this)
        input.setText(location.name)

        AlertDialog.Builder(this)
            .setTitle("Edit Location Name")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val updatedName = input.text.toString()
                val uid = FirebaseAuth.getInstance().uid ?: return@setPositiveButton
                val userRef = FirebaseDatabase.getInstance().reference
                    .child("savedLocations").child(uid).child(location.key)

                userRef.child("name").setValue(updatedName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun deleteAddress(location: SavedLocation) {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val userRef = FirebaseDatabase.getInstance().reference
            .child("savedLocations").child(uid).child(location.key)

        userRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Location deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveLocationToFirebase(name: String, lat: Double, lon: Double) {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val locationKey = databaseRef.push().key ?: return

        // Reverse geocoding to get address
        val geocoder = Geocoder(this)
        var address = ""
        try {
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                address = addresses[0].getAddressLine(0) ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val location = SavedLocation(
            name = name,
            latitude = lat,
            longitude = lon,
            address = address,
            key = locationKey,
            primary = false
        )

        databaseRef.child(locationKey).setValue(location)
            .addOnSuccessListener {
                Toast.makeText(this, "Location saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        locationAdapter = SavedLocationAdapter(
            savedLocationsList,
            onEdit = { editAddress(it) },
            onDelete = { deleteAddress(it) },
            onSetPrimary = { setPrimaryAddress(it) }
        )
        binding.savedLocationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.savedLocationsRecyclerView.adapter = locationAdapter
    }

    private fun fetchSavedLocations() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                savedLocationsList.clear()
                for (locationSnap in snapshot.children) {
                    val location = locationSnap.getValue(SavedLocation::class.java)
                    location?.let {
                        savedLocationsList.add(it.copy(key = locationSnap.key ?: ""))
                    }
                }
                locationAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SavedLocationActivity, "Failed to fetch locations", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
