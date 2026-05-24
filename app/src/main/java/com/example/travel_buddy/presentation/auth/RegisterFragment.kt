package com.example.travel_buddy.presentation.auth

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
import com.example.travel_buddy.data.model.RegisterRequest
import com.example.travel_buddy.databinding.FragmentRegisterBinding
import com.example.travel_buddy.presentation.common.AuthViewModelFactory
import com.example.travel_buddy.presentation.common.UiState
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory() }
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        binding.imageProfilePreview.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.buttonSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.buttonRegister.setOnClickListener {
            val request = RegisterRequest(
                username = binding.editTextUsername.text?.toString().orEmpty(),
                email = binding.editTextEmail.text?.toString().orEmpty(),
                password = binding.editTextPassword.text?.toString().orEmpty(),
                imageUri = selectedImageUri
            )
            val confirmPassword = binding.editTextConfirmPassword.text?.toString().orEmpty()
            authViewModel.register(request, confirmPassword)
        }

        binding.textViewGoLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.registerState.collect { state ->
                    binding.progressRegister.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
                    binding.buttonRegister.isEnabled = state !is UiState.Loading

                    when (state) {
                        is UiState.Success -> {
                            Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                            authViewModel.clearRegisterState()
                            findNavController().navigate(R.id.action_registerFragment_to_discoveryFragment)
                        }

                        is UiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            authViewModel.clearRegisterState()
                        }

                        UiState.Idle,
                        UiState.Loading -> Unit
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
