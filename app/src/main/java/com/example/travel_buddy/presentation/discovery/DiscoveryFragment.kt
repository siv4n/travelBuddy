package com.example.travel_buddy.presentation.discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.travel_buddy.R
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.di.ServiceLocator
import kotlinx.coroutines.launch
import com.example.travel_buddy.databinding.FragmentDiscoveryBinding

class DiscoveryFragment : Fragment() {

    private var _binding: FragmentDiscoveryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TripCardAdapter

    private val viewModel: DiscoveryViewModel by viewModels {
        DiscoveryViewModelFactory(ServiceLocator.postRepository)
    }

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
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DiscoveryState.Success -> {
                    adapter.updateData(state.posts)
                }
                is DiscoveryState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TripCardAdapter(
            trips = emptyList(),
            onTripClicked = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.postId)
                }
                findNavController().navigate(R.id.action_discoveryFragment_to_tripDetailFragment, bundle)
            },
            onLikeClicked = { post ->
                lifecycleScope.launch {
                    val result = ServiceLocator.postRepository.toggleLike(post.postId)
                    if (result is com.example.travel_buddy.core.common.AppResult.Success) {
                        val msg = if (result.data) "Liked!" else "Unliked!"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvDiscoveryGrid.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rvDiscoveryGrid.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddTrip.setOnClickListener {
            findNavController().navigate(R.id.action_discoveryFragment_to_createTripFragment)
        }
        
        binding.ivFilterBtn.setOnClickListener {
            Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show()
        }
        
        binding.ivProfileHead.setOnClickListener {
            findNavController().navigate(R.id.action_discoveryFragment_to_profileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
