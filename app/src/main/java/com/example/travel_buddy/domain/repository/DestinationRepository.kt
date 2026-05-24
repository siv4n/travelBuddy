package com.example.travel_buddy.domain.repository

import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.remote.Destination

interface DestinationRepository {
    suspend fun getDestinations(forceRefresh: Boolean = false): AppResult<List<Destination>>
    suspend fun getDestinationById(id: String): AppResult<Destination>
}
