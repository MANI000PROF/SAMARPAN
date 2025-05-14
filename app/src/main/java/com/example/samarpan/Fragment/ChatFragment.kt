package com.example.samarpan.Fragment

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.example.samarpan.Adapter.ChatListAdapter
import com.example.samarpan.ChatActivity
import com.example.samarpan.R
import com.example.samarpan.databinding.FragmentChatBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    data class ChatUser(
        val id: String = "",
        val fullName: String = "",
        val profileImageUrl: String? = null,
        val lastMessageTime: Long = 0L
    )

    private lateinit var adapter: ChatListAdapter
    private lateinit var archivedAdapter: ChatListAdapter

    private val chatUsers = mutableListOf<ChatUser>()
    private val fullChatUsers = mutableListOf<ChatUser>()
    private val archivedUsers = mutableListOf<ChatUser>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val databaseRef = FirebaseDatabase.getInstance().reference

    private var archivedVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()
        loadChatUsers()
        setupSearch()
        setupSwipeRefresh()
    }

    private fun setupRecyclerViews() {
        adapter = ChatListAdapter { user ->
            openChat(user.id)
        }
        binding.recyclerViewChats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChats.adapter = adapter
        archivedAdapter = ChatListAdapter { user ->
            openChat(user.id)
        }
        binding.recyclerViewArchived.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewArchived.adapter = archivedAdapter

        binding.recyclerViewChats.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                binding.swipeRefreshLayout.isEnabled = !rv.canScrollVertically(-1)
            }
        })

        attachSwipeGestures()
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener {
            val query = it.toString().trim()
            if (query.isEmpty()) {
                adapter.updateList(fullChatUsers)

                binding.recyclerViewChats.visibility = if (fullChatUsers.isNotEmpty()) View.VISIBLE else View.GONE
                binding.noChatsContainer.visibility = if (fullChatUsers.isEmpty()) View.VISIBLE else View.GONE
                binding.notFoundContainer.visibility = View.GONE
            } else {
                val filtered = fullChatUsers.filter { user ->
                    user.fullName.contains(query, ignoreCase = true)
                }
                adapter.updateList(filtered)

                // Handle visibility
                binding.recyclerViewChats.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
                binding.noChatsContainer.visibility = View.GONE // Never show "no chats" during search
                binding.notFoundContainer.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }


    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.blue, R.color.teal_700, R.color.black
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (archivedVisible) {
                hideArchivedChats()
            } else {
                showArchivedChats()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun openChat(userId: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("receiverId", userId)
        startActivity(intent)
    }

    private fun loadChatUsers() {
        val tempChatUsers = mutableListOf<ChatUser>()
        val tempFullChatUsers = mutableListOf<ChatUser>()

        val chatRef = databaseRef.child("chats")
        chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userIds = mutableMapOf<String, Long>() // Use Map to store last message timestamp for each user
                val archivedMap = mutableMapOf<String, Boolean>()
                snapshot.children.forEach { chatSnap ->
                    val participants = chatSnap.child("participants").children.mapNotNull { it.key }
                    if (currentUserId in participants) {
                        // Get the last message timestamp
                        val lastMsgTime = chatSnap.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                        val otherUserId = participants.firstOrNull { it != currentUserId }
                        val isArchived = chatSnap.child("archived").child(currentUserId).getValue(Boolean::class.java) ?: false
                        if (otherUserId != null && !isArchived) {
                            // Update the map with the last message timestamp
                            userIds[otherUserId] = lastMsgTime
                        }
                    }
                }

                // Now fetch users and combine with the last message timestamps
                databaseRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnap: DataSnapshot) {
                        for (userSnap in usersSnap.children) {
                            val user = userSnap.getValue(ChatUser::class.java)?.copy(id = userSnap.key ?: "",
                                lastMessageTime = userIds[userSnap.key] ?: 0L)
                            if (user != null && user.id in userIds) {
                                tempChatUsers.add(user)
                                tempFullChatUsers.add(user)
                            }
                        }

                        // Clear and update your actual lists
                        chatUsers.clear()
                        fullChatUsers.clear()
                        chatUsers.addAll(tempChatUsers)
                        fullChatUsers.addAll(tempFullChatUsers)

                        // Sort both lists by last message time in descending order
                        tempChatUsers.sortByDescending { it.lastMessageTime }
                        tempFullChatUsers.sortByDescending { it.lastMessageTime }

                        // Check if there are any chats, otherwise show "no chats" message
                        binding.noChatsContainer.visibility = if (chatUsers.isEmpty()) View.VISIBLE else View.GONE

                        // Update the adapter with fresh data
                        adapter.updateList(chatUsers)

                        tempChatUsers.clear()
                        tempFullChatUsers.clear()
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showArchivedChats() {
        archivedVisible = true
        binding.archivedHeader.visibility = View.VISIBLE
        binding.recyclerViewArchived.visibility = View.VISIBLE
        binding.recyclerViewArchived.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down_fade_in))
        archivedUsers.clear()

        val chatRef = databaseRef.child("chats")
        chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val archivedIds = mutableMapOf<String, Long>()

                snapshot.children.forEach { chatSnap ->
                    val participants = chatSnap.child("participants").children.mapNotNull { it.key }
                    val otherId = participants.firstOrNull { it != currentUserId }
                    val isArchived = chatSnap.child("archived").child(currentUserId).getValue(Boolean::class.java) ?: false
                    val lastMsgTime = chatSnap.child("lastMessageTime").getValue(Long::class.java) ?: 0L

                    if (currentUserId in participants && isArchived && otherId != null) {
                        archivedIds[otherId] = lastMsgTime
                    }
                }

                databaseRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnap: DataSnapshot) {
                        for (userSnap in usersSnap.children) {
                            val uid = userSnap.key ?: continue
                            val user = userSnap.getValue(ChatUser::class.java)?.copy(
                                id = uid,
                                lastMessageTime = archivedIds[uid] ?: 0L
                            )
                            if (user != null && archivedIds.containsKey(uid)) {
                                archivedUsers.add(user)
                            }
                        }

                        archivedUsers.sortByDescending { it.lastMessageTime }
                        archivedAdapter.updateList(archivedUsers)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun archiveChat(chatId: String, user: ChatUser, position: Int) {
        val archiveRef = databaseRef.child("chats").child(chatId).child("archived").child(currentUserId)
        archiveRef.setValue(true).addOnSuccessListener {
            chatUsers.removeAt(position)
            fullChatUsers.removeAll { it.id == user.id }
            adapter.notifyItemRemoved(position)
            loadChatUsers()
            Toast.makeText(requireContext(), "Chat archived", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unarchiveChat(chatId: String, user: ChatUser, position: Int) {
        val archiveRef = databaseRef.child("chats").child(chatId).child("archived").child(currentUserId)
        archiveRef.removeValue().addOnSuccessListener {
            archivedUsers.removeAt(position)
            archivedAdapter.notifyItemRemoved(position)
            loadChatUsers()
            showArchivedChats()
            Toast.makeText(requireContext(), "Chat unarchived", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideArchivedChats() {
        archivedVisible = false
        binding.archivedHeader.visibility = View.GONE
        binding.recyclerViewArchived.visibility = View.GONE
    }

    private fun attachSwipeGestures() {
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
        val archiveIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit)!!
        val background = ColorDrawable()
        val red = Color.parseColor("#f44336")
        val green = Color.parseColor("#4CAF50")

        // --- Chat List Swipe ---
        val chatTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= chatUsers.size) {
                    adapter.notifyDataSetChanged()
                    return
                }

                val user = chatUsers[position]
                val chatId = generateChatId(currentUserId, user.id)
                vh.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                when (direction) {
                    ItemTouchHelper.LEFT -> deleteChat(chatId, user, position)
                    ItemTouchHelper.RIGHT -> archiveChat(chatId, user, position)
                }
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
                drawSwipeBackground(c, vh, dX, deleteIcon, archiveIcon, background, red, green)
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        })

        // --- Archived List Swipe ---
        val archivedTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= archivedUsers.size) {
                    archivedAdapter.notifyDataSetChanged()
                    return
                }
                val user = archivedUsers[position]
                val chatId = generateChatId(currentUserId, user.id)
                vh.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                when (direction) {
                    ItemTouchHelper.RIGHT -> unarchiveChat(chatId, user, position)
                    else -> archivedAdapter.notifyItemChanged(position)
                }
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
                drawSwipeBackground(c, vh, dX, deleteIcon, archiveIcon, background, red, green)
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        })

        chatTouchHelper.attachToRecyclerView(binding.recyclerViewChats)
        archivedTouchHelper.attachToRecyclerView(binding.recyclerViewArchived)
    }

    private fun drawSwipeBackground(
        c: Canvas,
        vh: RecyclerView.ViewHolder,
        dX: Float,
        deleteIcon: Drawable,
        archiveIcon: Drawable,
        background: ColorDrawable,
        red: Int,
        green: Int
    ) {
        val itemView = vh.itemView
        val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2

        if (dX > 0) {
            background.color = green
            background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            background.draw(c)
            archiveIcon.setBounds(
                itemView.left + iconMargin, itemView.top + iconMargin,
                itemView.left + iconMargin + archiveIcon.intrinsicWidth,
                itemView.top + iconMargin + archiveIcon.intrinsicHeight
            )
            archiveIcon.draw(c)
        } else {
            background.color = red
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            background.draw(c)
            deleteIcon.setBounds(
                itemView.right - iconMargin - deleteIcon.intrinsicWidth,
                itemView.top + iconMargin,
                itemView.right - iconMargin,
                itemView.top + iconMargin + deleteIcon.intrinsicHeight
            )
            deleteIcon.draw(c)
        }
    }


    private fun deleteChat(chatId: String, user: ChatUser, position: Int) {
        val chatRef = databaseRef.child("chats").child(chatId)
        chatRef.get().addOnSuccessListener { snapshot ->
            val backup = snapshot.value
            chatRef.removeValue().addOnSuccessListener {
                chatUsers.removeAt(position)
                fullChatUsers.removeAll { it.id == user.id }
                adapter.notifyItemRemoved(position)

                Snackbar.make(binding.root, "Chat deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        if (backup != null) {
                            chatRef.setValue(backup)
                            loadChatUsers()
                        }
                    }.show()
            }
        }
    }

    private fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "$uid1-$uid2" else "$uid2-$uid1"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}