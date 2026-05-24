package com.example.travel_buddy.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.data.model.UserProfile
import com.example.travel_buddy.domain.repository.AuthRepository
import com.example.travel_buddy.domain.repository.PostRepository
import kotlinx.coroutines.launch

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val posts: List<Post>) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel(
    private val repository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<SearchState>(SearchState.Idle)
    val uiState: LiveData<SearchState> = _uiState

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    fun loadUserProfile() {
        viewModelScope.launch {
            when (val result = authRepository.getCurrentProfile()) {
                is AppResult.Success -> _userProfile.value = result.data
                else -> _userProfile.value = null
            }
        }
    }

    fun removePostFromCache(postId: String) {
        if (_uiState.value is SearchState.Success) {
            val currentPosts = (_uiState.value as SearchState.Success).posts.filter { it.postId != postId }
            _uiState.value = SearchState.Success(currentPosts)
        }
    }

    fun searchPosts(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchState.Idle
            return
        }

        _uiState.value = SearchState.Loading
        viewModelScope.launch {
            val result = repository.searchPosts(query)
            _uiState.value = when (result) {
                is AppResult.Success -> SearchState.Success(result.data)
                is AppResult.Error -> SearchState.Error(result.message)
            }
        }
    }

    fun clearSearch() {
        _uiState.value = SearchState.Idle
    }
}

class SearchViewModelFactory(
    private val repository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(repository, authRepository) as T
    }
}
