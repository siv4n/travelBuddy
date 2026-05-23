package com.example.travel_buddy.data.repository

import com.example.travel_buddy.data.remote.LocationDataSource
import com.example.travel_buddy.domain.repository.LocationRepository
import com.example.travel_buddy.core.common.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepositoryImpl(
    private val dataSource: LocationDataSource
) : LocationRepository {
    override suspend fun searchLocations(query: String): AppResult<List<String>> =
        withContext(Dispatchers.IO) {
            dataSource.getLocationSuggestions(query)
        }

    override suspend fun getLocationCoordinates(locationName: String): AppResult<Pair<Double, Double>> =
        withContext(Dispatchers.IO) {
            dataSource.getLocationCoordinates(locationName)
        }
}
