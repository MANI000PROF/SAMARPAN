package com.example.samarpan

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Model.*
import com.example.samarpan.adapter.SavedPostsAdapter
import com.example.samarpan.databinding.ActivitySavedPostsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SavedPostsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedPostsBinding
    private lateinit var adapter: SavedPostsAdapter
    private val postList = mutableListOf<UnifiedPost>()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var postsFetched = 0
    private var totalPostsToFetch = 0
    private var categoriesProcessed = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SavedPostsAdapter(this, postList, )
        binding.savedPostsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.savedPostsRecyclerView.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadSavedPosts()
        }

        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        loadSavedPosts()
    }

    private fun loadSavedPosts() {
        binding.swipeRefreshLayout.isRefreshing = true
        postList.clear()
        adapter.notifyDataSetChanged()

        postsFetched = 0
        totalPostsToFetch = 0

        val db = FirebaseDatabase.getInstance().reference
        val categories = listOf("Food", "Clothes", "Electronics")
        val dataPaths = mapOf(
            "Food" to "DonationPosts",
            "Clothes" to "DonationPostsClothes",
            "Electronics" to "DonationPostsElectronics"
        )

        for (category in categories) {
            val savedRef = db.child("SavedPosts").child(userId!!).child(category)
            val postDataRef = db.child(dataPaths[category] ?: continue)

            // Enable offline sync
            savedRef.keepSynced(true)
            postDataRef.keepSynced(true)

            savedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val postIds = snapshot.children.mapNotNull { it.key }
                    totalPostsToFetch += postIds.size

                    if (postIds.isEmpty()) {
                        categoriesProcessed++
                        checkIfAllProcessed()
                        return
                    }

                    for (postId in postIds) {
                        postDataRef.child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val post = when (category) {
                                    "Food" -> dataSnapshot.getValue(DonationPosts::class.java)?.toUnified(postId)
                                    "Clothes" -> dataSnapshot.getValue(DonationPostsClothes::class.java)?.toUnified(postId)
                                    "Electronics" -> dataSnapshot.getValue(DonationPostsElectronics::class.java)?.toUnified(postId)
                                    else -> null
                                }
                                post?.category = category
                                post?.let { postList.add(it) }

                                postsFetched++
                                checkDone()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                postsFetched++
                                checkDone()
                            }
                        })
                    }

                    categoriesProcessed++
                    checkIfAllProcessed()
                }

                override fun onCancelled(error: DatabaseError) {
                    categoriesProcessed++
                    checkIfAllProcessed()
                }
            })
        }
    }

    private fun checkIfAllProcessed() {
        if (categoriesProcessed == 3 && totalPostsToFetch == 0) {
            binding.swipeRefreshLayout.isRefreshing = false
            toggleEmptyState(true)
        }
    }

    private fun checkDone() {
        if (postsFetched == totalPostsToFetch && totalPostsToFetch > 0) {
            postList.sortByDescending { it.timestamp }
            adapter.notifyDataSetChanged()
            binding.swipeRefreshLayout.isRefreshing = false
            toggleEmptyState(postList.isEmpty())
        }
    }

    private fun toggleEmptyState(empty: Boolean) {
        binding.noSavedAnimation.visibility = if (empty) View.VISIBLE else View.GONE
        binding.noSavedTextView.visibility = if (empty) View.VISIBLE else View.GONE
        binding.savedPostsRecyclerView.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun DonationPosts.toUnified(postId: String?) = UnifiedPost(
        postId = postId,
        donorId = donorId,
        title = foodTitle,
        description = foodDescription,
        imageUrl = foodImage,
        location = location,
        profileName = profileName,
        timestamp = timestamp
    )

    private fun DonationPostsClothes.toUnified(postId: String?) = UnifiedPost(
        postId = postId,
        donorId = donorId,
        title = clothesTitle,
        description = clothesDescription,
        imageUrl = clothesImage,
        location = location,
        profileName = profileName,
        timestamp = timestamp
    )

    private fun DonationPostsElectronics.toUnified(postId: String?) = UnifiedPost(
        postId = postId,
        donorId = donorId,
        title = electronicsTitle,
        description = electronicsDescription,
        imageUrl = electronicsImage,
        location = location,
        profileName = profileName,
        timestamp = timestamp
    )
}
