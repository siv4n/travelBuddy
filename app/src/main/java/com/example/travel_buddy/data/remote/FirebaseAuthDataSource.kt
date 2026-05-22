package com.example.travel_buddy.data.remote

import android.net.Uri
import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.model.RegisterRequest
import com.example.travel_buddy.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    suspend fun login(email: String, password: String): AppResult<Unit> {
        return runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.message ?: "Login failed", it) }
        )
    }

    suspend fun register(request: RegisterRequest): AppResult<Unit> {
        return runCatching {
            val authResult = auth.createUserWithEmailAndPassword(request.email, request.password).await()
            val user = authResult.user ?: error("User creation failed")
            val imageUrl = request.imageUri?.let { uploadProfileImage(user.uid, it) }

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(request.username)
                .apply {
                    if (imageUrl != null) {
                        setPhotoUri(Uri.parse(imageUrl))
                    }
                }
                .build()
            user.updateProfile(profileUpdate).await()

            val userDocument = hashMapOf(
                "uid" to user.uid,
                "email" to request.email,
                "username" to request.username,
                "imageUrl" to imageUrl,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(USERS_COLLECTION).document(user.uid).set(userDocument).await()
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.message ?: "Registration failed", it) }
        )
    }

    suspend fun getCurrentProfile(): AppResult<UserProfile> {
        return runCatching {
            val currentUser = auth.currentUser ?: error("No user session found")
            val snapshot = firestore.collection(USERS_COLLECTION).document(currentUser.uid).get().await()

            if (!snapshot.exists()) {
                val fallbackProfile = UserProfile(
                    uid = currentUser.uid,
                    email = currentUser.email.orEmpty(),
                    username = currentUser.displayName ?: currentUser.email?.substringBefore("@").orEmpty(),
                    imageUrl = currentUser.photoUrl?.toString()
                )
                saveProfileDocument(fallbackProfile)
                fallbackProfile
            } else {
                UserProfile(
                    uid = currentUser.uid,
                    email = snapshot.getString("email") ?: currentUser.email.orEmpty(),
                    username = snapshot.getString("username")
                        ?: currentUser.displayName
                        ?: currentUser.email?.substringBefore("@").orEmpty(),
                    imageUrl = snapshot.getString("imageUrl") ?: currentUser.photoUrl?.toString()
                )
            }
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(it.message ?: "Failed to load profile", it) }
        )
    }

    suspend fun updateProfile(username: String, imageUri: Uri?): AppResult<Unit> {
        return runCatching {
            val currentUser = auth.currentUser ?: error("No user session found")
            val existingDoc = firestore.collection(USERS_COLLECTION).document(currentUser.uid).get().await()
            val currentImageUrl = existingDoc.getString("imageUrl") ?: currentUser.photoUrl?.toString()
            val updatedImageUrl = imageUri?.let { uploadProfileImage(currentUser.uid, it) } ?: currentImageUrl

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .apply {
                    if (updatedImageUrl != null) {
                        setPhotoUri(Uri.parse(updatedImageUrl))
                    }
                }
                .build()
            currentUser.updateProfile(profileUpdate).await()

            val updates = hashMapOf<String, Any?>(
                "username" to username,
                "imageUrl" to updatedImageUrl,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .set(updates, SetOptions.merge())
                .await()
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.message ?: "Failed to update profile", it) }
        )
    }

    suspend fun logout(): AppResult<Unit> {
        return runCatching {
            auth.signOut()
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.message ?: "Failed to logout", it) }
        )
    }

    private suspend fun uploadProfileImage(uid: String, imageUri: Uri): String {
        val imageRef = storage.reference
            .child(PROFILE_IMAGES_FOLDER)
            .child(uid)
            .child("${UUID.randomUUID()}.jpg")

        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    private suspend fun saveProfileDocument(profile: UserProfile) {
        val document = hashMapOf(
            "uid" to profile.uid,
            "email" to profile.email,
            "username" to profile.username,
            "imageUrl" to profile.imageUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection(USERS_COLLECTION).document(profile.uid).set(document).await()
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PROFILE_IMAGES_FOLDER = "profile_images"
    }
}
