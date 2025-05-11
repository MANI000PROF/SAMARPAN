package com.example.samarpan.ui

import android.content.Context

class CommandProcessor(private val context: Context) {

    enum class ActionType {
        NAVIGATE_REWARDS,
        NAVIGATE_DONATE_FOOD,
        NAVIGATE_DONATE_CLOTHES,
        NAVIGATE_DONATE_ELECTRONICS,
        NAVIGATE_SAVED_LOCATIONS,
        NAVIGATE_SAVED_POSTS,
        NAVIGATE_MY_CONTRIBUTIONS,
        NAVIGATE_ADD_POST,
        NAVIGATE_SETTINGS,
        NAVIGATE_CONTACT_SUPPORT,
        SWITCH_CATEGORY_FOOD,
        SWITCH_CATEGORY_CLOTHES,
        SWITCH_CATEGORY_ELECTRONICS,
        MESSAGE_USER,
        NONE
    }

    data class BotResponse(val reply: String,
                           val action: ActionType = ActionType.NONE,
                           val targetName: String? = null
    )


    fun processInput(input: String): BotResponse {
        val normalized = input.trim().lowercase()

        val messageRegex = Regex("^message\\s+(\\w+)", RegexOption.IGNORE_CASE)
        val matchResult = messageRegex.find(normalized)
        if (matchResult != null) {
            val targetName = matchResult.groupValues[1]
            return BotResponse("Opening chat with $targetName...", ActionType.MESSAGE_USER, targetName)
        }

        return when {

            normalized.contains("about") && normalized.contains("app") -> BotResponse(
                "Samarpan is a donation platform designed to connect generous donors with those in need. We make it easy for you to donate food, clothes, and electronics to help improve lives. 💖\n\nYour contribution can make a real difference, and it’s all done with just a few taps! Would you like to donate now and make an impact? 😊",
                ActionType.NONE
            )

            normalized.contains("hi") || normalized.contains("hello") -> BotResponse(
                "Hey there! 😊 How can I assist you today? I’m here to help with donations, rewards, and more! Just let me know.",
                ActionType.NONE
            )
            normalized.contains("help") || normalized.contains("assist") -> BotResponse(
                "I’m happy to help! You can ask me about donations, your rewards, saved locations, and more. What would you like to do today?",
                ActionType.NONE
            )

            // Command 1: Show My Contributions
            normalized.contains("contribution", ignoreCase = true) || normalized.contains("donations", ignoreCase = true) -> {
                BotResponse(
                    "You can view your contributions in the 'My Contributions' section!",
                    ActionType.NAVIGATE_MY_CONTRIBUTIONS
                )
            }
            // Command 2: Show My Rewards
            normalized.contains("reward", ignoreCase = true) || normalized.contains("progress", ignoreCase = true) -> {
                BotResponse(
                    "You're doing great! You can check your rewards progress in the 'Rewards' section!",
                    ActionType.NAVIGATE_REWARDS
                )
            }
            // Command 3: Show Saved Locations
            normalized.contains("location", ignoreCase = true) || normalized.contains("saved", ignoreCase = true) -> {
                BotResponse(
                    "You can view your saved locations in the 'Saved Locations' section.",
                    ActionType.NAVIGATE_SAVED_LOCATIONS
                )
            }
            normalized.contains("posts", ignoreCase = true) || normalized.contains("saved", ignoreCase = true) -> {
                BotResponse(
                    "You can view your saved posts in the 'Saved Posts' section.",
                    ActionType.NAVIGATE_SAVED_POSTS
                )
            }
            // Command 4: Post a Donation
            normalized.contains("donate", ignoreCase = true) || normalized.contains("post", ignoreCase = true) -> {
                BotResponse(
                    "To post a new donation, go to the 'Donate' section and select a category.",
                    ActionType.NAVIGATE_ADD_POST
                )
            }
            // Command 5: Help with Navigation
            normalized.contains("navigate", ignoreCase = true) || normalized.contains("help", ignoreCase = true) -> {
                BotResponse(
                    "I can help with navigation! Just tell me what you're looking for.",
                    ActionType.NONE
                )
            }
            // Command 6: Voice Command (Check status)
            normalized.contains("status", ignoreCase = true) -> {
                BotResponse(
                    "Here's your current status: You're making great progress in your donations!",
                    ActionType.NONE
                )
            }
            // Command 7: Give Feedback
            normalized.contains("settings", ignoreCase = true) -> {
                BotResponse(
                    "We'd love to hear your feedback! You can go to the 'Settings' section to share your thoughts.",
                    ActionType.NAVIGATE_SETTINGS
                )
            }
            // Command 8: Contact Support
            normalized.contains("support", ignoreCase = true) -> {
                BotResponse(
                    "I will connect you to support!",
                    ActionType.NAVIGATE_CONTACT_SUPPORT
                )
            }
            // Command 9: Switch Categories (Food)
            normalized.contains("food", ignoreCase = true) -> {
                BotResponse(
                    "Switching to Food Donations!",
                    ActionType.SWITCH_CATEGORY_FOOD
                )
            }
            // Command 10: Switch Categories (Clothes)
            normalized.contains("clothes", ignoreCase = true) -> {
                BotResponse(
                    "Switching to Clothes Donations!",
                    ActionType.SWITCH_CATEGORY_CLOTHES
                )
            }
            // Command 11: Switch Categories (Electronics)
            normalized.contains("electronics", ignoreCase = true) -> {
                BotResponse(
                    "Switching to Electronics Donations!",
                    ActionType.SWITCH_CATEGORY_ELECTRONICS
                )
            }
            // Default response
            else -> {
                BotResponse(
                    "Sorry, I didn't quite understand that. You can say things like 'Show rewards', 'Donate clothes', or 'Saved locations'.",
                    ActionType.NONE
                )
            }
        }
    }

}
