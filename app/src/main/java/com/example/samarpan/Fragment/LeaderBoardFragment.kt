package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Adapter.LeaderBoardAdapter
import com.example.samarpan.Model.LeaderBoardDonor
import com.example.samarpan.databinding.FragmentLeaderBoardBinding
import com.google.firebase.database.*

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

        // Initialize RecyclerView
        leaderBoardAdapter = LeaderBoardAdapter(leaderBoardList)
        binding.leaderboardRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaderBoardAdapter
        }

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("DonationPosts")

        fetchLeaderBoardData()
    }

    private fun fetchLeaderBoardData() {
        val leaderBoardMap = mutableMapOf<String, LeaderBoardDonor>()

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                leaderBoardMap.clear()

                for (dataSnapshot in snapshot.children) {
                    val donorId = dataSnapshot.child("userId").getValue(String::class.java) ?: continue
                    val donorName = dataSnapshot.child("profileName").getValue(String::class.java) ?: "Unknown"
                    val donorImage = dataSnapshot.child("foodImage").getValue(String::class.java) ?: ""

                    // Increment donation count
                    val donor = leaderBoardMap.getOrPut(donorId) {
                        LeaderBoardDonor(donorId, donorName, donorImage, 0)
                    }
                    donor.donationCount++
                }

                // Convert to sorted list
                leaderBoardList.clear()
                leaderBoardList.addAll(leaderBoardMap.values.sortedByDescending { it.donationCount })

                // Store in Firebase under LeaderBoard node
                val leaderBoardRef = FirebaseDatabase.getInstance().getReference("LeaderBoard")
                leaderBoardRef.setValue(leaderBoardList)

                // Refresh RecyclerView
                leaderBoardAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                println("LeaderBoardFragment: Firebase Error - ${error.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
