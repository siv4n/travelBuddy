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
}
