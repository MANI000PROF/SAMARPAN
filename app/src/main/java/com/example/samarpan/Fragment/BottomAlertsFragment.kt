package com.example.samarpan.Fragment

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.samarpan.Adapter.AlertAdapter
import com.example.samarpan.MainActivity
import com.example.samarpan.Model.Alert
import com.example.samarpan.databinding.FragmentBottomAlertsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import org.json.JSONObject


class BottomAlertsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentBottomAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertAdapter: AlertAdapter
    private lateinit var alertList: MutableList<Alert>
    private lateinit var database: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomAlertsBinding.inflate(inflater, container, false)

        alertList = mutableListOf()
        alertAdapter = AlertAdapter(alertList)
        binding.alertRecyclerView.adapter = alertAdapter
        binding.alertRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.clearBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val databaseRef = FirebaseDatabase.getInstance().getReference("Requests")

            databaseRef.get().addOnSuccessListener { snapshot ->
                val batchDelete = mutableListOf<DatabaseReference>()

                for (requestSnapshot in snapshot.children) {
                    val alert = requestSnapshot.getValue(Alert::class.java)
                    if (alert != null && (alert.donorId == currentUserId || alert.requesterId == currentUserId)) {
                        batchDelete.add(requestSnapshot.ref)
                    }
                }

                for (ref in batchDelete) {
                    ref.removeValue()
                }

                val size = alertList.size
                alertList.clear()
                alertAdapter.notifyItemRangeRemoved(0, size)
                binding.root.post {
                    binding.noAlertsAnimation.visibility = View.VISIBLE
                    binding.noAlertsTextView.visibility = View.VISIBLE
                    binding.alertRecyclerView.visibility = View.GONE
                    binding.clearBtn.visibility = View.GONE
                }
                Toast.makeText(requireContext(), "Alerts cleared", Toast.LENGTH_SHORT).show()
            }
        }

        fetchUserAlerts()
        enableSwipeGestures()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Subscribe to Firebase messaging topic
        FirebaseMessaging.getInstance().subscribeToTopic("alerts")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to alerts"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("Subscription Status", msg)
            }
    }

    private fun fetchUserAlerts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("Requests")

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if binding is not null before using it
                if (_binding == null) return

                alertList.clear()
                for (requestSnapshot in snapshot.children) {
                    val alert = requestSnapshot.getValue(Alert::class.java)

                    // Skip alerts with missing required fields
                    if (alert != null && (alert.donorId == currentUserId || alert.requesterId == currentUserId)) {
                        if (alert.title.isNullOrEmpty() || alert.message.isNullOrEmpty() || alert.status.isNullOrEmpty()) {
                            continue  // Skip alerts with missing required fields
                        }

                        val alertWithId = alert.copy(requestId = requestSnapshot.key)

                        // Only show non-"Declined" alerts or any "Declined" ones with complete data
                        if (alert.status != "Declined" || (alert.status == "Declined" && alert.donorId != null && alert.requesterId != null)) {
                            alertList.add(alertWithId)
                        }
                    }
                }

                if (alertList.isEmpty()) {
                    binding.noAlertsAnimation.visibility = View.VISIBLE
                    binding.noAlertsTextView.visibility = View.VISIBLE
                    binding.alertRecyclerView.visibility = View.GONE
                    binding.clearBtn.visibility = View.GONE  // 🔴 Hide clear button when no alerts
                } else {
                    binding.noAlertsAnimation.visibility = View.GONE
                    binding.noAlertsTextView.visibility = View.GONE
                    binding.alertRecyclerView.visibility = View.VISIBLE
                    binding.clearBtn.visibility = View.VISIBLE  // ✅ Show clear button when alerts are present
                }

                (activity as? MainActivity)?.updateAlertAnimation(alertList.isNotEmpty())

                alertAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Optionally handle error
            }
        }
        database.addValueEventListener(valueEventListener)
    }

    private fun sendPushNotificationToRequester(alert: Alert, newStatus: String) {
        val requesterId = alert.requesterId ?: return
        val postTitle = alert.title ?: "your request"

        val dynamicTitle = "Your request has been $newStatus"
        val dynamicMessage = "Your request for: $postTitle was $newStatus by the donor."

        val requesterTokenRef = FirebaseDatabase.getInstance().getReference("users")
            .child(requesterId)
            .child("fcmToken")

        requesterTokenRef.get().addOnSuccessListener { snapshot ->
            val receiverFcmToken = snapshot.getValue(String::class.java)
            if (!receiverFcmToken.isNullOrEmpty()) {
                lifecycleScope.launch {
                    val accessToken = FirebaseAccessToken.getAccessToken(requireContext())
                    accessToken?.let {
                        sendPushNotification(it, receiverFcmToken, dynamicTitle, dynamicMessage)
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
        val context = requireContext()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        database.removeEventListener(valueEventListener)
    }

    private fun enableSwipeGestures() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val alert = alertList[position]
                if (position >= alertList.size) return
                if (alert.status == "Pending" && alert.donorId == currentUserId) {
                    viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                    if (direction == ItemTouchHelper.RIGHT) {
                        alertAdapter.updateRequestStatusExternally(alert, "Accepted")
                        sendPushNotificationToRequester(alert, "Accepted")
                    } else if (direction == ItemTouchHelper.LEFT) {
                        alertAdapter.updateRequestStatusExternally(alert, "Declined")
                        sendPushNotificationToRequester(alert, "Declined")
                    }
                } else if ((alert.status == "Accepted" || alert.status == "Declined") && alert.requesterId == currentUserId) {
                    viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                    val requestId = alert.requestId ?: return
                    FirebaseDatabase.getInstance().getReference("Requests").child(requestId)
                        .removeValue()
                        .addOnSuccessListener {
                            alertList.removeAt(position)
                            alertAdapter.notifyItemRemoved(position)
                            Toast.makeText(context, "Alert removed", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to remove alert", Toast.LENGTH_SHORT).show()
                            alertAdapter.notifyItemChanged(position)
                        }
                } else {
                    alertAdapter.notifyItemChanged(position)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val alert = alertList[viewHolder.adapterPosition]
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                // Clamp swipe distance
                val swipeLimit = itemView.width / 3f
                val clampedDx = dX.coerceIn(-swipeLimit, swipeLimit)

                if (alert.status == "Pending" && alert.donorId == currentUserId) {
                    // Donor swiping: show background and action text
                    val paint = Paint().apply {
                        color = if (clampedDx > 0) Color.parseColor("#43A047") else Color.parseColor("#E53935")
                    }

                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 40f
                        isAntiAlias = true
                        textAlign = Paint.Align.LEFT
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }

                    if (clampedDx > 0) {
                        c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), clampedDx, itemView.bottom.toFloat(), paint)
                        c.drawText("Accept", itemView.left + 40f, itemView.top + itemView.height / 2f + 15f, textPaint)
                    } else if (clampedDx < 0) {
                        c.drawRect(itemView.right + clampedDx, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                        c.drawText("Decline", itemView.right - 200f, itemView.top + itemView.height / 2f + 15f, textPaint)
                    }
                }

                // For non-donors or other statuses: no background, only slight movement
                val finalDx = if (alert.donorId == currentUserId && alert.status == "Pending") clampedDx else dX * 0.1f

                super.onChildDraw(c, recyclerView, viewHolder, finalDx, dY, actionState, isCurrentlyActive)
            }

        })

        itemTouchHelper.attachToRecyclerView(binding.alertRecyclerView)
    }


}
