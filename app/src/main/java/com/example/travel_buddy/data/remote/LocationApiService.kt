package com.example.travel_buddy.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

interface LocationApiService {
    @GET("v1/search")
    suspend fun getLocationSuggestions(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en"
    ): GeocodingResponse
}

data class GeocodingResponse(
    @SerializedName("results")
    val results: List<LocationResult>?
)

data class LocationResult(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("country")
    val country: String?,
    @SerializedName("admin1")
    val admin1: String?,
    @SerializedName("admin2")
    val admin2: String?
)
