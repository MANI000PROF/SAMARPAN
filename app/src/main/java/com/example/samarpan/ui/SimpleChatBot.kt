package com.example.samarpan.ui

class SimpleChatBot {
    fun getResponse(input: String): String {
        return when {
            input.contains("hello", ignoreCase = true) -> "Hi there! How can I help you today?"
            input.contains("donate", ignoreCase = true) -> "To donate, go to the main screen and tap on the category you'd like to donate in!"
            input.contains("help", ignoreCase = true) -> "I'm here to assist you. Ask me anything about donating, rewards, or locations!"
            input.contains("location", ignoreCase = true) -> "You can view or save locations from the Saved Locations menu!"
            input.contains("reward", ignoreCase = true) -> "Rewards are earned by contributing! Check the Rewards section to track your progress."
            else -> "Sorry, I didn't quite get that. Could you please rephrase?"
        }
    }
}
