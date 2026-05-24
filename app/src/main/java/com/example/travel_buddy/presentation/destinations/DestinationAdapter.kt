package com.example.travel_buddy.presentation.destinations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.travel_buddy.data.remote.Destination
import com.example.travel_buddy.databinding.ItemDestinationBinding

class DestinationAdapter(
    private var destinations: List<Destination> = emptyList(),
    private val onClick: (Destination) -> Unit = {}
) : RecyclerView.Adapter<DestinationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemDestinationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(destination: Destination) {
            binding.apply {
                tvName.text = destination.name
                tvDescription.text = destination.description
                tvWeather.text = destination.weather ?: "N/A"
                if (destination.temperature != null) {
                    tvTemperature.text = "${destination.temperature}°C"
                }
                ivImage.load(destination.imageUrl) {
                    crossfade(true)
                }
                root.setOnClickListener { onClick(destination) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemDestinationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(destinations[position])
    }

    override fun getItemCount() = destinations.size

    fun updateData(newDestinations: List<Destination>) {
        destinations = newDestinations
        notifyDataSetChanged()
    }
}
