package com.example.travel_buddy.presentation.discovery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.data.model.UserProfile
import com.example.travel_buddy.domain.repository.AuthRepository
import com.example.travel_buddy.domain.repository.PostRepository
import kotlinx.coroutines.launch

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Loading : DiscoveryState()
    data class Success(val posts: List<Post>) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

class DiscoveryViewModel(
    private val repository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<DiscoveryState>(DiscoveryState.Idle)
    val uiState: LiveData<DiscoveryState> = _uiState

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private var allPosts: List<Post> = emptyList()

    init {
        loadPosts()
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            when (val result = authRepository.getCurrentProfile()) {
                is AppResult.Success -> {
                    val profile = result.data
                    _userProfile.value = profile
                    launch {
                        repository.syncUserPosts(profile.uid, profile.username, profile.imageUrl)
                    }
                }
                else -> _userProfile.value = null
            }
        }
    }

    fun removePostFromCache(postId: String) {
        allPosts = allPosts.filter { it.postId != postId }
        if (uiState.value is DiscoveryState.Success) {
            val currentPosts = (uiState.value as DiscoveryState.Success).posts.filter { it.postId != postId }
            _uiState.value = DiscoveryState.Success(currentPosts)
        }
    }

    fun loadPosts() {
        _uiState.value = DiscoveryState.Loading
        viewModelScope.launch {
            when (val result = repository.getAllPosts()) {
                is AppResult.Success -> {
                    allPosts = result.data
                    _uiState.value = DiscoveryState.Success(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = DiscoveryState.Error(result.message)
                }
            }
        }
    }

    fun searchPosts(query: String) {
        if (query.isBlank()) {
            _uiState.value = DiscoveryState.Success(allPosts)
            return
        }
        _uiState.value = DiscoveryState.Loading
        viewModelScope.launch {
            when (val result = repository.searchPosts(query)) {
                is AppResult.Success -> {
                    _uiState.value = DiscoveryState.Success(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = DiscoveryState.Error(result.message)
                }
            }
        }
    }
}
