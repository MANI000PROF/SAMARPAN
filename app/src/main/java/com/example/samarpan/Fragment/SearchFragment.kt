package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.R
import com.example.samarpan.Adapter.PostAdapter
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var postAdapter: PostAdapter
    private var postList: MutableList<DonationPosts> = mutableListOf()
    private var filteredList: MutableList<DonationPosts> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("DonationPosts")

        // Initialize RecyclerView with the PostAdapter
        postAdapter = PostAdapter(filteredList, null) { post ->
            // Handle post item click here (e.g., navigate to details page)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.searchRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = postAdapter

        // Initialize SearchView and set query listener
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterPosts(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText)
                return true
            }
        })

        // Load all posts initially
        loadPosts()
    }

    private fun loadPosts() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (dataSnapshot in snapshot.children) {
                    val post = dataSnapshot.getValue(DonationPosts::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }
                filteredList = postList.toMutableList() // Initially show all posts
                postAdapter.updatePostList(filteredList, null)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun filterPosts(query: String?) {
        filteredList = if (query.isNullOrEmpty()) {
            postList // Show all posts if query is empty
        } else {
            postList.filter {
                it.foodTitle?.contains(query, ignoreCase = true) == true ||
                        it.location?.contains(query, ignoreCase = true) == true ||
                        it.profileName?.contains(query, ignoreCase = true) == true
            }.toMutableList()
        }

        postAdapter.updatePostList(filteredList, null)
    }
}
