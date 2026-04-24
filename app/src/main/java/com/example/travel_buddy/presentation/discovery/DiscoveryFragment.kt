package com.example.travel_buddy.presentation.discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.travel_buddy.R
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.databinding.FragmentDiscoveryBinding

class DiscoveryFragment : Fragment() {

    private var _binding: FragmentDiscoveryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TripCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        loadDummyData()
    }

    private fun setupRecyclerView() {
        adapter = TripCardAdapter(
            trips = emptyList(),
            onTripClicked = { post ->
                Toast.makeText(requireContext(), "Clicked: ${post.title}", Toast.LENGTH_SHORT).show()
            },
            onLikeClicked = { post ->
                Toast.makeText(requireContext(), "Liked: ${post.title}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvDiscoveryGrid.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rvDiscoveryGrid.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddTrip.setOnClickListener {
            findNavController().navigate(R.id.action_discoveryFragment_to_createTripFragment)
        }
        
        binding.ivFilterIcon.setOnClickListener {
            Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDummyData() {
        val dummyPosts = listOf(
            Post(title = "Eiffel Tower Adventure", location = "Paris, France", authorId = "sivan_travels"),
            Post(title = "Canyon Hike", location = "Arizona, USA", authorId = "hiker_bob"),
            Post(title = "Machu Picchu Explore", location = "Cusco, Peru", authorId = "explorer_jane"),
            Post(title = "Santorini Sunset Vibes", location = "Santorini, Greece", authorId = "greece_lover"),
            Post(title = "Kyoto Temples Walk", location = "Kyoto, Japan", authorId = "ninja_sam"),
            Post(title = "Banff National Park Trip", location = "Alberta, Canada", authorId = "maple_leaf")
        )
        adapter.updateData(dummyPosts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
