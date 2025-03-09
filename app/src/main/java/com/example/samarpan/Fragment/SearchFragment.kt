package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.Model.DonationPost
import com.example.samarpan.R
import com.example.samarpan.Adapter.PostAdapter
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var postAdapter: PostAdapter
    private var postList = mutableListOf<DonationPost>()
    private var filteredList = mutableListOf<DonationPost>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("donationPosts")

        // Initialize RecyclerView with the PostAdapter
        postAdapter = PostAdapter(filteredList) { post ->
            // Handle post item click here (for example, open a detailed view)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.searchRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = postAdapter

        // Initialize SearchView and set query listener
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Trigger search when user submits query
                filterPosts(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Trigger search when user types
                filterPosts(newText)
                return true
            }
        })

        // Load all posts initially
        loadPosts()
    }

    private fun loadPosts() {
        // Fetch posts from Firebase database
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (dataSnapshot in snapshot.children) {
                    val post = dataSnapshot.getValue(DonationPost::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }
                filteredList = postList.toMutableList() // Initially show all posts
                postAdapter.updatePostList(filteredList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any database errors (optional)
            }
        })
    }

    private fun filterPosts(query: String?) {
        // Filter the posts based on the search query
        filteredList = if (query.isNullOrEmpty()) {
            postList // If query is empty, show all posts
        } else {
            postList.filter {
                // Check if any of the fields (foodTitle, location, profileName) contains the query (case-insensitive)
                it.foodTitle?.contains(query, ignoreCase = true) == true ||
                        it.location?.contains(query, ignoreCase = true) == true ||
                        it.profileName?.contains(query, ignoreCase = true) == true
            }.toMutableList() // Convert the filtered list to MutableList
        }

        // Update the adapter with the filtered list
        postAdapter.updatePostList(filteredList)
    }

}
