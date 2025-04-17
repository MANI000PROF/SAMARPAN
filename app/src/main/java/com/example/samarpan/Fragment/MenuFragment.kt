package com.example.samarpan.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.samarpan.IntroActivity
import com.example.samarpan.R
import com.example.samarpan.ui.MyContributionsActivity
import com.example.samarpan.ui.RewardsActivity
import com.example.samarpan.ui.ContactActivity
import com.example.samarpan.ui.SavedLocationActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class MenuFragment : DialogFragment() {

    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.ThemeOverlay_Samarpan_Menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_menu, container, false)
        return rootView
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.75).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.START)
            setWindowAnimations(R.style.SlideInMenuAnimation)
        }

        setupMenuItemListeners()
    }

    private fun setupMenuItemListeners() {
        rootView.findViewById<TextView>(R.id.myContributionsRow).setOnClickListener {
            startActivity(Intent(activity, MyContributionsActivity::class.java))
            dismiss()
        }

        rootView.findViewById<TextView>(R.id.savedLocationsRow).setOnClickListener {
            startActivity(Intent(activity, SavedLocationActivity::class.java))
            dismiss()
        }

        rootView.findViewById<TextView>(R.id.rewardsRow).setOnClickListener {
            startActivity(Intent(activity, RewardsActivity::class.java))
            dismiss()
        }

        rootView.findViewById<TextView>(R.id.guidelinesRow).setOnClickListener {
            Toast.makeText(activity, "Community Guidelines", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        rootView.findViewById<TextView>(R.id.shareRow).setOnClickListener {
            shareAppDirectly()
        }

        rootView.findViewById<TextView>(R.id.contactRow).setOnClickListener {
            startActivity(Intent(activity, ContactActivity::class.java))
            dismiss()
        }

        rootView.findViewById<TextView>(R.id.logoutOption).setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }
    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), IntroActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareAppDirectly() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out Samarpan!")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hey! Check out the Samarpan app to donate food, clothes, and electronics easily.\n\nDownload now:\nhttps://play.google.com/store/apps/details?id=${requireContext().packageName}"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share Samarpan via"))
        dismiss()
    }


}
