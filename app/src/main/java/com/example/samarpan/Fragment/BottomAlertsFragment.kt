package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samarpan.Adapter.AlertAdapter
import com.example.samarpan.Model.Alert
import com.example.samarpan.databinding.FragmentBottomAlertsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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

        fetchUserAlerts()

        return binding.root
    }

    private fun fetchUserAlerts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("Requests")

        database.orderByChild("donorId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    alertList.clear()
                    for (requestSnapshot in snapshot.children) {
                        val alert = requestSnapshot.getValue(Alert::class.java)
                        alert?.let { alertList.add(it) }
                    }
                    alertAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
