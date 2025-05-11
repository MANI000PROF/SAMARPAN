package com.example.samarpan.Adapter

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.Model.ChatMessage
import com.example.samarpan.R
import java.io.IOException

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

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessage = chatMessages[position]
        // Common long click listener for delete
        holder.itemView.setOnLongClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                onMessageLongClick(chatMessages[adapterPosition], adapterPosition)
            }
            true
        }

        // Handle Quoted Message (Reply)
        if (chatMessage.replyToMessageId != null) {
            // Fetch the original message using the `replyToMessageId`
            val repliedMessage = getRepliedMessageById(chatMessage.replyToMessageId!!)
            holder.quotedMessageTextView.visibility = View.VISIBLE
            holder.quotedMessageTextView.text = repliedMessage?.message ?: "Message not found"
        } else {
            holder.quotedMessageTextView.visibility = View.GONE
        }

        if (!chatMessage.audioUrl.isNullOrEmpty()) {
            holder.messageTextView.visibility = View.GONE
            holder.audioLayout.visibility = View.VISIBLE
            holder.audioDuration.text = "Audio"

            holder.playButton.setImageResource(R.drawable.ic_play)
            holder.playButton.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    if (currentlyPlayingPosition == adapterPosition) {
                        stopAudio(holder)
                    } else {
                        playAudio(chatMessage.audioUrl!!, holder)
                        currentlyPlayingPosition = adapterPosition
                    }
                }
            }
        } else {
            holder.audioLayout.visibility = View.GONE
            holder.messageTextView.visibility = View.VISIBLE
            holder.messageTextView.text = chatMessage.message
        }

        // Handle the timestamp visibility and background
        if (timestampVisibilityMap[chatMessage.replyToMessageId] == true) {
            holder.timestampTextView.visibility = View.VISIBLE
            holder.timestampTextView.text = chatMessage.timestamp.toString()
        } else {
            holder.timestampTextView.visibility = View.GONE
        }
    }

    // Helper function to get the replied message (this can be fetched from your data source)
    private fun getRepliedMessageById(replyToMessageId: String): ChatMessage? {
        return chatMessages.find { it.replyToMessageId == replyToMessageId }
    }

    override fun getItemCount(): Int = chatMessages.size

    override fun getItemViewType(position: Int): Int {
        val chatMessage = chatMessages[position]
        return if (chatMessage.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
        val audioLayout: View = view.findViewById(R.id.audioMessageLayout)
        val playButton: ImageView = view.findViewById(R.id.playButton)
        val audioDuration: TextView = view.findViewById(R.id.audioDuration)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val quotedMessageTextView: TextView = view.findViewById(R.id.quotedMessageTextView)
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
            }
        }
    }

    private fun stopAudio(holder: ChatViewHolder?) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }

        holder?.playButton?.setImageResource(R.drawable.ic_play)
        currentlyPlayingPosition = -1
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}
