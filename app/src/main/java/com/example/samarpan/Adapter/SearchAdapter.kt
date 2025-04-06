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

        fun bind(post: UnifiedPost) {
            title.text = post.title
            location.text = post.location
            profile.text = post.profileName

            Glide.with(itemView.context)
                .load(post.imageUrl)
                .into(image)

            itemView.setOnClickListener { onItemClick?.invoke(post) }
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
