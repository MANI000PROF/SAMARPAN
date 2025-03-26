package com.example.samarpan.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.DonationPosts
import com.example.samarpan.R

class HistoryAdapter(
    private val context: Context,
    private val postList: List<DonationPosts>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    // ViewHolder class
    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodImage: ImageView = itemView.findViewById(R.id.foodImage)
        val profileName: TextView = itemView.findViewById(R.id.profileName)
        val location: TextView = itemView.findViewById(R.id.location)
        val foodTitle: TextView = itemView.findViewById(R.id.foodTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val post = postList[position]

        // Load the data into the views
        holder.profileName.text = post.profileName ?: "Unknown"
        holder.location.text = post.location ?: "Not Specified"
        holder.foodTitle.text = post.foodTitle ?: "No Title"

        // Load the image using Glide
        Glide.with(context)
            .load(post.foodImage ?: R.drawable.placeholder) // Use placeholder if foodImage is null
            .into(holder.foodImage)
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}
