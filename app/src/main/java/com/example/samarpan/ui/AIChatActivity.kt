package com.example.samarpan.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Adapter.ChatAdapter
import com.example.samarpan.Adapter.ChatMessage
import com.example.samarpan.databinding.ActivityAichatActivityBinding

class AIChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAichatActivityBinding
    private val chatAdapter = ChatAdapter()
    private val aiBot = SimpleChatBot()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAichatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter
        chatAdapter.addMessage(ChatMessage("Hello! How can I assist you today?", isUser = false))
        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        binding.sendButton.setOnClickListener {
            val message = binding.userInput.text.toString().trim()
            if (message.isNotEmpty()) {
                chatAdapter.addMessage(ChatMessage(message, isUser = true))
                binding.userInput.text.clear()
                simulateBotResponse(message)
            }
        }
    }

    private fun simulateBotResponse(userMessage: String) {
        binding.typingAnimation.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            val reply = aiBot.getResponse(userMessage)
            chatAdapter.addMessage(ChatMessage(reply, isUser = false))
            binding.typingAnimation.visibility = View.GONE
        }, 1200)
    }
}
