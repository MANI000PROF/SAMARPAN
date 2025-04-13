package com.example.samarpan.Fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.example.samarpan.R

class MenuFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.ThemeOverlay_Samarpan_Menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onStart() {
        super.onStart()

        // Set width to half the screen and align to left
        dialog?.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.75).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.START)
            setWindowAnimations(R.style.SlideInMenuAnimation)
        }
    }
}
