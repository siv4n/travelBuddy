package com.example.travel_buddy.presentation.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.travel_buddy.databinding.FragmentEditPostBinding
import com.example.travel_buddy.di.ServiceLocator
import com.example.travel_buddy.domain.service.LocationService

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private val postId: String by lazy {
        arguments?.getString("postId") ?: ""
    }

    private var isDeleting = false

    private val viewModel: EditPostViewModel by viewModels {
        EditPostViewModelFactory(ServiceLocator.postRepository, postId)
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.setImageUri(uri)
            binding.ivTripPreview.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (postId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Post ID missing", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupLocationAutocomplete()
        setupListeners()
        observeViewModel()
    }

    private fun setupLocationAutocomplete() {
        val locationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            LocationService.getPopularLocations()
        )
        
        (binding.etLocation as? AutoCompleteTextView)?.apply {
            setAdapter(locationAdapter)
            addTextChangedListener { text ->
                val suggestions = LocationService.getSuggestions(text.toString())
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    suggestions
                )
                setAdapter(adapter)
            }
        }
    }

    private fun setupListeners() {
        binding.btnAddPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.ivTripPreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val location = binding.etLocation.text.toString()
            val description = binding.etDescription.text.toString()
            viewModel.updatePost(title, location, description)
        }
 
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ ->
                    isDeleting = true
                    viewModel.deletePost()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewModel.postData.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                binding.etTitle.setText(post.title)
                binding.etLocation.setText(post.location)
                binding.etDescription.setText(post.description)
                binding.ivTripPreview.load(post.imageUrl) {
                    crossfade(true)
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditPostState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.btnDelete.isEnabled = true
                }
                is EditPostState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                    binding.btnDelete.isEnabled = false
                }
                is EditPostState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val prev = findNavController().previousBackStackEntry
                    if (isDeleting) {
                        Toast.makeText(requireContext(), "Post deleted successfully!", Toast.LENGTH_SHORT).show()
                        // Notify previous screens that post was deleted
                        prev?.savedStateHandle?.set("post_deleted", postId)
                    } else {
                        Toast.makeText(requireContext(), "Post updated successfully!", Toast.LENGTH_SHORT).show()
                        // Notify previous screens that post was edited
                        prev?.savedStateHandle?.set("post_edited", postId)
                    }
                    findNavController().navigateUp()
                }
                is EditPostState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.btnDelete.isEnabled = true
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
