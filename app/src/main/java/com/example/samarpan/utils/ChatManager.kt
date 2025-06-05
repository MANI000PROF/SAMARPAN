package com.example.samarpan.utils

import android.content.Context
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.samarpan.Model.ChatMessage
import com.google.firebase.database.*
import java.io.File

class ChatManager(private val context: Context, private val chatId: String) {

    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val messagesRef: DatabaseReference = databaseRef.child("chats").child(chatId).child("messages")
    private var audioFilePath: String = ""

    fun sendMessage(senderId: String, receiverId: String, message: String, replyToMessageId: String? = null) {
        val chatMessage = ChatMessage(
            senderId = senderId,
            receiverId = receiverId,
            message = message,
            timestamp = System.currentTimeMillis(),
            replyToMessageId = replyToMessageId // ✅ include this
        )
        messagesRef.push().setValue(chatMessage)

        // Update participants list
        databaseRef.child("chats").child(chatId).child("participants")
            .updateChildren(mapOf(senderId to true, receiverId to true))
    }


    fun sendAudioMessage(audioPath: String, senderId: String, receiverId: String, replyToMessageId: String? = null) {
        val audioFile = File(audioPath)
        if (!audioFile.exists()) return

        // Upload audio to Cloudinary using signed (authenticated) upload
        MediaManager.get().upload(audioPath)
            .option("resource_type", "auto") // important for audio files
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val uploadedUrl = resultData["secure_url"] as? String ?: return

                    // Send audio message with the Cloudinary URL
                    val audioMessage = ChatMessage(
                        senderId = senderId,
                        receiverId = receiverId,
                        message = "",
                        audioUrl = uploadedUrl,
                        timestamp = System.currentTimeMillis(),
                        replyToMessageId = replyToMessageId
                    )
                    messagesRef.push().setValue(audioMessage)

                    // Update participants list
                    databaseRef.child("chats").child(chatId).child("participants")
                        .updateChildren(mapOf(senderId to true, receiverId to true))
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(context, "Upload failed: ${error?.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

}
