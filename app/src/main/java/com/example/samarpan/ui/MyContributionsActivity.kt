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
import com.bumptech.glide.Glide
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.Model.DonationPostsClothes
import com.example.samarpan.Model.DonationPostsElectronics
import com.example.samarpan.Model.UnifiedPost
import com.example.samarpan.R
import com.example.samarpan.databinding.ActivityMyContributionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyContributionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyContributionsBinding
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val userPosts = mutableListOf<UnifiedPost>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyContributionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        if (currentUserId == null || !isInternetAvailable()) {
            showNoData()
        } else {
            loadUserPosts()
        }
    }

    private fun loadUserPosts() {
        val db = FirebaseDatabase.getInstance().reference
        val references = listOf(
            Triple(db.child("DonationPosts"), "Food", DonationPosts::class.java),
            Triple(db.child("DonationPostsClothes"), "Clothes", DonationPostsClothes::class.java),
            Triple(db.child("DonationPostsElectronics"), "Electronics", DonationPostsElectronics::class.java)
        )

        var completed = 0
        val total = references.size

        references.forEach { (ref, category, clazz) ->
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val post = data.getValue(clazz)
                        val unifiedPost = when (post) {
                            is DonationPosts -> post.toUnified(data.key, "Food")
                            is DonationPostsClothes -> post.toUnified(data.key, "Clothes")
                            is DonationPostsElectronics -> post.toUnified(data.key, "Electronics")
                            else -> null
                        }
                        if (unifiedPost?.donorId == currentUserId) {
                            if (unifiedPost != null) {
                                userPosts.add(unifiedPost)
                            }
                        }
                    }
                    if (++completed == total) updateUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    if (++completed == total) updateUI()
                }
            })
        }
    }

    private fun DonationPosts.toUnified(id: String?, category: String) = UnifiedPost(
        postId = id,
        donorId = donorId,
        title = foodTitle,
        description = foodDescription,
        imageUrl = foodImage,
        location = location,
        profileName = profileName,
        timestamp = timestamp,
        category = category
    )

    private fun DonationPostsClothes.toUnified(id: String?, category: String) = UnifiedPost(
        postId = id,
        donorId = donorId,
        title = clothesTitle,
        description = clothesDescription,
        imageUrl = clothesImage,
        location = location,
        profileName = profileName,
        timestamp = timestamp,
        category = category
    )

    private fun DonationPostsElectronics.toUnified(id: String?, category: String) = UnifiedPost(
        postId = id,
        donorId = donorId,
        title = electronicsTitle,
        description = electronicsDescription,
        imageUrl = electronicsImage,
        location = location,
        profileName = profileName,
        timestamp = timestamp,
        category = category
    )

    private fun updateUI() {
        val (food, clothes, electronics) = listOf("Food", "Clothes", "Electronics").map { cat ->
            userPosts.filter { it.category == cat }
        }

        binding.foodCountText.text = food.size.toString()
        binding.clothesCountText.text = clothes.size.toString()
        binding.electronicsCountText.text = electronics.size.toString()
        binding.totalCount.text = userPosts.size.toString()

        loadPreview(binding.foodPreview, food.firstOrNull())
        loadPreview(binding.clothesPreview, clothes.firstOrNull())
        loadPreview(binding.electronicsPreview, electronics.firstOrNull())

        binding.noDataLayout.visibility = if (userPosts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadPreview(card: CardView, post: UnifiedPost?) {
        if (post != null) {
            card.visibility = View.VISIBLE

            // Choose the correct views based on the category
            val image: ImageView
            val title: TextView
            val location: TextView

            when (post.category) {
                "Food" -> {
                    image = card.findViewById(R.id.foodPreviewImage)
                    title = card.findViewById(R.id.foodPreviewTitle)
                    location = card.findViewById(R.id.foodPreviewLocation)
                }
                "Clothes" -> {
                    image = card.findViewById(R.id.clothesPreviewImage)
                    title = card.findViewById(R.id.clothesPreviewTitle)
                    location = card.findViewById(R.id.clothesPreviewLocation)
                }
                "Electronics" -> {
                    image = card.findViewById(R.id.electronicsPreviewImage)
                    title = card.findViewById(R.id.electronicsPreviewTitle)
                    location = card.findViewById(R.id.electronicsPreviewLocation)
                }
                else -> return
            }

            title.text = post.title ?: "No title"
            location.text = post.location ?: "No location"
            Glide.with(this).load(post.imageUrl).placeholder(R.drawable.placeholder).into(image)
        } else {
            card.visibility = View.GONE
        }
    }

    private fun showNoData() {
        binding.noDataLayout.visibility = View.VISIBLE
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }
}
