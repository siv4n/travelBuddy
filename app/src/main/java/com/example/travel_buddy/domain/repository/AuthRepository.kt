package com.example.travel_buddy.domain.repository

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.RegisterRequest
import com.example.travel_buddy.data.model.UserProfile

interface AuthRepository {
    fun isUserLoggedIn(): Boolean
    suspend fun login(email: String, password: String): AppResult<Unit>
    suspend fun register(request: RegisterRequest): AppResult<Unit>
    suspend fun getCurrentProfile(): AppResult<UserProfile>
    suspend fun updateProfile(username: String, imageUri: Uri?): AppResult<Unit>
    suspend fun logout(): AppResult<Unit>
}
