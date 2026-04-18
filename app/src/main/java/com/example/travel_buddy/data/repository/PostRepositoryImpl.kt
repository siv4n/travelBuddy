package com.example.travel_buddy.data.repository

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.data.remote.FirebasePostDataSource
import com.example.travel_buddy.domain.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepositoryImpl(
    private val dataSource: FirebasePostDataSource
) : PostRepository {
    override suspend fun createPost(post: Post, imageUri: Uri): AppResult<Unit> {
        return withContext(Dispatchers.IO) {
            dataSource.createPost(post, imageUri)
        }
    }
}
