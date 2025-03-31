package com.example.samarpan.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.samarpan.Model.LeaderBoardDonor
import com.example.samarpan.R
import com.example.samarpan.databinding.ItemLeaderboardBinding

class LeaderBoardAdapter(private val donors: List<LeaderBoardDonor>) :
    RecyclerView.Adapter<LeaderBoardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemLeaderboardBinding.bind(view)

        fun bind(donor: LeaderBoardDonor, position: Int) {
            binding.donorName.text = donor.name
            binding.donationCount.text = "Donations: ${donor.donationCount}"

            Glide.with(binding.root).load(donor.profileImage)
                .placeholder(R.drawable.profile_placeholder)
                .into(binding.donorImage)

            // Crown for top donor
            if (position == 0) {
                binding.crownIcon.visibility = View.VISIBLE
            } else {
                binding.crownIcon.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(donors[position], position)
    }

    override fun getItemCount() = donors.size
}
