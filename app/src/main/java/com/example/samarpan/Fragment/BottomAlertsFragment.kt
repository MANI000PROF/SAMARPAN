package com.example.samarpan.Fragment

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.Adapter.AlertAdapter
import com.example.samarpan.Model.Alert
import com.example.samarpan.databinding.FragmentBottomAlertsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage


class BottomAlertsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentBottomAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertAdapter: AlertAdapter
    private lateinit var alertList: MutableList<Alert>
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomAlertsBinding.inflate(inflater, container, false)

        alertList = mutableListOf()
        alertAdapter = AlertAdapter(alertList)
        binding.alertRecyclerView.adapter = alertAdapter
        binding.alertRecyclerView.layoutManager = LinearLayoutManager(requireContext())

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

        database.addValueEventListener(object : ValueEventListener {
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
                } else {
                    binding.noAlertsAnimation.visibility = View.GONE
                    binding.noAlertsTextView.visibility = View.GONE
                    binding.alertRecyclerView.visibility = View.VISIBLE
                }

                alertAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Optionally handle error
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun enableSwipeGestures() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Only allow swiping for alerts where the current user is the donor and the status is "Pending"
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Prevent any drag-and-drop behavior by returning false
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val alert = alertList[position]

                if (alert.status == "Pending" && alert.donorId == currentUserId) {
                    vibrateShort()
                    // Donor swiping to accept or decline
                    if (direction == ItemTouchHelper.RIGHT) {
                        alertAdapter.updateRequestStatusExternally(alert, "Accepted")
                    } else if (direction == ItemTouchHelper.LEFT) {
                        alertAdapter.updateRequestStatusExternally(alert, "Declined")
                    }
                } else if ((alert.status == "Accepted" || alert.status == "Declined") && alert.requesterId == currentUserId) {
                    // Requester swiping to delete accepted/declined alert
                    vibrateShort()
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
                    // Restrict swipe
                    Toast.makeText(context, "You can't perform this action", Toast.LENGTH_SHORT).show()
                    alertAdapter.notifyItemChanged(position)
                }
            }


            // Change background color during swipe only if the status is "Pending"
            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val paint = Paint()

                // Get the alert object
                val alert = alertList[viewHolder.adapterPosition]

                // Only allow color change if the status is "Pending"
                if (alert.status == "Pending") {
                    if (dX > 0) {
                        // Swipe Right (Accept) - Green color
                        paint.color = Color.parseColor("#4CAF50")  // Green
                    } else if (dX < 0) {
                        // Swipe Left (Decline) - Red color
                        paint.color = Color.parseColor("#F44336")  // Red
                    }

                    // Draw the background color
                    c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.alertRecyclerView)
    }

    private fun vibrateShort() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

}
