package com.example.travel_buddy.data.repository

import com.example.travel_buddy.core.common.AppResult
import com.example.travel_buddy.data.remote.Destination
import com.example.travel_buddy.data.remote.DestinationDataSource
import com.example.travel_buddy.domain.repository.DestinationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DestinationRepositoryImpl(
    private val dataSource: DestinationDataSource
) : DestinationRepository {

    override suspend fun getDestinations(forceRefresh: Boolean): AppResult<List<Destination>> =
        withContext(Dispatchers.IO) {
            dataSource.getDestinations()
        }

    override suspend fun getDestinationById(id: String): AppResult<Destination> =
        withContext(Dispatchers.IO) {
            AppResult.Error("Not implemented")
        }
}
