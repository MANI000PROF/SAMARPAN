package com.example.samarpan.Fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy
import com.example.samarpan.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AddPostElectronicsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var inputProfileName: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputElectronicsTitle: EditText
    private lateinit var inputElectronicsDescription: EditText
    private lateinit var postImage: ImageView
    private lateinit var cameraBtn: ImageButton
    private lateinit var errorTextView: TextView
    private lateinit var postButton: AppCompatButton
    private lateinit var cancelButton: AppCompatButton
    private lateinit var locationBtn: AppCompatButton

    private var imageUri: Uri? = null
    private var cloudinaryImageUrl: String? = null
    private lateinit var database: DatabaseReference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val LOCATION_PICKER_REQUEST = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_add_post_electronics_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputProfileName = view.findViewById(R.id.inputProfileName)
        inputLocation = view.findViewById(R.id.inputLocation)
        inputElectronicsTitle = view.findViewById(R.id.inputElectronicsTitle)
        inputElectronicsDescription = view.findViewById(R.id.inputElectronicsDescription)
        postImage = view.findViewById(R.id.postImage)
        cameraBtn = view.findViewById(R.id.cameraBtn)
        errorTextView = view.findViewById(R.id.errorTextView)
        postButton = view.findViewById(R.id.postButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        locationBtn = view.findViewById(R.id.pickLocationButton)

        database = FirebaseDatabase.getInstance().reference.child("DonationPostsElectronics")

        initCloudinary()

        cameraBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
            }
        }

        inputLocation.setOnClickListener {
            val intent = Intent(requireContext(), com.example.samarpan.LocationPickerActivity::class.java)
            startActivityForResult(intent, LOCATION_PICKER_REQUEST)
        }

        locationBtn.setOnClickListener {
            val intent = Intent(requireContext(), com.example.samarpan.LocationPickerActivity::class.java)
            startActivityForResult(intent, LOCATION_PICKER_REQUEST)
        }

        postButton.setOnClickListener {
            if (validateInputs()) {
                if (cloudinaryImageUrl != null) {
                    postFoodDetails()
                } else {
                    Toast.makeText(context, "Uploading image, please wait...", Toast.LENGTH_SHORT).show()
                }
            } else {
                errorTextView.visibility = View.VISIBLE
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            inputLocation.setText("Lat: $latitude, Lon: $longitude")
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                Log.d("CameraDebug", "Captured image successfully")
                analyzeImageWithMLKit(bitmap)
            } else {
                Log.e("CameraDebug", "Failed to capture image. Bitmap is null")
                Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeImageWithMLKit(bitmap: Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(context, "Failed to process image. Bitmap is null.", Toast.LENGTH_SHORT).show()
            Log.e("MLKit", "Bitmap is null. Cannot analyze image.")
            return
        }

        if (!isGooglePlayServicesAvailable(requireContext())) {
            Toast.makeText(context, "Google Play Services required for ML Kit.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    if (labels.isNotEmpty()) {
                        // ✅ Check for Electronics-related keywords
                        val electronicsKeywords = listOf("laptop", "computer", "mobile", "phone", "tablet", "monitor", "keyboard", "headphones", "TV", "camera", "electronic", "gadget")
                        val detectedElectronics = labels.firstOrNull { label ->
                            electronicsKeywords.any { keyword -> label.text.contains(keyword, ignoreCase = true) } && label.confidence >= 0.6
                        }

                        if (detectedElectronics != null) {
                            postImage.setImageBitmap(bitmap)
                            Toast.makeText(context, "Electronics detected! Uploading image...", Toast.LENGTH_SHORT).show()
                            uploadImageToCloudinary(bitmap)
                        } else {
                            Toast.makeText(context, "No Electronics detected. Try again.", Toast.LENGTH_LONG).show()
                            Log.d("MLKit", "Detected labels: ${labels.joinToString { "${it.text} (${it.confidence})" }}")
                        }
                    } else {
                        Toast.makeText(context, "No labels detected. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Image labeling failed: ${e.message}")
                    Toast.makeText(context, "Failed to analyze image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("MLKit", "Exception in ML Kit: ${e.message}")
            Toast.makeText(context, "Error processing image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }


    private fun uploadImageToCloudinary(bitmap: Bitmap) {
        val tempFile = File(requireContext().cacheDir, "temp_image.jpg")
        try {
            FileOutputStream(tempFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }

            MediaManager.get().upload(tempFile.absolutePath)
                .callback(object : UploadCallback {
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        cloudinaryImageUrl = resultData?.get("url") as? String
                        Log.d("Cloudinary", "Image uploaded successfully: $cloudinaryImageUrl")
                        Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        Toast.makeText(context, "Upload failed: ${error?.description}", Toast.LENGTH_SHORT).show()
                        Log.e("Cloudinary", "Upload error: ${error?.description}")
                    }

                    override fun onStart(requestId: String?) {
                        Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
                }).dispatch()
        } catch (e: Exception) {
            Log.e("Cloudinary", "File creation error: ${e.message}")
            Toast.makeText(context, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show()
        }
    }

    private fun postFoodDetails() {
        val name = inputProfileName.text.toString()
        val locationText = inputLocation.text.toString()
        val electronicsTitle = inputElectronicsTitle.text.toString()
        val electronicsDescription = inputElectronicsDescription.text.toString()
        val imageUrl = cloudinaryImageUrl ?: ""

        val postId = database.push().key // Generate unique key for the post

        // ✅ Extract latitude and longitude from location text
        val latLngPattern = "Lat: (-?\\d+\\.\\d+), Lon: (-?\\d+\\.\\d+)".toRegex()
        val matchResult = latLngPattern.find(locationText)

        val latitude = matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val longitude = matchResult?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

        if (postId != null) {
            val postDetails = mapOf(
                "postId" to postId,
                "donorId" to currentUserId,
                "profileName" to name,
                "location" to locationText,  // Keeping this for display
                "latitude" to latitude,      // ✅ Storing latitude
                "longitude" to longitude,    // ✅ Storing longitude
                "electronicsTitle" to electronicsTitle,
                "electronicsDescription" to electronicsDescription,
                "electronicsImage" to imageUrl,
                "timestamp" to System.currentTimeMillis()
            )

            database.child(postId).setValue(postDetails).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Post submitted successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(context, "Failed to submit post", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun validateInputs(): Boolean {
        return inputProfileName.text.isNotEmpty() &&
                inputLocation.text.isNotEmpty() &&
                inputElectronicsTitle.text.isNotEmpty() &&
                inputElectronicsDescription.text.isNotEmpty()
    }
}
