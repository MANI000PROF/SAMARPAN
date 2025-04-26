package com.example.samarpan.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.Model.SavedLocation
import com.example.samarpan.R
import com.example.samarpan.databinding.ItemSavedLocationBinding

class SavedLocationAdapter(private val savedLocations: List<SavedLocation>) :
    RecyclerView.Adapter<SavedLocationAdapter.SavedLocationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedLocationViewHolder {
        val binding = ItemSavedLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavedLocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedLocationViewHolder, position: Int) {
        val location = savedLocations[position]
        holder.bind(location)
    }

    override fun getItemCount(): Int = savedLocations.size

    inner class SavedLocationViewHolder(private val binding: ItemSavedLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(savedLocation: SavedLocation) {
            binding.locationName.text = savedLocation.locationName
            binding.locationAddress.text = savedLocation.locationAddress
        }
    }
}
