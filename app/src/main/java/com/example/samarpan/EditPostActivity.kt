package com.example.samarpan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.io.FileOutputStream

class EditPostActivity : AppCompatActivity() {

    private lateinit var inputProfileName: EditText
    private lateinit var inputLocation: EditText
    private lateinit var inputTitle: EditText
    private lateinit var inputDescription: EditText
    private lateinit var postImage: ImageView
    private lateinit var postButton: Button
    private lateinit var cancelButton: Button
    private lateinit var pickLocationButton: Button
    private lateinit var errorTextView: TextView
    private lateinit var backBtn: ImageView

    private var postId: String? = null
    private var selectedLatitude = 0.0
    private var selectedLongitude = 0.0
    private var imageUrl: String? = null
    private var newImageBitmap: Bitmap? = null
    private var postCategory: String? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1002
        private const val LOCATION_PICKER_REQUEST = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_post)

        inputProfileName = findViewById(R.id.inputProfileName)
        inputLocation = findViewById(R.id.inputLocation)
        inputTitle = findViewById(R.id.inputTitle)
        inputDescription = findViewById(R.id.inputDescription)
        postImage = findViewById(R.id.postImage)
        postButton = findViewById(R.id.postButton)
        cancelButton = findViewById(R.id.cancelButton)
        pickLocationButton = findViewById(R.id.pickLocationButton)
        errorTextView = findViewById(R.id.errorTextView)
        backBtn = findViewById(R.id.backBtn)

        initCloudinary()
        loadPostDetails()

        backBtn.setOnClickListener{
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }
        postImage.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
            }
        }

        pickLocationButton.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            startActivityForResult(intent, LOCATION_PICKER_REQUEST)
        }

        postButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            if (validateInputs()) {
                if (newImageBitmap != null) {
                    uploadImageToCloudinary(newImageBitmap!!)
                } else {
                    updatePostInFirebase(imageUrl)
                }
            } else {
                errorTextView.visibility = TextView.VISIBLE
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun loadPostDetails() {
        postId = intent.getStringExtra("postId")
        inputProfileName.setText(intent.getStringExtra("profileName"))
        inputLocation.setText(intent.getStringExtra("location"))
        inputTitle.setText(intent.getStringExtra("title"))
        inputDescription.setText(intent.getStringExtra("description"))
        imageUrl = intent.getStringExtra("imageUrl")
        postCategory = intent.getStringExtra("category")

        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.placeholder)
                .into(postImage)
        }
    }

    private fun initCloudinary() {
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            val config = mutableMapOf<String, String>()
            config["cloud_name"] = "dwkkfinda"
            config["api_key"] = "316841239362936"
            config["api_secret"] = "6Hlnwg4rEfE4-ytS_WrgP5tpySs"
            MediaManager.init(this, config)
            MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
                .maxConcurrentRequests(4)
                .networkPolicy(UploadPolicy.NetworkType.ANY)
                .build()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                analyzeImageWithMLKit(it)
            }
        }

        if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedLatitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            selectedLongitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            inputLocation.setText(getAddressFromCoordinates(selectedLatitude, selectedLongitude))
        }
    }

    private fun analyzeImageWithMLKit(bitmap: Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(this, "Failed to process image. Bitmap is null.", Toast.LENGTH_SHORT).show()
            Log.e("MLKit", "Bitmap is null. Cannot analyze image.")
            return
        }

        if (!isGooglePlayServicesAvailable()) {
            Toast.makeText(this, "Google Play Services required for ML Kit.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    if (labels.isNotEmpty()) {
                        when (postCategory) {
                            "Food" -> analyzeFoodImage(labels, bitmap)
                            "Clothes" -> analyzeClothesImage(labels, bitmap)
                            "Electronics" -> analyzeElectronicsImage(labels, bitmap)
                            else -> {
                                Toast.makeText(this, "Category not recognized for image analysis.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "No labels detected. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Image labeling failed: ${e.message}")
                    Toast.makeText(this, "Failed to analyze image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("MLKit", "Exception in ML Kit: ${e.message}")
            Toast.makeText(this, "Error processing image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeFoodImage(labels: List<ImageLabel>, bitmap: Bitmap) {
        val foodKeywords = listOf("food", "fruit", "vegetable", "meal", "dish", "snack", "drink", "beverage")
        val detectedFood = labels.firstOrNull { label ->
            foodKeywords.any { keyword -> label.text.contains(keyword, ignoreCase = true) } && label.confidence >= 0.8
        }

        if (detectedFood != null) {
            postImage.setImageBitmap(bitmap)
            newImageBitmap = bitmap
            Toast.makeText(this, "Food detected. Ready to upload!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No food detected.", Toast.LENGTH_SHORT).show()
            Log.d("MLKit", "Detected labels: ${labels.joinToString { "${it.text} (${it.confidence})" }}")
        }
    }

    private fun analyzeClothesImage(labels: List<ImageLabel>, bitmap: Bitmap) {
        val clothesKeywords = listOf("clothing", "apparel", "shirt", "pants", "jacket", "dress", "jeans", "fabric")
        val detectedClothes = labels.firstOrNull { label ->
            clothesKeywords.any { keyword -> label.text.contains(keyword, ignoreCase = true) } && label.confidence >= 0.6
        }

        if (detectedClothes != null) {
            postImage.setImageBitmap(bitmap)
            newImageBitmap = bitmap
            Toast.makeText(this, "Clothes detected. Ready to upload!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No clothes detected.", Toast.LENGTH_SHORT).show()
            Log.d("MLKit", "Detected labels: ${labels.joinToString { "${it.text} (${it.confidence})" }}")
        }
    }

    private fun analyzeElectronicsImage(labels: List<ImageLabel>, bitmap: Bitmap) {
        val electronicsKeywords = listOf("electronics", "phone", "laptop", "tablet", "camera", "television", "headphones", "gadget")
        val detectedElectronics = labels.firstOrNull { label ->
            electronicsKeywords.any { keyword -> label.text.contains(keyword, ignoreCase = true) } && label.confidence >= 0.7
        }

        if (detectedElectronics != null) {
            postImage.setImageBitmap(bitmap)
            newImageBitmap = bitmap
            Toast.makeText(this, "Electronics detected. Ready to upload!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No electronics detected.", Toast.LENGTH_SHORT).show()
            Log.d("MLKit", "Detected labels: ${labels.joinToString { "${it.text} (${it.confidence})" }}")
        }
    }


    private fun uploadImageToCloudinary(bitmap: Bitmap) {
        val tempFile = File(cacheDir, "temp_image.jpg")
        FileOutputStream(tempFile).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
        }

        MediaManager.get().upload(tempFile.absolutePath)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val uploadedUrl = resultData?.get("url") as? String
                    updatePostInFirebase(uploadedUrl)
                }

                override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                    Toast.makeText(this@EditPostActivity, "Upload failed: ${error?.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
            }).dispatch()
    }

    private fun updatePostInFirebase(newImageUrl: String?) {
        if (postId == null) {
            Toast.makeText(this, "Post ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        val firebaseRef = when (postCategory) {
            "Food" -> "DonationPosts"
            "Clothes" -> "DonationPostsClothes"
            "Electronics" -> "DonationPostsElectronics"
            else -> {
                Toast.makeText(this, "Unknown category!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val updatedPost = when (postCategory) {
            "Food" -> mapOf(
                "profileName" to inputProfileName.text.toString(),
                "location" to inputLocation.text.toString(),
                "latitude" to selectedLatitude,
                "longitude" to selectedLongitude,
                "foodTitle" to inputTitle.text.toString(),
                "foodDescription" to inputDescription.text.toString(),
                "foodImage" to (newImageUrl ?: imageUrl),
                "timestamp" to System.currentTimeMillis()
            )
            "Clothes" -> mapOf(
                "profileName" to inputProfileName.text.toString(),
                "location" to inputLocation.text.toString(),
                "latitude" to selectedLatitude,
                "longitude" to selectedLongitude,
                "clothesTitle" to inputTitle.text.toString(),
                "clothesDescription" to inputDescription.text.toString(),
                "clothesImage" to (newImageUrl ?: imageUrl),
                "timestamp" to System.currentTimeMillis()
            )
            "Electronics" -> mapOf(
                "profileName" to inputProfileName.text.toString(),
                "location" to inputLocation.text.toString(),
                "latitude" to selectedLatitude,
                "longitude" to selectedLongitude,
                "electronicsTitle" to inputTitle.text.toString(),
                "electronicsDescription" to inputDescription.text.toString(),
                "electronicsImage" to (newImageUrl ?: imageUrl),
                "timestamp" to System.currentTimeMillis()
            )
            else -> return
        }

        FirebaseDatabase.getInstance().reference
            .child(firebaseRef)
            .child(postId!!)
            .updateChildren(updatedPost)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Post updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update post.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getAddressFromCoordinates(lat: Double, lon: Double): String {
        return try {
            val addresses = Geocoder(this).getFromLocation(lat, lon, 1)
            val address = addresses?.firstOrNull()
            listOfNotNull(address?.featureName, address?.subLocality, address?.locality).joinToString(", ")
        } catch (e: Exception) {
            "Unknown Location"
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        return result == ConnectionResult.SUCCESS
    }

    private fun validateInputs(): Boolean {
        return inputProfileName.text.isNotEmpty() &&
                inputLocation.text.isNotEmpty() &&
                inputTitle.text.isNotEmpty() &&
                inputDescription.text.isNotEmpty()
    }
}
