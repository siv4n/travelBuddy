package com.example.travel_buddy.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travel_buddy.di.ServiceLocator
import com.example.travel_buddy.presentation.auth.AuthViewModel
import com.example.travel_buddy.presentation.profile.ProfileViewModel

class AuthViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = ServiceLocator.authRepository
        val postRepository = ServiceLocator.postRepository

        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository, postRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
