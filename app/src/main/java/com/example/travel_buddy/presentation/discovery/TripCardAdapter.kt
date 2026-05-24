package com.example.travel_buddy.presentation.discovery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.databinding.ItemTripCardBinding

class TripCardAdapter(
    private var trips: MutableList<Post>,
    private val onTripClicked: (Post) -> Unit,
    private val onLikeClicked: (Post) -> Unit
) : RecyclerView.Adapter<TripCardAdapter.TripViewHolder>() {

    inner class TripViewHolder(private val binding: ItemTripCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: Post) {
            binding.tvTitle.text = trip.title
            binding.tvLocation.text = trip.location
            binding.tvUsername.text = if (trip.authorUsername.isNotBlank()) trip.authorUsername else trip.authorId

            // load images with coil, with placeholders
            binding.ivTripImage.load(trip.imageUrl) {
                crossfade(true)
                placeholder(com.example.travel_buddy.R.drawable.ic_image_placeholder)
                error(com.example.travel_buddy.R.drawable.ic_image_placeholder)
            }

            binding.ivUserAvatar.load(trip.authorImageUrl) {
                crossfade(true)
                placeholder(com.example.travel_buddy.R.drawable.ic_profile_placeholder)
                error(com.example.travel_buddy.R.drawable.ic_profile_placeholder)
                transformations(coil.transform.CircleCropTransformation())
            }

            // likes badge
            binding.tvBadge.text = trip.likesCount.toString()

            // Update like button appearance
            if (trip.isLiked) {
                binding.ivLike.setImageResource(com.example.travel_buddy.R.drawable.ic_heart_filled)
                binding.ivLike.setColorFilter(android.graphics.Color.RED)
            } else {
                binding.ivLike.setImageResource(com.example.travel_buddy.R.drawable.ic_heart)
                binding.ivLike.setColorFilter(android.graphics.Color.parseColor("#757575"))
            }

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
        this.trips = newTrips.toMutableList()
        notifyDataSetChanged()
    }

    fun setLikeState(postId: String, likesCount: Int, isLiked: Boolean) {
        val index = trips.indexOfFirst { it.postId == postId }
        if (index >= 0) {
            val p = trips[index]
            trips[index] = p.copy(likesCount = likesCount, isLiked = isLiked)
            notifyItemChanged(index)
        }
    }

    fun setSaveState(postId: String, isSaved: Boolean) {
        val index = trips.indexOfFirst { it.postId == postId }
        if (index >= 0) {
            val p = trips[index]
            trips[index] = p.copy(isSaved = isSaved)
            notifyItemChanged(index)
        }
    }

    fun getPostById(postId: String): Post? = trips.firstOrNull { it.postId == postId }

    fun removePost(postId: String) {
        val index = trips.indexOfFirst { it.postId == postId }
        if (index >= 0) {
            trips.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
