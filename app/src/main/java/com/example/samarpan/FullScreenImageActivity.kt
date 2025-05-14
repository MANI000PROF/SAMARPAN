package com.example.samarpan

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageUrl = intent.getStringExtra("image_url")
        val photoView = findViewById<PhotoView>(R.id.fullScreenPhotoView)

        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.placeholder)
                .into(photoView)
        }

        // Optional: tap to close
        photoView.setOnClickListener {
            finish()
        }
    }
}
