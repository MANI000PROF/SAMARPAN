package com.example.samarpan.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.samarpan.IntroActivity
import com.example.samarpan.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.abs

class ProfileFragment : Fragment() {

    private lateinit var profileImageCard: CardView
    private lateinit var profileImage: ImageView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImageCard = view.findViewById(R.id.profileImageCard)
        profileImage = view.findViewById(R.id.profileImage)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        toolbar = view.findViewById(R.id.toolbar)
        userNameTextView = view.findViewById(R.id.userName)
        userEmailTextView = view.findViewById(R.id.userDetails)

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        setHasOptionsMenu(true)

        loadUserProfile()

        profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val percentage = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            val minScale = 0.4f  // Ensure profile image doesn't shrink too much
            val scale = 1 - (percentage * 0.6f)
            val adjustedScale = if (scale < minScale) minScale else scale

            profileImageCard.scaleX = adjustedScale
            profileImageCard.scaleY = adjustedScale

            val minTranslationX = toolbar.width / 3.5f
            val minTranslationY = toolbar.height / 4f
            profileImageCard.translationX = percentage * minTranslationX
            profileImageCard.translationY = -percentage * minTranslationY
        })

        return view
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        user?.let {
            userNameTextView.text = it.displayName ?: "User Name"
            userEmailTextView.text = it.email ?: "Email Not Available"

            val profileImageUrl = it.photoUrl
            if (profileImageUrl != null) {
                Glide.with(this).load(profileImageUrl).into(profileImage)
            }
        }
    }

    private fun showImagePickerDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Profile Picture")
            .setItems(arrayOf("Upload from device", "Take a picture")) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> captureImageFromCamera()
                }
            }
            .show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let { uploadProfileImage(it) }
        }
    }

    private fun pickImageFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun captureImageFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("profile_images/${user.uid}.jpg")

            val databaseReference = FirebaseDatabase.getInstance().reference // Initialize it properly

            storageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build()

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Update in Firebase Database
                                    databaseReference.child("users").child(user.uid)
                                        .child("profileImageUrl").setValue(uri.toString())

                                    // Update UI
                                    Glide.with(requireContext()).load(uri).into(profileImage)
                                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Upload failed!", Toast.LENGTH_SHORT).show()
                }
        }
    }



    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // TODO: Open settings screen
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                val intent = Intent(requireContext(), IntroActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
