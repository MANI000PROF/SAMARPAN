package com.example.samarpan.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.UnifiedPost
import com.example.samarpan.R

class HistoryAdapter(
    private val context: Context,
    private val postList: List<UnifiedPost>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var onItemLongClick: ((UnifiedPost) -> Unit)? = null

    fun setOnItemLongClickListener(listener: (UnifiedPost) -> Unit) {
        onItemLongClick = listener
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.foodImage)
        val profileName: TextView = itemView.findViewById(R.id.profileName)
        val location: TextView = itemView.findViewById(R.id.location)
        val title: TextView = itemView.findViewById(R.id.foodTitle) // You can rename in XML later if needed
        val category: TextView = itemView.findViewById(R.id.categoryTextView) // Optional: show category tag

        init {
            itemView.setOnLongClickListener {
                val post = postList[adapterPosition]
                onItemLongClick?.invoke(post)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val post = postList[position]
        holder.profileName.text = post.profileName ?: "Unknown"
        holder.location.text = post.location ?: "No location"
        holder.title.text = post.title ?: "No Title"

        // Optional: show post category
        holder.category.text = post.category ?: ""

        Glide.with(context)
            .load(post.imageUrl)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = postList.size
}
