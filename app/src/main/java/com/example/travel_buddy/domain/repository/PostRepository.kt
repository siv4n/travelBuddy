package com.example.travel_buddy.domain.repository

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post

interface PostRepository {
    suspend fun createPost(post: Post, imageUri: Uri): AppResult<Unit>
}
