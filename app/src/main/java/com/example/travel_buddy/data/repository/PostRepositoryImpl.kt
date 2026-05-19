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

    override suspend fun getPostById(postId: String): AppResult<Post> = withContext(Dispatchers.IO) {
        dataSource.getPostById(postId)
    }

    override suspend fun isPostLiked(postId: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.isPostLiked(postId)
    }

    override suspend fun isPostSaved(postId: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.isPostSaved(postId)
    }

    override suspend fun toggleLike(postId: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        dataSource.toggleLike(postId)
    }

    override suspend fun toggleSave(postId: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        dataSource.toggleSave(postId)
    }
}
