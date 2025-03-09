package com.example.samarpan.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.DonationPost
import com.example.samarpan.R
import com.example.samarpan.databinding.PostItemBinding

class PostAdapter(private var postList: MutableList<DonationPost>, private val onPostClick: (DonationPost) -> Unit) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(private val binding: PostItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: DonationPost) {
            binding.profileName.text = post.profileName
            binding.location.text = post.location
            binding.foodTitle.text = post.foodTitle
            // Log the image URL to verify it's correct
            Log.d("PostAdapter", "Loading image from URL: ${post.foodImage}")
            // Load image from URL into ImageView using Glide
            Glide.with(binding.foodImage.context)
                .load(post.foodImage) // This will load the image URL stored in foodImage
                .placeholder(R.drawable.placeholder)
                .into(binding.foodImage)

            // Set click listener for each post item
            binding.root.setOnClickListener {
                onPostClick(post) // Trigger the callback to open the PostInfoFragment
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postList[position])
    }

    override fun getItemCount(): Int = postList.size

    // Method to update the list when filter is applied
    fun updatePostList(newPosts: List<DonationPost>) {
        postList.clear() // Clear before updating
        postList.addAll(newPosts)
        notifyDataSetChanged()
    }

}

