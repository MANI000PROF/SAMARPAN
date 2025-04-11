package com.example.samarpan.Fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy
import com.example.samarpan.IntroActivity
import com.example.samarpan.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class ProfileFragment : Fragment() {

    private lateinit var profileImageCard: CardView
    private lateinit var profileImage: ImageView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().reference

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

        initCloudinary()
        loadUserProfile()

        profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val percentage = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            val scale = 1 - (percentage * 0.6f)
            val minScale = 0.4f
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

    private fun initCloudinary() {
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            val config: MutableMap<String, String> = HashMap()
            config["cloud_name"] = "dwkkfinda"
            config["api_key"] = "316841239362936"
            config["api_secret"] = "6Hlnwg4rEfE4-ytS_WrgP5tpySs"

            MediaManager.init(requireContext(), config)
            MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
                .maxConcurrentRequests(4)
                .networkPolicy(UploadPolicy.NetworkType.ANY)
                .build()
        }
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
        uri?.let { uploadImageToCloudinary(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { saveTempBitmapAndUpload(it) }
        }
    }

    private fun pickImageFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun captureImageFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun saveTempBitmapAndUpload(bitmap: Bitmap) {
        val tempFile = File(requireContext().cacheDir, "temp_profile_image.jpg")
        try {
            FileOutputStream(tempFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }
            uploadImageToCloudinary(tempFile.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToCloudinary(imagePathOrUri: Any) {
        val uploadRequest = when (imagePathOrUri) {
            is Uri -> MediaManager.get().upload(imagePathOrUri)
            is String -> MediaManager.get().upload(imagePathOrUri)
            else -> return
        }

        uploadRequest.callback(object : UploadCallback {
            override fun onStart(requestId: String?) {
                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            }

            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

            override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                val imageUrl = resultData?.get("url") as? String
                imageUrl?.let {
                    updateFirebaseProfile(it)
                }
            }

            override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                Toast.makeText(context, "Upload failed: ${error?.description}", Toast.LENGTH_SHORT).show()
            }

            override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
        }).dispatch()
    }

    private fun updateFirebaseProfile(imageUrl: String) {
        val user = auth.currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(Uri.parse(imageUrl))
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update in Firebase Realtime Database
                dbRef.child("users").child(user.uid)
                    .child("profileImageUrl").setValue(imageUrl)

                // Update UI
                Glide.with(this).load(imageUrl).into(profileImage)
                Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Profile update failed!", Toast.LENGTH_SHORT).show()
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
