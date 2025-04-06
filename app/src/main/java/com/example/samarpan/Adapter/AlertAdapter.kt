package com.example.samarpan.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.Alert
import com.example.samarpan.R
import com.google.firebase.database.FirebaseDatabase

class AlertAdapter(private val alertList: MutableList<Alert>) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alertTitle: TextView = itemView.findViewById(R.id.alertTitle)
        val alertMessage: TextView = itemView.findViewById(R.id.alertMessage)
        val acceptBtn: Button = itemView.findViewById(R.id.acceptBtn)
        val declineBtn: Button = itemView.findViewById(R.id.declineBtn)
        val alertIcon: ImageView = itemView.findViewById(R.id.alertIcon)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alert_item, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]

        holder.alertTitle.text = alert.title
        holder.alertMessage.text = alert.message
        Log.d("AlertAdapter", "Binding alert: ${alert.title}, ${alert.message}")

        // Handle Accept button click
        holder.acceptBtn.setOnClickListener {
            updateRequestStatus(alert, "Accepted", holder)
        }

        Glide.with(holder.itemView.context)
            .load(alert.postImageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.alertIcon)

        // Handle Decline button click
        holder.declineBtn.setOnClickListener {
            updateRequestStatus(alert, "Declined", holder)
        }
    }

    override fun getItemCount() = alertList.size

    private fun updateRequestStatus(alert: Alert, status: String, holder: AlertViewHolder) {
        val database = FirebaseDatabase.getInstance().getReference("Requests")
        alert.requestId?.let { requestId ->
            database.child(requestId).child("status").setValue(status).addOnSuccessListener {
                Toast.makeText(holder.itemView.context, "Request $status", Toast.LENGTH_SHORT).show()

                // Remove the alert from the list if handled
                alertList.remove(alert)
                notifyDataSetChanged()
            }.addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Failed to update request", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
