package com.example.samarpan.Adapter

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.R
import com.example.samarpan.databinding.PostItemBinding

class PostAdapter(
    private var postList: MutableList<DonationPosts>,
    private var userLocation: Location?,
    private val onPostClick: (DonationPosts) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(private val binding: PostItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: DonationPosts) {
            binding.profileName.text = post.profileName ?: "Unknown"
            binding.location.text = post.location ?: "Unknown"
            binding.foodTitle.text = post.foodTitle ?: "Unknown"

            // Load image with Glide
            Glide.with(binding.foodImage.context)
                .load(post.foodImage ?: "")
                .placeholder(R.drawable.placeholder)
                .into(binding.foodImage)

            // Calculate and display distance safely
            val currentUserLocation = userLocation
            if (currentUserLocation != null && post.latitude != null && post.longitude != null) {
                val postLocation = Location("").apply {
                    latitude = post.latitude ?: 0.0
                    longitude = post.longitude ?: 0.0
                }
                val distance = currentUserLocation.distanceTo(postLocation).div(1000) ?: 0.0

                binding.distanceText?.text = String.format("%.2f km away", distance)
                binding.distanceText?.visibility = View.VISIBLE
            } else {
                binding.distanceText?.visibility = View.GONE
            }

            // Click listener
            binding.root.setOnClickListener { onPostClick(post) }
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

    fun updatePostList(newPosts: List<DonationPosts>, userLocation: Location?) {
        this.userLocation = userLocation
        postList.clear()

        if (userLocation != null) {
            postList.addAll(newPosts.sortedBy { post ->
                val postLoc = Location("").apply {
                    latitude = post.latitude ?: 0.0
                    longitude = post.longitude ?: 0.0
                }
                userLocation.distanceTo(postLoc)
            })
        } else {
            postList.addAll(newPosts)
        }

        notifyDataSetChanged()
    }
}
