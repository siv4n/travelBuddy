package com.example.travel_buddy.presentation.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.travel_buddy.databinding.FragmentCreateTripBinding
import com.google.android.material.snackbar.Snackbar
import com.example.travel_buddy.di.ServiceLocator
import com.example.travel_buddy.domain.service.LocationService
import com.example.travel_buddy.presentation.util.LocationPermissionHelper

class CreateTripFragment : Fragment() {

    private var _binding: FragmentCreateTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateTripViewModel by viewModels {
        CreateTripViewModelFactory(ServiceLocator.postRepository)
    }

    private lateinit var locationPermissionHelper: LocationPermissionHelper

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

        locationPermissionHelper = LocationPermissionHelper(this)
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
        
        // Also allow clicking the preview area to select an image
        binding.ivTripPreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Request location permissions when location field is focused
        binding.etLocation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !locationPermissionHelper.hasLocationPermission()) {
                locationPermissionHelper.requestLocationPermissions()
            }
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val location = binding.etLocation.text.toString()
            val description = binding.etDescription.text.toString()

            // Validate location
            if (!LocationService.isValidLocation(location)) {
                Snackbar.make(binding.root, "Please enter a valid location", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createTrip(title, location, description)
        }

        binding.toolbarCreateTrip.setNavigationOnClickListener {
            findNavController().navigateUp()
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
                    Snackbar.make(binding.root, "Trip created successfully!", Snackbar.LENGTH_SHORT).show()
                    // Notify previous back stack entry to refresh posts
                    val previousEntry = findNavController().previousBackStackEntry
                    previousEntry?.savedStateHandle?.set("post_created", true)
                    findNavController().navigateUp()
                }
                is CreateTripState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (locationPermissionHelper.onPermissionResult(requestCode, permissions, grantResults)) {
            Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show()
        } else if (requestCode == LocationPermissionHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(requireContext(), "Location permission denied. You can still enter location manually.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
