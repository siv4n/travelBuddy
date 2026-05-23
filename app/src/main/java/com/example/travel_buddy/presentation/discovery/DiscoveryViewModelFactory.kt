package com.example.travel_buddy.presentation.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travel_buddy.domain.repository.PostRepository

class DiscoveryViewModelFactory(
    private val repository: PostRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoveryViewModel::class.java)) {
            return DiscoveryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
