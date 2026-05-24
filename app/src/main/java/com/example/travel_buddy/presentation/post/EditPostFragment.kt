package com.example.travel_buddy.presentation.post

import android.os.Bundle
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.travel_buddy.R
import com.example.travel_buddy.databinding.FragmentEditPostBinding
import com.example.travel_buddy.di.ServiceLocator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private val postId: String by lazy {
        arguments?.getString("postId") ?: ""
    }

    private var isDeleting = false

    private val viewModel: EditPostViewModel by viewModels {
        EditPostViewModelFactory(
            ServiceLocator.postRepository,
            ServiceLocator.locationRepository,
            postId
        )
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            val selectedUris = uris.take(5)
            viewModel.setImageUris(selectedUris)
            updateImagePreviews(selectedUris)
        }
    }

    private fun updateImagePreviews(uris: List<android.net.Uri>) {
        if (uris.isNotEmpty()) {
            binding.ivTripPreview.setImageURI(uris[0])
            binding.ivTripPreview.visibility = View.VISIBLE
            
            binding.llThumbnailPreviews.removeAllViews()
            if (uris.isNotEmpty()) {
                binding.llThumbnailPreviews.visibility = View.VISIBLE
                for (i in 0 until uris.size) {
                    val uri = uris[i]
                    val isFirst = (i == 0)
                    val card = createThumbnailCard(
                        sizeDp = 56,
                        isFirst = isFirst,
                        imageUri = uri
                    ) {
                        binding.ivTripPreview.setImageURI(uri)
                    }
                    binding.llThumbnailPreviews.addView(card)
                }
            } else {
                binding.llThumbnailPreviews.visibility = View.GONE
            }
        } else {
            binding.ivTripPreview.visibility = View.GONE
            binding.llThumbnailPreviews.visibility = View.GONE
        }
    }

    private fun loadExistingPreviews(urls: List<String>) {
        if (urls.isNotEmpty()) {
            binding.ivTripPreview.load(urls[0]) {
                crossfade(true)
            }
            binding.ivTripPreview.visibility = View.VISIBLE
            
            binding.llThumbnailPreviews.removeAllViews()
            if (urls.isNotEmpty()) {
                binding.llThumbnailPreviews.visibility = View.VISIBLE
                for (i in 0 until urls.size) {
                    val url = urls[i]
                    val isFirst = (i == 0)
                    val card = createThumbnailCard(
                        sizeDp = 56,
                        isFirst = isFirst,
                        imageUrl = url
                    ) {
                        binding.ivTripPreview.load(url) {
                            crossfade(true)
                        }
                    }
                    binding.llThumbnailPreviews.addView(card)
                }
            } else {
                binding.llThumbnailPreviews.visibility = View.GONE
            }
        } else {
            binding.ivTripPreview.visibility = View.GONE
            binding.llThumbnailPreviews.visibility = View.GONE
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

        val existingUris = viewModel.getSelectedImageUris()
        if (existingUris != null) {
            updateImagePreviews(existingUris)
        }
    }

    private fun setupLocationAutocomplete() {
        (binding.etLocation as? AutoCompleteTextView)?.apply {
            addTextChangedListener { text ->
                viewModel.searchLocations(text.toString())
            }
        }
    }

    private fun setupListeners() {
        binding.btnAddPhoto.setOnClickListener {
            pickImages.launch("image/*")
        }

        binding.ivTripPreview.setOnClickListener {
            pickImages.launch("image/*")
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

        binding.toolbarEditPost.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.postData.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId == null || post.authorId != currentUserId) {
                    Toast.makeText(requireContext(), "You can only edit your own posts", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    binding.etTitle.setText(post.title)
                    binding.etLocation.setText(post.location, false)
                    binding.etDescription.setText(post.description)
                    
                    val newlyPicked = viewModel.getSelectedImageUris()
                    if (newlyPicked != null) {
                        updateImagePreviews(newlyPicked)
                    } else {
                        val urls = post.imageUrls.ifEmpty {
                            if (post.imageUrl.isNotEmpty()) listOf(post.imageUrl) else emptyList()
                        }
                        loadExistingPreviews(urls)
                    }
                }
            }
        }

        viewModel.locationSuggestions.observe(viewLifecycleOwner) { suggestions ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                suggestions
            )
            (binding.etLocation as? AutoCompleteTextView)?.setAdapter(adapter)
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
                        prev?.savedStateHandle?.set("post_deleted", postId)
                    } else {
                        Toast.makeText(requireContext(), "Post updated successfully!", Toast.LENGTH_SHORT).show()
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

    private fun createThumbnailCard(
        sizeDp: Int,
        isFirst: Boolean,
        imageUrl: String? = null,
        imageUri: android.net.Uri? = null,
        onClick: () -> Unit
    ): androidx.cardview.widget.CardView {
        val context = requireContext()
        val density = resources.displayMetrics.density
        
        val cardView = androidx.cardview.widget.CardView(context).apply {
            val sizePx = (sizeDp * density).toInt()
            val marginStartPx = if (isFirst) 0 else (8 * density).toInt()
            
            val lp = android.widget.LinearLayout.LayoutParams(sizePx, sizePx).apply {
                marginStart = marginStartPx
            }
            layoutParams = lp
            radius = (8 * density)
            cardElevation = (2 * density)
            preventCornerOverlap = false
            setCardBackgroundColor(android.graphics.Color.WHITE)
        }

        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            val paddingPx = (2 * density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            
            if (imageUrl != null) {
                load(imageUrl) {
                    crossfade(true)
                    placeholder(com.example.travel_buddy.R.drawable.ic_image_placeholder)
                    error(com.example.travel_buddy.R.drawable.ic_image_placeholder)
                }
            } else if (imageUri != null) {
                setImageURI(imageUri)
            } else {
                setImageResource(com.example.travel_buddy.R.drawable.ic_image_placeholder)
            }
            
            setOnClickListener { onClick() }
        }

        cardView.addView(imageView)
        return cardView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
