package com.example.samarpan.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.Alert
import com.example.samarpan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AlertAdapter(private val alertList: MutableList<Alert>) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    private lateinit var context: Context

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alertTitle: TextView = itemView.findViewById(R.id.alertTitle)
        val alertMessage: TextView = itemView.findViewById(R.id.alertMessage)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val alertIcon: ImageView = itemView.findViewById(R.id.alertIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        context = parent.context  // Save context here ✅
        val view = LayoutInflater.from(context).inflate(R.layout.alert_item, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        // Title and message will now be properly set from Firebase during request creation
        // Set different title and message for donor and requester
        if (alert.donorId == currentUserId) {
            // Donor's view
            holder.alertTitle.text = alert.title
            holder.alertMessage.text = alert.message
        } else if (alert.requesterId == currentUserId) {
            // Requester's view
            holder.alertTitle.text = alert.requesterTitle
            holder.alertMessage.text = alert.requesterMessage
        }

        holder.statusTextView.text = alert.status

        Log.d("AlertAdapter", "Binding alert: ${alert.title}, ${alert.message}")

        Glide.with(context)
            .load(alert.postImageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.alertIcon)

        // Just show the status (no buttons anymore)
        holder.statusTextView.visibility = View.VISIBLE

        // Only show status change option if the current user is the donor
        if (currentUserId == alert.donorId) {
            // Only allow swipe actions for the donor
            holder.statusTextView.setOnClickListener {
                // Check if the status is still "Pending"
                if (alert.status == "Pending") {
                    showStatusDialog(alert)
                } else {
                    Toast.makeText(context, "Status already changed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = alertList.size

    private fun showStatusDialog(alert: Alert) {
        // Show a dialog with options to accept or decline the request
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == alert.donorId && alert.status == "Pending") {
            // Create and show a dialog to change the status (Accept or Decline)
            val status = "Accepted" // Example: User clicks "Accept"
            updateRequestStatusExternally(alert, status)
        }
    }

    private fun updateRequestStatus(alert: Alert, status: String) {
        val database = FirebaseDatabase.getInstance().getReference("Requests")
        alert.requestId?.let { requestId ->
            // Prevent status change if it's already set
            if (alert.status != "Pending") {
                Toast.makeText(context, "Status already changed", Toast.LENGTH_SHORT).show()
                return
            }

            database.child(requestId).child("status").setValue(status).addOnSuccessListener {
                Toast.makeText(context, "Request $status", Toast.LENGTH_SHORT).show()

                // Remove item after action
                alertList.remove(alert)
                notifyDataSetChanged()

            }.addOnFailureListener {
                Toast.makeText(context, "Failed to update request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Called externally from swipe gestures
    fun updateRequestStatusExternally(alert: Alert, status: String) {
        updateRequestStatus(alert, status)
    }
}
