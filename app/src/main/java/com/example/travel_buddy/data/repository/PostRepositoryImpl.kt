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
    override suspend fun createPost(post: Post, imageUris: List<Uri>): AppResult<Unit> {
        return withContext(Dispatchers.IO) {
            dataSource.createPost(post, imageUris)
        }
    }

    override suspend fun updatePost(postId: String, post: Post, imageUris: List<Uri>?): AppResult<Unit> {
        return withContext(Dispatchers.IO) {
            dataSource.updatePost(postId, post, imageUris)
        }
    }

    override suspend fun deletePost(postId: String): AppResult<Unit> {
        return withContext(Dispatchers.IO) {
            dataSource.deletePost(postId)
        }
    }

    override suspend fun updatePost(postId: String, post: Post, imageUri: Uri?): AppResult<Unit> {
        return withContext(Dispatchers.IO) {
            dataSource.updatePost(postId, post, imageUri)
        }
    }

    override suspend fun deletePost(postId: String): AppResult<Unit> {
        return withContext(Dispatchers.IO) {
            dataSource.deletePost(postId)
        }
    }

    override suspend fun getPostById(postId: String): AppResult<Post> = withContext(Dispatchers.IO) {
        dataSource.getPostById(postId)
    }

    override suspend fun getAllPosts(forceRefresh: Boolean): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        dataSource.getAllPosts()
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

    override suspend fun getUserPosts(userId: String): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        dataSource.getUserPosts(userId)
    }

    override suspend fun getUserSavedPosts(userId: String): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        dataSource.getUserSavedPosts(userId)
    }

    override suspend fun getUserStats(userId: String): AppResult<Triple<Int, Int, Int>> = withContext(Dispatchers.IO) {
        dataSource.getUserStats(userId)
    }

    override suspend fun searchPosts(query: String): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        dataSource.searchPosts(query)
    }

    override suspend fun syncUserPosts(userId: String, username: String, imageUrl: String?): AppResult<Unit> = withContext(Dispatchers.IO) {
        dataSource.syncUserPosts(userId, username, imageUrl)
    }
}
