package com.example.samarpan.Fragment

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Model.*
import com.example.samarpan.adapter.HistoryAdapter
import com.example.samarpan.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private val postList = mutableListOf<UnifiedPost>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val cacheKey = "cachedHistoryPosts"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        historyAdapter = HistoryAdapter(requireContext(), postList).apply {
            setOnItemLongClickListener { post ->
                showDeleteConfirmationDialog(post)
            }
        }

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            if (isInternetAvailable()) {
                loadUserPosts()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        if (currentUserId == null) {
            showNoPosts()
            return
        }

        if (isInternetAvailable()) {
            loadUserPosts()
        } else {
            loadFromCache()
        }
    }

    private fun loadUserPosts() {
        val db = FirebaseDatabase.getInstance().reference
        val foodRef = db.child("DonationPosts")
        val clothesRef = db.child("DonationPostsClothes")
        val electronicsRef = db.child("DonationPostsElectronics")

        postList.clear()

        val userPosts = mutableListOf<UnifiedPost>()
        var completed = 0
        val total = 3

        fun checkDone() {
            completed++
            if (completed == total) {
                postList.addAll(userPosts.filter { it.donorId == currentUserId })
                updateUI()
                saveToCache(postList)
            }
        }

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val post = data.getValue(DonationPosts::class.java)
                    if (post != null) {
                        userPosts.add(
                            UnifiedPost(
                                postId = data.key,
                                donorId = post.donorId,
                                title = post.foodTitle,
                                description = post.foodDescription,
                                imageUrl = post.foodImage,
                                location = post.location,
                                profileName = post.profileName,
                                timestamp = post.timestamp,
                                category = "Food"
                            )
                        )
                    }
                }
                checkDone()
            }

            override fun onCancelled(error: DatabaseError) {
                checkDone()
            }
        })

        clothesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val post = data.getValue(DonationPostsClothes::class.java)
                    if (post != null) {
                        userPosts.add(
                            UnifiedPost(
                                postId = data.key,
                                donorId = post.donorId,
                                title = post.clothesTitle,
                                description = post.clothesDescription,
                                imageUrl = post.clothesImage,
                                location = post.location,
                                profileName = post.profileName,
                                timestamp = post.timestamp,
                                category = "Clothes"
                            )
                        )
                    }
                }
                checkDone()
            }

            override fun onCancelled(error: DatabaseError) {
                checkDone()
            }
        })

        electronicsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val post = data.getValue(DonationPostsElectronics::class.java)
                    if (post != null) {
                        userPosts.add(
                            UnifiedPost(
                                postId = data.key,
                                donorId = post.donorId,
                                title = post.electronicsTitle,
                                description = post.electronicsDescription,
                                imageUrl = post.electronicsImage,
                                location = post.location,
                                profileName = post.profileName,
                                timestamp = post.timestamp,
                                category = "Electronics"
                            )
                        )
                    }
                }
                checkDone()
            }

            override fun onCancelled(error: DatabaseError) {
                checkDone()
            }
        })
    }

    private fun showDeleteConfirmationDialog(post: UnifiedPost) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { _, _ -> deletePost(post) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: UnifiedPost) {
        val database = FirebaseDatabase.getInstance()
        val nodeMap = mapOf(
            "Food" to "DonationPosts",
            "Clothes" to "DonationPostsClothes",
            "Electronics" to "DonationPostsElectronics"
        )

        val node = nodeMap[post.category]
        if (node != null) {
            database.getReference(node)
                .child(post.postId ?: "")
                .removeValue()
                .addOnSuccessListener {
                    postList.remove(post)
                    historyAdapter.notifyDataSetChanged()
                    saveToCache(postList)
                    Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUI() {
        if (postList.isEmpty()) {
            showNoPosts()
        } else {
            showPosts()
        }
        historyAdapter.notifyDataSetChanged()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showNoPosts() {
        binding.noDonationsAnimation.visibility = View.VISIBLE
        binding.noDonationsTextView.visibility = View.VISIBLE
        binding.historyRecyclerView.visibility = View.GONE
    }

    private fun showPosts() {
        binding.noDonationsAnimation.visibility = View.GONE
        binding.noDonationsTextView.visibility = View.GONE
        binding.historyRecyclerView.visibility = View.VISIBLE
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun saveToCache(posts: List<UnifiedPost>) {
        val prefs = requireContext().getSharedPreferences("HistoryCache", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(posts)
        editor.putString(cacheKey, json)
        editor.apply()
    }

    private fun loadFromCache() {
        val prefs = requireContext().getSharedPreferences("HistoryCache", Context.MODE_PRIVATE)
        val json = prefs.getString(cacheKey, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<UnifiedPost>>() {}.type
            val cachedPosts: List<UnifiedPost> = Gson().fromJson(json, type)
            postList.clear()
            postList.addAll(cachedPosts)
            updateUI()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
