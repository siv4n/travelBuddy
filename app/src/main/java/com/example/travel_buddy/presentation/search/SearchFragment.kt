package com.example.travel_buddy.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_buddy.R
import com.example.travel_buddy.databinding.FragmentSearchBinding
import com.example.travel_buddy.di.ServiceLocator
import com.example.travel_buddy.presentation.discovery.TripCardAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(ServiceLocator.postRepository)
    }

    private lateinit var adapter: TripCardAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        observeViewModel()
        setupBackButton()

        // Listen for like/unlike events from detail screen
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Map<String, Any>>("post_liked")
            ?.observe(viewLifecycleOwner) { data ->
                val postId = data["postId"] as? String
                val isLiked = data["isLiked"] as? Boolean
                val likesCount = data["likesCount"] as? Int
                if (postId != null && isLiked != null && likesCount != null) {
                    adapter.setLikeState(postId, likesCount, isLiked)
                }
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Map<String, Any>>("post_liked")
            }
    }

    private fun setupBackButton() {
        binding.toolbarSearch.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSearchBar() {
        // Debounce search input to avoid too many queries
        binding.etSearchQuery.addTextChangedListener { text ->
            searchJob?.cancel()
            searchJob = MainScope().launch {
                delay(500) // 500ms delay
                val query = text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchPosts(query)
                } else {
                    viewModel.clearSearch()
                }
            }
        }

        // Focus on search input automatically
        binding.etSearchQuery.requestFocus()
    }

    private fun setupRecyclerView() {
        adapter = TripCardAdapter(
            trips = mutableListOf(),
            onTripClicked = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.postId)
                }
                findNavController().navigate(R.id.action_searchFragment_to_tripDetailFragment, bundle)
            },
            onLikeClicked = { post ->
                val current = adapter.getPostById(post.postId) ?: post
                val optimisticLiked = !current.isLiked
                val optimisticCount = if (optimisticLiked) {
                    current.likesCount + 1
                } else {
                    (current.likesCount - 1).coerceAtLeast(0)
                }
                // apply optimistic update
                adapter.setLikeState(post.postId, optimisticCount, optimisticLiked)

                lifecycleScope.launch {
                    when (val result = ServiceLocator.postRepository.toggleLike(post.postId)) {
                        is com.example.travel_buddy.core.common.AppResult.Success -> {
                            val serverLiked = result.data
                            // if server disagrees, correct UI
                            if (serverLiked != optimisticLiked) {
                                val correctedCount = if (serverLiked) {
                                    current.likesCount + 1
                                } else {
                                    (current.likesCount - 1).coerceAtLeast(0)
                                }
                                adapter.setLikeState(post.postId, correctedCount, serverLiked)
                            }
                        }
                        is com.example.travel_buddy.core.common.AppResult.Error -> {
                            // revert optimistic like on error
                            adapter.setLikeState(post.postId, current.likesCount, !optimisticLiked)
                            Toast.makeText(requireContext(), "Failed to update like", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvSearchResults.visibility = View.GONE
                    binding.tvNoResults.visibility = View.GONE
                }
                is SearchState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvSearchResults.visibility = View.GONE
                    binding.tvNoResults.visibility = View.GONE
                }
                is SearchState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.posts.isEmpty()) {
                        binding.rvSearchResults.visibility = View.GONE
                        binding.tvNoResults.visibility = View.VISIBLE
                    } else {
                        binding.rvSearchResults.visibility = View.VISIBLE
                        binding.tvNoResults.visibility = View.GONE
                        adapter.updateData(state.posts)
                    }
                }
                is SearchState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvSearchResults.visibility = View.GONE
                    binding.tvNoResults.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
