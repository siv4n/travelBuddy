package com.example.travel_buddy.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName

interface DestinationApiService {
    @GET("v3.1/all")
    suspend fun getAllCountries(): List<CountryResponse>
}

data class CountryResponse(
    @SerializedName("name")
    val name: CountryName,
    @SerializedName("capital")
    val capital: List<String>?,
    @SerializedName("region")
    val region: String,
    @SerializedName("flags")
    val flags: FlagData?,
    @SerializedName("population")
    val population: Long?,
    @SerializedName("area")
    val area: Double?
)

data class CountryName(
    @SerializedName("common")
    val common: String,
    @SerializedName("official")
    val official: String
)

data class FlagData(
    @SerializedName("png")
    val png: String?,
    @SerializedName("svg")
    val svg: String?
)
