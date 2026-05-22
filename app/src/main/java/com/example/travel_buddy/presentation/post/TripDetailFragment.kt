package com.example.travel_buddy.presentation.post

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.travel_buddy.R
import com.example.travel_buddy.databinding.FragmentTripDetailBinding
import com.example.travel_buddy.di.ServiceLocator
import java.util.Date

class TripDetailFragment : Fragment() {

    private var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private val postId: String by lazy {
        arguments?.getString("postId") ?: ""
    }

    private val viewModel: TripDetailViewModel by viewModels {
        TripDetailViewModelFactory(ServiceLocator.postRepository, postId)
    }

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

        binding.ivSave.setOnClickListener {
            viewModel.toggleSave()
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
                    binding.tvTitle.text = post.title
                    binding.tvLocation.text = post.location
                    binding.tvDescription.text = post.description
                    binding.tvUsername.text = post.authorId
                    binding.tvDate.text = DateFormat.format("MMMM dd, yyyy", Date(post.timestamp))
                    
                    // Ideally, use Glide to load post.imageUrl into binding.ivHeaderImage
                    // Glide.with(requireContext()).load(post.imageUrl).into(binding.ivHeaderImage)

                    // Update UI for Save Status
                    if (state.isSaved) {
                        binding.ivSave.setImageResource(R.drawable.ic_bookmark_filled)
                    } else {
                        binding.ivSave.setImageResource(R.drawable.ic_bookmark_border)
                    }
                    
                    // Note: If you want a Like heart in detail view, you'd add it to the layout
                    // and toggle it using viewModel.toggleLike()
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
