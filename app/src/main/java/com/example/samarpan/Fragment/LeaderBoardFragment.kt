package com.example.samarpan.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.samarpan.Adapter.LeaderBoardAdapter
import com.example.samarpan.Model.LeaderBoardDonor
import com.example.samarpan.databinding.FragmentLeaderBoardBinding
import com.google.firebase.database.*
import com.example.samarpan.R

class LeaderBoardFragment : Fragment() {

    private var _binding: FragmentLeaderBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var leaderBoardAdapter: LeaderBoardAdapter
    private val leaderBoardList = mutableListOf<LeaderBoardDonor>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaderBoardAdapter = LeaderBoardAdapter(leaderBoardList)
        binding.leaderboardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leaderboardRecyclerView.adapter = leaderBoardAdapter

        database = FirebaseDatabase.getInstance().getReference("DonationPosts")

        // Enable disk caching
        database.keepSynced(true)

        // Swipe-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchLeaderBoardData()
        }

        fetchLeaderBoardData()
    }

    private fun fetchLeaderBoardData() {
        binding.swipeRefreshLayout.isRefreshing = true

        val leaderBoardMap = mutableMapOf<String, LeaderBoardDonor>()
        val allPostsRefs = listOf(
            FirebaseDatabase.getInstance().getReference("DonationPosts"), // Food
            FirebaseDatabase.getInstance().getReference("DonationPostClothes"), // Clothes
            FirebaseDatabase.getInstance().getReference("DonationPostElectronics") // Electronics
        )

        var remainingFetches = allPostsRefs.size

        fun onDataFetched() {
            if (--remainingFetches == 0) {
                leaderBoardList.clear()
                leaderBoardList.addAll(leaderBoardMap.values.sortedByDescending { it.donationCount })

                // Store to Firebase
                FirebaseDatabase.getInstance().getReference("LeaderBoard").setValue(leaderBoardList)

                // Update RecyclerView
                leaderBoardAdapter.notifyDataSetChanged()

                // Show/Hide based on donors
                if (leaderBoardList.isNotEmpty()) {
                    val topDonor = leaderBoardList.first()
                    if (topDonor.profileImage?.isNotEmpty() == true) {
                        Glide.with(requireContext())
                            .load(topDonor.profileImage)
                            .placeholder(R.drawable.profile_placeholder)
                            .circleCrop()
                            .into(binding.topDonorImage)
                    }
                    binding.topDonorName.text = topDonor.name
                    binding.topDonorDonations.text = "${topDonor.donationCount} Donations"

                    // Show top donor views
                    binding.topDonorImage.visibility = View.VISIBLE
                    binding.topDonorName.visibility = View.VISIBLE
                    binding.topDonorDonations.visibility = View.VISIBLE
                    binding.crownIcon.visibility = View.VISIBLE

                    // Hide empty animation
                    binding.noDonorsAnimation.visibility = View.GONE
                    binding.noDonorsText.visibility = View.GONE
                } else {
                    // No donors case
                    binding.topDonorImage.visibility = View.GONE
                    binding.topDonorName.visibility = View.GONE
                    binding.topDonorDonations.visibility = View.GONE
                    binding.crownIcon.visibility = View.GONE

                    // Show animation
                    binding.noDonorsAnimation.setAnimation(R.raw.empty_box)
                    binding.noDonorsAnimation.visibility = View.VISIBLE
                    binding.noDonorsAnimation.playAnimation()

                    binding.noDonorsText.visibility = View.VISIBLE
                }

                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        for (ref in allPostsRefs) {
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (dataSnapshot in snapshot.children) {
                        val donorId = dataSnapshot.child("donorId").getValue(String::class.java) ?: continue
                        val donorName = dataSnapshot.child("profileName").getValue(String::class.java) ?: "Unknown"
                        val donorImage = dataSnapshot.child("foodImage").getValue(String::class.java)
                            ?: dataSnapshot.child("clothesImage").getValue(String::class.java)
                            ?: dataSnapshot.child("electronicsImage").getValue(String::class.java)
                            ?: ""

                        val donor = leaderBoardMap.getOrPut(donorId) {
                            LeaderBoardDonor(donorId, donorName, donorImage, 0)
                        }
                        donor.donationCount++
                    }
                    Log.d("LeaderBoard", "Fetched ${leaderBoardList.size} donors")
                    onDataFetched()
                }

                override fun onCancelled(error: DatabaseError) {
                    onDataFetched()
                }
            })
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
