package com.example.travel_buddy.presentation.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.travel_buddy.R
import com.example.travel_buddy.presentation.common.AuthViewModelFactory
import com.example.travel_buddy.presentation.common.SessionState
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeSession()
        authViewModel.checkSession()
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.sessionState.collect { state ->
                    when (state) {
                        SessionState.Authenticated -> findNavController().navigate(R.id.action_splashFragment_to_profileFragment)
                        SessionState.Unauthenticated -> {
                            val intent = android.content.Intent(requireContext(), com.example.travel_buddy.LoginActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        SessionState.Loading -> Unit
                    }
                }
            }
        }
    }
}
