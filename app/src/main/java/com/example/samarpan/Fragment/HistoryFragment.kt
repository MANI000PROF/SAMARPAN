package com.example.samarpan.Fragment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.EditPostActivity
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
    private var currentUserId: String? = ""
    private val cacheKey = "cachedHistoryPosts"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        historyAdapter = HistoryAdapter(requireContext(), postList)

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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showNoPosts()
            return
        }
        currentUserId = currentUser.uid

        if (isInternetAvailable()) {
            loadUserPosts()
        } else {
            loadFromCache()
        }

        // Enable swipe gestures for editing and deleting posts
        enableSwipeGestures()
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

    private fun enableSwipeGestures() {

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= postList.size) return
                val post = postList[position]  // Replace with your actual list of posts
                viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                // Handle swipe actions
                if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe Right -> Edit Post
                    val intent = Intent(requireContext(), EditPostActivity::class.java).apply {
                        putExtra("postId", post.postId)
                        putExtra("profileName", post.profileName)
                        putExtra("location", post.location)
                        putExtra("title", post.title)
                        putExtra("description", post.description)
                        putExtra("imageUrl", post.imageUrl)
                        putExtra("category", post.category)
                        putExtra("latitude", post.latitude)
                        putExtra("longitude", post.longitude)
                    }
                    historyAdapter.notifyItemChanged(position)
                    startActivity(intent)
                } else if (direction == ItemTouchHelper.LEFT) {
                    // Swipe Left -> Delete Post
                    post.postId?.let {
                        val categoryRef = when (post.category) {
                            "Food" -> "DonationPosts"
                            "Clothes" -> "DonationPostsClothes"
                            "Electronics" -> "DonationPostsElectronics"
                            else -> null
                        }

                        if (categoryRef != null) {
                            FirebaseDatabase.getInstance().getReference(categoryRef).child(post.postId!!)
                                .removeValue()
                                .addOnSuccessListener {
                                    postList.removeAt(position)
                                    historyAdapter.notifyItemRemoved(position)
                                    Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show()
                                    historyAdapter.notifyItemChanged(position)
                                }
                        }
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                val position = viewHolder.adapterPosition
                // ✅ Prevent crash by ignoring invalid positions
                if (position == RecyclerView.NO_POSITION || position >= postList.size) {
                    return
                }
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 40f
                    isAntiAlias = true
                    textAlign = Paint.Align.LEFT
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                postList[viewHolder.adapterPosition]

                if (dX > 0) {
                    // Swipe Right (Edit)
                    paint.color = Color.parseColor("#2c8bc9") // blue
                    c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat(), paint)
                    c.drawText("Edit", itemView.left + 40f, itemView.top + itemView.height / 2f + 15f, textPaint)
                } else if (dX < 0) {
                    // Swipe Left (Delete)
                    paint.color = Color.parseColor("#E53935") // Red
                    c.drawRect(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                    c.drawText("Delete", itemView.right - 200f, itemView.top + itemView.height / 2f + 15f, textPaint)
                }

                // Clamp swipe distance for smoother transition
                val swipeLimit = itemView.width / 3f
                val clampedDx = dX.coerceIn(-swipeLimit, swipeLimit)
                super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.historyRecyclerView)  // Attach the swipe gesture to your RecyclerView
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
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return cm?.activeNetworkInfo?.isConnectedOrConnecting == true
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
