package com.example.samarpan.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.samarpan.Model.LeaderBoardDonor
import com.example.samarpan.R
import com.example.samarpan.databinding.ActivityRewardsBinding
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private lateinit var currentUserId: String
    private var userDonations = 0
    private var userRank = -1

    private val milestones = listOf(1, 5, 10, 20, 50)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
        }

        loadUserInfo()
        loadLeaderboardRank()
        setupLeaderboardButton()
        showMotivationalQuote()
    }

    private fun loadUserInfo() {
        val postsRefs = listOf(
            "DonationPosts", "DonationPostsClothes", "DonationPostsElectronics"
        )
        var total = 0
        var count = 0

        for (refName in postsRefs) {
            FirebaseDatabase.getInstance().getReference(refName)
                .orderByChild("donorId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        total += snapshot.childrenCount.toInt()
                        if (++count == postsRefs.size) {
                            userDonations = total
                            binding.totalDonations.text = "You've made $total donations"
                            updateMilestones()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.userName.text = "Hello, ${snapshot.child("fullName").value ?: "User"}!"
                Glide.with(this@RewardsActivity)
                    .load(snapshot.child("profileImageUrl").value)
                    .placeholder(R.drawable.profile_placeholder)
                    .circleCrop()
                    .into(binding.profileImage)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadLeaderboardRank() {
        FirebaseDatabase.getInstance().getReference("LeaderBoard")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull { it.getValue(LeaderBoardDonor::class.java) }
                        .sortedByDescending { it.donationCount }

                    userRank = list.indexOfFirst { it.userId == currentUserId } + 1
                    if (userRank > 0) {
                        binding.userRank.text = "Rank: #$userRank"
                    } else {
                        binding.userRank.text = "Not ranked yet"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateMilestones() {
        val inflater = LayoutInflater.from(this)
        binding.badgeContainer.removeAllViews()

        val colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)

        for (milestone in milestones) {
            val view = inflater.inflate(R.layout.item_badge, binding.badgeContainer, false)
            val badgeText = view.findViewById<TextView>(R.id.badgeText)
            val badgeIcon = view.findViewById<ImageView>(R.id.badgeIcon)

            badgeText.text = "$milestone Donations"
            val badgeRes = if (userDonations >= milestone) R.drawable.ic_badge_unlocked else R.drawable.ic_badge_locked
            badgeIcon.setImageResource(badgeRes)
            badgeIcon.setColorFilter(colorOnSurface, PorterDuff.Mode.SRC_IN)

            binding.badgeContainer.addView(view)
        }
    }


    private fun setupLeaderboardButton() {
        binding.viewLeaderboardBtn.setOnClickListener {
            // Open LeaderboardFragment via MainActivity
            finish()
        }
    }

    private fun showMotivationalQuote() {
        val quotes = listOf(
            "Helping one person might not change the world, but it could change the world for one person.",
            "The best way to find yourself is to lose yourself in the service of others.",
            "No act of kindness, no matter how small, is ever wasted."
        )
        val quote = quotes.random()
        binding.motivationQuote.text = "💬 $quote"
    }
}

