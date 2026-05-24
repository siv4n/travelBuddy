package com.example.travel_buddy.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : AppResult<Nothing>
}
