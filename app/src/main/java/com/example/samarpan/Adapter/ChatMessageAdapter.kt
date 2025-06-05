package com.example.samarpan.Adapter

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.Model.ChatMessage
import com.example.samarpan.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessageAdapter(
    private val chatMessages: List<ChatMessage>,
    private val currentUserId: String,
    private val timestampVisibilityMap: Map<String, Boolean>,
    private val replyResolver: (String) -> String,
    private val onMessageLongClick: (ChatMessage, Int) -> Unit
) : RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_chat_sent
        } else {
            R.layout.item_chat_received
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    @Suppress("RecyclerView")
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessage = chatMessages[position]

        // Handle long click
        holder.itemView.setOnLongClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                onMessageLongClick(chatMessages[adapterPosition], adapterPosition)
            }
            true
        }

        // Handle reply
        if (chatMessage.replyToMessageId != null) {
            val repliedMessage = getRepliedMessageById(chatMessage.replyToMessageId!!)
            holder.replyLayout.visibility = View.VISIBLE
            holder.quotedMessageTextView.text = repliedMessage?.message ?: "Message not found"
        } else {
            holder.replyLayout.visibility = View.GONE
        }

        // Handle audio
        if (!chatMessage.audioUrl.isNullOrEmpty()) {
            holder.messageTextView.visibility = View.GONE
            holder.audioLayout.visibility = View.VISIBLE

            holder.playButton.setImageResource(
                if (currentlyPlayingPosition == position && mediaPlayer?.isPlaying == true)
                    R.drawable.ic_pause
                else
                    R.drawable.ic_play
            )

            // Get audio duration
            val duration = getAudioDuration(chatMessage.audioUrl!!)
            holder.audioDuration.text = duration

            holder.playButton.setOnClickListener {
                if (currentlyPlayingPosition == position) {
                    stopAudio(holder)
                } else {
                    playAudio(chatMessage.audioUrl!!, holder)
                    currentlyPlayingPosition = position
                }
            }
        } else {
            holder.audioLayout.visibility = View.GONE
            holder.messageTextView.visibility = View.VISIBLE
            holder.messageTextView.text = chatMessage.message
        }

        // Timestamp
        val showTimestamp = timestampVisibilityMap[chatMessage.messageId] == true
        holder.timestampTextView.visibility = if (showTimestamp) View.VISIBLE else View.GONE
        if (showTimestamp) {
            holder.timestampTextView.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(Date(chatMessage.timestamp))
        }
    }

    private fun getRepliedMessageById(replyToMessageId: String): ChatMessage? {
        return chatMessages.find { it.messageId == replyToMessageId }
    }

    override fun getItemCount(): Int = chatMessages.size

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
        val audioLayout: View = view.findViewById(R.id.audioMessageLayout)
        val playButton: ImageView = view.findViewById(R.id.playButton)
        val audioDuration: TextView = view.findViewById(R.id.audioDuration)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val quotedMessageTextView: TextView = view.findViewById(R.id.quotedMessageTextView)
        val replyLayout: View = view.findViewById(R.id.replyLayout)
    }

    private fun playAudio(url: String, holder: ChatViewHolder) {
        stopAudio(null)

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                prepare()
                start()

                holder.playButton.setImageResource(R.drawable.ic_pause)

                setOnCompletionListener {
                    holder.playButton.setImageResource(R.drawable.ic_play)
                    currentlyPlayingPosition = -1
                    releaseMediaPlayer()
                }

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(holder.itemView.context, "Error playing audio", Toast.LENGTH_SHORT).show()
                holder.playButton.setImageResource(R.drawable.ic_play)
            }
        }
    }

    private fun stopAudio(holder: ChatViewHolder?) {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        currentlyPlayingPosition = -1

        holder?.playButton?.setImageResource(R.drawable.ic_play)
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun getAudioDuration(url: String): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(url, HashMap())
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()

            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / 1000) / 60
            String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}
