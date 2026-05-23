package com.example.travel_buddy.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.data.model.UserProfile
import com.example.travel_buddy.domain.repository.AuthRepository
import com.example.travel_buddy.domain.repository.PostRepository
import com.example.travel_buddy.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<UserProfile>>(UiState.Idle)
    val profileState: StateFlow<UiState<UserProfile>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateState: StateFlow<UiState<Unit>> = _updateState.asStateFlow()

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState.asStateFlow()

    private val _statsState = MutableStateFlow<UiState<Triple<Int, Int, Int>>>(UiState.Idle)
    val statsState: StateFlow<UiState<Triple<Int, Int, Int>>> = _statsState.asStateFlow()

    private val _postsState = MutableStateFlow<UiState<List<Post>>>(UiState.Idle)
    val postsState: StateFlow<UiState<List<Post>>> = _postsState.asStateFlow()

    private var currentUserId: String? = null

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            when (val result = authRepository.getCurrentProfile()) {
                is AppResult.Success -> {
                    _profileState.value = UiState.Success(result.data)
                    currentUserId = result.data.uid
                    loadStatsAndPosts(result.data.uid)
                }
                is AppResult.Error -> {
                    _profileState.value = UiState.Error(result.message)
                }
            }
        }
    }

    private fun loadStatsAndPosts(uid: String) {
        viewModelScope.launch {
            _statsState.value = UiState.Loading
            _statsState.value = when (val result = postRepository.getUserStats(uid)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Error -> UiState.Error(result.message)
            }
            loadMyTrips()
        }
    }

    fun loadMyTrips() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _postsState.value = UiState.Loading
            _postsState.value = when (val result = postRepository.getUserPosts(uid)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun loadSavedTrips() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _postsState.value = UiState.Loading
            _postsState.value = when (val result = postRepository.getUserSavedPosts(uid)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun updateProfile(username: String, imageUri: Uri?) {
        if (username.isBlank()) {
            _updateState.value = UiState.Error("Username cannot be empty")
            return
        }
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            _updateState.value = when (val result = authRepository.updateProfile(username.trim(), imageUri)) {
                is AppResult.Success -> UiState.Success(Unit)
                is AppResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = UiState.Loading
            _logoutState.value = when (val result = authRepository.logout()) {
                is AppResult.Success -> UiState.Success(Unit)
                is AppResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun clearUpdateState() {
        _updateState.value = UiState.Idle
    }

    fun clearLogoutState() {
        _logoutState.value = UiState.Idle
    }
}
