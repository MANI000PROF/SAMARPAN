package com.example.samarpan.Fragment

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.samarpan.FullScreenImageActivity
import com.example.samarpan.FullScreenMapActivity
import com.example.samarpan.Model.Alert
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentPostInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL

class PostInfoFragment : Fragment() {

    private var _binding: FragmentPostInfoBinding? = null
    private val binding get() = _binding!!

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var post: DonationPosts
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var userGeoPoint: GeoPoint? = null
    private var donorGeoPoint: GeoPoint? = null
    private val locationHandler = Handler(Looper.getMainLooper())

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
        post = arguments?.getSerializable("post_data") as? DonationPosts ?: return

        Log.d("PostInfoFragment", "Post retrieved: ${post.foodTitle}, Lat: ${post.latitude}, Lng: ${post.longitude}")

        latitude = post.latitude ?: 0.0
        longitude = post.longitude ?: 0.0
        donorGeoPoint = GeoPoint(latitude, longitude)

        // Populate UI with post data
        displayPostDetails()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.uid == post.donorId) {
            // If the current user is the donor himself
            binding.requestFoodBtn.isEnabled = false
            binding.requestFoodBtn.text = "Your Post"
            binding.requestFoodBtn.alpha = 0.6f // Make it slightly faded
        }

        // Initialize OpenStreetMap
        mapView = binding.locationMapView
        setupOSMMap()

        // Expand Map on Click
        binding.mapHintText.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            showFullScreenMap()
        }

        val postImageView = view.findViewById<ImageView>(R.id.postImage)

        postImageView.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val intent = Intent(requireContext(), FullScreenImageActivity::class.java)
            intent.putExtra("image_url", post.foodImage) // Replace with your actual image URL
            startActivity(intent)
        }

        // Handle "Request Food" button click
        binding.requestFoodBtn.setOnClickListener {
            Log.d("RequestBtn", "Clicked!")
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            sendFoodRequest()
        }
    }

    private fun sendFoodRequest() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val requesterId = currentUser.uid
        val donorId = post.donorId
        val postId = post.postId

        if (donorId == null || postId == null) return

        val dynamicTitle = "New Request for: ${post.foodTitle ?: "your post"}"
        val dynamicMessage = "${currentUser.displayName ?: "Someone"} is requesting your donation."

        val postImageUrl = post.foodImage ?: "https://default-image-url.com"

        val requestRef = FirebaseDatabase.getInstance().getReference("Requests").push()
        val requestId = requestRef.key // ✅ get the generated requestId

        val alert = Alert(
            postId = postId,
            donorId = donorId,
            requesterId = requesterId,
            requestId = requestId, // ✅ Save the requestId inside the object
            status = "Pending",
            timestamp = System.currentTimeMillis(),
            message = dynamicMessage,
            title = dynamicTitle,
            requesterMessage = "You requested ${post.profileName ?: "Someone"}.",
            requesterTitle = "Your request for: ${post.foodTitle ?: "post"}",
            postImageUrl = postImageUrl
        )

        val donorTokenRef = FirebaseDatabase.getInstance().getReference("users")
            .child(donorId ?: return)
            .child("fcmToken")

        donorTokenRef.get().addOnSuccessListener { snapshot ->
            val receiverFcmToken = snapshot.getValue(String::class.java)
            if (!receiverFcmToken.isNullOrEmpty()) {
                lifecycleScope.launch {
                    val accessToken = FirebaseAccessToken.getAccessToken(requireContext())
                    accessToken?.let {
                        sendPushNotification(it, receiverFcmToken, dynamicTitle, dynamicMessage)
                    }
                }
            }
        }



        requestRef.setValue(alert)
            .addOnSuccessListener {
                binding.requestFoodBtn.isEnabled = false
                binding.requestFoodBtn.text = "Request Sent"
                Toast.makeText(requireContext(), "Your request has been sent successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send request. Please try again later.", Toast.LENGTH_SHORT).show()
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

    private fun sendPushNotification(
        accessToken: String,
        fcmToken: String,
        title: String,
        message: String
    )
    {
        val context = requireContext()
        val projectId = "samarpan-42c86" // 🔁 Replace with your actual project ID

        val json = JSONObject()
        val messageObj = JSONObject()
        val notificationObj = JSONObject()

        notificationObj.put("title", title)
        notificationObj.put("body", message)

        messageObj.put("token", fcmToken)
        messageObj.put("notification", notificationObj)
        json.put("message", messageObj)

        val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        val request = object : JsonObjectRequest(
            Method.POST, url, json,
            Response.Listener { response -> Log.d("FCM", "Push sent: $response") },
            Response.ErrorListener { error -> Log.e("FCM", "Error: ${error.message}") }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            }
        }
        Volley.newRequestQueue(context).add(request)
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
        donorMarker.title = "Food Location"
        overlays.add(donorMarker)

        // 🔴 Step 2: Setup Location Provider with a Delay for Accuracy
        val locationProvider = GpsMyLocationProvider(requireContext())
        myLocationOverlay = MyLocationNewOverlay(locationProvider, mapView)
        myLocationOverlay.enableMyLocation()
        overlays.add(myLocationOverlay)

        // 🔴 Step 3: Wait for Accurate Location Update
        locationHandler.postDelayed({
            if (!isAdded || _binding == null) return@postDelayed

            userGeoPoint = myLocationOverlay.myLocation
            if (userGeoPoint != null && userGeoPoint!!.latitude != 0.0 && userGeoPoint!!.longitude != 0.0) {
                Log.d("PostInfoFragment", "User Location Found: ${userGeoPoint!!.latitude}, ${userGeoPoint!!.longitude}")
                mapView.zoomToBoundingBox(
                    BoundingBox.fromGeoPoints(listOf(donorGeoPoint!!, userGeoPoint!!)), true
                )
                drawRoute(donorGeoPoint!!, userGeoPoint!!)
            } else {
                Log.e("PostInfoFragment", "User Location Not Found, Defaulting to Donor Location")
                Toast.makeText(requireContext(), "Unable to fetch current location!", Toast.LENGTH_SHORT).show()
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(donorGeoPoint)
            }
            mapView.invalidate()
        }, 4000)
        // Wait 4 seconds to get a more accurate location
    }


    private fun drawRoute(start: GeoPoint, end: GeoPoint) {
        val url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=5b3ce3597851110001cf6248c120dbeecf954883b4f3e262894a07c1&start=${start.longitude},${start.latitude}&end=${end.longitude},${end.latitude}"

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().use { it.readText() }

                val json = JSONObject(response)
                val coordinates = json
                    .getJSONArray("features")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")

                val roadPoints = mutableListOf<GeoPoint>()
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    roadPoints.add(GeoPoint(lat, lon))
                }

                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread  // ✅ check if view is safe to access

                    val polyline = Polyline()
                    polyline.setPoints(roadPoints)
                    polyline.color = resources.getColor(R.color.route_color, null)
                    polyline.width = 8f
                    mapView.overlays.add(polyline)
                    mapView.invalidate()
                }

            } catch (e: Exception) {
                Log.e("RouteError", "Failed to get route", e)
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    Toast.makeText(requireContext(), "No route found!", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    private fun showFullScreenMap() {
        val intent = Intent(requireContext(), FullScreenMapActivity::class.java)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        userGeoPoint?.let {
            intent.putExtra("user_lat", it.latitude)
            intent.putExtra("user_lng", it.longitude)
        }
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        locationHandler.removeCallbacksAndMessages(null) // ✅ cancel pending tasks
        _binding = null
    }

}
