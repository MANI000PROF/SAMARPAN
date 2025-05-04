package com.example.samarpan.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.UnifiedPost
import com.example.samarpan.databinding.ItemSavedPostBinding

class SavedPostsAdapter(
    private val context: Context,
    private val postList: List<UnifiedPost>,
) : RecyclerView.Adapter<SavedPostsAdapter.SavedPostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedPostViewHolder {
        val binding = ItemSavedPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedPostViewHolder, position: Int) {
        val post = postList[position]

        holder.binding.apply {
            savedTitle.text = post.title
            savedDescription.text = post.description
            savedLocation.text = post.location
            savedCategory.text = post.category
            savedProfileName.text = "Donor: ${post.profileName}"

            Glide.with(context)
                .load(post.imageUrl)
                .into(savedImageView)
        }
    }

    override fun getItemCount(): Int = postList.size

    inner class SavedPostViewHolder(val binding: ItemSavedPostBinding) :
        RecyclerView.ViewHolder(binding.root)
}
