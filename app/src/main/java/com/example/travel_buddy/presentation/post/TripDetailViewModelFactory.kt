package com.example.travel_buddy.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travel_buddy.domain.repository.PostRepository

class TripDetailViewModelFactory(
    private val repository: PostRepository,
    private val postId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripDetailViewModel::class.java)) {
            return TripDetailViewModel(repository, postId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
