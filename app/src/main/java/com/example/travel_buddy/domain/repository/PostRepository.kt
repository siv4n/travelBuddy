package com.example.travel_buddy.domain.repository

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post

interface PostRepository {
    suspend fun createPost(post: Post, imageUris: List<Uri>): AppResult<Unit>
    suspend fun updatePost(postId: String, post: Post, imageUris: List<Uri>?): AppResult<Unit>
    suspend fun deletePost(postId: String): AppResult<Unit>
    suspend fun getPostById(postId: String): AppResult<Post>
    suspend fun getAllPosts(forceRefresh: Boolean = false): AppResult<List<Post>>
    suspend fun isPostLiked(postId: String): Boolean
    suspend fun isPostSaved(postId: String): Boolean
    suspend fun toggleLike(postId: String): AppResult<Boolean>
    suspend fun toggleSave(postId: String): AppResult<Boolean>
    suspend fun getUserPosts(userId: String): AppResult<List<Post>>
    suspend fun getUserSavedPosts(userId: String): AppResult<List<Post>>
    suspend fun getUserStats(userId: String): AppResult<Triple<Int, Int, Int>>
    suspend fun searchPosts(query: String): AppResult<List<Post>>
    suspend fun syncUserPosts(userId: String, username: String, imageUrl: String?): AppResult<Unit>
}

