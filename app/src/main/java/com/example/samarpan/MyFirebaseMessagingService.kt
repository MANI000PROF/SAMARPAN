package com.example.samarpan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import android.Manifest
import android.graphics.BitmapFactory
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "alerts_channel"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "New Alert", it.body ?: "You have a new alert.")
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        // Check if notification permission is granted (Android 13 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermission()) {
                // Permission granted, send notification
                sendNotificationCompat(title, messageBody)
            } else {
                // Permission not granted, notify user to grant permission
                showPermissionRequestNotification()
            }
        } else {
            // No need to check for permission on older versions
            sendNotificationCompat(title, messageBody)
        }
    }

    private fun sendNotificationCompat(title: String, messageBody: String) {
        // Create notification channel (required for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alerts"
            val descriptionText = "Notifications for new alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        // Create the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_white)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            checkPermission()
            notify(0, builder.build())
        }
    }

    private fun checkPermission(): Boolean {
        // Check for POST_NOTIFICATIONS permission
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true  // No permission needed on older versions
        }
    }

    private fun showPermissionRequestNotification() {
        // This could be a notification directing users to app settings to enable the permission
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Permission Needed")
            .setContentText("Please enable notification permissions in settings.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            checkPermission()
            notify(1, builder.build())
        }
    }
}
