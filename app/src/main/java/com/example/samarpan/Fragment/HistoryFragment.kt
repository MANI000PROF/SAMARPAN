package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Model.DonationPost
import com.example.samarpan.R
import com.example.samarpan.adapter.HistoryAdapter
import com.example.samarpan.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var historyAdapter: HistoryAdapter
    private val postList = mutableListOf<DonationPost>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        historyAdapter = HistoryAdapter(requireContext(), postList)
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("donationPosts")

        // Load user's posts
        loadUserPosts()
    }

    private fun loadUserPosts() {
        if (currentUserId == null) {
            showNoPosts()
            return
        }

        // Query posts created by the current user
        database.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postList.clear()
                    for (dataSnapshot in snapshot.children) {
                        val post = dataSnapshot.getValue(DonationPost::class.java)
                        if (post != null) {
                            postList.add(post)
                        }
                    }

                    if (postList.isEmpty()) {
                        showNoPosts()
                    } else {
                        showPosts()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error (optional: show a message to the user)
                }
            })
    }

    private fun showNoPosts() {
        binding.donationPostImg.visibility = View.VISIBLE
        binding.historyRecyclerView.visibility = View.GONE
    }

    private fun showPosts() {
        binding.donationPostImg.visibility = View.GONE
        binding.historyRecyclerView.visibility = View.VISIBLE
        historyAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
