package com.example.samarpan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL

class FullScreenMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", 0))
        setContentView(R.layout.activity_full_screen_map)

        mapView = findViewById(R.id.fullScreenMapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val userLat = intent.getDoubleExtra("user_lat", 0.0)
        val userLng = intent.getDoubleExtra("user_lng", 0.0)

        val donorLocation = GeoPoint(latitude, longitude)
        val userLocation = GeoPoint(userLat, userLng)

        // Define the custom icons
        val userIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_home_marker) // User Marker icon
        val resizedUserIcon = Bitmap.createScaledBitmap(userIcon, 80, 80 , false)

        val donorIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_dest_marker) // Donor Marker icon
        val resizedDonorIcon = Bitmap.createScaledBitmap(donorIcon, 50, 80 , false)

        // Donor Location Marker
        val donorMarker = Marker(mapView)
        donorMarker.position = donorLocation
        donorMarker.icon = BitmapDrawable(resources, resizedDonorIcon)
        donorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        donorMarker.title = "Donor Location"
        mapView.overlays.add(donorMarker)

        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val fabBack: FloatingActionButton = findViewById(R.id.fabBack)
        fabBack.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onBackPressedDispatcher.onBackPressed()
        }

        if (userLat != 0.0 && userLng != 0.0) {
            // User Location Marker
            val userMarker = Marker(mapView)
            userMarker.position = userLocation
            userMarker.icon = BitmapDrawable(resources, resizedUserIcon)
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            userMarker.title = "Your Location"
            mapView.overlays.add(userMarker)

            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(donorLocation)

            Log.d("MapDebug", "User: ${userLocation.latitude}, ${userLocation.longitude}")
            Log.d("MapDebug", "Donor: ${donorLocation.latitude}, ${donorLocation.longitude}")

            // Draw route between user and donor
            drawRoute(donorLocation, userLocation)

        } else {
            Log.w("FullScreenMap", "User location not available")
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(donorLocation)
        }

        mapView.invalidate()

        ViewCompat.getWindowInsetsController(window.decorView)?.apply {
            isAppearanceLightStatusBars = false
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            val progressBar = ProgressBar(this)
            loadingDialog = AlertDialog.Builder(this)
                .setTitle("Loading route...")
                .setView(progressBar)
                .setCancelable(false)
                .create()
        }
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun drawRoute(start: GeoPoint, end: GeoPoint) {
        showLoadingDialog()
        if (start.latitude == 0.0 || start.longitude == 0.0 || end.latitude == 0.0 || end.longitude == 0.0) {
            Toast.makeText(this, "Invalid coordinates for routing.", Toast.LENGTH_SHORT).show()
            return
        }
        val url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=5b3ce3597851110001cf6248c120dbeecf954883b4f3e262894a07c1&start=${start.longitude},${start.latitude}&end=${end.longitude},${end.latitude}"

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/geo+json")
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

                runOnUiThread {
                    val polyline = Polyline()
                    polyline.setPoints(roadPoints)
                    polyline.color = resources.getColor(R.color.route_color, null)
                    polyline.width = 8f
                    mapView.overlays.add(polyline)

                    mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(roadPoints), true)
                    mapView.invalidate()
                    hideLoadingDialog()
                }
            } catch (e: Exception) {
                Log.e("RouteError", "Failed to get route", e)
                runOnUiThread {
                    hideLoadingDialog()
                    Toast.makeText(this@FullScreenMapActivity, "No route found!", Toast.LENGTH_SHORT).show()
                    showRetryDialog(start, end)
                }
            }
        }.start()
    }

    private fun showRetryDialog(start: GeoPoint, end: GeoPoint) {
        AlertDialog.Builder(this)
            .setTitle("Route Unavailable")
            .setMessage("Unable to fetch the route. Would you like to try again?")
            .setPositiveButton("Retry") { _, _ ->
                drawRoute(start, end)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
