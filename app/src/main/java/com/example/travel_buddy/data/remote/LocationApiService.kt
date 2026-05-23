package com.example.travel_buddy.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

interface LocationApiService {
    @GET("place/autocomplete/json")
    suspend fun getLocationSuggestions(
        @Query("input") input: String,
        @Query("sessionToken") sessionToken: String,
        @Query("components") components: String = "country:*"
    ): PlacesAutocompleteResponse

    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("sessionToken") sessionToken: String,
        @Query("fields") fields: String = "formatted_address,geometry"
    ): PlaceDetailsResponse
}

data class PlacesAutocompleteResponse(
    @SerializedName("predictions")
    val predictions: List<PlacePrediction>,
    @SerializedName("status")
    val status: String
)

data class PlacePrediction(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("main_text")
    val mainText: String,
    @SerializedName("secondary_text")
    val secondaryText: String?
)

data class PlaceDetailsResponse(
    @SerializedName("result")
    val result: PlaceResult?,
    @SerializedName("status")
    val status: String
)

data class PlaceResult(
    @SerializedName("formatted_address")
    val formattedAddress: String,
    @SerializedName("geometry")
    val geometry: PlaceGeometry?
)

data class PlaceGeometry(
    @SerializedName("location")
    val location: PlaceLocation
)

data class PlaceLocation(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)
