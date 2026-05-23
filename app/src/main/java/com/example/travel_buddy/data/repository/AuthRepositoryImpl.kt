package com.example.travel_buddy.data.repository

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.RegisterRequest
import com.example.travel_buddy.data.model.UserProfile
import com.example.travel_buddy.data.remote.FirebaseAuthDataSource
import com.example.travel_buddy.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val dataSource: FirebaseAuthDataSource
) : AuthRepository {

    override fun isUserLoggedIn(): Boolean = dataSource.isUserLoggedIn()

    override suspend fun login(email: String, password: String): AppResult<Unit> {
        return dataSource.login(email, password)
    }

    override suspend fun register(request: RegisterRequest): AppResult<Unit> {
        return dataSource.register(request)
    }

    override suspend fun getCurrentProfile(): AppResult<UserProfile> {
        return dataSource.getCurrentProfile()
    }

    override suspend fun updateProfile(username: String, imageUri: Uri?): AppResult<Unit> {
        return dataSource.updateProfile(username, imageUri)
    }

    override suspend fun logout(): AppResult<Unit> {
        return dataSource.logout()
    }
}
