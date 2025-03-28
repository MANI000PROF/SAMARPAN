package com.example.samarpan.Fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentPostInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class PostInfoFragment : Fragment() {

    private var _binding: FragmentPostInfoBinding? = null
    private val binding get() = _binding!!

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var post: DonationPosts
    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get post data from arguments
        post = arguments?.getSerializable("post_data") as? DonationPosts
            ?: return

        Log.d("PostInfoFragment", "Post retrieved: ${post.foodTitle}, Lat: ${post.latitude}, Lng: ${post.longitude}")

        latitude = post.latitude ?: 0.0
        longitude = post.longitude ?: 0.0

        // Populate UI with post data
        displayPostDetails()

        // Initialize OpenStreetMap
        mapView = binding.locationMapView
        setupOSMMap()

        // Expand Map on Click
        binding.cardView3.setOnClickListener {
            Log.d("PostInfoFragment", "CardView clicked!") // 🔴 Debugging log
            showFullScreenMap()
        }

        // Handle "Request Food" button click
        binding.requestFoodBtn.setOnClickListener {
            sendFoodRequest()
        }

    }

    private fun sendFoodRequest() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to request food.", Toast.LENGTH_SHORT).show()
            return
        }

        val requesterId = currentUser.uid
        val donorId = post.donorId ?: return
        val postId = post.postId ?: return

        val requestData = hashMapOf(
            "postId" to postId,
            "donorId" to donorId,
            "requesterId" to requesterId,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        val requestRef = FirebaseDatabase.getInstance().getReference("Requests").push()
        requestRef.setValue(requestData)
            .addOnSuccessListener {
                binding.requestFoodBtn.isEnabled = false
                binding.requestFoodBtn.text = "Request Sent"
                Toast.makeText(requireContext(), "Request sent successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send request. Try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayPostDetails() {
        binding.ProfileName.text = post.profileName ?: "Unknown User"
        binding.Location.text = post.location ?: "Unknown Location"
        binding.foodTitle.text = post.foodTitle ?: "No Title"
        binding.foodDescription.text = post.foodDescription ?: "No Description"

        // Load image using Glide
        Glide.with(this)
            .load(post.foodImage)
            .placeholder(R.drawable.placeholder)
            .into(binding.postImage)
    }

    private fun setupOSMMap() {
        // Configure OSM
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", 0))

        // Set Tile Source
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // 🔴 Debugging Logs
        Log.d("PostInfoFragment", "setupOSMMap called. Latitude: $latitude, Longitude: $longitude")

        if (latitude != 0.0 && longitude != 0.0) {
            val geoPoint = GeoPoint(latitude, longitude)

            Log.d("PostInfoFragment", "Setting marker at: $geoPoint") // 🔴 Debugging Log

            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(geoPoint)

            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Food Location"
            mapView.overlays.add(marker)
            mapView.invalidate()
        } else {
            Log.e("PostInfoFragment", "Invalid location: Latitude = $latitude, Longitude = $longitude") // 🔴 Debugging Log
        }
    }


    private fun showFullScreenMap() {
        Log.d("PostInfoFragment", "Opening full-screen map...") // 🔴 Debugging log

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_map)

        val fullScreenMapView = dialog.findViewById<MapView>(R.id.fullScreenMapView)
        fullScreenMapView.setTileSource(TileSourceFactory.MAPNIK)
        fullScreenMapView.setMultiTouchControls(true)

        // Ensure correct location
        val geoPoint = GeoPoint(latitude, longitude)

        Log.d("PostInfoFragment", "Full-screen map location: Latitude = $latitude, Longitude = $longitude") // 🔴 Debugging log

        fullScreenMapView.controller.setZoom(15.0)
        fullScreenMapView.controller.setCenter(geoPoint)

        // Add marker
        val marker = Marker(fullScreenMapView)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Food Location"
        fullScreenMapView.overlays.add(marker)
        fullScreenMapView.invalidate()

        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
