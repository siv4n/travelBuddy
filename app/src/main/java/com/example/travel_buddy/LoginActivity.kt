package com.example.travel_buddy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.travel_buddy.databinding.ActivityLoginBinding
import com.example.travel_buddy.presentation.auth.AuthViewModel
import com.example.travel_buddy.presentation.common.AuthViewModelFactory
import com.example.travel_buddy.presentation.common.UiState
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            authViewModel.login(email, password)
        }

        binding.textViewSignUp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("destination", "register")
            startActivity(intent)
            finish()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.loginState.collect { state ->
                    binding.buttonLogin.isEnabled = state !is UiState.Loading

                    when (state) {
                        is UiState.Success -> {
                            Toast.makeText(this@LoginActivity, "Welcome back", Toast.LENGTH_SHORT).show()
                            authViewModel.clearLoginState()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                        is UiState.Error -> {
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                            authViewModel.clearLoginState()
                        }
                        UiState.Idle,
                        UiState.Loading -> Unit
                    }
                }
            }
        }
    }
}
