package com.example.travel_buddy.di

import android.content.Context
import com.example.travel_buddy.BuildConfig
import com.example.travel_buddy.data.remote.DestinationApiService
import com.example.travel_buddy.data.remote.DestinationDataSource
import com.example.travel_buddy.data.remote.FirebaseAuthDataSource
import com.example.travel_buddy.data.remote.FirebasePostDataSource
import com.example.travel_buddy.data.remote.LocationApiService
import com.example.travel_buddy.data.remote.LocationDataSource
import com.example.travel_buddy.data.repository.AuthRepositoryImpl
import com.example.travel_buddy.data.repository.DestinationRepositoryImpl
import com.example.travel_buddy.data.repository.LocationRepositoryImpl
import com.example.travel_buddy.data.repository.PostRepositoryImpl
import com.example.travel_buddy.domain.repository.AuthRepository
import com.example.travel_buddy.domain.repository.DestinationRepository
import com.example.travel_buddy.domain.repository.LocationRepository
import com.example.travel_buddy.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceLocator {
    private var appContext: Context? = null
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
        PostRepositoryImpl(dataSource = postDataSource)
    }

    // Retrofit client with OkHttp logging
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://restcountries.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private val destinationApiService: DestinationApiService by lazy {
        retrofit.create(DestinationApiService::class.java)
    }

    private val destinationDataSource: DestinationDataSource by lazy {
        DestinationDataSource(destinationApiService)
    }

    val destinationRepository: DestinationRepository by lazy {
        DestinationRepositoryImpl(dataSource = destinationDataSource)
    }

    // Location API Retrofit instance with separate base URL and API key interceptor
    private val locationOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val urlWithKey = originalRequest.url.newBuilder()
                    .addQueryParameter("key", BuildConfig.GOOGLE_PLACES_API_KEY)
                    .build()
                val requestWithKey = originalRequest.newBuilder()
                    .url(urlWithKey)
                    .build()
                chain.proceed(requestWithKey)
            }
            .build()
    }

    private val locationRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(locationOkHttpClient)
            .build()
    }

    private val locationApiService: LocationApiService by lazy {
        locationRetrofit.create(LocationApiService::class.java)
    }

    private val locationDataSource: LocationDataSource by lazy {
        LocationDataSource(locationApiService)
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepositoryImpl(dataSource = locationDataSource)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}
