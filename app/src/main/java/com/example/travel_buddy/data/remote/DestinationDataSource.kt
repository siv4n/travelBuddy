package com.example.travel_buddy.data.remote

import com.example.travel_buddy.core.common.AppResult

data class Destination(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val temperature: Double? = null,
    val humidity: Int? = null,
    val weather: String? = null
)

class DestinationDataSource(
    private val api: DestinationApiService
) {
    suspend fun getDestinations(): AppResult<List<Destination>> {
        return try {
            val response = api.getAllCountries()

            val destinations = response
                .filter { it.name?.common != null && it.flags?.png != null }
                .sortedBy { it.population }
                .takeLast(12)
                .map { country ->
                    Destination(
                        id = country.name!!.common.replace(" ", "-"),
                        name = country.name.common,
                        description = "Capital: ${country.capital?.firstOrNull() ?: "N/A"} | Region: ${country.region}",
                        imageUrl = country.flags?.png ?: "https://via.placeholder.com/400x300",
                        temperature = null,
                        humidity = null,
                        weather = "Explore"
                    )
                }

            if (destinations.isEmpty()) {
                return AppResult.Error("No destinations available")
            }

            AppResult.Success(destinations)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to fetch destinations", e)
        }
    }
}
