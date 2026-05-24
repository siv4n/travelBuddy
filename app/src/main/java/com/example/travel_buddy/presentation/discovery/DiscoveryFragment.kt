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
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.travel_buddy.databinding.FragmentDiscoveryBinding
import coil.load
import coil.transform.CircleCropTransformation

class DiscoveryFragment : Fragment() {

    private var _binding: FragmentDiscoveryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TripCardAdapter
    private var searchJob: Job? = null

    private val viewModel: DiscoveryViewModel by viewModels {
        DiscoveryViewModelFactory(ServiceLocator.postRepository, ServiceLocator.authRepository)
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
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("post_created")
            ?.observe(viewLifecycleOwner) { created ->
                if (created == true) {
                    viewModel.loadPosts()
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>("post_created")
                }
            }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_saved")
            ?.observe(viewLifecycleOwner) { postId ->
                viewModel.loadPosts()
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_saved")
            }

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

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_edited")
            ?.observe(viewLifecycleOwner) { postId ->
                viewModel.loadPosts()
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_edited")
            }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_deleted")
            ?.observe(viewLifecycleOwner) { postId ->
                if (postId != null) {
                    adapter.removePost(postId)
                    viewModel.removePostFromCache(postId)
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_deleted")
                }
            }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPosts()
        viewModel.loadUserProfile()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DiscoveryState.Success -> {
                    updatePostsList()
                }
                is DiscoveryState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            updatePostsList()
        }
    }

    private fun updatePostsList() {
        val state = viewModel.uiState.value
        if (state is DiscoveryState.Success) {
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val currentUserProfile = viewModel.userProfile.value
            val posts = if (currentUserId != null && currentUserProfile != null) {
                state.posts.map { post ->
                    if (post.authorId == currentUserId) {
                        post.copy(
                            authorUsername = currentUserProfile.username,
                            authorImageUrl = currentUserProfile.imageUrl
                        )
                    } else {
                        post
                    }
                }
            } else {
                state.posts
            }
            adapter.updateData(posts)
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
                val optimisticCount = if (optimisticLiked) {
                    current.likesCount + 1
                } else {
                    (current.likesCount - 1).coerceAtLeast(0)
                }
                adapter.setLikeState(post.postId, optimisticCount, optimisticLiked)

                lifecycleScope.launch {
                    when (val result = ServiceLocator.postRepository.toggleLike(post.postId)) {
                        is com.example.travel_buddy.core.common.AppResult.Success -> {
                            val serverLiked = result.data
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

        binding.ivProfileHead.setOnClickListener {
            findNavController().navigate(R.id.action_discoveryFragment_to_profileFragment)
        }

        binding.etSearch.addTextChangedListener { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(500)
                val query = text?.toString()?.trim().orEmpty()
                viewModel.searchPosts(query)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
