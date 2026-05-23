package com.example.travel_buddy.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travel_buddy.domain.repository.PostRepository
import com.example.travel_buddy.domain.repository.LocationRepository

class CreateTripViewModelFactory(
    private val repository: PostRepository,
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateTripViewModel::class.java)) {
            return CreateTripViewModel(repository, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
