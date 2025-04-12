package com.example.samarpan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupCard(R.id.cardNotifications, "Notifications", R.drawable.ic_notifications)
        setupCard(R.id.cardTheme, "Appearance & Theme", R.drawable.ic_theme)
        setupCard(R.id.cardPrivacy, "Privacy Policy", R.drawable.ic_privacy)
        setupCard(R.id.cardSupport, "Contact Support", R.drawable.ic_support)
        setupCard(R.id.cardAbout, "About Samarpan", R.drawable.ic_about)

        findViewById<androidx.cardview.widget.CardView>(R.id.cardNotifications).setOnClickListener {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardTheme).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(arrayOf("System Default", "Light", "Dark"), ThemeHelper.getCurrentTheme(this)) { dialog, which ->
                    when (which) {
                        0 -> ThemeHelper.setTheme(this, ThemeHelper.THEME_SYSTEM)
                        1 -> ThemeHelper.setTheme(this, ThemeHelper.THEME_LIGHT)
                        2 -> ThemeHelper.setTheme(this, ThemeHelper.THEME_DARK)
                    }
                    dialog.dismiss()
                    recreate() // Apply the theme immediately
                }.show()
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardPrivacy).setOnClickListener {
            openWebPage("https://yoursite.com/privacy-policy")
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardSupport).setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@samarpan.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardAbout).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("About Samarpan")
                .setMessage("Version 1.0\nDeveloped with ❤️ by Samarpan Team.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setupCard(cardId: Int, title: String, iconRes: Int) {
        val card = findViewById<View>(cardId)
        val titleView = card.findViewById<TextView>(R.id.title)
        val iconView = card.findViewById<ImageView>(R.id.icon)

        titleView.text = title
        iconView.setImageResource(iconRes)
    }

    private fun openWebPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
