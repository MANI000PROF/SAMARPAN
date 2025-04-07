package com.example.samarpan.Fragment

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.samarpan.Model.*
import com.example.samarpan.Adapter.SearchAdapter
import com.example.samarpan.R
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchFragment : Fragment() {
    private lateinit var emptyAnimationView: com.airbnb.lottie.LottieAnimationView
    private lateinit var noDataTextView: android.widget.TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchAdapter: SearchAdapter
    private val unifiedList = mutableListOf<UnifiedPost>()
    private val filteredList = mutableListOf<UnifiedPost>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: EditText
    private val cacheKey = "cachedUnifiedPosts"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.searchRecyclerView)
        searchView = view.findViewById(R.id.searchView)

        searchAdapter = SearchAdapter(filteredList)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = searchAdapter

        emptyAnimationView = view.findViewById(R.id.noResultsAnimation)
        noDataTextView = view.findViewById(R.id.noDataTextView)

        swipeRefreshLayout.setOnRefreshListener {
            if (isInternetAvailable()) {
                loadAllPosts()
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        if (isInternetAvailable()) {
            loadAllPosts()
        } else {
            loadFromCache()
        }

        setupSearchBar()
    }

    private fun setupSearchBar() {
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPosts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun loadAllPosts() {
        val db = FirebaseDatabase.getInstance().reference
        val foodRef = db.child("DonationPosts")
        val clothesRef = db.child("DonationPostsClothes")
        val electronicsRef = db.child("DonationPostsElectronics")

        unifiedList.clear()

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val post = data.getValue(DonationPosts::class.java)
                    if (post != null) {
                        unifiedList.add(
                            UnifiedPost(
                                postId = post.postId,
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
                loadClothesPosts(clothesRef, electronicsRef)
            }

            override fun onCancelled(error: DatabaseError) {
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun loadClothesPosts(clothesRef: DatabaseReference, electronicsRef: DatabaseReference) {
        clothesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val post = data.getValue(DonationPostsClothes::class.java)
                    if (post != null) {
                        unifiedList.add(
                            UnifiedPost(
                                postId = post.postId,
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
                loadElectronicsPosts(electronicsRef)
            }

            override fun onCancelled(error: DatabaseError) {
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun loadElectronicsPosts(electronicsRef: DatabaseReference) {
        electronicsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val post = data.getValue(DonationPostsElectronics::class.java)
                    if (post != null) {
                        unifiedList.add(
                            UnifiedPost(
                                postId = post.postId,
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
                saveToCache(unifiedList)
                filteredList.clear()
                filteredList.addAll(unifiedList)
                searchAdapter.updateList(filteredList)
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun filterPosts(query: String?) {
        val q = query?.trim()?.lowercase() ?: ""
        filteredList.clear()
        if (q.isEmpty()) {
            filteredList.addAll(unifiedList)
        } else {
            filteredList.addAll(unifiedList.filter {
                it.title?.contains(q, ignoreCase = true) == true ||
                        it.location?.contains(q, ignoreCase = true) == true ||
                        it.profileName?.contains(q, ignoreCase = true) == true
            })
        }
        searchAdapter.updateList(filteredList)

        // 🎯 Show or hide Lottie animation
        emptyAnimationView.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        noDataTextView.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun saveToCache(posts: List<UnifiedPost>) {
        val prefs = requireContext().getSharedPreferences("SearchCache", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(posts)
        editor.putString(cacheKey, json)
        editor.apply()
    }

    private fun loadFromCache() {
        val prefs = requireContext().getSharedPreferences("SearchCache", Context.MODE_PRIVATE)
        val json = prefs.getString(cacheKey, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<UnifiedPost>>() {}.type
            val cachedPosts: List<UnifiedPost> = Gson().fromJson(json, type)
            unifiedList.clear()
            unifiedList.addAll(cachedPosts)
            filteredList.clear()
            filteredList.addAll(cachedPosts)
            searchAdapter.updateList(filteredList)
        }
    }
}
