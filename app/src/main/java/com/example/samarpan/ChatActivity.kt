package com.example.samarpan

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy

import com.example.samarpan.Adapter.ChatMessageAdapter
import com.example.samarpan.Model.ChatMessage
import com.example.samarpan.databinding.ActivityChatBinding
import com.example.samarpan.utils.AudioRecordTouchListener
import com.example.samarpan.utils.ChatManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatManager: ChatManager
    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private val messageKeyList = mutableListOf<String>()

    private lateinit var senderId: String
    private lateinit var receiverId: String
    private lateinit var chatId: String

    private lateinit var databaseRef: DatabaseReference
    private var messageListener: ChildEventListener? = null
    private val timestampVisibilityMap = mutableMapOf<String, Boolean>()
    private var replyToMessage: ChatMessage? = null

    private lateinit var profileImage: ImageView
    private lateinit var userTitle: TextView
    private lateinit var onlineStatus: TextView


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        senderId = FirebaseAuth.getInstance().currentUser ?.uid ?: return
        receiverId = intent.getStringExtra("receiverId") ?: run {
            Toast.makeText(this, "Missing receiverId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatId = if (senderId < receiverId) "$senderId-$receiverId" else "$receiverId-$senderId"
        chatManager = ChatManager(this, chatId)

        profileImage = findViewById(R.id.profileImage)
        userTitle = findViewById(R.id.userTitle)
        onlineStatus = findViewById(R.id.onlineStatus)
        initCloudinary()
        loadUserDetails(receiverId)

        setupRecyclerView()
        listenForMessages()
        setupTypingDetection()

        binding.sendButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val messageText = binding.userInput.text.toString().trim()

            if (messageText.isNotEmpty()) {
                // Send with or without reply
                if (replyToMessage != null) {
                    chatManager.sendMessage(
                        senderId = senderId,
                        receiverId = receiverId,
                        message = messageText,
                        replyToMessageId = replyToMessage!!.messageId  // Assuming you have this ID in your model
                    )
                } else {
                    chatManager.sendMessage(senderId, receiverId, messageText)
                }

                binding.userInput.setText("")

                // ✅ CLEAR REPLY MODE
                replyToMessage = null
                binding.replyLayout.visibility = View.GONE
            }
        }

        val micButton = findViewById<ImageButton>(R.id.micButton)
        val waveformView = findViewById<WaveformView>(R.id.recordingWaveform)
        val recordingPopup = findViewById<View>(R.id.recordingPopup)
        val micAnimation = findViewById<LottieAnimationView>(R.id.recordingMicAnimation)
        val timerTextView = findViewById<TextView>(R.id.recordingTimer)
        val swipeHint = findViewById<TextView>(R.id.swipeHint)

        micButton.setOnTouchListener(
            AudioRecordTouchListener(
                context = this,
                waveformView = waveformView,
                recordingPopup = recordingPopup,
                micAnimation = micAnimation,
                timerTextView = timerTextView,
                swipeHint = swipeHint,
                onAudioRecorded = { filePath ->
                    chatManager.sendAudioMessage(
                        audioPath = filePath,
                        senderId = senderId,
                        receiverId = receiverId,
                        replyToMessageId = replyToMessage?.messageId
                    )
                }
            )
        )

        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        FirebaseDatabase.getInstance().reference
            .child("activeChats")
            .child(senderId)
            .setValue(receiverId) // mark this user as chatting with receiver
    }

    private fun loadUserDetails(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("fullName").getValue(String::class.java)
                val profileUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false

                userTitle.text = name ?: "User"
                onlineStatus.visibility = if (isOnline) View.VISIBLE else View.GONE

                // Load profile image (using Glide or Coil)
                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(this@ChatActivity)
                        .load(profileUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log or handle error
            }
        })
    }

    private fun setupRecyclerView() {
        chatMessageAdapter = ChatMessageAdapter(
            messageList,
            senderId,
            timestampVisibilityMap,
            replyResolver = { replyId ->
                messageList.find { it.replyToMessageId == replyId }?.message ?: "Replied message"
            }
        ) { message, position ->
            val key = messageKeyList.getOrNull(position)
            key?.let { showDeleteConfirmation(message, it, position) }
        }

        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatMessageAdapter
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val message = messageList[position]

                if (direction == ItemTouchHelper.RIGHT) {
                    viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    enterReplyMode(message)
                    chatMessageAdapter.notifyItemChanged(position) // <--- Add this line to reset view
                }
                else if (direction == ItemTouchHelper.LEFT) {
                    // Show timestamp
                    showTimestampTemporarily(viewHolder.itemView, message.timestamp)
                    chatMessageAdapter.notifyItemChanged(position) // Reset swipe
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerViewChat)
    }

    private fun listenForMessages() {
        val messagesRef = FirebaseDatabase.getInstance().reference.child("chats").child(chatId).child("messages")
        messageListener = messagesRef.limitToLast(50).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prevChildKey: String?) {
                val message = snapshot.getValue(ChatMessage::class.java)
                val messageKey = snapshot.key ?: return
                message?.let {
                    messageList.add(it)
                    messageKeyList.add(messageKey)
                    chatMessageAdapter.notifyItemInserted(messageList.size - 1)
                    binding.recyclerViewChat.scrollToPosition(messageList.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, prevChildKey: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, prevChildKey: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Error loading messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTypingDetection() {
        val typingRef = FirebaseDatabase.getInstance().reference.child("chats").child(chatId).child("typing").child(senderId)

        binding.userInput.setOnFocusChangeListener { _, hasFocus ->
            typingRef.setValue(hasFocus && binding.userInput.text.isNotEmpty())
        }

        binding.userInput.addTextChangedListener {
            typingRef.setValue(!it.isNullOrEmpty())
        }
    }

    private fun initCloudinary() {
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            val config: MutableMap<String, String> = HashMap()
            config["cloud_name"] = "dwkkfinda"
            config["api_key"] = "316841239362936"
            config["api_secret"] = "6Hlnwg4rEfE4-ytS_WrgP5tpySs"

            MediaManager.init(this, config)
            MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
                .maxConcurrentRequests(4)
                .networkPolicy(UploadPolicy.NetworkType.ANY)
                .build()
        }
    }

    private fun showDeleteConfirmation(message: ChatMessage, messageKey: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete message?")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                FirebaseDatabase.getInstance().reference.child("chats").child(chatId).child("messages").child(messageKey).removeValue().addOnSuccessListener {
                    messageList.removeAt(position)
                    messageKeyList.removeAt(position)
                    chatMessageAdapter.notifyItemRemoved(position)
                    chatMessageAdapter.notifyItemRangeChanged(position, messageList.size)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun enterReplyMode(message: ChatMessage) {
        replyToMessage = message
        binding.replyLayout.visibility = View.VISIBLE
        binding.replyText.text = message.message.ifEmpty { "Audio message" }

        binding.cancelReply.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            replyToMessage = null
            binding.replyLayout.visibility = View.GONE
        }
    }

    private fun showTimestampTemporarily(view: View, timestamp: Long) {
        val key = messageKeyList.getOrNull(binding.recyclerViewChat.getChildAdapterPosition(view)) ?: return
        timestampVisibilityMap[key] = true
        chatMessageAdapter.notifyDataSetChanged()

        view.postDelayed({
            timestampVisibilityMap[key] = false
            chatMessageAdapter.notifyDataSetChanged()
        }, 1500)
    }

    override fun onPause() {
        super.onPause()
        val typingRef = FirebaseDatabase.getInstance().reference.child("chats").child(chatId).child("typing").child(senderId)
        typingRef.setValue(false)
    }

    override fun onDestroy() {
        messageListener?.let { FirebaseDatabase.getInstance().reference.child("chats").child(chatId).child("messages").removeEventListener(it) }
        super.onDestroy()
        FirebaseDatabase.getInstance().reference
            .child("activeChats")
            .child(senderId)
            .removeValue() // clear chat status
    }
}
