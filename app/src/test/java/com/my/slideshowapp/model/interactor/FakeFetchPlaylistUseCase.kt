package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.usecase.FetchPlaylistUseCase
import com.my.slideshowapp.model.repository.PlaylistRepository
import com.my.slideshowapp.model.entity.PlaylistItemsResponse

internal class FakeFetchPlaylistUseCase(
    private val items: List<PlaylistItem> = emptyList(),
    private val throwOn: Throwable? = null
) : FetchPlaylistUseCase(object : PlaylistRepository {
    override suspend fun fetchPlaylistItems(): PlaylistItemsResponse = PlaylistItemsResponse()
}) {
    override suspend fun execute(): List<PlaylistItem> {
        throwOn?.let { throw it }
        return items
    }
}


