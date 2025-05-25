package com.example.samarpan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedMarker: Marker? = null
    private var selectedGeoPoint: GeoPoint? = null

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getCurrentLocation()
            } else {
                Snackbar.make(findViewById(R.id.main), "Location permission denied.", Snackbar.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_location_picker)

        // Status bar transparent
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false
        window.statusBarColor = Color.TRANSPARENT

        mapView = findViewById(R.id.osmMapView)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupButtons()
        requestLocationPermission()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 4.0
        mapView.maxZoomLevel = 20.0
        mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

        val defaultPoint = GeoPoint(20.5937, 78.9629) // India center
        mapView.controller.setZoom(5.0)
        mapView.controller.setCenter(defaultPoint)

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                addMarker(geoPoint)
                return true
            }
        })

        mapView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupButtons() {
        findViewById<FloatingActionButton>(R.id.fabZoomIn).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            mapView.controller.zoomIn()
        }

        findViewById<FloatingActionButton>(R.id.fabZoomOut).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            mapView.controller.zoomOut()
        }

        findViewById<FloatingActionButton>(R.id.fabCenter).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            getCurrentLocation()
        }

        findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        findViewById<TextView>(R.id.btnConfirmLocation).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            selectedGeoPoint?.let { geoPoint ->
                val intent = Intent().apply {
                    putExtra("latitude", geoPoint.latitude)
                    putExtra("longitude", geoPoint.longitude)
                }
                setResult(Activity.RESULT_OK, intent)
            } ?: setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun requestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentPoint = GeoPoint(it.latitude, it.longitude)
                animateToLocation(currentPoint)
                addMarker(currentPoint)
            }
        }
    }

    private fun animateToLocation(point: GeoPoint) {
        mapView.controller.animateTo(point)
        mapView.controller.setZoom(17.0) // Smoothly zoom-in
        mapView.invalidate()
    }

    private fun addMarker(point: GeoPoint) {
        selectedMarker?.let { mapView.overlays.remove(it) }

        selectedMarker = Marker(mapView).apply {
            position = point
            title = "Selected Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        selectedGeoPoint = point
        mapView.overlays.add(selectedMarker)
        mapView.invalidate()
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
