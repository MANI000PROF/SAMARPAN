package com.example.samarpan.Fragment

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.ChatActivity
import com.example.samarpan.Adapter.ChatListAdapter
import com.example.samarpan.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.samarpan.R
import com.google.android.material.snackbar.Snackbar

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatListAdapter
    private val chatUserIds = mutableListOf<String>()
    private lateinit var currentUserId: String
    private lateinit var databaseRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        databaseRef = FirebaseDatabase.getInstance().reference

        setupRecyclerView()
        loadChatUsers()
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(chatUserIds) { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("receiverId", user.id)
            startActivity(intent)
        }

        binding.recyclerViewChats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChats.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
            val archiveIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit)!!

            val background = ColorDrawable()
            val backgroundColorDelete = Color.parseColor("#f44336") // red
            val backgroundColorArchive = Color.parseColor("#4CAF50") // green

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val chatUserId = chatUserIds[position]

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        deleteChatForUser(chatUserId, position)
                    }
                    ItemTouchHelper.RIGHT -> {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        archiveChat(chatUserId)
                        adapter.notifyItemChanged(position) // Restore item since archive is mock
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2

                if (dX < 0) { // Swiping left
                    background.color = backgroundColorDelete
                    background.setBounds(
                        itemView.right + dX.toInt(), itemView.top,
                        itemView.right, itemView.bottom
                    )
                    background.draw(c)

                    val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                    val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.draw(c)

                } else if (dX > 0) { // Swiping right
                    background.color = backgroundColorArchive
                    background.setBounds(
                        itemView.left, itemView.top,
                        itemView.left + dX.toInt(), itemView.bottom
                    )
                    background.draw(c)

                    val iconTop = itemView.top + (itemView.height - archiveIcon.intrinsicHeight) / 2
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = iconLeft + archiveIcon.intrinsicWidth
                    val iconBottom = iconTop + archiveIcon.intrinsicHeight

                    archiveIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    archiveIcon.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewChats)

    }

    private fun generateChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"  // Use hyphen instead of underscore
    }

    private fun deleteChatForUser(otherUserId: String, position: Int) {
        val chatId = generateChatId(currentUserId, otherUserId)
        val chatRef = databaseRef.child("chats").child(chatId)

        // Temporarily hold deleted chat data for undo
        chatRef.get().addOnSuccessListener { snapshot ->
            val deletedChatData = snapshot.value

            // Delete entire chat for both users
            chatRef.removeValue().addOnSuccessListener {
                chatUserIds.removeAt(position)
                adapter.notifyItemRemoved(position)

                // Show Snackbar to undo deletion
                Snackbar.make(binding.root, "Chat deleted for both users", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        if (deletedChatData != null) {
                            chatRef.setValue(deletedChatData)
                            chatUserIds.add(position, otherUserId)
                            adapter.notifyItemInserted(position)
                        }
                    }
                    .show()
            }
        }
    }


    private fun archiveChat(otherUserId: String) {
        // This is a mock operation — just a placeholder for future enhancement
        Toast.makeText(requireContext(), "Chat archived", Toast.LENGTH_SHORT).show()
    }

    private fun loadChatUsers() {
        databaseRef.child("chats").get().addOnSuccessListener { snapshot ->
            chatUserIds.clear()
            snapshot.children.forEach { chat ->
                val participants = chat.child("participants").children.mapNotNull { it.key }
                if (participants.contains(currentUserId)) {
                    val otherUserId = participants.firstOrNull { it != currentUserId }
                    otherUserId?.let {
                        if (!chatUserIds.contains(it)) {
                            chatUserIds.add(it)
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            // handle failure (optional)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
