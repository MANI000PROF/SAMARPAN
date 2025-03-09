package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.samarpan.R
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

class ProfileFragment : Fragment() {

    private lateinit var profileImageCard: CardView
    private lateinit var profileImage: ImageView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImageCard = view.findViewById(R.id.profileImageCard)
        profileImage = view.findViewById(R.id.profileImage)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        toolbar = view.findViewById(R.id.toolbar)

        // Set toolbar as action bar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        // Enable options menu in toolbar
        setHasOptionsMenu(true)

        // Handle Profile Image Shrinking on Scroll
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val percentage = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            val scale = 1 - (percentage * 0.5f)

            profileImageCard.scaleX = scale
            profileImageCard.scaleY = scale

            profileImageCard.translationX = percentage * 60
            profileImageCard.translationY = percentage * (-30)
        })

        return view
    }

    // Inflate the menu options
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
                // TODO: Handle logout
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
