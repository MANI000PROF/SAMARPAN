package com.example.samarpan.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.samarpan.Model.DonationPost
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentPostInfoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

class PostInfoFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var _binding: FragmentPostInfoBinding? = null
    private val binding get() = _binding!!

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var post: DonationPost

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
        post = arguments?.getSerializable("post_data") as? DonationPost
            ?: run {
                Snackbar.make(binding.root, "Error: Post data is missing.", Snackbar.LENGTH_LONG)
                    .show()
                return
            }

        latitude = post.latitude ?: 0.0
        longitude = post.longitude ?: 0.0

        // Populate UI with post data
        displayPostDetails()

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.locationMapView) as SupportMapFragment?
        if (mapFragment != null) {
            mapFragment.getMapAsync(this)
        } else {
            Log.e("PostInfoFragment", "Map fragment is null")
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (latitude != 0.0 && longitude != 0.0) {
            // Set marker and move camera
            val location = LatLng(latitude, longitude)
            googleMap.addMarker(MarkerOptions().position(location).title("Food Location"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            // Handle invalid location
            Snackbar.make(
                binding.root,
                "Location data is unavailable for this post.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
