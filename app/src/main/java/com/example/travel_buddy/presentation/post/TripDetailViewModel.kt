package com.example.travel_buddy.presentation.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.domain.repository.PostRepository
import kotlinx.coroutines.launch

sealed class TripDetailState {
    object Loading : TripDetailState()
    data class Success(
        val post: Post,
        val isLiked: Boolean,
        val isSaved: Boolean
    ) : TripDetailState()
    data class Error(val message: String) : TripDetailState()
}

class TripDetailViewModel(
    private val repository: PostRepository,
    private val postId: String
) : ViewModel() {

    private val _uiState = MutableLiveData<TripDetailState>(TripDetailState.Loading)
    val uiState: LiveData<TripDetailState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = TripDetailState.Loading
        viewModelScope.launch {
            val postResult = repository.getPostById(postId)
            val isLiked = repository.isPostLiked(postId)
            val isSaved = repository.isPostSaved(postId)

            if (postResult is AppResult.Success) {
                _uiState.value = TripDetailState.Success(postResult.data, isLiked, isSaved)
            } else if (postResult is AppResult.Error) {
                _uiState.value = TripDetailState.Error(postResult.message)
            }
        }
    }

    fun toggleLike() {
        val currentState = _uiState.value
        if (currentState is TripDetailState.Success) {
            val newIsLiked = !currentState.isLiked
            val newLikesCount = if (newIsLiked) {
                currentState.post.likesCount + 1
            } else {
                currentState.post.likesCount - 1
            }
            val updatedPost = currentState.post.copy(likesCount = newLikesCount, isLiked = newIsLiked)
            _uiState.value = currentState.copy(post = updatedPost, isLiked = newIsLiked)

            viewModelScope.launch {
                val result = repository.toggleLike(postId)
                if (result is AppResult.Error) {
                    _uiState.value = currentState
                }
            }
        }
    }

    fun toggleSave() {
        val currentState = _uiState.value
        if (currentState is TripDetailState.Success) {
            val newState = !currentState.isSaved
            _uiState.value = currentState.copy(isSaved = newState)
            
            viewModelScope.launch {
                val result = repository.toggleSave(postId)
                if (result is AppResult.Error) {
                    _uiState.value = currentState
                } else if (result is AppResult.Success) {
                    if (result.data != newState) {
                        _uiState.value = currentState.copy(isSaved = result.data)
                    }
                }
            }
        }
    }
}
