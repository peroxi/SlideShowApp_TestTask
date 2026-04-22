package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.repository.PlaylistRepository
import timber.log.Timber
import javax.inject.Inject

open class FetchPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : BaseUseCase() {

    override suspend fun execute(): List<PlaylistItem> {
        val playlistResponse = playlistRepository.fetchPlaylistItems()
        val items = playlistResponse.playlists
            ?.flatMap { it.playlistItems.orEmpty() }
            .orEmpty()

        Timber.d("Fetched ${items.size} playlist items")
        return items
    }
}
