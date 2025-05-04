package com.example.samarpan

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.samarpan.Model.*
import com.example.samarpan.adapter.SavedPostsAdapter
import com.example.samarpan.databinding.ActivitySavedPostsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import org.json.JSONObject

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

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val post = postList[position]

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // ✅ Add haptic feedback here
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        // Reset the swiped item so it doesn't disappear
                        adapter.notifyItemChanged(position)

                        // Trigger your request logic
                        sendRequest(post)
                    }

                    ItemTouchHelper.RIGHT -> {
                        // ✅ Add haptic feedback here
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        // Perform delete
                        deleteFromSavedPosts(post)
                        postList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        toggleEmptyState(postList.isEmpty())
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
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 40f
                    isAntiAlias = true
                    textAlign = Paint.Align.LEFT
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (dX > 0) {
                        // Swiping Right (Delete)
                        paint.color = Color.parseColor("#E53935") // Elegant Red
                        c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat(), paint)
                        c.drawText("Delete", itemView.left + 40f, itemView.top + itemView.height / 2f + 15f, textPaint)
                    } else if (dX < 0) {
                        // Swiping Left (Request)
                        paint.color = Color.parseColor("#43A047") // Elegant Green
                        c.drawRect(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                        c.drawText("Request", itemView.right - 200f, itemView.top + itemView.height / 2f + 15f, textPaint)
                    }

                    // Translate item (you can control this to reduce how far it moves)
                    val swipeLimit = itemView.width / 3f
                    val clampedDx = dX.coerceIn(-swipeLimit, swipeLimit)
                    super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.savedPostsRecyclerView)


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

    private fun sendRequest(post: UnifiedPost) {
        val requestRef = FirebaseDatabase.getInstance().getReference("Requests").push()

        val dynamicTitle = "New Request for: ${post.title ?: "your post"}"
        val dynamicMessage = "${post.profileName ?: "Someone"} is requesting your donation."

        val postImageUrl = post.imageUrl ?: "https://default-image-url.com"

        val requesterId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val alert = Alert(
            postId = post.postId,
            donorId = post.donorId,
            requesterId = requesterId,
            requestId = requestRef.key, // ✅ Save the requestId inside the object
            status = "Pending",
            timestamp = System.currentTimeMillis(),
            message = dynamicMessage,
            title = dynamicTitle,
            requesterMessage = "You requested ${post.profileName ?: "Someone"}.",
            requesterTitle = "Your request for: ${post.title ?: "post"}",
            postImageUrl = postImageUrl
        )

        val donorTokenRef = FirebaseDatabase.getInstance().getReference("users")
            .child(post.donorId ?: return)
            .child("fcmToken")

        donorTokenRef.get().addOnSuccessListener { snapshot ->
            val receiverFcmToken = snapshot.getValue(String::class.java)
            if (!receiverFcmToken.isNullOrEmpty()) {
                lifecycleScope.launch {
                    val accessToken = FirebaseAccessToken.getAccessToken(applicationContext)
                    accessToken?.let {
                        sendPushNotification(it, receiverFcmToken, dynamicTitle, dynamicMessage)
                    }
                }
            }
        }

        requestRef.setValue(alert)
            .addOnSuccessListener {
                Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendPushNotification(
        accessToken: String,
        fcmToken: String,
        title: String,
        message: String
    )
    {
        val context = this
        val projectId = "samarpan-42c86" // 🔁 Replace with your actual project ID

        val json = JSONObject()
        val messageObj = JSONObject()
        val notificationObj = JSONObject()

        notificationObj.put("title", title)
        notificationObj.put("body", message)

        messageObj.put("token", fcmToken)
        messageObj.put("notification", notificationObj)
        json.put("message", messageObj)

        val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        val request = object : JsonObjectRequest(
            Method.POST, url, json,
            Response.Listener { response -> Log.d("FCM", "Push sent: $response") },
            Response.ErrorListener { error -> Log.e("FCM", "Error: ${error.message}") }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun deleteFromSavedPosts(post: UnifiedPost) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("SavedPosts")
            .child(uid)
            .child(post.category ?: return)
            .child(post.postId ?: return)

        ref.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show()
            }
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
