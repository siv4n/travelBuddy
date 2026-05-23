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
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            
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
            if (post != null) AppResult.Success(post)
            else AppResult.Error("Post not found")
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
            AppResult.Success(posts)
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
            AppResult.Success(posts)
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
            // Sort by timestamp descending locally since Firestore requires compound indexes for ordering
            AppResult.Success(posts.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to get all posts", e)
        }
    }
}
