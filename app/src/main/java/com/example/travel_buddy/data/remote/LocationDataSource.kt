package com.example.travel_buddy.data.remote

import com.example.travel_buddy.core.common.AppResult
import java.util.UUID

class LocationDataSource(
    private val api: LocationApiService
) {
    private val sessionToken = UUID.randomUUID().toString()

    suspend fun getLocationSuggestions(query: String): AppResult<List<String>> {
        return try {
            if (query.isBlank()) {
                return AppResult.Success(emptyList())
            }
            val response = api.getLocationSuggestions(query, sessionToken)
            if (response.status == "OK") {
                val suggestions = response.predictions.map { it.description }
                AppResult.Success(suggestions)
            } else {
                AppResult.Error("Failed to fetch location suggestions: ${response.status}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to fetch location suggestions", e)
        }
    }

    suspend fun getPlaceDetails(placeId: String): AppResult<Pair<Double, Double>> {
        return try {
            val response = api.getPlaceDetails(placeId, sessionToken)
            if (response.status == "OK" && response.result?.geometry?.location != null) {
                val location = response.result.geometry.location
                AppResult.Success(Pair(location.lat, location.lng))
            } else {
                AppResult.Error("Failed to fetch place details: ${response.status}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to fetch place details", e)
        }
    }
}
