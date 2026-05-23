package com.example.travel_buddy.presentation.discovery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.domain.repository.PostRepository
import kotlinx.coroutines.launch

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Loading : DiscoveryState()
    data class Success(val posts: List<Post>) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

class DiscoveryViewModel(
    private val repository: PostRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<DiscoveryState>(DiscoveryState.Idle)
    val uiState: LiveData<DiscoveryState> = _uiState

    private var allPosts: List<Post> = emptyList()

    init {
        loadPosts()
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
