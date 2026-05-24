package com.example.travel_buddy.presentation.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.travel_buddy.R
import com.example.travel_buddy.databinding.FragmentEditProfileBinding
import com.example.travel_buddy.presentation.common.AuthViewModelFactory
import com.example.travel_buddy.presentation.common.UiState
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels { AuthViewModelFactory() }
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        binding.imageProfilePreview.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
            transformations(coil.transform.CircleCropTransformation())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)

        setupListeners()
        observeStates()
        profileViewModel.loadProfile()
    }

    private fun setupListeners() {
        binding.buttonSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.buttonSaveProfile.setOnClickListener {
            profileViewModel.updateProfile(
                username = binding.editTextUsername.text?.toString().orEmpty(),
                imageUri = selectedImageUri
            )
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    profileViewModel.profileState.collect { state ->
                        if (state is UiState.Success) {
                            binding.editTextUsername.setText(state.data.username)
                            binding.imageProfilePreview.load(state.data.imageUrl) {
                                crossfade(true)
                                placeholder(R.drawable.ic_profile_placeholder)
                                error(R.drawable.ic_profile_placeholder)
                                transformations(coil.transform.CircleCropTransformation())
                            }
                        }
                    }
                }

                launch {
                    profileViewModel.updateState.collect { state ->
                        binding.progressSave.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
                        binding.buttonSaveProfile.isEnabled = state !is UiState.Loading

                        when (state) {
                            is UiState.Success -> {
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                                profileViewModel.clearUpdateState()
                                findNavController().navigateUp()
                            }

                            is UiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                profileViewModel.clearUpdateState()
                            }

                            UiState.Idle,
                            UiState.Loading -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
