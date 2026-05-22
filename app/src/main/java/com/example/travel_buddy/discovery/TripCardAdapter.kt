package com.example.travel_buddy.presentation.discovery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.databinding.ItemTripCardBinding

class TripCardAdapter(
    private var trips: List<Post>,
    private val onTripClicked: (Post) -> Unit,
    private val onLikeClicked: (Post) -> Unit
) : RecyclerView.Adapter<TripCardAdapter.TripViewHolder>() {

    inner class TripViewHolder(private val binding: ItemTripCardBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(trip: Post) {
            binding.tvTitle.text = trip.title
            binding.tvLocation.text = trip.location
            binding.tvUsername.text = trip.authorId
            
            // Set listeners
            binding.root.setOnClickListener { onTripClicked(trip) }
            binding.ivLike.setOnClickListener { onLikeClicked(trip) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount(): Int = trips.size

    fun updateData(newTrips: List<Post>) {
        this.trips = newTrips
        notifyDataSetChanged()
    }
}
