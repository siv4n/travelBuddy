package com.example.travel_buddy.presentation.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.remote.Destination
import com.example.travel_buddy.domain.repository.DestinationRepository
import com.example.travel_buddy.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DestinationsViewModel(
    private val repository: DestinationRepository
) : ViewModel() {

    private val _destinationsState = MutableStateFlow<UiState<List<Destination>>>(UiState.Idle)
    val destinationsState: StateFlow<UiState<List<Destination>>> = _destinationsState.asStateFlow()

    fun loadDestinations(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _destinationsState.value = UiState.Loading
            _destinationsState.value = when (val result = repository.getDestinations(forceRefresh)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Error -> UiState.Error(result.message)
            }
        }
    }
}

class DestinationsViewModelFactory(
    private val repository: DestinationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DestinationsViewModel(repository) as T
    }
}
