package com.example.samarpan.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.samarpan.Adapter.PostAdapter
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val postList = ArrayList<DonationPosts>()
    private val fullPostList = ArrayList<DonationPosts>()
    private lateinit var postAdapter: PostAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    private val sharedPrefsKey = "cached_food_posts"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImageSlider()
        setupRecyclerView()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getUserLocation()

        setupSwipeToRefresh()
        loadData()

        binding.filterBtn.setOnClickListener {
            val location = binding.locationEditText.text.toString().trim()
            if (location.isEmpty()) {
                postAdapter.updatePostList(fullPostList, userLocation)
            } else {
                val filtered = fullPostList.filter {
                    it.location?.contains(location, ignoreCase = true) ?: false
                }
                postAdapter.updatePostList(filtered, userLocation)
            }
        }

        binding.addPostBtn.setOnClickListener {
            val addPostBottomSheet = AddPostBottomSheet()
            addPostBottomSheet.show(parentFragmentManager, "AddPostBottomSheet")
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupImageSlider() {
        val imageList = arrayListOf(
            SlideModel(R.drawable.donation1, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation5, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation6, ScaleTypes.CENTER_CROP)
        )
        binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(ArrayList(), userLocation) { selectedPost ->
            openPostInfoFragment(selectedPost)
        }
        binding.postRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postRecyclerView.adapter = postAdapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = it
                    postAdapter.updatePostList(postList, userLocation)
                }
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun loadData() {
        if (isConnectedToInternet()) {
            loadPostsFromFirebase()
        } else {
            loadPostsFromCache()
        }
    }

    private fun loadPostsFromFirebase() {
        binding.swipeRefreshLayout.isRefreshing = true
        database = FirebaseDatabase.getInstance().getReference("DonationPosts")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                fullPostList.clear()

                for (dataSnapshot in snapshot.children) {
                    val postMap = dataSnapshot.value as? Map<*, *>
                    if (postMap != null) {
                        val post = DonationPosts(
                            postId = postMap["postId"] as? String,
                            profileName = postMap["profileName"] as? String,
                            location = postMap["location"] as? String,
                            foodTitle = postMap["foodTitle"] as? String,
                            foodDescription = postMap["foodDescription"] as? String,
                            foodImage = postMap["foodImage"] as? String,
                            latitude = (postMap["latitude"] as? Number)?.toDouble(),
                            longitude = (postMap["longitude"] as? Number)?.toDouble(),
                            donorId = postMap["donorId"] as? String,
                            timestamp = (postMap["timestamp"] as? Number)?.toLong() ?: 0L,
                            userId = postMap["userId"] as? String
                        )
                        postList.add(post)
                    }
                }

                fullPostList.addAll(postList)
                sortPostsByDistance()
                updateUI(postList)
                cachePostsLocally(postList)
                binding.swipeRefreshLayout.isRefreshing = false

                Log.d("HomeFragment", "Fetched ${postList.size} posts from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Database error: ${error.message}")
                binding.swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun loadPostsFromCache() {
        val sharedPreferences = requireContext().getSharedPreferences("SAMARPAN_PREFS", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(sharedPrefsKey, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<DonationPosts>>() {}.type
            val cachedPosts: List<DonationPosts> = Gson().fromJson(json, type)

            postList.clear()
            postList.addAll(cachedPosts)

            fullPostList.clear()
            fullPostList.addAll(cachedPosts)

            sortPostsByDistance()
            updateUI(postList)
        }
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun cachePostsLocally(posts: List<DonationPosts>) {
        val sharedPreferences = requireContext().getSharedPreferences("SAMARPAN_PREFS", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(posts)
        editor.putString(sharedPrefsKey, json)
        editor.apply()
    }

    private fun updateUI(list: List<DonationPosts>) {
        if (list.isEmpty()) {
            binding.noPostsTextView.visibility = View.VISIBLE
            binding.postRecyclerView.visibility = View.GONE
        } else {
            binding.noPostsTextView.visibility = View.GONE
            binding.postRecyclerView.visibility = View.VISIBLE
        }

        postAdapter.updatePostList(list, userLocation)
        postAdapter.notifyDataSetChanged()
    }

    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun sortPostsByDistance() {
        userLocation?.let { location ->
            postList.sortBy {
                val postLocation = Location("").apply {
                    latitude = it.latitude ?: 0.0
                    longitude = it.longitude ?: 0.0
                }
                location.distanceTo(postLocation)
            }
        }
    }

    private fun openPostInfoFragment(selectedPost: DonationPosts) {
        val bundle = Bundle().apply {
            putSerializable("post_data", selectedPost)
        }
        findNavController().navigate(R.id.action_homeFragment2_to_postInfoFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
