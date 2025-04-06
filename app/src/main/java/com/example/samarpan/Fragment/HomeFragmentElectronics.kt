package com.example.samarpan.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.samarpan.Adapter.PostElectronicsAdapter
import com.example.samarpan.Model.DonationPostsElectronics
import com.example.samarpan.R
import com.example.samarpan.databinding.ActivityHomeFragmentElectronicsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragmentElectronics : Fragment() {

    private var _binding: ActivityHomeFragmentElectronicsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var postAdapterElectronics: PostElectronicsAdapter

    private val postList = ArrayList<DonationPostsElectronics>()
    private val fullPostList = ArrayList<DonationPostsElectronics>()

    private var userLocation: Location? = null
    private val cacheKey = "cachedElectronicsPosts"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityHomeFragmentElectronicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Adapter early
        postAdapterElectronics = PostElectronicsAdapter(postList, userLocation) {
            openPostElectronicsInfoFragment(it)
        }
        binding.postRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postRecyclerView.adapter = postAdapterElectronics

        // Image slider
        val imageList = arrayListOf(
            SlideModel(R.drawable.donation1, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation5, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation6, ScaleTypes.CENTER_CROP)
        )
        binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)

        // Location → then data
        getUserLocation {
            if (isInternetAvailable()) loadPosts() else loadFromCache()
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (isInternetAvailable()) {
                loadPosts()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
                loadFromCache()
            }
        }

        // Filter button
        binding.filterBtn.setOnClickListener {
            val location = binding.locationEditText.text.toString().trim()
            if (location.isEmpty()) {
                postAdapterElectronics.updatePostList(fullPostList, userLocation)
            } else {
                val filtered = fullPostList.filter {
                    it.location?.contains(location, ignoreCase = true) ?: false
                }
                postAdapterElectronics.updatePostList(filtered, userLocation)
            }
        }

        // Add Post
        binding.addPostBtn.setOnClickListener {
            AddPostElectronicsBottomSheet().show(parentFragmentManager, "AddPostElectronicsBottomSheet")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(onReady: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                userLocation = location
                postAdapterElectronics.updatePostList(postList, userLocation)
                onReady()
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            onReady()
        }
    }

    private fun loadPosts() {
        binding.swipeRefreshLayout.isRefreshing = true
        database = FirebaseDatabase.getInstance().getReference("DonationPostsElectronics")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (dataSnapshot in snapshot.children) {
                    val post = dataSnapshot.getValue(DonationPostsElectronics::class.java)
                    if (post != null) postList.add(post)
                }

                fullPostList.clear()
                fullPostList.addAll(postList)

                sortPostsByDistance()

                binding.noPostsTextView.visibility =
                    if (postList.isEmpty()) View.VISIBLE else View.GONE
                binding.postRecyclerView.visibility =
                    if (postList.isEmpty()) View.GONE else View.VISIBLE

                postAdapterElectronics.updatePostList(postList, userLocation)
                saveToCache(postList)
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun sortPostsByDistance() {
        userLocation?.let { userLoc ->
            postList.sortBy {
                val loc = Location("").apply {
                    latitude = it.latitude ?: 0.0
                    longitude = it.longitude ?: 0.0
                }
                userLoc.distanceTo(loc)
            }
        }
    }

    private fun loadFromCache() {
        val prefs = requireContext().getSharedPreferences("ElectronicsCache", Context.MODE_PRIVATE)
        val json = prefs.getString(cacheKey, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<DonationPostsElectronics>>() {}.type
            val cached = Gson().fromJson<List<DonationPostsElectronics>>(json, type)

            postList.clear()
            postList.addAll(cached)

            fullPostList.clear()
            fullPostList.addAll(cached)

            sortPostsByDistance()
            postAdapterElectronics.updatePostList(postList, userLocation)

            binding.noPostsTextView.visibility =
                if (postList.isEmpty()) View.VISIBLE else View.GONE
            binding.postRecyclerView.visibility =
                if (postList.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun saveToCache(posts: List<DonationPostsElectronics>) {
        val prefs = requireContext().getSharedPreferences("ElectronicsCache", Context.MODE_PRIVATE)
        val json = Gson().toJson(posts)
        prefs.edit().putString(cacheKey, json).apply()
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun openPostElectronicsInfoFragment(selectedPost: DonationPostsElectronics) {
        val bundle = Bundle().apply {
            putSerializable("post_data", selectedPost)
        }
        findNavController().navigate(R.id.action_homeFragment2_to_postElectronicsInfoFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
