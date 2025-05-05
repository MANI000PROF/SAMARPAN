package com.example.samarpan.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.UnifiedPost
import com.example.samarpan.R

class SearchAdapter(
    private var postList: List<UnifiedPost>,
    private val onItemClick: ((UnifiedPost) -> Unit)? = null
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.foodTitle)
        private val location: TextView = itemView.findViewById(R.id.location)
        private val profile: TextView = itemView.findViewById(R.id.profileName)
        private val image: ImageView = itemView.findViewById(R.id.foodImage)
        private val time: TextView = itemView.findViewById(R.id.timeStamp)

        fun bind(post: UnifiedPost) {
            title.text = post.title
            location.text = post.location
            profile.text = post.profileName
            post.timestamp.let {
                time.text = getTimeAgo(it)
            }

            Glide.with(itemView.context)
                .load(post.imageUrl)
                .into(image)

            itemView.setOnClickListener { onItemClick?.invoke(post) }
        }
    }

    private fun getTimeAgo(time: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hrs ago"
            days < 7 -> "$days days ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(time))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return SearchViewHolder(view)
    }

    override fun getItemCount(): Int = postList.size

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(postList[position])
    }

    fun updateList(newList: List<UnifiedPost>) {
        postList = newList
        notifyDataSetChanged()
    }
}
