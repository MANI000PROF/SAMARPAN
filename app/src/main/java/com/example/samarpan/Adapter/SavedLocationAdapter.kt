package com.example.samarpan.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.samarpan.Model.SavedLocation
import com.example.samarpan.R
import com.example.samarpan.databinding.ItemSavedLocationBinding

class SavedLocationAdapter(
    private val locations: List<SavedLocation>,
    private val onEdit: (SavedLocation) -> Unit,
    private val onDelete: (SavedLocation) -> Unit,
    private val onSetPrimary: (SavedLocation) -> Unit
) : RecyclerView.Adapter<SavedLocationAdapter.LocationViewHolder>() {

    inner class LocationViewHolder(val binding: ItemSavedLocationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemSavedLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.binding.locationNameTextView.text = location.name
        holder.binding.locationAddressTextView.text = location.address

        holder.binding.editButton.setOnClickListener { onEdit(location) }
        holder.binding.deleteButton.setOnClickListener { onDelete(location) }
        holder.binding.setPrimaryButton.setOnClickListener { onSetPrimary(location) }
        // Clear and set appropriate icon
        val iconRes = if (location.primary) {
            R.drawable.ic_primary_checked
        } else {
            R.drawable.ic_primary_unchecked
        }
        holder.binding.setPrimaryButton.setImageResource(iconRes)
    }

    override fun getItemCount() = locations.size
}
