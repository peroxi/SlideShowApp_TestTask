package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.usecase.FetchPlaylistFetchUseCase
import com.my.slideshowapp.model.repository.PlaylistRepository
import com.my.slideshowapp.model.entity.PlaylistItemsResponse

internal class FakeFetchPlaylistFetchUseCase(
    private val items: List<PlaylistItem> = emptyList(),
    private val throwOn: Throwable? = null
) : FetchPlaylistFetchUseCase(object : PlaylistRepository {
    override suspend fun fetchPlaylistItems(): PlaylistItemsResponse = PlaylistItemsResponse()
}) {
    override suspend fun execute(): List<PlaylistItem> {
        throwOn?.let { throw it }
        return items
    }
}


