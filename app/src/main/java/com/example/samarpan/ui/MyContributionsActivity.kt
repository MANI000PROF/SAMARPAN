package com.example.samarpan.ui

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Adapter.MyContributionAdapter
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.Model.DonationPostsClothes
import com.example.samarpan.Model.DonationPostsElectronics
import com.example.samarpan.Model.UnifiedPost
import com.example.samarpan.R
import com.example.samarpan.databinding.ActivityMyContributionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyContributionsActivity : AppCompatActivity() {

    private lateinit var foodRecyclerView: RecyclerView
    private lateinit var clothesRecyclerView: RecyclerView
    private lateinit var electronicsRecyclerView: RecyclerView
    private lateinit var noDataLayout: View

    private lateinit var totalCountText: TextView
    private lateinit var foodCountText: TextView
    private lateinit var clothesCountText: TextView
    private lateinit var electronicsCountText: TextView

    private val foodList = mutableListOf<UnifiedPost>()
    private val clothesList = mutableListOf<UnifiedPost>()
    private val electronicsList = mutableListOf<UnifiedPost>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_contributions)

        // Initialize views
        foodRecyclerView = findViewById(R.id.foodRecyclerView)
        clothesRecyclerView = findViewById(R.id.clothesRecyclerView)
        electronicsRecyclerView = findViewById(R.id.electronicsRecyclerView)
        noDataLayout = findViewById(R.id.noDataLayout)

        totalCountText = findViewById(R.id.totalCount)
        foodCountText = findViewById(R.id.foodCountText)
        clothesCountText = findViewById(R.id.clothesCountText)
        electronicsCountText = findViewById(R.id.electronicsCountText)

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        // Set Adapters
        foodRecyclerView.adapter = MyContributionAdapter(foodList)
        clothesRecyclerView.adapter = MyContributionAdapter(clothesList)
        electronicsRecyclerView.adapter = MyContributionAdapter(electronicsList)

        foodRecyclerView.setHasFixedSize(true)
        clothesRecyclerView.setHasFixedSize(true)
        electronicsRecyclerView.setHasFixedSize(true)

        foodRecyclerView.layoutManager = LinearLayoutManager(this)
        clothesRecyclerView.layoutManager = LinearLayoutManager(this)
        electronicsRecyclerView.layoutManager = LinearLayoutManager(this)


        loadContributions()
    }

    private fun loadContributions() {
        if (currentUserId == null) {
            showNoData()
            return
        }

        val db = FirebaseDatabase.getInstance().reference

        val foodRef = db.child("DonationPosts")
        val clothesRef = db.child("DonationPostsClothes")
        val electronicsRef = db.child("DonationPostsElectronics")

        foodList.clear()
        clothesList.clear()
        electronicsList.clear()

        val userPosts = mutableListOf<UnifiedPost>()
        var completed = 0
        val total = 3

        fun checkDone() {
            completed++
            if (completed < total) return

            // Filter and update lists
            val myPosts = userPosts.filter { it.donorId == currentUserId }
            foodList.addAll(myPosts.filter { it.category == "Food" })
            clothesList.addAll(myPosts.filter { it.category == "Clothes" })
            electronicsList.addAll(myPosts.filter { it.category == "Electronics" })

            foodRecyclerView.adapter?.notifyDataSetChanged()
            clothesRecyclerView.adapter?.notifyDataSetChanged()
            electronicsRecyclerView.adapter?.notifyDataSetChanged()

            updateTotalCount()
            updateNoDataVisibility()
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

    private fun updateTotalCount() {
        val total = foodList.size + clothesList.size + electronicsList.size
        totalCountText.text = total.toString()
        foodCountText.text = foodList.size.toString()
        clothesCountText.text = clothesList.size.toString()
        electronicsCountText.text = electronicsList.size.toString()
    }

    private fun updateNoDataVisibility() {
        val isEmpty = foodList.isEmpty() && clothesList.isEmpty() && electronicsList.isEmpty()
        noDataLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showNoData() {
        noDataLayout.visibility = View.VISIBLE
    }
}

