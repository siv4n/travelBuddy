package com.example.travel_buddy.presentation.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.travel_buddy.databinding.FragmentCreateTripBinding
import com.example.travel_buddy.di.ServiceLocator

class CreateTripFragment : Fragment() {

    private var _binding: FragmentCreateTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateTripViewModel by viewModels {
        CreateTripViewModelFactory(ServiceLocator.postRepository)
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
        _binding = FragmentCreateTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnAddPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }
        
        // Also allow clicking the preview area to select an image
        binding.ivTripPreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val location = binding.etLocation.text.toString()
            val description = binding.etDescription.text.toString()

            viewModel.createTrip(title, location, description)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreateTripState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }
                is CreateTripState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                }
                is CreateTripState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Trip created successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is CreateTripState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
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
