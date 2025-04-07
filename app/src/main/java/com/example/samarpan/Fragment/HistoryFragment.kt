package com.example.samarpan.Fragment

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Model.DonationPosts
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
    private val postList = mutableListOf<DonationPosts>()
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
                fetchAllUserPosts()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        if (currentUserId == null) {
            showNoPosts()
            return
        }

        if (isInternetAvailable()) {
            fetchAllUserPosts()
        } else {
            loadFromCache()
        }
    }

    private fun fetchAllUserPosts() {
        val foodRef = FirebaseDatabase.getInstance().getReference("DonationPosts")
        val clothesRef = FirebaseDatabase.getInstance().getReference("DonationPostClothes")
        val electronicsRef = FirebaseDatabase.getInstance().getReference("DonationElectronicsPosts")

        postList.clear()

        var completedRequests = 0
        val totalRequests = 3

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val post = child.getValue(DonationPosts::class.java)
                    if (post?.donorId == currentUserId) {
                        post?.postId = child.key
                        if (post != null) {
                            postList.add(post)
                        }
                    }
                }
                completedRequests++
                if (completedRequests == totalRequests) {
                    updateUI()
                    saveToCache(postList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completedRequests++
                if (completedRequests == totalRequests) {
                    updateUI()
                }
            }
        }

        foodRef.addListenerForSingleValueEvent(listener)
        clothesRef.addListenerForSingleValueEvent(listener)
        electronicsRef.addListenerForSingleValueEvent(listener)
    }

    private fun showDeleteConfirmationDialog(post: DonationPosts) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { _, _ -> deletePost(post) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: DonationPosts) {
        val database = FirebaseDatabase.getInstance()

        val nodeNames = listOf("DonationPosts", "DonationPostClothes", "DonationElectronicsPosts")

        for (node in nodeNames) {
            val ref = database.getReference(node)
            ref.child(post.postId ?: "").removeValue().addOnSuccessListener {
                postList.remove(post)
                historyAdapter.notifyDataSetChanged()
                saveToCache(postList)
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                // Ignore failure for now; it just means this post wasn't in that node
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
        binding.noDonationsTextView.visibility = View.GONE
        binding.noDonationsAnimation.visibility = View.GONE
        binding.historyRecyclerView.visibility = View.VISIBLE
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun saveToCache(posts: List<DonationPosts>) {
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
            val type = object : TypeToken<List<DonationPosts>>() {}.type
            val cachedPosts: List<DonationPosts> = Gson().fromJson(json, type)
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
