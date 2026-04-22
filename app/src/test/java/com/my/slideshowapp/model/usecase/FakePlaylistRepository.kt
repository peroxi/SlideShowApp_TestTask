package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.model.entity.PlaylistItemsResponse
import com.my.slideshowapp.model.repository.PlaylistRepository

internal class FakePlaylistRepository(
    private val response: PlaylistItemsResponse = PlaylistItemsResponse(),
    private val throwOn: Throwable? = null
) : PlaylistRepository {

    var callCount = 0

    override suspend fun fetchPlaylistItems(): PlaylistItemsResponse {
        callCount++
        throwOn?.let { throw it }
        return response
    }
}

