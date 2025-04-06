package com.example.samarpan.Fragment

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.samarpan.Model.Alert
import com.example.samarpan.Model.DonationPostsClothes
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentPostClothesInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class PostClothesInfoFragment : Fragment() {

    private var _binding: FragmentPostClothesInfoBinding? = null
    private val binding get() = _binding!!

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var post: DonationPostsClothes
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var userGeoPoint: GeoPoint? = null
    private var donorGeoPoint: GeoPoint? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("PostClothesInfoFragment", "onCreateView called!") // 🔴 Debugging Log
        _binding = FragmentPostClothesInfoBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("PostClothesInfoFragment", "onViewCreated called!") // 🔴 Debugging Log

        // Get post data from arguments
        post = arguments?.getSerializable("post_data") as? DonationPostsClothes
            ?: return

        Log.d("PostClothesInfoFragment", "Post retrieved: ${post.clothesTitle}, Lat: ${post.latitude}, Lng: ${post.longitude}")

        latitude = post.latitude ?: 0.0
        longitude = post.longitude ?: 0.0
        donorGeoPoint = GeoPoint(latitude, longitude)

        // Populate UI with post data
        displayPostDetails()

        // Initialize OpenStreetMap
        mapView = binding.locationMapView
        setupOSMMap()

        // Expand Map on Click
        binding.cardView3.setOnClickListener {
            Log.d("PostClothesInfoFragment", "CardView clicked!") // 🔴 Debugging log
            showFullScreenMap()
        }

        // Handle "Request Food" button click
        binding.requestClothesBtn.setOnClickListener {
            sendClothesRequest()
        }

    }

    private fun sendClothesRequest() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val requesterId = currentUser.uid
        val donorId = post.donorId
        val postId = post.postId

        if (donorId == null || postId == null) return

        val alert = post.clothesImage?.let {
            Alert(
                postId = postId,
                donorId = donorId,
                requesterId = requesterId,
                status = "Pending", // Capitalize for consistency
                timestamp = System.currentTimeMillis(),
                message = "You have a new request on your post.",
                title = "New Request",
                postImageUrl = it
            )
        }

        val requestRef = FirebaseDatabase.getInstance().getReference("Requests").push()
        requestRef.setValue(alert)
            .addOnSuccessListener {
                binding.requestClothesBtn.isEnabled = false
                binding.requestClothesBtn.text = "Request Sent"
                Toast.makeText(requireContext(), "Request sent successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send request. Try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayPostDetails() {
        binding.ProfileName.text = post.profileName ?: "Unknown User"
        binding.Location.text = post.location ?: "Unknown Location"
        binding.clothesTitle.text = post.clothesTitle ?: "No Title"
        binding.clothesDescription.text = post.clothesDescription ?: "No Description"

        // Force refresh UI
        binding.root.invalidate()

        // Load image using Glide
        Glide.with(this)
            .load(post.clothesImage)
            .placeholder(R.drawable.placeholder)
            .into(binding.postImage)
    }

    private fun setupOSMMap() {
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", 0))

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val overlays = mapView.overlays
        overlays.clear() // Clear previous overlays

        // 🔴 Step 1: Add Donor Location Marker
        val donorLocation = GeoPoint(latitude, longitude)
        val donorMarker = Marker(mapView)
        donorMarker.position = donorLocation
        donorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        donorMarker.title = "Clothes Location"
        overlays.add(donorMarker)

        // 🔴 Step 2: Setup Location Provider with a Delay for Accuracy
        val locationProvider = GpsMyLocationProvider(requireContext())
        myLocationOverlay = MyLocationNewOverlay(locationProvider, mapView)
        myLocationOverlay.enableMyLocation()
        overlays.add(myLocationOverlay)

        // 🔴 Step 3: Wait for Accurate Location Update
        Handler(Looper.getMainLooper()).postDelayed({
            userGeoPoint = myLocationOverlay.myLocation
            if (userGeoPoint != null && userGeoPoint!!.latitude != 0.0 && userGeoPoint!!.longitude != 0.0) {
                Log.d("PostClothesInfoFragment", "User Location Found: ${userGeoPoint!!.latitude}, ${userGeoPoint!!.longitude}")
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(listOf(donorLocation, userGeoPoint!!)), true)
                drawRoute(donorLocation, userGeoPoint!!)
            } else {
                Log.e("PostClothesInfoFragment", "User Location Not Found, Defaulting to Donor Location")
                Toast.makeText(requireContext(), "Unable to fetch current location!", Toast.LENGTH_SHORT).show()
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(donorLocation)
            }
            mapView.invalidate()
        }, 4000) // Wait 4 seconds to get a more accurate location
    }

    private fun drawRoute(start: GeoPoint, end: GeoPoint) {
        val polyline = Polyline()
        polyline.addPoint(start)
        polyline.addPoint(end)
        polyline.color = resources.getColor(R.color.route_color, null)
        polyline.width = 8f

        mapView.overlays.add(polyline)
        mapView.invalidate()
    }


    private fun showFullScreenMap() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_map)

        val fullScreenMapView = dialog.findViewById<MapView>(R.id.fullScreenMapView)
        fullScreenMapView.setTileSource(TileSourceFactory.MAPNIK)
        fullScreenMapView.setMultiTouchControls(true)

        val donorLocation = GeoPoint(latitude, longitude)
        fullScreenMapView.controller.setZoom(15.0)
        fullScreenMapView.controller.setCenter(donorLocation)

        // Add donor marker
        val donorMarker = Marker(fullScreenMapView)
        donorMarker.position = donorLocation
        donorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        donorMarker.title = "Clothes Location"
        fullScreenMapView.overlays.add(donorMarker)

        // Add user marker (if available)
        if (userGeoPoint != null) {
            val userMarker = Marker(fullScreenMapView)
            userMarker.position = userGeoPoint!!
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            userMarker.title = "Your Location"
            fullScreenMapView.overlays.add(userMarker)

            drawRoute(donorLocation, userGeoPoint!!)
        }

        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
