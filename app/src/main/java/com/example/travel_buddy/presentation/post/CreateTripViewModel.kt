package com.example.travel_buddy.presentation.post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.domain.repository.PostRepository
import com.example.travel_buddy.domain.repository.LocationRepository
import kotlinx.coroutines.launch

sealed class CreateTripState {
    object Idle : CreateTripState()
    object Loading : CreateTripState()
    object Success : CreateTripState()
    data class Error(val message: String) : CreateTripState()
}

class CreateTripViewModel(
    private val repository: PostRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<CreateTripState>(CreateTripState.Idle)
    val uiState: LiveData<CreateTripState> = _uiState

    private val _locationSuggestions = MutableLiveData<List<String>>(emptyList())
    val locationSuggestions: LiveData<List<String>> = _locationSuggestions

    private val _locationLoading = MutableLiveData(false)
    val locationLoading: LiveData<Boolean> = _locationLoading

    private var selectedImageUris: List<Uri> = emptyList()

    fun setImageUris(uris: List<Uri>) {
        selectedImageUris = uris
    }

    fun getSelectedImageUris(): List<Uri> = selectedImageUris

    fun searchLocations(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _locationSuggestions.value = emptyList()
                return@launch
            }

            _locationLoading.value = true
            when (val result = locationRepository.searchLocations(query)) {
                is AppResult.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    _locationSuggestions.value = (result as AppResult.Success<List<String>>).data
                }
                is AppResult.Error -> {
                    _locationSuggestions.value = emptyList()
                }
            }
            _locationLoading.value = false
        }
    }

    fun createTrip(title: String, location: String, description: String) {
        if (title.isBlank() || location.isBlank() || description.isBlank()) {
            _uiState.value = CreateTripState.Error("Please fill all fields")
            return
        }

        val uris = selectedImageUris
        if (uris.isEmpty()) {
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
            when (val result = repository.createPost(post, uris)) {
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
