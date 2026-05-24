package com.example.travel_buddy.presentation.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.RegisterRequest
import com.example.travel_buddy.domain.repository.AuthRepository
import com.example.travel_buddy.presentation.common.SessionState
import com.example.travel_buddy.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val registerState: StateFlow<UiState<Unit>> = _registerState.asStateFlow()

    fun checkSession() {
        _sessionState.value = if (repository.isUserLoggedIn()) {
            SessionState.Authenticated
        } else {
            SessionState.Unauthenticated
        }
    }

    fun login(email: String, password: String) {
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        if (emailError != null) {
            _loginState.value = UiState.Error(emailError)
            return
        }
        if (passwordError != null) {
            _loginState.value = UiState.Error(passwordError)
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading
            _loginState.value = when (val result = repository.login(email.trim(), password)) {
                is AppResult.Success -> UiState.Success(Unit)
                is AppResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun register(request: RegisterRequest, confirmPassword: String) {
        val usernameError = validateUsername(request.username)
        val emailError = validateEmail(request.email)
        val passwordError = validatePassword(request.password)
        val confirmError = validateConfirmPassword(request.password, confirmPassword)

        val firstError = listOf(usernameError, emailError, passwordError, confirmError).firstOrNull { it != null }
        if (firstError != null) {
            _registerState.value = UiState.Error(firstError)
            return
        }

        viewModelScope.launch {
            _registerState.value = UiState.Loading
            _registerState.value = when (val result = repository.register(request)) {
                is AppResult.Success -> UiState.Success(Unit)
                is AppResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun clearLoginState() {
        _loginState.value = UiState.Idle
    }

    fun clearRegisterState() {
        _registerState.value = UiState.Idle
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Please confirm your password"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }

    private fun validateUsername(username: String): String? {
        return if (username.isBlank()) {
            "Username cannot be empty"
        } else {
            null
        }
    }
}
