package com.example.samarpan.Fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import com.example.samarpan.Model.DonationPost
import com.example.samarpan.databinding.AddPostBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy
import java.io.ByteArrayOutputStream

class AddPostBottomSheet : BottomSheetDialogFragment() {

    private var _binding: AddPostBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private var onPostAddedListener: ((DonationPost) -> Unit)? = null

    fun setOnPostAddedListener(listener: (DonationPost) -> Unit) {
        onPostAddedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddPostBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Cloudinary
        initCloudinary()

        // Camera button to open the camera
        binding.cameraBtn.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }

        // Post button functionality
        binding.postButton.setOnClickListener {
            val profileName = binding.inputProfileName.text.toString()
            val location = binding.inputLocation.text.toString()
            val foodTitle = binding.inputfoodTitle.text.toString()
            val foodDescription = binding.inputfoodDescription.text.toString()

            if (profileName.isBlank() || location.isBlank() || foodDescription.isBlank() || foodTitle.isBlank() || imageUri == null) {
                binding.errorTextView.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Pass the uploaded image URL and other data
            val newPost = DonationPost(
                profileName = profileName,
                location = location,
                foodTitle = foodTitle,
                foodDescription = foodDescription,
                foodImage = imageUri.toString(),
                timestamp = System.currentTimeMillis()
            )
            onPostAddedListener?.invoke(newPost)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun initCloudinary() {
        try {
            // Attempt to get the instance to check if already initialized
            MediaManager.get()
        } catch (e: IllegalStateException) {
            // Initialize MediaManager only if not already initialized
            val config: MutableMap<String, String> = HashMap()
            config["cloud_name"] = "dwkkfinda" // Replace with your Cloudinary Cloud Name
            config["api_key"] = "316841239362936" // Replace with your Cloudinary API Key
            config["api_secret"] = "6Hlnwg4rEfE4-ytS_WrgP5tpySs" // Replace with your Cloudinary API Secret

            MediaManager.init(requireContext(), config)
            MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
                .maxConcurrentRequests(4)
                .networkPolicy(UploadPolicy.NetworkType.ANY)
                .build()
        }
    }


    private fun uploadImageToCloudinary(bitmap: Bitmap, onComplete: (String) -> Unit) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            val uploadOptions: MutableMap<String, String> = HashMap()
            uploadOptions["upload_preset"] = "samarpan" // Set your upload preset here

            MediaManager.get().upload(imageBytes)
                .options(uploadOptions)  // Apply the upload preset
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String?) {
                        Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                        // Optional: Update progress
                    }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val imageUrl = resultData?.get("url") as String
                        onComplete(imageUrl)
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        Log.e("Cloudinary", "Upload failed: ${error?.description}")
                        Toast.makeText(context, "Upload failed: ${error?.description}", Toast.LENGTH_SHORT).show()
                    }

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        // Handle reschedule if needed
                    }
                }).dispatch()
        } catch (e: Exception) {
            Log.e("Cloudinary", "Exception during image upload: ${e.message}")
            Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as Bitmap
            uploadImageToCloudinary(bitmap) { imageUrl ->
                imageUri = imageUrl.toUri()
                binding.postImage.setImageBitmap(bitmap)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1001
    }
}
