package com.example.samarpan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class IntroPagerAdapter(private val activity: AppCompatActivity) :
    RecyclerView.Adapter<IntroPagerAdapter.IntroViewHolder>() {

    private val pages = listOf(
        IntroPage("Donate Food", "Help eliminate hunger by donating surplus or unused food. Every meal counts!", R.raw.food_donation),
        IntroPage("Share Clothes", "Give your gently-used clothes a new life. Help someone stay warm and confident.", R.raw.clothes_donation),
        IntroPage("Pass On Electronics", "Old gadgets can bring light to someone's future. Recycle and donate with purpose.", R.raw.electronics_donation)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_intro_page, parent, false)
        return IntroViewHolder(view)
    }

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: IntroViewHolder, position: Int) {
        val page = pages[position]
        holder.bind(page)
    }

    class IntroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val animationView = view.findViewById<LottieAnimationView>(R.id.animationView)
        private val titleText = view.findViewById<TextView>(R.id.titleText)
        private val descText = view.findViewById<TextView>(R.id.descriptionText)

        fun bind(page: IntroPage) {
            titleText.text = page.title
            descText.text = page.description
            animationView.setAnimation(page.animationRes)
        }
    }

    data class IntroPage(val title: String, val description: String, val animationRes: Int)
}
