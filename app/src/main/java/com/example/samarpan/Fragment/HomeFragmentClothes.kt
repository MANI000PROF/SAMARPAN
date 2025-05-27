package com.example.samarpan.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.samarpan.Adapter.PostAdapter
import com.example.samarpan.Adapter.PostClothesAdapter
import com.example.samarpan.Fragment.HomeFragment.FilterMode
import com.example.samarpan.MainActivity
import com.example.samarpan.Model.DonationPostsClothes
import com.example.samarpan.R
import com.example.samarpan.databinding.ActivityHomeFragmentClothesBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragmentClothes : Fragment() {

    private var _binding: ActivityHomeFragmentClothesBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val postList = ArrayList<DonationPostsClothes>()
    private val fullPostList = ArrayList<DonationPostsClothes>()
    private lateinit var postClothesAdapter: PostClothesAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    private val sharedPrefsKey = "cached_clothes_posts"
    private var currentFilterMode: FilterMode = FilterMode.LOCATION

    private var scrollDySum = 0
    private var isHeaderHidden = false
    private var canShowHeader = true
    private val scrollThreshold = 100
    private val headerCooldownMillis = 300L
    private var lastScrollDirection = 0

    enum class FilterMode {
        LOCATION, CLOTHES_NAME, PERSON_NAME, DATE_TIME
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityHomeFragmentClothesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImageSlider()
        setupRecyclerView()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getUserLocation()

        setupSwipeToRefresh()
        loadData()

        binding.locationEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    postClothesAdapter.updatePostList(fullPostList, userLocation)
                } else {
                    val filtered = fullPostList.filter { post ->
                        when (currentFilterMode) {
                            FilterMode.LOCATION -> post.location?.contains(query, true) == true
                            FilterMode.CLOTHES_NAME -> post.clothesTitle?.contains(
                                query,
                                true
                            ) == true

                            FilterMode.PERSON_NAME -> post.profileName?.contains(
                                query,
                                true
                            ) == true

                            FilterMode.DATE_TIME -> {
                                val formattedTime = formatTimestamp(post.timestamp)
                                formattedTime.contains(query, ignoreCase = true)
                            }
                        }
                    }
                    postClothesAdapter.updatePostList(filtered, userLocation)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.filterBtn.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menu.apply {
                add("Location")
                add("Clothes Name")
                add("Person Name")
                add("Date/Time")
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title.toString()) {
                    "Location" -> {
                        currentFilterMode = FilterMode.LOCATION
                        binding.locationEditText.hint = "Enter location"
                    }
                    "Clothes Name" -> {
                        currentFilterMode = FilterMode.CLOTHES_NAME
                        binding.locationEditText.hint = "Enter clothes name"
                    }
                    "Person Name" -> {
                        currentFilterMode = FilterMode.PERSON_NAME
                        binding.locationEditText.hint = "Enter person name"
                    }
                    "Date/Time" -> {
                        currentFilterMode = FilterMode.DATE_TIME
                        binding.locationEditText.hint = "Enter date/time"
                    }
                }
                true
            }
            popupMenu.show()
        }

        binding.addPostBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val addPostClothesBottomSheet = AddPostClothesBottomSheet()
            addPostClothesBottomSheet.show(parentFragmentManager, "AddPostClothesBottomSheet")
        }

        val mainActivity = activity as? MainActivity ?: return

        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY

            if (kotlin.math.abs(dy) < 5) return@setOnScrollChangeListener

            // Detect direction change
            val currentDirection = if (dy > 0) 1 else -1
            if (currentDirection != lastScrollDirection) {
                scrollDySum = 0
                lastScrollDirection = currentDirection
            }

            scrollDySum += dy

            // Hide when scrolling up enough
            if (scrollDySum > scrollThreshold && !isHeaderHidden) {
                mainActivity.hideTopHeaderSmooth()
                isHeaderHidden = true
                canShowHeader = false
                scrollDySum = 0

                // Lock re-showing for a short duration to prevent bounce
                Handler(Looper.getMainLooper()).postDelayed({
                    canShowHeader = true
                }, headerCooldownMillis)
            }

            // Show only if allowed and scrolled down enough
            if (scrollDySum < -scrollThreshold && isHeaderHidden && canShowHeader) {
                mainActivity.showTopHeaderSmooth()
                isHeaderHidden = false
                scrollDySum = 0
            }
        }
    }
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupImageSlider() {
        val imageList1 = arrayListOf(
            SlideModel(R.drawable.donation_clothes_1, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation_clothes_2, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation_clothes_3, ScaleTypes.CENTER_CROP)
        )
        val imageList2 = arrayListOf(
            SlideModel(R.drawable.donation_clothes_2, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation_clothes_3, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_1, ScaleTypes.CENTER_CROP)
        )
        val imageList3 = arrayListOf(
            SlideModel(R.drawable.donation_clothes_3, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_1, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_2, ScaleTypes.CENTER_CROP)
        )
        val imageList4 = arrayListOf(
            SlideModel(R.drawable.clothes_bg_1, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_2, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_3, ScaleTypes.CENTER_CROP)
        )
        val imageList5 = arrayListOf(
            SlideModel(R.drawable.clothes_bg_2, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_3, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_4, ScaleTypes.CENTER_CROP)
        )
        val imageList6 = arrayListOf(
            SlideModel(R.drawable.clothes_bg_3, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.clothes_bg_4, ScaleTypes.CENTER_CROP),
            SlideModel(R.drawable.donation_clothes_1, ScaleTypes.CENTER_CROP)
        )
        binding.imageSlider1.setImageList(imageList1, ScaleTypes.FIT)
        binding.imageSlider2.setImageList(imageList2, ScaleTypes.FIT)
        binding.imageSlider3.setImageList(imageList3, ScaleTypes.FIT)
        binding.imageSlider4.setImageList(imageList4, ScaleTypes.FIT)
        binding.imageSlider5.setImageList(imageList5, ScaleTypes.FIT)
        binding.imageSlider6.setImageList(imageList6, ScaleTypes.FIT)
    }

    private fun setupRecyclerView() {
        postClothesAdapter = PostClothesAdapter(
            requireContext(),
            ArrayList(),
            userLocation,
            onPostClick = { selectedPost ->
                openPostInfoFragment(selectedPost)
            }
        )

        binding.postRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postRecyclerView.adapter = postClothesAdapter
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
                    postClothesAdapter.updatePostList(postList, userLocation)
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
    private var postListener: ValueEventListener? = null

    private fun loadPostsFromFirebase() {
        binding.swipeRefreshLayout.isRefreshing = true
        database = FirebaseDatabase.getInstance().getReference("DonationPostsClothes")
        postListener?.let { database.removeEventListener(it) }  // Avoid multiple listeners

        postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                fullPostList.clear()

                for (dataSnapshot in snapshot.children) {
                    val postMap = dataSnapshot.value as? Map<*, *>
                    if (postMap != null) {
                        val post = DonationPostsClothes(
                            postId = postMap["postId"] as? String,
                            profileName = postMap["profileName"] as? String,
                            location = postMap["location"] as? String,
                            clothesTitle = postMap["clothesTitle"] as? String,
                            clothesDescription = postMap["clothesDescription"] as? String,
                            clothesImage = postMap["clothesImage"] as? String,
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

                Log.d("HomeFragmentClothes", "Fetched ${postList.size} posts from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragmentClothes", "Database error: ${error.message}")
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        database.addValueEventListener(postListener as ValueEventListener)
    }

    private fun loadPostsFromCache() {
        val sharedPreferences = requireContext().getSharedPreferences("SAMARPAN_PREFS", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(sharedPrefsKey, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<DonationPostsClothes>>() {}.type
            val cachedPosts: List<DonationPostsClothes> = Gson().fromJson(json, type)

            postList.clear()
            postList.addAll(cachedPosts)

            fullPostList.clear()
            fullPostList.addAll(cachedPosts)

            sortPostsByDistance()
            updateUI(postList)
        }
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun cachePostsLocally(posts: List<DonationPostsClothes>) {
        val sharedPreferences = requireContext().getSharedPreferences("SAMARPAN_PREFS", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(posts)
        editor.putString(sharedPrefsKey, json)
        editor.apply()
    }

    private fun updateUI(list: List<DonationPostsClothes>) {
        if (list.isEmpty()) {
            binding.noPostsTextView.visibility = View.VISIBLE
            binding.noPostsAnimation.visibility = View.VISIBLE
            binding.postRecyclerView.visibility = View.GONE
        } else {
            binding.noPostsTextView.visibility = View.GONE
            binding.noPostsAnimation.visibility = View.GONE
            binding.postRecyclerView.visibility = View.VISIBLE
        }

        postClothesAdapter.updatePostList(list, userLocation)
        postClothesAdapter.notifyDataSetChanged()
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

    private fun openPostInfoFragment(selectedPost: DonationPostsClothes) {
        val bundle = Bundle().apply {
            putSerializable("post_data", selectedPost)
        }
        findNavController().navigate(R.id.action_homeFragment2_to_postClothesInfoFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postListener?.let { database.removeEventListener(it) }
        _binding = null
    }
}
