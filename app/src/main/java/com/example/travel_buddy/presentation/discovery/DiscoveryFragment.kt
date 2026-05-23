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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
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
        // Listen for navigation results (e.g., after creating/editing a post)
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("post_created")
            ?.observe(viewLifecycleOwner) { created ->
                if (created == true) {
                    viewModel.loadPosts()
                    // clear the flag
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>("post_created")
                }
            }

        // Listen for save/unsave events coming back from detail screen
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_saved")
            ?.observe(viewLifecycleOwner) { postId ->
                viewModel.loadPosts()
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_saved")
            }

        // Listen for post edit/delete events
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_edited")
            ?.observe(viewLifecycleOwner) { postId ->
                viewModel.loadPosts()
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_edited")
            }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_deleted")
            ?.observe(viewLifecycleOwner) { postId ->
                viewModel.loadPosts()
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_deleted")
            }
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
            trips = mutableListOf(),
            onTripClicked = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.postId)
                }
                findNavController().navigate(R.id.action_discoveryFragment_to_tripDetailFragment, bundle)
            },
            onLikeClicked = { post ->
                val current = adapter.getPostById(post.postId) ?: post
                val optimisticLiked = !current.isLiked
                val optimisticCount = current.likesCount + if (optimisticLiked) 1 else -1
                // apply optimistic update
                adapter.setLikeState(post.postId, optimisticCount, optimisticLiked)

                lifecycleScope.launch {
                    when (val result = ServiceLocator.postRepository.toggleLike(post.postId)) {
                        is com.example.travel_buddy.core.common.AppResult.Success -> {
                            val serverLiked = result.data
                            // if server disagrees, correct UI
                            if (serverLiked != optimisticLiked) {
                                val correctedCount = current.likesCount + if (serverLiked) 1 else -1
                                adapter.setLikeState(post.postId, correctedCount, serverLiked)
                            }
                        }
                        is com.example.travel_buddy.core.common.AppResult.Error -> {
                            // revert optimistic change
                            adapter.setLikeState(post.postId, current.likesCount, current.isLiked)
                            Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                        }
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

        // Navigate to search when search bar is clicked
        binding.etSearch.setOnClickListener {
            findNavController().navigate(R.id.action_discoveryFragment_to_searchFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
