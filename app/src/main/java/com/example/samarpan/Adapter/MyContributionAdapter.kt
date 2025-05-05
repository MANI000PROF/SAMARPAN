package com.example.samarpan.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.UnifiedPost
import com.example.samarpan.R
import java.text.SimpleDateFormat
import java.util.*

class MyContributionAdapter(private val postList: List<UnifiedPost>) :
    RecyclerView.Adapter<MyContributionAdapter.ContributionViewHolder>() {

    inner class ContributionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val profileName: TextView = itemView.findViewById(R.id.profileName)
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
        val title: TextView = itemView.findViewById(R.id.title)
        val location: TextView = itemView.findViewById(R.id.location)
        val timestamp: TextView = itemView.findViewById(R.id.timeStamp)
        val categoryBadge: TextView = itemView.findViewById(R.id.categoryBadge)
        val cardView: CardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_contribution, parent, false)
        return ContributionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContributionViewHolder, position: Int) {
        val post = postList[position]

        // Bind title and location
        holder.title.text = post.title
        holder.location.text = post.location ?: "Unknown location"

        // Format and set timestamp
        holder.timestamp.text = formatTimeAgo(post.timestamp)

        // Load post image
        Glide.with(holder.itemView.context)
            .load(post.imageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.postImage)

        // Load profile image (optional: fallback to default)
        Glide.with(holder.itemView.context)
            .load(post.profileImageUrl)
            .placeholder(R.drawable.profile)
            .circleCrop()
            .into(holder.profileImage)

        // Set profile name
        holder.profileName.text = post.profileName ?: "Unknown User"

        // Set category badge and color
        holder.categoryBadge.text = post.category
        val badgeColor = when (post.category?.lowercase(Locale.ROOT)) {
            "food" -> R.color.foodColor
            "clothes" -> R.color.clothesColor
            "electronics" -> R.color.electronicsColor
            else -> com.denzcoskun.imageslider.R.color.grey_font
        }
        holder.categoryBadge.setBackgroundResource(badgeColor)
    }

    override fun getItemCount(): Int = postList.size

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
}
