package com.example.samarpan.Adapter

import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.DonationPostsClothes
import com.example.samarpan.R
import com.example.samarpan.databinding.PostItemBinding

class PostClothesAdapter(
    private var postList: MutableList<DonationPostsClothes>,
    private var userLocation: Location? = null,
    private val onPostClick: (DonationPostsClothes) -> Unit
) : RecyclerView.Adapter<PostClothesAdapter.PostViewHolder>() {

    inner class PostViewHolder(private val binding: PostItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: DonationPostsClothes) {
            binding.profileName.text = post.profileName ?: "Unknown"
            binding.location.text = post.location ?: "Unknown"
            binding.foodTitle.text = post.clothesTitle ?: "Unknown" // Replace this with clothesTitle in XML for clothes posts

            // Load image with Glide (Make sure the image is related to clothes)
            Glide.with(binding.foodImage.context)
                .load(post.clothesImage ?: "") // You may want to change the foodImage reference to clothesImage
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

    fun updatePostList(newPosts: List<DonationPostsClothes>, userLocation: Location?) {
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
