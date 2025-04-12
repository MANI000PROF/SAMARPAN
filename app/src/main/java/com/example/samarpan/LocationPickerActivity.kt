package com.example.samarpan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var selectedMarker: Marker? = null
    private var selectedGeoPoint: GeoPoint? = null  // Stores the selected location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)
        val topAppBar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        // Initialize OSM settings
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        mapView = findViewById(R.id.osmMapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Default location (India)
        val startPoint = GeoPoint(20.5937, 78.9629)
        mapView.controller.setZoom(5.0)
        mapView.controller.setCenter(startPoint)

        // Set marker on map click
        mapView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val geoPoint = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                addMarker(geoPoint)
            }
            false
        }

        // Confirm Location Button
        findViewById<Button>(R.id.btnConfirmLocation).setOnClickListener {
            selectedGeoPoint?.let {
                val intent = Intent().apply {
                    putExtra("latitude", it.latitude)
                    putExtra("longitude", it.longitude)
                }
                setResult(Activity.RESULT_OK, intent)
            } ?: setResult(Activity.RESULT_CANCELED)

            finish()
        }
    }

    private fun addMarker(point: GeoPoint) {
        selectedMarker?.let { mapView.overlays.remove(it) }  // Remove previous marker

        selectedMarker = Marker(mapView).apply {
            position = point
            title = "Selected Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        selectedGeoPoint = point  // Store the selected location
        mapView.overlays.add(selectedMarker)
        mapView.invalidate()
    }
}
