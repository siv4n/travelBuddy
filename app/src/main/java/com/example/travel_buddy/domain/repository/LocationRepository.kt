package com.example.travel_buddy.domain.repository

import com.example.travel_buddy.core.common.AppResult

interface LocationRepository {
    suspend fun searchLocations(query: String): AppResult<List<String>>
    suspend fun getLocationCoordinates(placeId: String): AppResult<Pair<Double, Double>>
}
