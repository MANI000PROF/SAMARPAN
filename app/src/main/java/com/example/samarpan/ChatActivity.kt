package com.example.samarpan

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.samarpan.Adapter.ChatMessageAdapter
import com.example.samarpan.Model.ChatMessage
import com.example.samarpan.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private val messageKeyList = mutableListOf<String>()

    private lateinit var senderId: String
    private lateinit var receiverId: String
    private lateinit var chatId: String

    private lateinit var databaseRef: DatabaseReference
    private lateinit var messagesRef: DatabaseReference
    private var messageListener: ChildEventListener? = null

    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""
    private var isRecording = false
    private val timestampVisibilityMap = mutableMapOf<String, Boolean>()
    private var replyToMessage: ChatMessage? = null
    private var recordingDialog: AlertDialog? = null
    private var initialX = 0f
    private var isCancelled = false
    private lateinit var recordingView: View
    private var waveformRunnable: Runnable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        receiverId = intent.getStringExtra("receiverId") ?: run {
            Toast.makeText(this, "Missing receiverId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatId = if (senderId < receiverId) "$senderId-$receiverId" else "$receiverId-$senderId"
        databaseRef = FirebaseDatabase.getInstance().reference
        messagesRef = databaseRef.child("chats").child(chatId).child("messages")

        fetchReceiverInfo()
        setupRecyclerView()
        listenForMessages()
        listenToTypingStatus()
        setupTypingDetection()

        binding.sendButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val messageText = binding.userInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.userInput.setText("")
            }
        }

        binding.micButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            if (isRecording) {
                stopRecording(send = true)
            } else {
                startRecording()
            }
        }


        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        FirebaseDatabase.getInstance().reference
            .child("activeChats")
            .child(senderId)
            .setValue(receiverId) // mark this user as chatting with receiver

    }

    private fun startRecording() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 200)
            return
        }

        val outputFile = File.createTempFile("audio_", ".3gp", cacheDir)
        audioFilePath = outputFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)
        }

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            isRecording = true
            isCancelled = false

            showRecordingDialog()
            binding.micButton.setImageResource(R.drawable.ic_record)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    private fun stopRecording(send: Boolean) {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
            stopWaveformUpdates()
            runOnUiThread {
                binding.micButton.setImageResource(R.drawable.ic_mic)
                dismissRecordingDialog()

                if (send && !isCancelled && !audioFilePath.isNullOrEmpty()) {
                    sendAudioMessage(audioFilePath!!)
                    Toast.makeText(this, "Recording sent!", Toast.LENGTH_SHORT).show()
                } else {
                    deleteAudioFileIfExists()
                    Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showRecordingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recording, null)
        val micAnimation = dialogView.findViewById<LottieAnimationView>(R.id.micAnimation)
        val waveformView = dialogView.findViewById<WaveformView>(R.id.waveformView)

        recordingDialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        recordingDialog?.show()
        micAnimation.playAnimation()
        startWaveformUpdates(waveformView)

        enableSwipeToCancel(dialogView)

        // 🔥 Tap dialog to stop and send recording
        dialogView.setOnClickListener {
            stopRecording(send = true)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableSwipeToCancel(dialogView: View) {
        dialogView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.x
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - initialX
                    if (deltaX < -150) { // left swipe
                        if (isRecording) {
                            cancelRecording()
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun cancelRecording() {
        stopWaveformUpdates()
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: IllegalStateException) {
                    // Already stopped or not recording
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaRecorder = null
        isRecording = false
        isCancelled = true
        binding.micButton.setImageResource(R.drawable.ic_mic)
        dismissRecordingDialog()
        Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun startWaveformUpdates(waveformView: WaveformView) {
        waveformRunnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    val amplitude = mediaRecorder?.maxAmplitude ?: 0
                    waveformView.addAmplitude(amplitude)
                    waveformView.invalidate()
                    waveformView.postDelayed(this, 100)
                }
            }
        }
        waveformRunnable?.run()
    }

    private fun stopWaveformUpdates() {
        waveformRunnable = null
    }

    private fun dismissRecordingDialog() {
        recordingDialog?.dismiss()
        recordingDialog = null
    }

    private fun deleteAudioFileIfExists() {
        audioFilePath?.let {
            val file = File(it)
            if (file.exists()) file.delete()
        }
    }


    override fun onPause() {
        super.onPause()
        val typingRef = databaseRef.child("chats").child(chatId).child("typing").child(senderId)
        typingRef.setValue(false)
    }

    private fun sendAudioMessage(audioPath: String) {
        val audioFile = File(audioPath)
        if (!audioFile.exists()) return

        val audioMessage = ChatMessage(
            senderId = senderId,
            receiverId = receiverId,
            message = "", // keep text empty for audio messages
            audioUrl = audioFile.absolutePath, // temporarily local path
            timestamp = System.currentTimeMillis()
        )

        val newMsgRef = messagesRef.push()
        newMsgRef.setValue(audioMessage)

        // Update participants list
        databaseRef.child("chats").child(chatId).child("participants")
            .updateChildren(mapOf(senderId to true, receiverId to true))
    }

    private fun fetchReceiverInfo() {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId)
        userRef.keepSynced(true)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fullName = snapshot.child("fullName").value?.toString() ?: "User"
                val profileUrl = snapshot.child("profileImageUrl").value?.toString()
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false

                binding.userTitle.text = fullName

                if (isOnline) {
                    binding.onlineStatus.visibility = View.VISIBLE
                } else {
                    binding.onlineStatus.visibility = View.GONE
                }

                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(this@ChatActivity)
                        .load(profileUrl)
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun listenToTypingStatus() {
        val typingRef = databaseRef.child("chats").child(chatId).child("typing").child(receiverId)
        typingRef.keepSynced(true)
        typingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                binding.typingAnimation.visibility = if (isTyping) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupTypingDetection() {
        val typingRef = databaseRef.child("chats").child(chatId).child("typing").child(senderId)

        binding.userInput.setOnFocusChangeListener { _, hasFocus ->
            typingRef.setValue(hasFocus && binding.userInput.text.isNotEmpty())
        }

        binding.userInput.addTextChangedListener {
            typingRef.setValue(!it.isNullOrEmpty())
        }
    }

    private fun showDeleteConfirmation(message: ChatMessage, messageKey: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete message?")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                messagesRef.child(messageKey).removeValue().addOnSuccessListener {
                    messageList.removeAt(position)
                    messageKeyList.removeAt(position)
                    chatMessageAdapter.notifyItemRemoved(position)
                    chatMessageAdapter.notifyItemRangeChanged(position, messageList.size)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                TODO("Not yet implemented")
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val message = messageList[position]

                if (direction == ItemTouchHelper.RIGHT) {
                    // Reply to this message
                    viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    enterReplyMode(message)
                } else if (direction == ItemTouchHelper.LEFT) {
                    // Show timestamp
                    showTimestampTemporarily(viewHolder.itemView, message.timestamp)
                    chatMessageAdapter.notifyItemChanged(position) // Reset swipe
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView

                    if (dX < 0) {
                        // Swipe left — show timestamp
                        val paint = Paint().apply {
                            color = Color.GRAY
                            textSize = 36f
                            textAlign = Paint.Align.RIGHT
                        }

                        val timeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(messageList[viewHolder.adapterPosition].timestamp))
                        c.drawText(timeText, itemView.right - 32f, itemView.top + 60f, paint)
                    } else if (dX > 0) {
                        // Swipe right — show reply icon
                        val icon = ContextCompat.getDrawable(this@ChatActivity, R.drawable.ic_reply)
                        icon?.setBounds(itemView.left + 32, itemView.top + 32, itemView.left + 96, itemView.bottom - 32)
                        icon?.draw(c)
                    }

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerViewChat)

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

    private fun listenForMessages() {
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

    private fun sendChatPushNotification(message: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(receiverId)

        userRef.child("fcmToken").get().addOnSuccessListener { tokenSnap ->
            val fcmToken = tokenSnap.getValue(String::class.java)
            if (!fcmToken.isNullOrEmpty()) {
                lifecycleScope.launch {
                    val accessToken = FirebaseAccessToken.getAccessToken(this@ChatActivity)
                    accessToken?.let {
                        val title = "New message from ${FirebaseAuth.getInstance().currentUser?.displayName ?: "Someone"}"
                        sendPushNotification(it, fcmToken, title, message)
                    }
                }
            }
        }
    }

    private fun sendPushNotification(
        accessToken: String,
        fcmToken: String,
        title: String,
        message: String
    )
    {
        val context = this@ChatActivity
        val projectId = "samarpan-42c86" // 🔁 Replace with your actual project ID

        val json = JSONObject()
        val messageObj = JSONObject()
        val notificationObj = JSONObject()

        notificationObj.put("title", title)
        notificationObj.put("body", message)

        messageObj.put("token", fcmToken)
        messageObj.put("notification", notificationObj)
        json.put("message", messageObj)

        val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        val request = object : JsonObjectRequest(
            Method.POST, url, json,
            Response.Listener { response -> Log.d("FCM", "Push sent: $response") },
            Response.ErrorListener { error -> Log.e("FCM", "Error: ${error.message}") }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun sendMessage(text: String) {
        val message = ChatMessage(
            senderId = senderId,
            receiverId = receiverId,
            message = text,
            timestamp = System.currentTimeMillis(),
            replyToMessageId = replyToMessage?.replyToMessageId // Add reply message ID to differentiate
        )

        val newMsgRef = messagesRef.push()
        newMsgRef.setValue(message)

        // Hide the reply layout after sending the message
        binding.replyLayout.visibility = View.GONE
        replyToMessage = null  // Reset the reply message state

        // Update participants list
        databaseRef.child("chats").child(chatId).child("participants")
            .updateChildren(mapOf(senderId to true, receiverId to true))

        val activeChatRef = FirebaseDatabase.getInstance().reference
            .child("activeChats")
            .child(receiverId)

        activeChatRef.get().addOnSuccessListener { snapshot ->
            val currentlyChattingWith = snapshot.getValue(String::class.java)
            val isReceiverInChat = currentlyChattingWith == senderId

            if (!isReceiverInChat) {
                sendChatPushNotification(text)
            }
        }
    }


    override fun onDestroy() {
        messageListener?.let { messagesRef.removeEventListener(it) }
        super.onDestroy()
        FirebaseDatabase.getInstance().reference
            .child("activeChats")
            .child(senderId)
            .removeValue() // clear chat status
    }
}
