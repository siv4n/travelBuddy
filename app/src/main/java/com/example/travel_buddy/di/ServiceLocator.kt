package com.example.travel_buddy.di

import com.example.travel_buddy.data.remote.FirebaseAuthDataSource
import com.example.travel_buddy.data.remote.FirebasePostDataSource
import com.example.travel_buddy.data.repository.AuthRepositoryImpl
import com.example.travel_buddy.data.repository.PostRepositoryImpl
import com.example.travel_buddy.domain.repository.AuthRepository
import com.example.travel_buddy.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object ServiceLocator {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseStorage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val authDataSource: FirebaseAuthDataSource by lazy {
        FirebaseAuthDataSource(
            auth = firebaseAuth,
            firestore = firestore,
            storage = firebaseStorage
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authDataSource)
    }

    private val postDataSource: FirebasePostDataSource by lazy {
        FirebasePostDataSource(
            firestore = firestore,
            storage = firebaseStorage,
            auth = firebaseAuth
        )
    }

    val postRepository: PostRepository by lazy {
        PostRepositoryImpl(postDataSource)
    }
}
