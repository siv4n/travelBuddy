package com.example.travel_buddy.data.model

import android.net.Uri

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val imageUri: Uri?
)
