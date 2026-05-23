package com.example.travel_buddy.data.remote

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebasePostDataSource(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    suspend fun createPost(post: Post, imageUri: Uri): AppResult<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return AppResult.Error("User not logged in")
            val postId = UUID.randomUUID().toString()
            
            // Upload image to Firebase Storage
            val imageRef = storage.reference.child("post_images/$postId.jpg")
            try {
                imageRef.putFile(imageUri).await()
            } catch (e: Exception) {
                return AppResult.Error("Image upload failed: ${e.message}", e)
            }
            val downloadUrl = try {
                imageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                return AppResult.Error("Failed to obtain uploaded image URL: ${e.message}", e)
            }
            
            // Create post document in Firestore
            // Assumes Post model has copy() method (is a data class)
            val finalPost = post.copy(
                postId = postId,
                authorId = uid,
                imageUrl = downloadUrl,
                timestamp = System.currentTimeMillis()
            )
            
            firestore.collection("posts").document(postId).set(finalPost).await()
            
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to create post", e)
        }
    }

    suspend fun getPostById(postId: String): AppResult<Post> {
        return try {
            val doc = firestore.collection("posts").document(postId).get().await()
            val post = doc.toObject(Post::class.java)
            if (post != null) {
                val enriched = try {
                    val userDoc = firestore.collection("users").document(post.authorId).get().await()
                    val authorUsername = userDoc.getString("username") ?: post.authorId
                    val authorImage = userDoc.getString("imageUrl")

                    val likesCount = firestore.collection("likes").whereEqualTo("postId", postId).get().await().size()

                    val uid = auth.currentUser?.uid
                    val isLiked = uid?.let { firestore.collection("likes").document("${it}_${postId}").get().await().exists() } ?: false
                    val isSaved = uid?.let { firestore.collection("saves").document("${it}_${postId}").get().await().exists() } ?: false

                    post.copy(
                        likesCount = likesCount,
                        authorUsername = authorUsername,
                        authorImageUrl = authorImage,
                        isLiked = isLiked,
                        isSaved = isSaved
                    )
                } catch (e: Exception) {
                    post
                }
                AppResult.Success(enriched)
            } else AppResult.Error("Post not found")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to get post", e)
        }
    }

    suspend fun isPostLiked(postId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val docId = "${uid}_${postId}"
        val doc = firestore.collection("likes").document(docId).get().await()
        return doc.exists()
    }

    suspend fun isPostSaved(postId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val docId = "${uid}_${postId}"
        val doc = firestore.collection("saves").document(docId).get().await()
        return doc.exists()
    }

    suspend fun toggleLike(postId: String): AppResult<Boolean> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error("User not logged in")
        val docId = "${uid}_${postId}"
        val ref = firestore.collection("likes").document(docId)
        return try {
            val doc = ref.get().await()
            if (doc.exists()) {
                ref.delete().await()
                AppResult.Success(false)
            } else {
                ref.set(mapOf("userId" to uid, "postId" to postId)).await()
                AppResult.Success(true)
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to toggle like", e)
        }
    }

    suspend fun toggleSave(postId: String): AppResult<Boolean> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error("User not logged in")
        val docId = "${uid}_${postId}"
        val ref = firestore.collection("saves").document(docId)
        return try {
            val doc = ref.get().await()
            if (doc.exists()) {
                ref.delete().await()
                AppResult.Success(false)
            } else {
                ref.set(mapOf("userId" to uid, "postId" to postId)).await()
                AppResult.Success(true)
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to toggle save", e)
        }
    }

    suspend fun getUserPosts(userId: String): AppResult<List<Post>> {
        return try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("authorId", userId)
                .get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }

            val enriched = posts.map { post ->
                try {
                    val userDoc = firestore.collection("users").document(post.authorId).get().await()
                    val authorUsername = userDoc.getString("username") ?: post.authorId
                    val authorImage = userDoc.getString("imageUrl")

                    val likesCount = firestore.collection("likes").whereEqualTo("postId", post.postId).get().await().size()

                    val uid = auth.currentUser?.uid
                    val isLiked = uid?.let { firestore.collection("likes").document("${it}_${post.postId}").get().await().exists() } ?: false
                    val isSaved = uid?.let { firestore.collection("saves").document("${it}_${post.postId}").get().await().exists() } ?: false

                    post.copy(
                        likesCount = likesCount,
                        authorUsername = authorUsername,
                        authorImageUrl = authorImage,
                        isLiked = isLiked,
                        isSaved = isSaved
                    )
                } catch (e: Exception) {
                    post
                }
            }
            AppResult.Success(enriched.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to get user posts", e)
        }
    }

    suspend fun getUserSavedPosts(userId: String): AppResult<List<Post>> {
        return try {
            val savesSnapshot = firestore.collection("saves")
                .whereEqualTo("userId", userId)
                .get().await()

            val postIds = savesSnapshot.documents.mapNotNull { it.getString("postId") }
            if (postIds.isEmpty()) return AppResult.Success(emptyList())

            val posts = postIds.mapNotNull { id ->
                firestore.collection("posts").document(id).get().await().toObject(Post::class.java)
            }

            val enriched = posts.map { post ->
                try {
                    val userDoc = firestore.collection("users").document(post.authorId).get().await()
                    val authorUsername = userDoc.getString("username") ?: post.authorId
                    val authorImage = userDoc.getString("imageUrl")

                    val likesCount = firestore.collection("likes").whereEqualTo("postId", post.postId).get().await().size()

                    val uid = auth.currentUser?.uid
                    val isLiked = uid?.let { firestore.collection("likes").document("${it}_${post.postId}").get().await().exists() } ?: false
                    val isSaved = uid?.let { firestore.collection("saves").document("${it}_${post.postId}").get().await().exists() } ?: false

                    post.copy(
                        likesCount = likesCount,
                        authorUsername = authorUsername,
                        authorImageUrl = authorImage,
                        isLiked = isLiked,
                        isSaved = isSaved
                    )
                } catch (e: Exception) {
                    post
                }
            }
            AppResult.Success(enriched.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to get saved posts", e)
        }
    }

    suspend fun getUserStats(userId: String): AppResult<Triple<Int, Int, Int>> {
        return try {
            val postsCount = firestore.collection("posts")
                .whereEqualTo("authorId", userId).get().await().size()
            val likesCount = firestore.collection("likes")
                .whereEqualTo("userId", userId).get().await().size()
            val savesCount = firestore.collection("saves")
                .whereEqualTo("userId", userId).get().await().size()

            AppResult.Success(Triple(postsCount, likesCount, savesCount))
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to get user stats", e)
        }
    }

    suspend fun getAllPosts(): AppResult<List<Post>> {
        return try {
            val snapshot = firestore.collection("posts")
                .get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }

            // Enrich posts with author profile and likes/save state
            val enriched = posts.map { post ->
                try {
                    // fetch author profile document
                    val userDoc = firestore.collection("users").document(post.authorId).get().await()
                    val authorUsername = userDoc.getString("username") ?: post.authorId
                    val authorImage = userDoc.getString("imageUrl")

                    // count likes for the post
                    val likesCount = firestore.collection("likes").whereEqualTo("postId", post.postId).get().await().size()

                    // check if current user liked/saved
                    val uid = auth.currentUser?.uid
                    val isLiked = uid?.let { firestore.collection("likes").document("${it}_${post.postId}").get().await().exists() } ?: false
                    val isSaved = uid?.let { firestore.collection("saves").document("${it}_${post.postId}").get().await().exists() } ?: false

                    post.copy(
                        likesCount = likesCount,
                        authorUsername = authorUsername,
                        authorImageUrl = authorImage,
                        isLiked = isLiked,
                        isSaved = isSaved
                    )
                } catch (e: Exception) {
                    post
                }
            }

            AppResult.Success(enriched.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to get all posts", e)
        }
    }

    suspend fun updatePost(postId: String, post: Post, imageUri: Uri?): AppResult<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return AppResult.Error("User not logged in")

            // Check if user is the author
            val existingDoc = firestore.collection("posts").document(postId).get().await()
            val existingPost = existingDoc.toObject(Post::class.java)
            if (existingPost?.authorId != uid) {
                return AppResult.Error("You can only edit your own posts")
            }

            // Handle image update if provided
            val imageUrl = if (imageUri != null) {
                val imageRef = storage.reference.child("post_images/$postId.jpg")
                imageRef.putFile(imageUri).await()
                imageRef.downloadUrl.await().toString()
            } else {
                existingPost.imageUrl
            }

            val updatedPost = post.copy(
                postId = postId,
                authorId = uid,
                imageUrl = imageUrl,
                timestamp = existingPost.timestamp
            )

            firestore.collection("posts").document(postId).set(updatedPost).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to update post", e)
        }
    }

    suspend fun deletePost(postId: String): AppResult<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return AppResult.Error("User not logged in")

            // Check if user is the author
            val doc = firestore.collection("posts").document(postId).get().await()
            val post = doc.toObject(Post::class.java)
            if (post?.authorId != uid) {
                return AppResult.Error("You can only delete your own posts")
            }

            // Delete the post image from storage
            try {
                val imageRef = storage.reference.child("post_images/$postId.jpg")
                imageRef.delete().await()
            } catch (e: Exception) {
                // Image might not exist, continue with deletion
            }

            // Delete the post document
            firestore.collection("posts").document(postId).delete().await()

            // Clean up likes and saves
            val likesSnapshot = firestore.collection("likes")
                .whereEqualTo("postId", postId).get().await()
            likesSnapshot.documents.forEach {
                firestore.collection("likes").document(it.id).delete().await()
            }

            val savesSnapshot = firestore.collection("saves")
                .whereEqualTo("postId", postId).get().await()
            savesSnapshot.documents.forEach {
                firestore.collection("saves").document(it.id).delete().await()
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to delete post", e)
        }
    }

    suspend fun searchPosts(query: String): AppResult<List<Post>> {
        return try {
            if (query.isBlank()) {
                return AppResult.Success(emptyList())
            }
            
            val lowerQuery = query.lowercase()
            val snapshot = firestore.collection("posts").get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
            
            // Filter posts by title, location, or description containing the query
            val filtered = posts.filter { post ->
                post.title.lowercase().contains(lowerQuery) ||
                post.location.lowercase().contains(lowerQuery) ||
                post.description.lowercase().contains(lowerQuery)
            }
            
            // Enrich filtered posts with author profile and likes/save state (same as getAllPosts)
            val enriched = filtered.map { post ->
                try {
                    val userDoc = firestore.collection("users").document(post.authorId).get().await()
                    val authorUsername = userDoc.getString("username") ?: post.authorId
                    val authorImage = userDoc.getString("imageUrl")
                    
                    val likesCount = firestore.collection("likes").whereEqualTo("postId", post.postId).get().await().size()
                    
                    val uid = auth.currentUser?.uid
                    val isLiked = uid?.let { firestore.collection("likes").document("${it}_${post.postId}").get().await().exists() } ?: false
                    val isSaved = uid?.let { firestore.collection("saves").document("${it}_${post.postId}").get().await().exists() } ?: false
                    
                    post.copy(
                        likesCount = likesCount,
                        authorUsername = authorUsername,
                        authorImageUrl = authorImage,
                        isLiked = isLiked,
                        isSaved = isSaved
                    )
                } catch (e: Exception) {
                    post
                }
            }
            
            AppResult.Success(enriched.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to search posts", e)
        }
    }
}
