package com.example.travel_buddy.data.remote

import com.example.travel_buddy.core.common.AppResult

class LocationDataSource(
    private val api: LocationApiService
) {

    suspend fun getLocationSuggestions(query: String): AppResult<List<String>> {
        return try {
            if (query.isBlank()) {
                return AppResult.Success(emptyList())
            }
            val response = api.getLocationSuggestions(query)
            if (!response.results.isNullOrEmpty()) {
                val suggestions = response.results.map { location ->
                    buildLocationString(location)
                }
                AppResult.Success(suggestions)
            } else {
                AppResult.Success(emptyList())
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to fetch location suggestions", e)
        }
    }

    private fun buildLocationString(location: LocationResult): String {
        return buildString {
            append(location.name)
            if (!location.admin1.isNullOrEmpty() && location.admin1 != location.name) {
                append(", ${location.admin1}")
            }
            if (!location.country.isNullOrEmpty()) {
                append(", ${location.country}")
            }
        }
    }
}
