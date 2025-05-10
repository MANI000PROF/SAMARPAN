package com.example.samarpan.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.User
import com.example.samarpan.databinding.ItemChatUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatListAdapter(
    private val chatUserIds: List<String>, // List of user IDs from participants
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatUserIds[position])
    }

    override fun getItemCount(): Int = chatUserIds.size

    inner class ChatViewHolder(private val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(userId: String) {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        binding.userName.text = user.fullName

                        Glide.with(binding.userImage.context)
                            .load(user.profileImageUrl ?: "")
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .circleCrop()
                            .into(binding.userImage)

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
                                        android.text.format.DateFormat.format("hh:mm a", it).toString()
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
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}
