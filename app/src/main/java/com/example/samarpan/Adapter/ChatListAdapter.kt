package com.example.samarpan.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Fragment.ChatFragment
import com.example.samarpan.databinding.ItemChatUserBinding
import com.example.samarpan.R
import android.text.format.DateFormat
import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.example.samarpan.FullScreenImageActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatListAdapter(
    private val onUserClick: (ChatFragment.ChatUser) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private val chatUsers = mutableListOf<ChatFragment.ChatUser>()

    fun updateList(newList: List<ChatFragment.ChatUser>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = chatUsers.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return chatUsers[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return chatUsers[oldItemPosition] == newList[newItemPosition]
            }
        })

        chatUsers.clear()
        chatUsers.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        if (position < 0 || position >= chatUsers.size) {
            Log.w("ChatListAdapter", "Skipping bind: invalid position $position for size ${chatUsers.size}")
            return
        }
        val user = chatUsers[position]
        holder.bind(user)
        Log.d("ChatListAdapter", "Binding user: ${user.fullName}")
    }


    override fun getItemCount(): Int = chatUsers.size

    inner class ChatViewHolder(private val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: ChatFragment.ChatUser) {
            binding.userName.text = user.fullName

            Glide.with(binding.userImage.context)
                .load(user.profileImageUrl ?: "")
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.userImage)

            // Load last message
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val chatId = if (currentUid < user.id) "$currentUid-${user.id}" else "${user.id}-$currentUid"

            val lastMessageRef = FirebaseDatabase.getInstance()
                .reference.child("chats").child(chatId).child("messages")

            lastMessageRef.limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val lastMessageSnap = snapshot.children.first()
                        val message = lastMessageSnap.child("message").getValue(String::class.java)
                        val timestamp = lastMessageSnap.child("timestamp").getValue(Long::class.java)

                        binding.lastMessage.text = message ?: ""
                        binding.messageTime.text = timestamp?.let {
                            DateFormat.format("hh:mm a", it).toString()
                        } ?: ""
                    } else {
                        binding.lastMessage.text = ""
                        binding.messageTime.text = ""
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            binding.root.setOnClickListener {
                onUserClick(user)
            }
            binding.userImage.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, FullScreenImageActivity::class.java)
                intent.putExtra("image_url", user.profileImageUrl)
                context.startActivity(intent)
            }
        }
    }
}
