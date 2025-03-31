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
import com.example.samarpan.Model.DonationPostsElectronics
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentPostElectronicsInfoBinding
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

class PostElectronicsInfoFragment : Fragment() {

    private var _binding: FragmentPostElectronicsInfoBinding? = null
    private val binding get() = _binding!!

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var post: DonationPostsElectronics
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var userGeoPoint: GeoPoint? = null
    private var donorGeoPoint: GeoPoint? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostElectronicsInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get post data from arguments
        post = arguments?.getSerializable("post_data") as? DonationPostsElectronics
            ?: return

        Log.d("PostInfoFragment", "Post retrieved: ${post.electronicsTitle}, Lat: ${post.latitude}, Lng: ${post.longitude}")

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
            Log.d("PostElectronicsInfoFragment", "CardView clicked!") // 🔴 Debugging log
            showFullScreenMap()
        }

        // Handle "Request Food" button click
        binding.requestElectronicsBtn.setOnClickListener {
            sendElectronicsRequest()
        }

    }

    private fun sendElectronicsRequest() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to request electronics.", Toast.LENGTH_SHORT).show()
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
                binding.requestElectronicsBtn.isEnabled = false
                binding.requestElectronicsBtn.text = "Request Sent"
                Toast.makeText(requireContext(), "Request sent successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send request. Try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayPostDetails() {
        binding.ProfileName.text = post.profileName ?: "Unknown User"
        binding.Location.text = post.location ?: "Unknown Location"
        binding.electronicsTitle.text = post.electronicsTitle ?: "No Title"
        binding.electronicsDescription.text = post.electronicsDescription ?: "No Description"

        // Load image using Glide
        Glide.with(this)
            .load(post.electronicsImage)
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

        val overlays = mapView.overlays
        overlays.clear() // Clear previous overlays

        // 🔴 Step 1: Add Donor Location Marker
        val donorLocation = GeoPoint(latitude, longitude)
        val donorMarker = Marker(mapView)
        donorMarker.position = donorLocation
        donorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        donorMarker.title = "Food Location"
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
                Log.d("PostElectronicsInfoFragment", "User Location Found: ${userGeoPoint!!.latitude}, ${userGeoPoint!!.longitude}")
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(listOf(donorLocation, userGeoPoint!!)), true)
                drawRoute(donorLocation, userGeoPoint!!)
            } else {
                Log.e("PostElectronicsInfoFragment", "User Location Not Found, Defaulting to Donor Location")
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
        donorMarker.title = "Electronics Location"
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
