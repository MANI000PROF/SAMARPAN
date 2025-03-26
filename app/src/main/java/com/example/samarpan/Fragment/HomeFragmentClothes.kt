package com.example.samarpan.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
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
import com.example.samarpan.Adapter.PostClothesAdapter
import com.example.samarpan.Model.DonationPostsClothes
import com.example.samarpan.R
import com.example.samarpan.databinding.ActivityHomeFragmentClothesBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragmentClothes : Fragment() {

    private var _binding: ActivityHomeFragmentClothesBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val postList = ArrayList<DonationPostsClothes>()
    private var fullPostList = ArrayList<DonationPostsClothes>() // Store all posts before filtering
    private lateinit var postClothesAdapter: PostClothesAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityHomeFragmentClothesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getUserLocation()

        // Image slider setup
        val imageList = ArrayList<SlideModel>()
        imageList.add(SlideModel(R.drawable.donation1, ScaleTypes.CENTER_CROP))
        imageList.add(SlideModel(R.drawable.donation5, ScaleTypes.CENTER_CROP))
        imageList.add(SlideModel(R.drawable.donation6, ScaleTypes.CENTER_CROP))
        binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)

        // RecyclerView setup
        postClothesAdapter = PostClothesAdapter(postList, userLocation) { selectedPost ->
            openPostClothesInfoFragment(selectedPost)
        }

        binding.postRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postRecyclerView.adapter = postClothesAdapter

        database = FirebaseDatabase.getInstance().getReference("DonationPostsClothes")

        // Load Posts from Firebase
        loadPosts()

        // Filter Button Functionality

        binding.filterBtn.setOnClickListener {
            val location = binding.locationEditText.text.toString().trim()
            if (location.isEmpty()) {
                postClothesAdapter.updatePostList(fullPostList, userLocation) // Restore full list
            } else {
                val filteredPosts = fullPostList.filter { it.location?.contains(location, ignoreCase = true) ?: false }
                postClothesAdapter.updatePostList(filteredPosts, userLocation)
            }
        }


        // Add Post Button
        binding.addPostBtn.setOnClickListener {
            val addPostClothesBottomSheet = AddPostClothesBottomSheet()
            addPostClothesBottomSheet.show(parentFragmentManager, "AddPostClothesBottomSheet")
        }

    }

    override fun onResume() {
        super.onResume()
        loadPosts() // Reload posts when fragment is visible again
    }


    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    postClothesAdapter.updatePostList(postList, userLocation) // Update with location
                }
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }


    private fun loadPosts() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (dataSnapshot in snapshot.children) {
                    val post = dataSnapshot.getValue(DonationPostsClothes::class.java)
                    if (post != null) postList.add(post)
                }

                fullPostList.clear()
                fullPostList.addAll(postList) // Store original data

                sortPostsByDistance()

                if (postList.isEmpty()) {
                    binding.noPostsTextView.visibility = View.VISIBLE
                    binding.postRecyclerView.visibility = View.GONE
                } else {
                    binding.noPostsTextView.visibility = View.GONE
                    binding.postRecyclerView.visibility = View.VISIBLE
                }

                postClothesAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun sortPostsByDistance() {
        if (userLocation != null) {
            postList.sortBy { post ->
                val postLocation = Location("").apply {
                    latitude = post.latitude ?: 0.0
                    longitude = post.longitude ?: 0.0
                }
                userLocation!!.distanceTo(postLocation)
            }
        }
    }

    private fun openPostClothesInfoFragment(selectedPost: DonationPostsClothes) {
        Log.d("NavigationDebug", "Navigating with post: ${selectedPost.clothesTitle}, Lat: ${selectedPost.latitude}, Lng: ${selectedPost.longitude}")

        val bundle = Bundle().apply {
            putSerializable("post_data", selectedPost)
        }
        findNavController().navigate(R.id.action_homeFragment2_to_postClothesInfoFragment, bundle)
    }


    private fun addPostToFirebase(post: DonationPostsClothes) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val postId = database.push().key ?: return
        val updatedPost = post.copy(donorId = currentUserId)

        database.child(postId).setValue(updatedPost).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                postList.add(updatedPost)
                postClothesAdapter.notifyItemInserted(postList.size - 1)
                binding.noPostsTextView.visibility = View.GONE
                binding.postRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

