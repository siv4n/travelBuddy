package com.example.travel_buddy.data.repository

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.local.dao.PostDao
import com.example.travel_buddy.data.local.entity.PostEntity
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.data.remote.FirebasePostDataSource
import com.example.travel_buddy.domain.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepositoryImpl(
    private val dataSource: FirebasePostDataSource,
    private val postDao: PostDao
) : PostRepository {

    override suspend fun createPost(post: Post, imageUris: List<Uri>): AppResult<Unit> = withContext(Dispatchers.IO) {
        val result = dataSource.createPost(post, imageUris)
        if (result is AppResult.Success) {
            refreshLocalCache()
        }
        result
    }

    override suspend fun updatePost(postId: String, post: Post, imageUris: List<Uri>?): AppResult<Unit> = withContext(Dispatchers.IO) {
        val result = dataSource.updatePost(postId, post, imageUris)
        if (result is AppResult.Success) {
            refreshLocalCache()
        }
        result
    }

    override suspend fun deletePost(postId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        val result = dataSource.deletePost(postId)
        if (result is AppResult.Success) {
            postDao.deletePost(postId)
        }
        result
    }

    override suspend fun getPostById(postId: String): AppResult<Post> = withContext(Dispatchers.IO) {
        val cached = postDao.getPostById(postId)
        if (cached != null) {
            AppResult.Success(cached.toPost())
        } else {
            val remoteResult = dataSource.getPostById(postId)
            if (remoteResult is AppResult.Success) {
                postDao.insertPost(PostEntity.fromPost(remoteResult.data))
            }
            remoteResult
        }
    }

    override suspend fun getAllPosts(forceRefresh: Boolean): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = postDao.getAllPosts()
            if (cached.isNotEmpty()) {
                return@withContext AppResult.Success(cached.map { it.toPost() })
            }
        }
        val remoteResult = dataSource.getAllPosts()
        if (remoteResult is AppResult.Success) {
            postDao.clearAllPosts()
            postDao.insertPosts(remoteResult.data.map { PostEntity.fromPost(it) })
        }
        remoteResult
    }

    override suspend fun isPostLiked(postId: String): Boolean = withContext(Dispatchers.IO) {
        val cached = postDao.getPostById(postId)
        if (cached != null) {
            cached.isLiked
        } else {
            dataSource.isPostLiked(postId)
        }
    }

    override suspend fun isPostSaved(postId: String): Boolean = withContext(Dispatchers.IO) {
        val cached = postDao.getPostById(postId)
        if (cached != null) {
            cached.isSaved
        } else {
            dataSource.isPostSaved(postId)
        }
    }

    override suspend fun toggleLike(postId: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        val result = dataSource.toggleLike(postId)
        if (result is AppResult.Success) {
            val isLiked = result.data
            val cached = postDao.getPostById(postId)
            if (cached != null) {
                val updatedLikes = if (isLiked) cached.likesCount + 1 else maxOf(0, cached.likesCount - 1)
                postDao.insertPost(cached.copy(isLiked = isLiked, likesCount = updatedLikes))
            }
        }
        result
    }

    override suspend fun toggleSave(postId: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        val result = dataSource.toggleSave(postId)
        if (result is AppResult.Success) {
            val isSaved = result.data
            val cached = postDao.getPostById(postId)
            if (cached != null) {
                postDao.insertPost(cached.copy(isSaved = isSaved))
            }
        }
        result
    }

    override suspend fun getUserPosts(userId: String): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        val cached = postDao.getUserPosts(userId)
        if (cached.isNotEmpty()) {
            AppResult.Success(cached.map { it.toPost() })
        } else {
            val remoteResult = dataSource.getUserPosts(userId)
            if (remoteResult is AppResult.Success) {
                postDao.insertPosts(remoteResult.data.map { PostEntity.fromPost(it) })
            }
            remoteResult
        }
    }

    override suspend fun getUserSavedPosts(userId: String): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        val remoteResult = dataSource.getUserSavedPosts(userId)
        if (remoteResult is AppResult.Success) {
            postDao.insertPosts(remoteResult.data.map { PostEntity.fromPost(it) })
        }
        remoteResult
    }

    override suspend fun getUserStats(userId: String): AppResult<Triple<Int, Int, Int>> = withContext(Dispatchers.IO) {
        dataSource.getUserStats(userId)
    }

    override suspend fun searchPosts(query: String): AppResult<List<Post>> = withContext(Dispatchers.IO) {
        val remoteResult = dataSource.searchPosts(query)
        if (remoteResult is AppResult.Success) {
            postDao.insertPosts(remoteResult.data.map { PostEntity.fromPost(it) })
        }
        remoteResult
    }

    override suspend fun syncUserPosts(userId: String, username: String, imageUrl: String?): AppResult<Unit> = withContext(Dispatchers.IO) {
        val result = dataSource.syncUserPosts(userId, username, imageUrl)
        if (result is AppResult.Success) {
            refreshLocalCache()
        }
        result
    }

    private suspend fun refreshLocalCache() {
        val remoteResult = dataSource.getAllPosts()
        if (remoteResult is AppResult.Success) {
            postDao.clearAllPosts()
            postDao.insertPosts(remoteResult.data.map { PostEntity.fromPost(it) })
        }
    }
}
