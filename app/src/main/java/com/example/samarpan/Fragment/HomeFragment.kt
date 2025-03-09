package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.samarpan.Adapter.PostAdapter
import com.example.samarpan.Model.DonationPost
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val postList = ArrayList<DonationPost>()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Image slider setup
        val imageList = ArrayList<SlideModel>()
        imageList.add(SlideModel(R.drawable.donation1, ScaleTypes.CENTER_CROP))
        imageList.add(SlideModel(R.drawable.donation5, ScaleTypes.CENTER_CROP))
        imageList.add(SlideModel(R.drawable.donation6, ScaleTypes.CENTER_CROP))
        binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)

        // RecyclerView setup with a click listener
        postAdapter = PostAdapter(postList) { selectedPost ->
            // Use NavController to navigate to PostInfoFragment
            openPostInfoFragment(selectedPost)
        }
        binding.postRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.postRecyclerView.adapter = postAdapter

        // Firebase setup
        database = FirebaseDatabase.getInstance().getReference("donationPosts")
        loadPosts()

        // Filter functionality
        binding.filterBtn.setOnClickListener {
            val location = binding.locationEditText.text.toString()
            val filteredPosts = postList.filter { it.location?.contains(location, true) ?: false }
            postAdapter.updatePostList(filteredPosts)
        }

        // Add Post Button functionality
        binding.addPostBtn.setOnClickListener {
            val addPostBottomSheet = AddPostBottomSheet()
            addPostBottomSheet.setOnPostAddedListener { newPost ->
                addPostToFirebase(newPost)
            }
            addPostBottomSheet.show(parentFragmentManager, "AddPostBottomSheet")
        }
    }

    // Using NavController for navigation instead of manually replacing fragments
    private fun openPostInfoFragment(selectedPost: DonationPost) {
        val bundle = Bundle().apply {
            putSerializable("post_data", selectedPost) // Passing selected post data
        }

        // Navigate to PostInfoFragment using the NavController
        findNavController().navigate(R.id.action_currentFragment_to_postInfoFragment, bundle)
    }

    private fun addPostToFirebase(post: DonationPost) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get the current user's ID
        val postId = database.push().key ?: return

        val updatedPost = post.copy(userId = currentUserId) // Add userId to the post
        database.child(postId).setValue(updatedPost).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                postList.add(updatedPost)
                postAdapter.notifyItemInserted(postList.size - 1)
                binding.noPostsTextView.visibility = View.GONE
                binding.postRecyclerView.visibility = View.VISIBLE
            }
        }
    }



    private fun loadPosts() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (dataSnapshot in snapshot.children) {
                    val post = dataSnapshot.getValue(DonationPost::class.java)
                    if (post != null) postList.add(post)
                }

                if (postList.isEmpty()) {
                    binding.noPostsTextView.visibility = View.VISIBLE
                    binding.postRecyclerView.visibility = View.GONE
                } else {
                    binding.noPostsTextView.visibility = View.GONE
                    binding.postRecyclerView.visibility = View.VISIBLE
                }

                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
