package com.example.samarpan.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samarpan.Adapter.ChatAdapter
import com.example.samarpan.Adapter.ChatMessage
import com.example.samarpan.MainActivity
import com.example.samarpan.databinding.ActivityAichatActivityBinding
import android.app.Activity
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import android.widget.Toast
import java.util.Locale
import android.media.MediaPlayer
import com.example.samarpan.ChatActivity
import com.example.samarpan.EditPostActivity
import com.example.samarpan.Fragment.AddPostBottomSheet
import com.example.samarpan.R
import com.example.samarpan.SavedPostsActivity
import com.example.samarpan.SettingsActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AIChatActivity : AppCompatActivity() {

    private val REQUEST_CODE_VOICE_INPUT = 100
    private lateinit var binding: ActivityAichatActivityBinding
    private val chatAdapter = ChatAdapter()
    private val commandProcessor = CommandProcessor(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAichatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupRecyclerView()
        sendWelcomeMessage()

        binding.micButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            startVoiceInput()
        }

        binding.backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        binding.sendButton.setOnClickListener {
            val message = binding.userInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun sendWelcomeMessage() {
        chatAdapter.addMessage(ChatMessage("Hello! How can I assist you today?", isUser = false))
    }

    private fun sendMessage(message: String) {
        chatAdapter.addMessage(ChatMessage(message, isUser = true))
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        binding.userInput.text.clear()
        simulateBotResponse(message)
    }

    private fun simulateBotResponse(userMessage: String) {
        binding.typingAnimation.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            val botResponse = commandProcessor.processInput(userMessage)
            chatAdapter.addMessage(ChatMessage(botResponse.reply, isUser = false))
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            binding.typingAnimation.visibility = View.GONE
            handleAction(botResponse)
            scrollToBottom()
        }, 1000)
    }

    private fun handleAction(botResponse: CommandProcessor.BotResponse) {
        when (botResponse.action) {
            CommandProcessor.ActionType.NAVIGATE_REWARDS -> navigateToRewards()
            CommandProcessor.ActionType.NAVIGATE_DONATE_FOOD -> navigateToDonation("Food")
            CommandProcessor.ActionType.NAVIGATE_DONATE_CLOTHES -> navigateToDonation("Clothes")
            CommandProcessor.ActionType.NAVIGATE_DONATE_ELECTRONICS -> navigateToDonation("Electronics")
            CommandProcessor.ActionType.NAVIGATE_SAVED_LOCATIONS -> navigateToSavedLocations()
            CommandProcessor.ActionType.NAVIGATE_SAVED_POSTS -> navigateToSavedPosts()
            CommandProcessor.ActionType.NONE -> { /* No action needed */ }
            CommandProcessor.ActionType.NAVIGATE_MY_CONTRIBUTIONS -> navigateToMyContributions()
            CommandProcessor.ActionType.NAVIGATE_ADD_POST -> navigateToAddPost()
            CommandProcessor.ActionType.NAVIGATE_SETTINGS -> navigateToSettings()
            CommandProcessor.ActionType.NAVIGATE_CONTACT_SUPPORT -> navigateToContactSupport()
            CommandProcessor.ActionType.SWITCH_CATEGORY_FOOD -> switchToCategoryFood()
            CommandProcessor.ActionType.SWITCH_CATEGORY_CLOTHES -> switchToCategoryClothes()
            CommandProcessor.ActionType.SWITCH_CATEGORY_ELECTRONICS -> switchToCategoryElectronics()
            CommandProcessor.ActionType.MESSAGE_USER -> {
                botResponse.targetName?.let { openChatWithUser(it) }
            }
        }
    }


    private fun openChatWithUser(name: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.orderByChild("fullName").equalTo(name).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val receiverId = userSnapshot.child("id").value.toString()
                        val intent = Intent(this@AIChatActivity, ChatActivity::class.java)
                        intent.putExtra("receiverId", receiverId)
                        startActivity(intent)
                        return
                    }
                } else {
                    Toast.makeText(this@AIChatActivity, "User '$name' not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AIChatActivity, "Error fetching user info.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun navigateToRewards() {
        startActivity(Intent(this, RewardsActivity::class.java))
    }

    private fun navigateToDonation(category: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("donation_category", category)
        startActivity(intent)
    }

    private fun navigateToSavedLocations() {
        startActivity(Intent(this, SavedLocationActivity::class.java))
    }

    private fun navigateToMyContributions() {
        startActivity(Intent(this, MyContributionsActivity::class.java))
    }

    private fun navigateToAddPost() {
        startActivity(Intent(this, AddPostBottomSheet::class.java))
    }

    private fun navigateToSavedPosts() {
        startActivity(Intent(this, SavedPostsActivity::class.java))
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun navigateToContactSupport() {
        startActivity(Intent(this, ContactActivity::class.java))
    }

    private fun switchToCategoryFood() {
        // Logic to switch category to Food, for example:
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("donation_category", "Food")
        startActivity(intent)
    }

    private fun switchToCategoryClothes() {
        // Logic to switch category to Clothes, for example:
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("donation_category", "Clothes")
        startActivity(intent)
    }

    private fun switchToCategoryElectronics() {
        // Logic to switch category to Electronics, for example:
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("donation_category", "Electronics")
        startActivity(intent)
    }


    private fun scrollToBottom() {
        binding.chatRecyclerView.post {
            binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Voice input not supported!", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API...")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_VOICE_INPUT && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                playDingSound()
                sendMessage(spokenText)
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun playDingSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.ding)
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
