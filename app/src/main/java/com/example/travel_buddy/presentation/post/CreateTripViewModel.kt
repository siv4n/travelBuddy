package com.example.travel_buddy.presentation.post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.domain.repository.PostRepository
import kotlinx.coroutines.launch

sealed class CreateTripState {
    object Idle : CreateTripState()
    object Loading : CreateTripState()
    object Success : CreateTripState()
    data class Error(val message: String) : CreateTripState()
}

class CreateTripViewModel(
    private val repository: PostRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<CreateTripState>(CreateTripState.Idle)
    val uiState: LiveData<CreateTripState> = _uiState

    private var selectedImageUri: Uri? = null

    fun setImageUri(uri: Uri) {
        selectedImageUri = uri
    }

    fun getSelectedImageUri(): Uri? = selectedImageUri

    fun createTrip(title: String, location: String, description: String) {
        if (title.isBlank() || location.isBlank() || description.isBlank()) {
            _uiState.value = CreateTripState.Error("Please fill all fields")
            return
        }

        val uri = selectedImageUri
        if (uri == null) {
            _uiState.value = CreateTripState.Error("Please select a photo")
            return
        }

        val post = Post(
            title = title,
            location = location,
            description = description
        )

        _uiState.value = CreateTripState.Loading
        viewModelScope.launch {
            when (val result = repository.createPost(post, uri)) {
                is AppResult.Success -> {
                    _uiState.value = CreateTripState.Success
                }
                is AppResult.Error -> {
                    _uiState.value = CreateTripState.Error(result.message)
                }
            }
        }
    }
}
