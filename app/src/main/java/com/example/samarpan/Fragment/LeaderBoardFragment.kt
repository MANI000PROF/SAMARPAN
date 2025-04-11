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

    private lateinit var leaderBoardAdapter: LeaderBoardAdapter
    private val leaderBoardList = mutableListOf<LeaderBoardDonor>()

    private val allPostsRefs = listOf(
        FirebaseDatabase.getInstance().getReference("DonationPosts"), // Food
        FirebaseDatabase.getInstance().getReference("DonationPostsClothes"), // Clothes ✅ fixed
        FirebaseDatabase.getInstance().getReference("DonationPostsElectronics") // Electronics ✅ fixed
    )

    private val leaderBoardRef = FirebaseDatabase.getInstance().getReference("LeaderBoard")

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

        // Enable disk caching
        allPostsRefs.forEach { it.keepSynced(true) }
        leaderBoardRef.keepSynced(true)

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchLeaderBoardData()
        }

        fetchLeaderBoardData()
    }

    private fun fetchLeaderBoardData() {
        binding.swipeRefreshLayout.isRefreshing = true

        // Try loading from local cache first
        leaderBoardRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cachedList = mutableListOf<LeaderBoardDonor>()
                for (snap in snapshot.children) {
                    val donor = snap.getValue(LeaderBoardDonor::class.java)
                    if (donor != null) cachedList.add(donor)
                }

                if (cachedList.isNotEmpty()) {
                    leaderBoardList.clear()
                    leaderBoardList.addAll(cachedList)
                    updateUI()
                }

                // Then fetch fresh data from DonationPosts
                fetchAndBuildLiveLeaderBoard()
            }

            override fun onCancelled(error: DatabaseError) {
                fetchAndBuildLiveLeaderBoard() // fallback to live fetch
            }
        })
    }

    private fun fetchAndBuildLiveLeaderBoard() {
        val leaderBoardMap = mutableMapOf<String, LeaderBoardDonor>()
        var remainingFetches = allPostsRefs.size

        fun finalizeLeaderBoard() {
            val userRef = FirebaseDatabase.getInstance().getReference("users")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for ((donorId, donor) in leaderBoardMap) {
                        val profileImageUrl = snapshot.child(donorId).child("profileImageUrl").getValue(String::class.java)
                        if (!profileImageUrl.isNullOrEmpty()) {
                            donor.profileImage = profileImageUrl
                        }
                    }

                    leaderBoardList.clear()
                    leaderBoardList.addAll(leaderBoardMap.values.sortedByDescending { it.donationCount })

                    // Cache updated data
                    leaderBoardRef.setValue(leaderBoardList)

                    updateUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    updateUI()
                }
            })
        }

        fun onDataFetched() {
            if (--remainingFetches == 0) {
                finalizeLeaderBoard()
            }
        }

        for (ref in allPostsRefs) {
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (dataSnapshot in snapshot.children) {
                        val donorId = dataSnapshot.child("donorId").getValue(String::class.java) ?: continue
                        val donorName = dataSnapshot.child("profileName").getValue(String::class.java) ?: "Unknown"

                        val donor = leaderBoardMap.getOrPut(donorId) {
                            LeaderBoardDonor(donorId, donorName, "", 0)
                        }
                        donor.donationCount++
                    }
                    onDataFetched()
                }

                override fun onCancelled(error: DatabaseError) {
                    onDataFetched()
                }
            })
        }
    }



    private fun updateUI() {
        leaderBoardAdapter.notifyDataSetChanged()

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
            binding.topDonorImage.visibility = View.VISIBLE
            binding.topDonorName.visibility = View.VISIBLE
            binding.topDonorDonations.visibility = View.VISIBLE
            binding.crownIcon.visibility = View.VISIBLE
            binding.noDonorsAnimation.visibility = View.GONE
            binding.noDonorsText.visibility = View.GONE
        } else {
            binding.topDonorImage.visibility = View.GONE
            binding.topDonorName.visibility = View.GONE
            binding.topDonorDonations.visibility = View.GONE
            binding.crownIcon.visibility = View.GONE
            binding.noDonorsAnimation.setAnimation(R.raw.empty_box)
            binding.noDonorsAnimation.visibility = View.VISIBLE
            binding.noDonorsAnimation.playAnimation()
            binding.noDonorsText.visibility = View.VISIBLE
        }

        binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
