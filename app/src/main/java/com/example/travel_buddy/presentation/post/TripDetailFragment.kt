package com.example.travel_buddy.presentation.post

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.travel_buddy.R
import com.google.firebase.auth.FirebaseAuth
import com.example.travel_buddy.databinding.FragmentTripDetailBinding
import com.example.travel_buddy.di.ServiceLocator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripDetailFragment : Fragment() {

    private var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private val postId: String by lazy {
        arguments?.getString("postId") ?: ""
    }

    private val viewModel: TripDetailViewModel by viewModels {
        TripDetailViewModelFactory(ServiceLocator.postRepository, postId)
    }

    private var lastSavedState: Boolean? = null
    private var lastLikedState: Boolean? = null
    private var currentPostAuthorId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (postId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Post ID missing", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.llLike.setOnClickListener {
            viewModel.toggleLike()
        }

        binding.ivSave.setOnClickListener {
            viewModel.toggleSave()
        }

        binding.ivEdit.setOnClickListener {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null || currentPostAuthorId != currentUserId) {
                Toast.makeText(requireContext(), "You can only edit your own posts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putString("postId", postId)
            }
            findNavController().navigate(R.id.action_tripDetailFragment_to_editPostFragment, bundle)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripDetailState.Loading -> {
                    // Could show loader
                }
                is TripDetailState.Success -> {
                    val post = state.post
                    currentPostAuthorId = post.authorId
                    binding.tvTitle.text = post.title
                    binding.tvLocation.text = post.location
                    binding.tvDescription.text = post.description
                    binding.tvUsername.text = if (post.authorUsername.isNotBlank()) post.authorUsername else post.authorId
                    binding.tvDate.text = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(Date(post.timestamp))
                    binding.tvLikesCount.text = post.likesCount.toString()

                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    binding.ivEdit.isVisible = currentUserId != null && post.authorId == currentUserId

                    binding.ivHeaderImage.load(post.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_image_placeholder)
                        error(R.drawable.ic_image_placeholder)
                    }

                    binding.ivThumbnail1.load(post.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_image_placeholder)
                        error(R.drawable.ic_image_placeholder)
                    }

                    binding.ivThumbnail2.load(post.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_image_placeholder)
                        error(R.drawable.ic_image_placeholder)
                    }

                    binding.ivUserAvatar.load(post.authorImageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile_placeholder)
                        error(R.drawable.ic_profile_placeholder)
                        transformations(coil.transform.CircleCropTransformation())
                    }

                    // Update UI for Like Status
                    if (state.isLiked) {
                        binding.ivHeart.setImageResource(R.drawable.ic_heart_filled)
                        binding.ivHeart.setColorFilter(android.graphics.Color.RED)
                    } else {
                        binding.ivHeart.setImageResource(R.drawable.ic_heart)
                        binding.ivHeart.setColorFilter(android.graphics.Color.parseColor("#757575"))
                    }

                    // Update UI for Save Status
                    if (state.isSaved) {
                        binding.ivSave.setImageResource(R.drawable.ic_bookmark_filled)
                    } else {
                        binding.ivSave.setImageResource(R.drawable.ic_bookmark_border)
                    }

                    // Notify previous fragment (if any) that saved state changed
                    val prev = findNavController().previousBackStackEntry
                    if (prev != null && lastSavedState != null && lastSavedState != state.isSaved) {
                        prev.savedStateHandle.set("post_saved", post.postId)
                    }
                    lastSavedState = state.isSaved

                    // Notify previous fragment (if any) that like state changed
                    if (prev != null && lastLikedState != null && lastLikedState != state.isLiked) {
                        prev.savedStateHandle.set("post_liked", mapOf("postId" to post.postId, "isLiked" to state.isLiked, "likesCount" to post.likesCount))
                    }
                    lastLikedState = state.isLiked

                    // Listen for post edit/delete from EditPostFragment
                    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_edited")
                        ?.observe(viewLifecycleOwner) { editedPostId ->
                            if (editedPostId == postId) {
                                // Reload the post to show updated content
                                viewModel.loadData()
                            }
                            // Pass the edit event up to parent fragment (e.g., DiscoveryFragment)
                            val prev = findNavController().previousBackStackEntry
                            prev?.savedStateHandle?.set("post_edited", editedPostId)
                            findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_edited")
                        }

                    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_deleted")
                        ?.observe(viewLifecycleOwner) { deletedPostId ->
                            if (deletedPostId != null) {
                                // Pass the delete event up to parent fragment FIRST
                                val prev = findNavController().previousBackStackEntry
                                prev?.savedStateHandle?.set("post_deleted", deletedPostId)
                                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_deleted")
                                
                                if (deletedPostId == postId) {
                                    // Navigate up since the post no longer exists
                                    findNavController().navigateUp()
                                }
                            }
                        }
                }
                is TripDetailState.Error -> {
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

