package com.example.travel_buddy.presentation.post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.domain.repository.PostRepository
import com.example.travel_buddy.domain.repository.LocationRepository
import kotlinx.coroutines.launch

sealed class EditPostState {
    object Idle : EditPostState()
    object Loading : EditPostState()
    object Success : EditPostState()
    data class Error(val message: String) : EditPostState()
}

class EditPostViewModel(
    private val repository: PostRepository,
    private val locationRepository: LocationRepository,
    private val postId: String
) : ViewModel() {

    private val _uiState = MutableLiveData<EditPostState>(EditPostState.Idle)
    val uiState: LiveData<EditPostState> = _uiState

    private val _postData = MutableLiveData<Post?>()
    val postData: LiveData<Post?> = _postData

    private val _locationSuggestions = MutableLiveData<List<String>>(emptyList())
    val locationSuggestions: LiveData<List<String>> = _locationSuggestions

    private var selectedImageUris: List<Uri>? = null
    private var originalPost: Post? = null

    init {
        loadPost()
    }

    private fun loadPost() {
        viewModelScope.launch {
            when (val result = repository.getPostById(postId)) {
                is AppResult.Success -> {
                    originalPost = result.data
                    _postData.value = result.data
                }
                is AppResult.Error -> {
                    _uiState.value = EditPostState.Error("Failed to load post")
                }
            }
        }
    }

    fun setImageUris(uris: List<Uri>) {
        selectedImageUris = uris
    }

    fun getSelectedImageUris(): List<Uri>? = selectedImageUris

    fun searchLocations(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _locationSuggestions.value = emptyList()
                return@launch
            }

            when (val result = locationRepository.searchLocations(query)) {
                is AppResult.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    _locationSuggestions.value = (result as AppResult.Success<List<String>>).data
                }
                is AppResult.Error -> {
                    _locationSuggestions.value = emptyList()
                }
            }
        }
    }

    fun updatePost(title: String, location: String, description: String) {
        if (title.isBlank() || location.isBlank() || description.isBlank()) {
            _uiState.value = EditPostState.Error("Please fill all fields")
            return
        }

        val post = Post(
            postId = postId,
            title = title,
            location = location,
            description = description
        )

        _uiState.value = EditPostState.Loading
        viewModelScope.launch {
            when (val result = repository.updatePost(postId, post, selectedImageUris)) {
                is AppResult.Success -> {
                    _uiState.value = EditPostState.Success
                }
                is AppResult.Error -> {
                    _uiState.value = EditPostState.Error(result.message)
                }
            }
        }
    }

    fun deletePost() {
        _uiState.value = EditPostState.Loading
        viewModelScope.launch {
            when (val result = repository.deletePost(postId)) {
                is AppResult.Success -> {
                    // Mark as deleted (not just Success) so fragment can handle it differently
                    _uiState.value = EditPostState.Success
                }
                is AppResult.Error -> {
                    _uiState.value = EditPostState.Error(result.message)
                }
            }
        }
    }

    fun isDeleteInProgress(): Boolean {
        return uiState.value is EditPostState.Loading
    }
}

class EditPostViewModelFactory(
    private val repository: PostRepository,
    private val locationRepository: LocationRepository,
    private val postId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditPostViewModel(repository, locationRepository, postId) as T
    }
}
